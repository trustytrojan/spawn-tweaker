package dev.trustytrojan.spawn_tweaker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public class SpawnTweaker
{
	private static final Logger logger = LogManager.getLogger();
	private static Biome[] ALL_BIOMES;

	public static void init()
	{
		ALL_BIOMES = new Biome[Biome.REGISTRY.getKeys().size()];
		var i = 0;
		for (final var b : Biome.REGISTRY) ALL_BIOMES[i++] = b;

		loadConfiguration();
	}

	private static Configuration configuration;

	/**
	 * Load spawn configuration from the new YAML format (config/spawn_tweaker.yml).
	 */

	public static void loadConfiguration()
	{
		logger.info("Loading spawn_tweaker configuration...");

		// Load new config in config directory
		final var configFile = new File("config/spawn_tweaker.yml");
		if (!configFile.exists())
		{
			logger.warn("No configuration file found at config/spawn_tweaker.yml");
			return;
		}

		configuration = YamlHandler.readConfiguration(configFile);
		if (configuration != null)
		{
			applySpawnEntryCombinations(configuration.spawn);
			logger.info("Loaded configuration from config/spawn_tweaker.yml");
			return;
		}

		logger.warn("Failed to load configuration from config/spawn_tweaker.yml");
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
	 * Evaluate whether an entity's spawn should be allowed at join, per configured settings.
	 */
	public static boolean shouldAllowOnJoin(final EntityJoinWorldEvent event)
	{
		if (!(event.getEntity() instanceof EntityLiving))
			return true;

		if (configuration == null || configuration.onJoinDeny == null || configuration.onJoinDeny.isEmpty())
			return true;

		for (final var rule : configuration.onJoinDeny)
		{
			if (rule.check(event))
				return false; // Deny on match
		}
		return true;
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

				for (final var entityEntry : ForgeRegistries.ENTITIES)
				{
					final var rl = entityEntry.getRegistryName();
					if (rl == null)
						continue; // Should not happen, but guard against nulls

					final var key = rl.toString();
					if (!pattern.matcher(key).matches())
						continue;

					@SuppressWarnings("unchecked")
					final var entityClass = (Class<? extends EntityLiving>) entityEntry.getEntityClass();

					final var targetBiomesForEntity = getTargetBiomesForEntity(rule, entityClass, ruleIdx, key);
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

	// Legacy export/import logic removed; configuration must be provided via config/spawn_tweaker.yml
}
