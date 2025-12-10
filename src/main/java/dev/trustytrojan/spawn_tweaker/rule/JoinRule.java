package dev.trustytrojan.spawn_tweaker.rule;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

public class JoinRule extends Rule<EntityJoinWorldEvent, Boolean>
{
	private static final Logger logger = LogManager.getLogger();

	public static final IEventQuery<EntityJoinWorldEvent> EVENT_QUERY_JOIN = new IEventQuery<EntityJoinWorldEvent>() {
		@Override
		public World getWorld(final EntityJoinWorldEvent o)
		{
			return o.getWorld();
		}

		@Override
		public BlockPos getPos(final EntityJoinWorldEvent o)
		{
			return o.getEntity().getPosition();
		}

		@Override
		public BlockPos getValidBlockPos(final EntityJoinWorldEvent o)
		{
			return o.getEntity().getPosition().down();
		}

		@Override
		public int getY(final EntityJoinWorldEvent o)
		{
			return o.getEntity().getPosition().getY();
		}

		@Override
		public Entity getEntity(final EntityJoinWorldEvent o)
		{
			return o.getEntity();
		}
	};

	public JoinRule()
	{
		super(EVENT_QUERY_JOIN);
	}

	/**
	 * Parse a JoinRule from YAML data.
	 */
	public static JoinRule fromYaml(final Map<String, Object> raw)
	{
		final var rule = new JoinRule();
		logger.debug("Parsing JoinRule: {}", raw);

		// Parse mob - convert to mobs map for selector
		final var mob = (String) raw.get("mob");
		final Map<String, List<String>> mobsMap;
		if (mob != null)
		{
			final var parts = mob.split(":", 2);
			final var domain = parts[0];
			final var path = parts.length > 1 ? parts[1] : "";
			final var map = new HashMap<String, List<String>>();
			final var list = new ArrayList<String>();
			list.add(path);
			map.put(domain, list);
			mobsMap = map;
			rule.addSelectorIf(mobsMap, c -> !mobsMap.isEmpty() && c.mobs(mobsMap));
		}
		else
		{
			mobsMap = null;
		}

		// Parse mod
		final var mod = raw.get("mod");
		rule.addSelectorIf(mod, c -> c.mod(mod));

		// Parse count - needs mobs and mod
		final var count = CountCondition.fromYaml(raw.get("count"));
		if (count != null)
		{
			rule.addConditionIf(count, c -> c.count(count, mobsMap, mod));
		}

		logger.info("JoinRule: {} selectors, {} conditions", rule.selectorChecks.size(), rule.conditionChecks.size());
		return rule;
	}

	@Override
	public Boolean getDefaultResult()
	{
		return false;
	}

	@Override
	public Boolean check(EntityJoinWorldEvent event)
	{
		final var living = getLiving(event, EVENT_QUERY_JOIN);
		if (living == null)
			return false;
		if (!hasRegistryEntry(living))
			return false;

		logger.debug("Checking join: {}", living.getName());

		// Check selectors
		if (!matchSelectors(event, EVENT_QUERY_JOIN))
		{
			logger.debug("  Selector failed");
			return false;
		}
		logger.debug("  Selector passed");

		// Evaluate conditions
		final var evalResult = evaluateConditions(event, EVENT_QUERY_JOIN);
		logger.debug("  Conditions result: {}", evalResult);
		if (evalResult != null)
			return evalResult;

		// If there were no explicit conditions, a selector match should be considered a match
		if (conditionChecks.isEmpty())
		{
			logger.debug("  No conditions, match=true");
			return true;
		}

		// Otherwise return then/default result (probably false for JoinRule)
		final var result = thenResult != null ? thenResult : getDefaultResult();
		logger.debug("  Final: {}", result);
		return result;
	}
}
