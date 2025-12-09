package dev.trustytrojan.spawn_tweaker;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SpawnTweaker
{
	private static final Logger logger = LogManager.getLogger();
	private static Biome[] ALL_BIOMES;

	// Configuration applied on entity join events
	private static volatile OnJoinConfig onJoinConfig = null;

	public static void init()
	{
		ALL_BIOMES = new Biome[Biome.REGISTRY.getKeys().size()];
		var i = 0;
		for (final var b : Biome.REGISTRY) ALL_BIOMES[i++] = b;

		importMonsterSpawnData();
	}

	private static SpawnConfiguration configuration;

	/**
	 * Import monster spawn data from YAML file.
	 */
	public static void importMonsterSpawnData()
	{
		logger.info("Importing monster spawn data...");

		// Try new config in config directory
		final var configFile = new File("config/spawn_tweaker.yml");
		if (configFile.exists())
		{
			configuration = YamlHandler.readConfiguration(configFile);
			if (configuration != null)
			{
				applySpawnEntryCombinations(configuration.spawn);
				logger.info("Loaded configuration from config/spawn_tweaker.yml");
				return;
			}
		}

		final var dataDir = new File("spawn_tweaker");
		final var yamlFile = new File(dataDir, "monster_spawns.yml");

		List<SpawnEntryCombination> rules = null;

		if (yamlFile.exists())
		{
			rules = YamlHandler.readRules(yamlFile);
		}
		else
		{
			logger.warn("No monster spawn data file found (tried .yml)");
			return;
		}

		if (rules == null || rules.isEmpty())
		{
			logger.warn("No valid spawn rules to import");
			return;
		}

		applySpawnEntryCombinations(rules);
		logger.info("Monster spawn data imported successfully");
	}

	public static void setOnJoinConfig(final OnJoinConfig cfg)
	{
		onJoinConfig = cfg;
	}

	public static OnJoinConfig getOnJoinConfig()
	{
		return onJoinConfig;
	}

	/**
	 * Evaluate whether an entity's spawn should be allowed at join, per configured settings.
	 */
	public static boolean shouldAllowOnJoin(final EntityJoinWorldEvent event)
	{
		if (!(event.getEntity() instanceof EntityLiving))
			return true;
		final var el = (EntityLiving) event.getEntity();

		if (configuration != null && !configuration.onJoinDeny.isEmpty())
		{
			for (final var rule : configuration.onJoinDeny)
			{
				if (rule.check(event))
					return false; // Deny
			}
		}

		final var cfg = onJoinConfig;
		if (cfg == null)
			return true; // no config => allow
		return cfg.shouldAllowJoin(el, new Random());
	}

	public static void checkSpawn(final LivingSpawnEvent.CheckSpawn event)
	{
		if (configuration == null || configuration.checkSpawn.isEmpty())
			return;

		for (final var rule : configuration.checkSpawn)
		{
			final var result = rule.check(event);
			if (result != null)
			{
				event.setResult(result);
				return;
			}
			// PASS -> continue
		}
	}

	/**
	 * Apply a list of spawn rules to the game.
	 *
	 * @param rules The spawn rules to apply
	 */
	private static void applySpawnEntryCombinations(final List<SpawnEntryCombination> rules)
	{
		for (var ruleIdx = 0; ruleIdx < rules.size(); ruleIdx++)
		{
			final var rule = rules.get(ruleIdx);

			if (rule.forSelector.entities == null || rule.forSelector.entities.isEmpty())
			{
				logger.error("Rule #{}: missing or empty 'entities', skipping", ruleIdx + 1);
				continue;
			}

			// Note: 'biomes' on a rule is optional. If absent, we will use the
			// set of biomes where each matched entity is currently listed to spawn.

			// Match entities and apply spawn settings
			var totalEntitiesMatched = 0;
			var totalBiomesMatched = 0;
			for (final var entityPattern : rule.forSelector.entities)
			{
				final var pattern = Pattern.compile(GlobUtils.globToRegex(entityPattern));
				var entitiesMatched = 0;

				for (final var rl : EntityList.getEntityNameList())
				{
					final var key = rl.toString();
					if (!pattern.matcher(key).matches())
						continue;

					@SuppressWarnings("unchecked")
					final var entityClass = (Class<? extends EntityLiving>) EntityList.getClass(rl);

					final var targetBiomesForEntity = getTargetBiomesForEntity(rule, entityClass, rl, ruleIdx, key);
					if (targetBiomesForEntity == null || targetBiomesForEntity.length == 0)
					{
						// getTargetBiomesForEntity logs warning when needed
						continue;
					}

					applySpawnForEntity(entityClass, rule.spawn, targetBiomesForEntity);

					totalBiomesMatched += targetBiomesForEntity.length;
					++entitiesMatched;
				}

				if (entitiesMatched == 0)
				{
					logger.warn("Rule #{}: pattern \"{}\" matched zero entities", ruleIdx + 1, entityPattern);
				}
				else
				{
					totalEntitiesMatched += entitiesMatched;
				}
			}

			if (totalEntitiesMatched > 0)
			{
				logger.info("Rule #{} applied for {} entities in {} biomes", ruleIdx + 1, totalEntitiesMatched,
					totalBiomesMatched);
			}
		}
	}

	private static Biome[] getTargetBiomesForEntity(
		final SpawnEntryCombination rule,
		final Class<? extends EntityLiving> entityClass,
		final ResourceLocation rl,
		final int ruleIdx,
		final String entityKey)
	{
		if (rule.forSelector.biomes == null || rule.forSelector.biomes.isEmpty())
		{
			final var biomesForEntity = new ArrayList<Biome>();
			for (final var b : Biome.REGISTRY)
			{
				for (final var entry : b.getSpawnableList(EnumCreatureType.MONSTER))
				{
					if (entry.entityClass.equals(entityClass))
					{
						biomesForEntity.add(b);
						break;
					}
				}
			}
			if (biomesForEntity.isEmpty())
			{
				logger.warn("Rule #{}: entity {} currently registered in zero biomes, skipping", ruleIdx + 1,
					entityKey);
				return new Biome[0];
			}
			return biomesForEntity.toArray(new Biome[biomesForEntity.size()]);
		}

		final var targetBiomesGlob = GlobUtils.resolveBiomesFromGlobs(rule.forSelector.biomes);
		if (targetBiomesGlob.length == 0)
		{
			logger.warn("Rule #{}: no biomes matched from patterns {} — skipping", ruleIdx + 1,
				rule.forSelector.biomes);
			return new Biome[0];
		}
		return targetBiomesGlob;
	}

	private static void applySpawnForEntity(
		final Class<? extends EntityLiving> clazz,
		final SpawnEntryCombination.SpawnConfig spawn,
		final Biome[] biomes)
	{
		EntityRegistry.removeSpawn(clazz, EnumCreatureType.MONSTER, ALL_BIOMES);
		EntityRegistry.addSpawn(clazz, spawn.weight, spawn.minGroupSize, spawn.maxGroupSize, EnumCreatureType.MONSTER,
			biomes);
	}

	/**
	 * Export monster spawn data to YAML format.
	 *
	 * @param patterns Entity patterns to export (e.g., "modid:*", "*")
	 */
	public static void exportMonsterSpawnData(final List<String> patterns)
	{
		logger.info("Exporting monster spawn data for patterns: {}", patterns);

		// Precompile patterns once
		var matchAll = false;
		final var compiled = new ArrayList<Pattern>();
		if (patterns != null)
		{
			for (final var p : patterns)
			{
				if ("*".equals(p))
				{
					matchAll = true;
					break;
				}
				compiled.add(Pattern.compile(GlobUtils.globToRegex(p)));
			}
		}

		// Convert spawn data to rules
		final var rules = new ArrayList<SpawnEntryCombination>();
		final var entitySpawnMap = new LinkedHashMap<String, EntitySpawnInfo>();

		for (final var biome : Biome.REGISTRY)
		{
			var biomeName = biome.getRegistryName().toString();

			for (final var entry : biome.getSpawnableList(EnumCreatureType.MONSTER))
			{
				final var registration = EntityRegistry.instance().lookupModSpawn(entry.entityClass, true);
				if (registration == null)
					continue;

				final var entityKey = registration.getRegistryName().toString();
				if (!(matchAll || GlobUtils.matchesAnyCompiled(entityKey, compiled)))
					continue;

				// Get or create entity entry
				var spawnInfo = entitySpawnMap.get(entityKey);
				if (spawnInfo == null)
				{
					spawnInfo = new EntitySpawnInfo(entry.itemWeight, entry.minGroupCount, entry.maxGroupCount);
					entitySpawnMap.put(entityKey, spawnInfo);
				}

				// Add biome to the list
				spawnInfo.biomes.add(biomeName);
			}
		}

		// Convert to SpawnEntryCombination format
		for (final var entry : entitySpawnMap.entrySet())
		{
			final var entities = new ArrayList<String>();
			entities.add(entry.getKey());

			final var forSelector = new SpawnEntryCombination.ForSelector(entities, entry.getValue().biomes);
			final var spawnConfig = new SpawnEntryCombination.SpawnConfig(
				entry.getValue().weight,
				entry.getValue().minGroupSize,
				entry.getValue().maxGroupSize);

			rules.add(new SpawnEntryCombination(forSelector, spawnConfig));
		}

		// Export to both formats
		final var dataDir = new File("spawn_tweaker");
		if (!dataDir.exists())
		{
			dataDir.mkdirs();
		}

		YamlHandler.writeRules(new File(dataDir, "monster_spawns_export.yml"), rules);
	}

	// Backward-compatible overload: single modid or '*' handled as a single glob
	public static void exportMonsterSpawnData(final String modid)
	{
		final var patterns = Collections.singletonList(modid);
		exportMonsterSpawnData(patterns);
	}

	// Helper class to store entity spawn information
	private static class EntitySpawnInfo
	{
		int weight;
		int minGroupSize;
		int maxGroupSize;
		List<String> biomes;

		EntitySpawnInfo(final int weight, final int minGroupSize, final int maxGroupSize)
		{
			this.weight = weight;
			this.minGroupSize = minGroupSize;
			this.maxGroupSize = maxGroupSize;
			this.biomes = new ArrayList<>();
		}
	}
}
