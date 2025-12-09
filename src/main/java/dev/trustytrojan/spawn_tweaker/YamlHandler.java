package dev.trustytrojan.spawn_tweaker;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Handles reading and writing spawn rules in YAML format.
 */
public class YamlHandler
{
	private static final Logger logger = LogManager.getLogger();
	private static final Yaml YAML = new Yaml();

	/**
	 * Read spawn rules from a YAML file.
	 *
	 * @param file The YAML file to read from
	 * @return List of spawn rules, or null if file doesn't exist or an error occurs
	 */
	@SuppressWarnings("unchecked")
	public static List<SpawnEntryCombination> readRules(final File file)
	{
		if (!file.exists())
		{
			logger.warn("YAML file not found: {}", file.getAbsolutePath());
			return null;
		}

		try (final var reader = new FileReader(file))
		{
			// YAML now has a root mapping: it contains keys like 'on_join' and 'rules'
			final var loaded = YAML.load(reader);
			if (loaded == null)
			{
				logger.warn("No spawn rules found in YAML file");
				return new ArrayList<>();
			}

			// Extract rules list from either new format (Map with 'rules' key) or legacy format (List at root)
			List<Map<String, Object>> rawRules = null;
			if (loaded instanceof Map)
			{
				final var root = (Map<String, Object>) loaded;
				// Parse 'on_join' if present
				try
				{
					final var onJoinRaw = (Map<String, Object>) root.get("on_join");
					if (onJoinRaw != null)
					{
						logger.debug("Parsing on_join section from YAML");
						parseOnJoin(onJoinRaw);
					}
					else
					{
						// Clear previous on_join config if YAML doesn't contain it
						SpawnTweaker.setOnJoinConfig(null);
						logger.debug("No on_join section found in YAML; cleared on_join config");
					}
				}
				catch (final ClassCastException e)
				{
					logger.error("Malformed 'on_join' section in YAML - expected mapping", e);
				}

				rawRules = (List<Map<String, Object>>) root.get("rules");
			}
			else if (loaded instanceof List)
			{
				// Fallback to legacy format (plain list of rules at root)
				rawRules = (List<Map<String, Object>>) loaded;
				// Clear on-join config (legacy file won't have it)
				SpawnTweaker.setOnJoinConfig(null);
			}

			// Parse rules
			final var rules = new ArrayList<SpawnEntryCombination>();
			if (rawRules != null)
			{
				for (var i = 0; i < rawRules.size(); i++)
				{
					final var rawRule = rawRules.get(i);
					final var rule = parseRule(rawRule, i + 1);
					if (rule != null)
					{
						rules.add(rule);
					}
				}
			}
			else if (!(loaded instanceof List || loaded instanceof Map))
			{
				logger.warn("YAML file contains invalid structure; expected mapping with 'rules' or list of rules");
			}

			logger.info("Loaded {} spawn rules from YAML: {}", rules.size(), file.getName());
			return rules;
		}
		catch (final IOException e)
		{
			logger.error("Failed to read YAML file: " + file.getAbsolutePath(), e);
			return null;
		}
		catch (final Exception e)
		{
			logger.error("Failed to parse YAML file: " + file.getAbsolutePath(), e);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private static void parseOnJoin(final Map<String, Object> onJoinRaw)
	{
		try
		{
			final var minHealthChance = new LinkedHashMap<Integer, Double>();
			final var rawMin = (Map<Object, Object>) onJoinRaw.get("min_health_chance");
			if (rawMin != null)
			{
				for (final var e : rawMin.entrySet())
				{
					try
					{
						final var key = Integer.parseInt(e.getKey().toString());
						final var val = e.getValue();
						if (val instanceof Number)
						{
							minHealthChance.put(key, ((Number) val).doubleValue());
						}
					}
					catch (final NumberFormatException nfe)
					{
						logger.warn("Skipping invalid min_health_chance key: {}", e.getKey());
					}
				}
			}
			final var cfg = new OnJoinConfig(minHealthChance);
			SpawnTweaker.setOnJoinConfig(cfg);
			logger.info("Loaded on_join config: min_health_chance entries={}", minHealthChance.size());

			// Log each threshold for visibility
			for (final var e : minHealthChance.entrySet())
			{
				logger.debug("on_join: min_health_chance threshold {} -> {}", e.getKey(), e.getValue());
			}
		}
		catch (final Exception e)
		{
			logger.error("Failed to parse on_join section", e);
		}
	}

	/**
	 * Write spawn rules to a YAML file.
	 *
	 * @param file  The YAML file to write to
	 * @param rules The spawn rules to write
	 * @return true if successful, false otherwise
	 */
	public static boolean writeRules(final File file, final List<SpawnEntryCombination> rules)
	{
		// Ensure parent directory exists
		final var parentDir = file.getParentFile();
		if (parentDir != null && !parentDir.exists())
		{
			parentDir.mkdirs();
		}

		try (final var writer = new FileWriter(file))
		{
			// Build entity-centric export list: one top-level list item per entity
			final var entityList = new ArrayList<>();

			for (final var rule : rules)
			{
				if (rule == null || rule.forSelector == null || rule.spawn == null)
					continue;
				final var entities = rule.forSelector.entities;
				final var biomes = rule.forSelector.biomes;

				for (final var entityKey : entities)
				{
					final var ent = new LinkedHashMap<>();
					ent.put("entity", entityKey);
					ent.put("weight", rule.spawn.weight);
					ent.put("minGroupSize", rule.spawn.minGroupSize);
					ent.put("maxGroupSize", rule.spawn.maxGroupSize);

					// Group biomes by resource domain
					final var domainMap = new LinkedHashMap<String, List<String>>();
					if (biomes != null)
					{
						for (final var biome : biomes)
						{
							var domain = "minecraft";
							var name = biome;
							var idx = biome.indexOf(':');
							if (idx >= 0)
							{
								domain = biome.substring(0, idx);
								name = biome.substring(idx + 1);
							}
							domainMap.computeIfAbsent(domain, k -> new ArrayList<String>()).add(name);
						}
					}
					ent.put("biomes", domainMap);
					entityList.add(ent);
				}
			}

			// Prepare YAML dumper options to avoid line wrapping and use block style
			final var options = new DumperOptions();
			options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
			options.setWidth(Integer.MAX_VALUE);
			final var yaml = new Yaml(options);
			yaml.dump(entityList, writer);
			logger.info("Exported {} spawn rules to YAML: {}", rules.size(), file.getAbsolutePath());
			return true;
		}
		catch (final IOException e)
		{
			logger.error("Failed to write YAML file: " + file.getAbsolutePath(), e);
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	private static SpawnEntryCombination parseRule(final Map<String, Object> rawRule, final int ruleIndex)
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
				minGroupSize = getIntValue(spawnSection, "minGroupSize", 1);
				maxGroupSize = getIntValue(spawnSection, "maxGroupSize", 1);
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
	public static SpawnConfiguration readConfiguration(final File file)
	{
		if (!file.exists())
			return null;
		try (final var reader = new FileReader(file))
		{
			final var loaded = YAML.load(reader);
			if (!(loaded instanceof Map))
				return null;

			final var root = (Map<String, Object>) loaded;
			final var config = new SpawnConfiguration();

			// Parse on_join_deny
			if (root.containsKey("on_join_deny"))
			{
				final var list = (List<Map<String, Object>>) root.get("on_join_deny");
				for (final var raw : list)
				{
					final var rule = new OnJoinRule();
					rule.mob = (String) raw.get("mob");
					rule.mod = raw.get("mod");
					rule.count = (String) raw.get("count");
					config.onJoinDeny.add(rule);
				}
			}

			// Parse check_spawn
			if (root.containsKey("check_spawn"))
			{
				final var list = (List<Map<String, Object>>) root.get("check_spawn");
				for (final var raw : list)
				{
					final var rule = new CheckSpawnRule();
					// Parse for
					if (raw.containsKey("for"))
					{
						final var forRaw = (Map<String, Object>) raw.get("for");
						rule.selector = new CheckSpawnRule.Selector();
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
						rule.conditions = new CheckSpawnRule.Conditions();
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
					final var rule = parseRule(list.get(i), i);
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
