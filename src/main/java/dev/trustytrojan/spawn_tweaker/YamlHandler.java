package dev.trustytrojan.spawn_tweaker;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import dev.trustytrojan.spawn_tweaker.rule.SpawnRule;
import dev.trustytrojan.spawn_tweaker.rule.JoinRule;
import dev.trustytrojan.spawn_tweaker.rule.RuleUtils;

/**
 * Handles reading and writing spawn rules in YAML format.
 */
public class YamlHandler
{
	private static final Logger logger = LogManager.getLogger();
	private static final Yaml YAML = new Yaml();

	@SuppressWarnings("unchecked")
	private static SpawnEntryCombination parseSpawnEntryCombination(final Map<String, Object> rawRule, final int ruleIndex)
	{
		try
		{
			List<String> entities = new ArrayList<>();

			// Support both new format ('for' section with 'entities') and legacy format ('mobs'/'mod' at root)
			if (rawRule.containsKey("for"))
			{
				final var forSection = (Map<String, Object>) rawRule.get("for");
				if (forSection == null)
				{
					logger.error("Rule #{}: missing 'for' section, skipping", ruleIndex);
					return null;
				}

				final var entitiesFromFor = (List<String>) forSection.get("entities");
				if (entitiesFromFor == null || entitiesFromFor.isEmpty())
				{
					logger.error("Rule #{}: missing or empty 'entities' in 'for' section, skipping", ruleIndex);
					return null;
				}
				entities.addAll(entitiesFromFor);
			}

			// Support legacy format with 'mobs' and 'mod' at root level
			if (rawRule.containsKey("mobs"))
			{
				entities.addAll((List<String>) rawRule.get("mobs"));
			}
			if (rawRule.containsKey("mod"))
			{
				entities.add(rawRule.get("mod") + ":*");
			}

			if (entities.isEmpty())
			{
				logger.error("Rule #{}: no entities found, skipping", ruleIndex);
				return null;
			}

			// Parse biomes
			List<String> biomes = null;
			if (rawRule.containsKey("for"))
			{
				final var forSection = (Map<String, Object>) rawRule.get("for");
				biomes = (List<String>) forSection.get("biomes");
			}
			else if (rawRule.containsKey("biomes"))
			{
				biomes = (List<String>) rawRule.get("biomes");
			}

			if (biomes != null && biomes.isEmpty())
			{
				logger.debug("Rule #{}: no 'biomes' specified; will use entity's current spawn biomes", ruleIndex);
				biomes = null;
			}

			// Parse spawn section
			int weight, minGroupSize, maxGroupSize;
			if (rawRule.containsKey("spawn"))
			{
				final var spawnSection = (Map<String, Object>) rawRule.get("spawn");
				if (spawnSection == null)
				{
					logger.error("Rule #{}: missing 'spawn' section, skipping", ruleIndex);
					return null;
				}
				weight = getIntValue(spawnSection, "weight", 1);
				// Support both explicit min/max and the new 'group_size' array inside 'spawn'
				if (spawnSection.containsKey("group_size"))
				{
					final var groupSize = (List<Integer>) spawnSection.get("group_size");
					if (groupSize != null && groupSize.size() >= 2)
					{
						minGroupSize = groupSize.get(0);
						maxGroupSize = groupSize.get(1);
					}
					else
					{
						minGroupSize = 1;
						maxGroupSize = 1;
					}
				}
				else
				{
					minGroupSize = getIntValue(spawnSection, "minGroupSize", 1);
					maxGroupSize = getIntValue(spawnSection, "maxGroupSize", 1);
				}
			}
			else
			{
				// Legacy format: group_size at root level
				weight = getIntValue(rawRule, "weight", 1);
				List<Integer> groupSize = (List<Integer>) rawRule.get("group_size");
				minGroupSize = 1;
				maxGroupSize = 1;
				if (groupSize != null && groupSize.size() >= 2)
				{
					minGroupSize = groupSize.get(0);
					maxGroupSize = groupSize.get(1);
				}
			}

			final var forSelector = new SpawnEntryCombination.ForSelector(entities, biomes);
			final var spawnConfig = new SpawnEntryCombination.SpawnConfig(weight, minGroupSize, maxGroupSize);

			return new SpawnEntryCombination(forSelector, spawnConfig);
		}
		catch (final Exception e)
		{
			logger.error("Rule #{}: failed to parse rule", ruleIndex, e);
			return null;
		}
	}

