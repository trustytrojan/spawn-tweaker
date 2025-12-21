package dev.trustytrojan.spawn_tweaker.rule;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.gson.annotations.SerializedName;

import dev.trustytrojan.spawn_tweaker.EntityCounter;
import dev.trustytrojan.spawn_tweaker.Util;
import dev.trustytrojan.spawn_tweaker.event.EntityEventWrapper;
import net.minecraftforge.fml.common.eventhandler.Event;

public class SpawnRule
{
	// Raw data
	public String on; // "join", "spawn" by default

	@SerializedName("for")
	public Conditions forParams;

	@SerializedName("if")
	public Conditions ifParams;

	public String then; // "allow", "deny", "default"

	@SerializedName("else")
	public String elseAction; // "allow", "deny", "default"

	// Compiled data
	// @formatter:off
	private final transient List<Predicate<EntityEventWrapper<?>>>
		selectorChecks = new ArrayList<>(),
		conditionChecks = new ArrayList<>();
	// @formatter:on
	private transient Event.Result thenResult;
	private transient Event.Result elseResult;

	public void compile()
	{
		this.thenResult = parseResult(then);
		this.elseResult = parseResult(elseAction);

		if (forParams != null)
		{
			forParams.compile();
			bake(forParams, selectorChecks);
		}
		if (ifParams != null)
		{
			ifParams.compile();
			bake(ifParams, conditionChecks);
		}
	}

	private Event.Result parseResult(final String action)
	{
		if ("allow".equalsIgnoreCase(action))
			return Event.Result.ALLOW;
		if ("deny".equalsIgnoreCase(action))
			return Event.Result.DENY;
		return Event.Result.DEFAULT;
	}

	private void bake(final Conditions c, final List<Predicate<EntityEventWrapper<?>>> checks)
	{
		if (c.mobs != null)
		{
			final var mobs = Util.resolveEntityClasses(c.mobs).collect(Collectors.toSet());
			checks.add(ctx -> mobs.contains(ctx.getEntity().getClass()));
		}

		if (c.dimension != null)
			checks.add(ctx -> ctx.getDimension() == c.dimension);

		if (c.light != null)
			checks.add(ctx -> c.light.check(ctx.getLightLevel()));

		if (c.height != null)
			checks.add(ctx -> c.height.check(ctx.getHeight()));

		if (c.health != null)
			checks.add(ctx -> c.health.check((int) ctx.getEntity().getMaxHealth()));

		if (c.random != null)
		{
			final var rng = new Random();
			checks.add(ctx -> rng.nextFloat() < c.random);
		}

		if (c.count != null)
			checks.add(ctx -> checkCount(c.count, ctx));
	}

	private boolean checkCount(final Count count, final EntityEventWrapper<?> ctx)
	{
		final var world = ctx.getWorld();
		final var entitiesFound = EntityCounter.getCount(world, ctx.getEntity().getClass());
		final int factor = count.scaler.apply(ctx);
		return count.check(entitiesFound, factor);
	}

	public Event.Result evaluate(final EntityEventWrapper<?> ctx)
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
