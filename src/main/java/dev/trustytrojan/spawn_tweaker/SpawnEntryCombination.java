package dev.trustytrojan.spawn_tweaker;

import java.util.List;

/**
 * Represents a spawn rule with 'for' (selector) and 'spawn' (config) sections.
 */
public class SpawnEntryCombination
{
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
}
