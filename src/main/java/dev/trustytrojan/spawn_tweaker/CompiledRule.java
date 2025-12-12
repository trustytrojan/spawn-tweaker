package dev.trustytrojan.spawn_tweaker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

import dev.trustytrojan.spawn_tweaker.data.ConditionsRaw;
import dev.trustytrojan.spawn_tweaker.data.CountRaw;
import dev.trustytrojan.spawn_tweaker.data.RangeRaw;
import dev.trustytrojan.spawn_tweaker.data.SpawnRuleRaw;
import net.minecraft.entity.Entity;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.fml.common.eventhandler.Event;

public class CompiledRule<E extends EntityEvent>
{
	private final boolean onJoin;
	private final List<Predicate<EntityEventWrapper<E>>> selectorChecks = new ArrayList<>();
	private final List<Predicate<EntityEventWrapper<E>>> conditionChecks = new ArrayList<>();
	private final Event.Result thenResult;
	private final Event.Result elseResult;

	public CompiledRule(final SpawnRuleRaw raw)
	{
		onJoin = (raw.on != null) && raw.on.equals("join");
		this.thenResult = parseResult(raw.then);
		this.elseResult = parseResult(raw.elseAction);

		// Compile blocks into separate lists
		if (raw.forParams != null)
			bake(raw.forParams, selectorChecks);
		if (raw.ifParams != null)
			bake(raw.ifParams, conditionChecks);
	}

	public boolean isOnJoin()
	{
		return onJoin;
	}

	private Event.Result parseResult(final String action)
	{
		if ("allow".equalsIgnoreCase(action))
			return Event.Result.ALLOW;
		if ("deny".equalsIgnoreCase(action))
			return Event.Result.DENY;
		return Event.Result.DEFAULT; // Pass logic to next rule or vanilla
	}

	private void bake(final ConditionsRaw c, final List<Predicate<EntityEventWrapper<E>>> checks)
	{
		// Mods = namespace part of resource location string
		// Mobs = path part of resource location string
		// Prefer plural over singular checks, prefer mod over mob checks
		if (c.mods != null)
			checks.add(ctx -> c.mods.contains(ctx.getEntityRl().getNamespace()));
		else if (c.mod != null)
			checks.add(ctx -> c.mod.equals(ctx.getEntityRl().getNamespace()));
		else if (c.mobs instanceof Map)
		{
			@SuppressWarnings("unchecked")
			final var modToMobs = (Map<String, List<String>>) c.mobs;
			checks.add(ctx ->
			{
				final var rl = ctx.getEntityRl();
				final var mobs = modToMobs.get(rl.getNamespace());
				return mobs.contains(rl.getPath());
			});
		}
		else if (c.mobs instanceof List)
		{
			@SuppressWarnings("unchecked")
			final var mobs = (List<String>) c.mobs;
			checks.add(ctx -> mobs.contains(ctx.getEntityRl().toString()));
		}
		else if (c.mob != null)
			checks.add(ctx -> c.mob.equals(ctx.getEntityRl().getPath()));

		if (c.dimension != null)
			checks.add(ctx -> ctx.getDimension() == c.dimension);

		if (c.light != null)
			checks.add(ctx -> checkRange(c.light, ctx.getLightLevel()));

		if (c.height != null)
			checks.add(ctx -> checkRange(c.height, ctx.getHeight()));

		if (c.health != null)
			checks.add(ctx -> checkRange(c.health, (int) ctx.getEntity().getMaxHealth()));

		if (c.random != null)
		{
			final var rng = new Random();
			checks.add(ctx -> rng.nextFloat() < c.random);
		}

		if (c.count != null)
			checks.add(ctx -> checkCount(c.count, ctx));
	}

	private boolean checkCount(final CountRaw count, final EntityEventWrapper<E> ctx)
	{
		final Collection<Entity> entities;

		if ("chunk".equalsIgnoreCase(count.per))
		{
			// In 1.12.2, we have to iterate entity lists in the chunk
			entities = new ArrayList<>();
			for (final var entityList : ctx.getChunk().getEntityLists())
				entities.addAll(entityList);
		}
		else
		{
			// Default to world
			entities = ctx.getWorld().loadedEntityList;
		}

		var entitiesFound = 0;
		for (final var entity : entities)
		{
			if (entity.getClass() == ctx.getEntity().getClass())
				++entitiesFound;
		}

		return checkRange(count, entitiesFound);
	}

	private static boolean checkRange(final RangeRaw range, final int value)
	{
		if (range.between != null && range.between.size() >= 2)
			return value >= range.between.get(0) && value <= range.between.get(1);
		if (range.at_least != null && value < range.at_least)
			return false;
		if (range.at_most != null && value > range.at_most)
			return false;
		return true;
	}

	/**
	 * The master method called every tick. Returns NULL if the rule doesn't match (logic flows to next rule). Returns
	 * ALLOW/DENY/DEFAULT if the rule matches.
	 */
	public Event.Result evaluate(final EntityEventWrapper<E> ctx)
	{
		for (final var check : selectorChecks)
		{
			if (!check.test(ctx))
				// Selector failed, so this rule does not apply at all.
				return null;
		}

		for (final var check : conditionChecks)
		{
			if (!check.test(ctx))
				// Condition failed. If we have an 'else' clause, return that.
				// Otherwise, return null (treat as if rule didn't apply).
				return (elseResult != Event.Result.DEFAULT) ? elseResult : null;
		}

		// All checks passed
		return thenResult;
	}
}
