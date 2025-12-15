package dev.trustytrojan.spawn_tweaker;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import dev.trustytrojan.spawn_tweaker.data.ConditionsRaw;
import dev.trustytrojan.spawn_tweaker.data.CountRaw;
import dev.trustytrojan.spawn_tweaker.data.RangeRaw;
import dev.trustytrojan.spawn_tweaker.data.SpawnRuleRaw;
import dev.trustytrojan.spawn_tweaker.event.EntityEventWrapper;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.fml.common.eventhandler.Event;

public class CompiledRule<E extends EntityEvent>
{
	private final List<Predicate<EntityEventWrapper<E>>> selectorChecks = new ArrayList<>();
	private final List<Predicate<EntityEventWrapper<E>>> conditionChecks = new ArrayList<>();
	private final Event.Result thenResult;
	private final Event.Result elseResult;

	public CompiledRule(final SpawnRuleRaw raw)
	{
		this.thenResult = parseResult(raw.then);
		this.elseResult = parseResult(raw.elseAction);

		// Compile blocks into separate lists
		if (raw.forParams != null)
			bake(raw.forParams, selectorChecks);
		if (raw.ifParams != null)
			bake(raw.ifParams, conditionChecks);
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
		if (c.mobs != null)
		{
			final var mobs = Util.resolveEntityClasses(c.mobs).collect(Collectors.toSet());
			checks.add(ctx -> mobs.contains(ctx.getEntity().getClass()));
		}

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

	// Hot code path: DON'T allocate memory, reduce method calls.
	private boolean checkCount(final CountRaw count, final EntityEventWrapper<E> ctx)
	{
		final var targetClass = ctx.getEntity().getClass();
		var entitiesFound = 0;

		if (count.per.equalsIgnoreCase("chunk"))
		{
			final var entityLists = ctx.getChunk().getEntityLists();
			for (var i = 0; i < entityLists.length; ++i)
			{
				// unfortunately need to allocate iterator here,
				// ClassInheritanceMultiMap doesn't expose the internal list.
				for (final var entity : entityLists[i])
				{
					if (entity.getClass() == targetClass)
						++entitiesFound;
				}
			}
		}
		else
		{
			final var loadedEntityList = ctx.getWorld().loadedEntityList;
			for (int i = 0, n = loadedEntityList.size(); i < n; ++i)
			{
				if (loadedEntityList.get(i).getClass() == targetClass)
					++entitiesFound;
			}
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
	 * 
	 * Hot code path: DON'T allocate memory and reduce method calls.
	 */
	public Event.Result evaluate(final EntityEventWrapper<E> ctx)
	{
		for (int i = 0, n = selectorChecks.size(); i < n; ++i)
		{
			if (!selectorChecks.get(i).test(ctx))
				return null;
		}

		for (int i = 0, n = conditionChecks.size(); i < n; ++i)
		{
			if (!conditionChecks.get(i).test(ctx))
				return (elseResult != Event.Result.DEFAULT) ? elseResult : null;
		}

		return thenResult;
	}
}
