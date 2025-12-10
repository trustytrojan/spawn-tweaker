package dev.trustytrojan.spawn_tweaker.rule;

import java.util.List;
import java.util.Map;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingSpawnEvent.CheckSpawn;
import net.minecraftforge.fml.common.eventhandler.Event;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// No extra imports required here

public class SpawnRule extends Rule<CheckSpawn, Event.Result>
{
	private static final Logger logger = LogManager.getLogger();

	public SpawnRule()
	{
		super(EVENT_QUERY);
	}

	public static final IEventQuery<CheckSpawn> EVENT_QUERY = new IEventQuery<>() {
		@Override
		public World getWorld(final CheckSpawn o)
		{
			return o.getWorld();
		}

		@Override
		public BlockPos getPos(final CheckSpawn o)
		{
			return new BlockPos((int) o.getX(), (int) o.getY(), (int) o.getZ());
		}

		@Override
		public BlockPos getValidBlockPos(final CheckSpawn o)
		{
			return new BlockPos((int) o.getX(), (int) o.getY() - 1, (int) o.getZ());
		}

		@Override
		public int getY(final CheckSpawn o)
		{
			return (int) o.getY();
		}

		@Override
		public Entity getEntity(final CheckSpawn o)
		{
			return o.getEntity();
		}
	};

	/**
	 * Parse a SpawnRule from YAML data.
	 */
	@SuppressWarnings("unchecked")
	public static SpawnRule fromYaml(final Map<String, Object> raw)
	{
		final var rule = new SpawnRule();

		// Parse 'for' section - build selector closures immediately
		if (raw.containsKey("for"))
		{
			final var forRaw = (Map<String, Object>) raw.get("for");
			logger.debug("Parsing 'for' section: {}", forRaw);

			// mod check
			rule.addSelectorIf(forRaw.get("mod"), c -> c.mod(forRaw.get("mod")));

			// dimension check
			final var dimRaw = forRaw.get("dimension");
			if (dimRaw instanceof Number)
			{
				final var dim = ((Number) dimRaw).intValue();
				rule.addSelectorIf(dim, c -> c.dimension(dim));
			}

			// mobs check
			final var mobs = (Map<String, List<String>>) forRaw.get("mobs");
			logger.debug("Parsed mobs: {}", mobs);
			rule.addSelectorIf(mobs, c -> !mobs.isEmpty() && c.mobs(mobs));

			// health check (selector)
			final var health = NumberCondition.fromYaml(forRaw.get("health"));
			rule.addSelectorIf(health, c -> c.health(health));
		}

		// Parse 'if' section - build condition closures immediately
		if (raw.containsKey("if"))
		{
			final var ifRaw = (Map<String, Object>) raw.get("if");
			logger.debug("Parsing 'if' section: {}", ifRaw);

			// random check
			final var randRaw = ifRaw.get("random");
			if (randRaw instanceof Number)
			{
				final var random = ((Number) randRaw).doubleValue();
				rule.addConditionIf(random, c -> c.random(random));
			}

			// light check
			final var light = (List<Integer>) ifRaw.get("light");
			if (light != null && light.size() == 2)
				rule.addConditionIf(light, c -> c.light(light.get(0), light.get(1)));

			// height check
			final var height = NumberCondition.fromYaml(ifRaw.get("height"));
			logger.debug("Parsed height condition: {}", height);
			rule.addConditionIf(height, c -> c.height(height));

			// health check (condition)
			final var health = NumberCondition.fromYaml(ifRaw.get("health"));
			rule.addConditionIf(health, c -> c.health(health));

			// count check - needs mobs and mod from 'for' section
			final var count = CountCondition.fromYaml(ifRaw.get("count"));
			if (count != null)
			{
				final var forRaw = (Map<String, Object>) raw.get("for");
				final var mobs = forRaw != null ? (Map<String, List<String>>) forRaw.get("mobs") : null;
				final var mod = forRaw != null ? forRaw.get("mod") : null;
				rule.addConditionIf(count, c -> c.count(count, mobs, mod));
			}
		}
		else
		{
			logger.warn("check_spawn rule missing 'if' clause; rule may not function correctly");
		}

		// Parse 'then' and 'else' results
		rule.thenResult = RuleUtils.parseActionResult((String) raw.get("then"));
		rule.elseResult = RuleUtils.parseActionResult((String) raw.get("else"));
		logger.info("SpawnRule: {} selectors, {} conditions, then={}, else={}", rule.selectorChecks.size(),
			rule.conditionChecks.size(), rule.thenResult, rule.elseResult);

		return rule;
	}

	@Override
	public Event.Result getDefaultResult()
	{
		return Event.Result.DEFAULT;
	}

	@Override
	public Event.Result check(CheckSpawn event)
	{
		final var living = getLiving(event, EVENT_QUERY);
		if (living == null)
			return Event.Result.DEFAULT;

		final var pos = EVENT_QUERY.getPos(event);
		logger.debug("Checking spawn for {} at {}", living.getName(), pos);

		// Check selectors
		if (!matchSelectors(event, EVENT_QUERY))
		{
			logger.debug("  Selector check failed, returning null (PASS)");
			return null; // PASS
		}
		logger.debug("  Selector check passed");

		// Evaluate conditions
		final var evalResult = evaluateConditions(event, EVENT_QUERY);
		logger.debug("  Condition evaluation result: {}", evalResult);
		if (evalResult != null)
			return evalResult;

		// If there were no conditions, we can't make a decision - treat as PASS (not handled)
		if (conditionChecks.isEmpty())
		{
			logger.debug("  No conditions, returning null (PASS)");
			return null;
		}

		final var result = thenResult != null ? thenResult : Event.Result.DEFAULT;
		logger.debug("  Final result: {}", result);
		return result;
	}
}