	private static int getIntValue(final Map<String, Object> map, final String key, final int defaultValue)
	{
		final var value = map.get(key);
		if (value instanceof final Number n)
		{
			return n.intValue();
		}
		return defaultValue;
	}

	/**
	 * Read the new configuration format.
	 */
	@SuppressWarnings("unchecked")
	public static Configuration readConfiguration(final File file)
	{
		if (!file.exists())
			return null;
		try (final var reader = new FileReader(file))
		{
			final var loaded = YAML.load(reader);
			if (!(loaded instanceof Map))
				return null;

			final var root = (Map<String, Object>) loaded;
			final var config = new Configuration();

			// Parse on_join_deny
			if (root.containsKey("on_join_deny"))
			{
				final var list = (List<Map<String, Object>>) root.get("on_join_deny");
				for (final var raw : list)
				{
					final var rule = new JoinRule();
					rule.mob = (String) raw.get("mob");
					rule.mod = raw.get("mod");
					rule.count = (String) raw.get("count");
					rule.buildChecks();
					config.onJoinDeny.add(rule);
				}
			}

			// Parse check_spawn
			if (root.containsKey("check_spawn"))
			{
				final var list = (List<Map<String, Object>>) root.get("check_spawn");
				for (final var raw : list)
				{
					final var rule = new SpawnRule();
					// Parse for
					if (raw.containsKey("for"))
					{
						final var forRaw = (Map<String, Object>) raw.get("for");
						rule.selector = new SpawnRule.Selector();
						rule.selector.mod = forRaw.get("mod");
						rule.selector.health = (String) forRaw.get("health");
						final var dimRaw = forRaw.get("dimension");
						if (dimRaw instanceof Number)
							rule.selector.dimension = ((Number) dimRaw).intValue();
						rule.selector.mobs = (Map<String, List<String>>) forRaw.get("mobs");
					}

					// Parse if
					if (raw.containsKey("if"))
					{
						final var ifRaw = (Map<String, Object>) raw.get("if");
						rule.conditions = new SpawnRule.Conditions();
						final var randRaw = ifRaw.get("random");
						rule.conditions.random = (randRaw instanceof Number) ? ((Number) randRaw).doubleValue() : null;
						rule.conditions.light = (List<Integer>) ifRaw.get("light");
						rule.conditions.height = (String) ifRaw.get("height");
						rule.conditions.health = (String) ifRaw.get("health");
						rule.conditions.count = (String) ifRaw.get("count");
					}
					else
					{
						logger.warn("check_spawn rule missing 'if' clause; skipping rule");
						continue;
					}

					rule.thenResult = RuleUtils.parseActionResult((String) raw.get("then"));
					rule.elseResult = RuleUtils.parseActionResult((String) raw.get("else"));
					rule.buildEvaluator();
					config.checkSpawn.add(rule);
				}
			}

			// Parse spawn
			if (root.containsKey("spawn"))
			{
				final var list = (List<Map<String, Object>>) root.get("spawn");
				for (var i = 0; i < list.size(); i++)
				{
					final var rule = parseSpawnEntryCombination(list.get(i), i);
					if (rule != null)
						config.spawn.add(rule);
				}
			}

			return config;
		}
		catch (Exception e)
		{
			logger.error("Failed to read configuration", e);
			return null;
		}
	}
}
