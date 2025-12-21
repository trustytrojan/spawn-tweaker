package dev.trustytrojan.spawn_tweaker.rule;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.annotations.SerializedName;

import dev.trustytrojan.spawn_tweaker.EntityCounter;
import dev.trustytrojan.spawn_tweaker.Util;
import dev.trustytrojan.spawn_tweaker.event.EntityEventWrapper;
import net.minecraft.entity.EntityList;
import net.minecraftforge.fml.common.eventhandler.Event;

public class SpawnRule
{
	private static final Logger logger = LogManager.getLogger();

	// Raw data
	public String on; // "join", "spawn" by default

	@SerializedName("for")
	public Conditions forParams;

	@SerializedName("if")
	public Conditions ifParams;

	public String then; // "allow", "deny", "default"

	@SerializedName("else")
	public String elseAction; // "allow", "deny", "default"

	public boolean debug; // when true, log evaluation results and counts

	// Debug accumulator (transient because it's runtime-only)
	private transient StringBuilder debugSb = new StringBuilder();

	// Compiled data
	// @formatter:off
	private final transient List<Predicate<EntityEventWrapper<?>>>
		selectorChecks = new ArrayList<>(),
		conditionChecks = new ArrayList<>();
	// @formatter:on
	private transient Event.Result thenResult;
	private transient Event.Result elseResult;
	private transient BiFunction<Integer, EntityEventWrapper<?>, Event.Result> evaluateFunction;

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

		evaluateFunction = debug ? this::evaluateDebug : this::evaluateNoDebug;
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
			if (debug)
			{
				checks.add(ctx ->
				{
					final var entityClass = ctx.getEntity().getClass();
					final var registryName = EntityList.getKey(entityClass);
					// might want to null-check here
					final var pass = mobs.contains(entityClass);
					debugSb.append("\n  mobs check: entity=")
						.append(registryName)
						.append(" match=")
						.append(pass)
						.append(";");
					return pass;
				});
			}
			else
				checks.add(ctx -> mobs.contains(ctx.getEntity().getClass()));
		}

		if (c.dimension != null)
		{
			if (debug)
			{
				checks.add(ctx ->
				{
					final var pass = ctx.getDimension() == c.dimension;
					debugSb.append("\n  dimension check: expected=")
						.append(c.dimension)
						.append(" actual=")
						.append(ctx.getDimension())
						.append(" pass=")
						.append(pass)
						.append(";");
					return pass;
				});
			}
			else
				checks.add(ctx -> ctx.getDimension() == c.dimension);
		}

		if (c.light != null)
		{
			if (debug)
			{
				checks.add(ctx ->
				{
					final var level = ctx.getLightLevel();
					final var pass = c.light.check(level);
					debugSb.append("\n  light check: level=")
						.append(level)
						.append(" pass=")
						.append(pass)
						.append(";");
					return pass;
				});
			}
			else
				checks.add(ctx -> c.light.check(ctx.getLightLevel()));
		}

		if (c.height != null)
		{
			if (debug)
			{
				checks.add(ctx ->
				{
					final var h = ctx.getHeight();
					final var pass = c.height.check(h);
					debugSb.append("\n  height check: height=")
						.append(h)
						.append(" pass=")
						.append(pass)
						.append(";");
					return pass;
				});
			}
			else
				checks.add(ctx -> c.height.check(ctx.getHeight()));
		}

		if (c.health != null)
		{
			if (debug)
			{
				checks.add(ctx ->
				{
					final var hp = (int) ctx.getEntity().getMaxHealth();
					final var pass = c.health.check(hp);
					debugSb.append("\n  health check: maxHp=")
						.append(hp)
						.append(" pass=")
						.append(pass)
						.append(";");
					return pass;
				});
			}
			else
				checks.add(ctx -> c.health.check((int) ctx.getEntity().getMaxHealth()));
		}

		if (c.random != null)
		{
			final var rng = new Random();
			if (debug)
			{
				checks.add(ctx ->
				{
					final var v = rng.nextFloat();
					final var pass = v < c.random;
					debugSb.append("\n  random check: value=")
						.append(v)
						.append(" thresh=")
						.append(c.random)
						.append(" pass=")
						.append(pass)
						.append(";");
					return pass;
				});
			}
			else
				checks.add(ctx -> rng.nextFloat() < c.random);
		}

		if (c.count != null)
		{
			if (debug)
				checks.add(ctx -> checkCountDebug(c.count, ctx));
			else
				checks.add(ctx -> checkCount(c.count, ctx));
		}
	}

	private boolean checkCount(final Count count, final EntityEventWrapper<?> ctx)
	{
		final var world = ctx.getWorld();
		final var entitiesFound = (count.isGroupCount)
			? EntityCounter.getCount(world, count.mobClasses)
			: EntityCounter.getCount(world, ctx.getEntity().getClass());
		final var factor = count.scaler.apply(ctx);
		final var pass = count.check(entitiesFound, factor);
		return pass;
	}

	private boolean checkCountDebug(final Count count, final EntityEventWrapper<?> ctx)
	{
		final var world = ctx.getWorld();
		final var entitiesFound = (count.isGroupCount)
			? EntityCounter.getCount(world, count.mobClasses)
			: EntityCounter.getCount(world, ctx.getEntity().getClass());
		final var factor = count.scaler.apply(ctx);
		final var pass = count.check(entitiesFound, factor);
		debugSb.append("\n  count check: counted=")
			.append(entitiesFound)
			.append(" factor=")
			.append(factor)
			.append(" pass=")
			.append(pass)
			.append(";");
		return pass;
	}

	private Event.Result evaluateDebug(final int index, final EntityEventWrapper<?> ctx)
	{
		debugSb.setLength(0);
		debugSb.append("\n  Evaluating Debug Rule #").append(index);

		for (final var selector : selectorChecks)
		{
			if (!selector.test(ctx))
			{
				// Intentionally no logging on failure (avoids log spam).
				return null;
			}
		}

		debugSb.append("\n  selectors passed;");
		final Event.Result result;

		for (final var condition : conditionChecks)
		{
			if (!condition.test(ctx))
			{
				// Intentionally no logging on failure (avoids log spam).
				return (elseResult != Event.Result.DEFAULT) ? elseResult : null;
			}
		}

		result = thenResult;
		debugSb.append("\n  conditions passed; ")
			.append((char) 27)
			.append("[33mresult=")
			.append(result)
			.append((char) 27)
			.append("[39m");
		logger.info(debugSb.toString());

		return result;
	}

	private Event.Result evaluateNoDebug(final int index, final EntityEventWrapper<?> ctx)
	{
		for (final var selector : selectorChecks)
		{
			if (!selector.test(ctx))
				return null;
		}

		for (final var condition : conditionChecks)
		{
			if (!condition.test(ctx))
				return (elseResult != Event.Result.DEFAULT) ? elseResult : null;
		}

		return thenResult;
	}

	public Event.Result evaluate(final int index, final EntityEventWrapper<?> ctx)
	{
		return evaluateFunction.apply(index, ctx);
	}
}
