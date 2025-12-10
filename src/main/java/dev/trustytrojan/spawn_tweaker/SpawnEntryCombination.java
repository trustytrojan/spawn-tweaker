package dev.trustytrojan.spawn_tweaker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Represents a spawn rule with 'for' (selector) and 'spawn' (config) sections.
 */
public class SpawnEntryCombination
{
	private static final Logger logger = LogManager.getLogger();

	/**
	 * The 'for' section that specifies which entities and biomes this rule applies to.
	 */
	public static class ForSelector
	{
		public List<String> entities;
		public List<String> biomes;

		public ForSelector()
		{}

		public ForSelector(final List<String> entities, final List<String> biomes)
		{
			this.entities = entities;
			this.biomes = biomes;
		}
	}

	/**
	 * The 'spawn' section that specifies spawn configuration.
	 */
	public static class SpawnConfig
	{
		public int weight;
		public int minGroupSize;
		public int maxGroupSize;

		public SpawnConfig()
		{}

		public SpawnConfig(final int weight, final int minGroupSize, final int maxGroupSize)
		{
			this.weight = weight;
			this.minGroupSize = minGroupSize;
			this.maxGroupSize = maxGroupSize;
		}
	}

	public ForSelector forSelector;
	public SpawnConfig spawn;

	public SpawnEntryCombination()
	{}

	public SpawnEntryCombination(final ForSelector forSelector, final SpawnConfig spawn)
	{
		this.forSelector = forSelector;
		this.spawn = spawn;
	}

	/**
	 * Parse a SpawnEntryCombination from YAML data.
	 * Supports both new format (with 'for' section) and legacy format (with 'mobs'/'mod' at root).
	 */
	@SuppressWarnings("unchecked")
	public static SpawnEntryCombination fromYaml(final Map<String, Object> rawRule, final int ruleIndex)
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

			final var forSelector = new ForSelector(entities, biomes);
			final var spawnConfig = new SpawnConfig(weight, minGroupSize, maxGroupSize);

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
}
