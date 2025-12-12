package dev.trustytrojan.spawn_tweaker;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.trustytrojan.spawn_tweaker.data.SpawnEntryRaw;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public class SpawnEntryManager
{
	private static final Logger logger = LogManager.getLogger();
	private static File lastConfigFile;
	private static Biome[] ALL_BIOMES;

	// Should be called in post-init since it's expected
	// most mods would register their biomes by then.
	public static void init()
	{
		ALL_BIOMES = StreamSupport.stream(Biome.REGISTRY.spliterator(), false).toArray(Biome[]::new);
	}

	public static void load(final File configFile)
	{
		lastConfigFile = configFile;

		final var entries = YamlLoader
			.loadListFromYaml(configFile, SpawnEntryRaw.class, e -> logger.error("Failed to load spawn entries", e));

		for (var i = 0; i < entries.size(); ++i)
			applyEntry(i + 1, entries.get(i));

		logger.info("Entries applied!");
	}

	public static void reload()
	{
		if (lastConfigFile != null)
			load(lastConfigFile);
	}

	@SuppressWarnings("unchecked")
	private static void applyEntry(final int i, final SpawnEntryRaw entry)
	{
		final Collection<Class<? extends EntityLiving>> matchedEntities;

		// 1. Resolve entities (plural mods/mobs preferred over singular mod/mob)
		if (entry.mods != null)
		{
			matchedEntities = entry.mods.stream()
				.map(SpawnEntryManager::getEntitiesForMod)
				.flatMap(Collection::stream)
				.collect(Collectors.toList());
		}
		else if (entry.mod != null)
		{
			matchedEntities = getEntitiesForMod(entry.mod);
		}
		else if (entry.mobs != null)
		{
			if (entry.mobs instanceof List)
				matchedEntities = ((List<String>) entry.mobs).stream()
					.map(SpawnEntryManager::getEntityClass)
					.filter(c -> c != null)
					.collect(Collectors.toList());
			else if (entry.mobs instanceof Map)
				matchedEntities = ((Map<String, List<String>>) entry.mobs).entrySet()
					.stream()
					.flatMap(e -> e.getValue().stream().map(m -> e.getKey() + ':' + m))
					.map(SpawnEntryManager::getEntityClass)
					.filter(c -> c != null)
					.collect(Collectors.toList());
			else
			{
				logger.warn("Entry #{}: key 'mobs' is not a list or object, skipping", i);
				return;
			}
		}
		else if (entry.mob != null)
		{
			final var clazz = getEntityClass(entry.mob);
			if (clazz == null)
			{
				logger.warn("Entry #{}: key 'mob' did not find an entity class, skipping", i);
				return;
			}
			// java 8 doesn't have List.of()...
			matchedEntities = Arrays.asList(clazz);
		}
		else
		{
			matchedEntities = Collections.emptyList();
		}

		if (matchedEntities.isEmpty())
		{
			logger.warn("Entry #{}: no entities matched, skipping", i);
			return;
		}

		// 2. Resolve biomes
		final Biome[] targetBiomes;
		if (entry.biomes != null && !entry.biomes.isEmpty())
			targetBiomes = resolveBiomes(entry.biomes);
		else
			targetBiomes = null;

		// 3. Apply changes
		for (final var entityClass : matchedEntities)
		{
			final var biomesForEntity = (targetBiomes != null) ? targetBiomes : getCurrentSpawnBiomes(entityClass);

			if (biomesForEntity.length == 0)
			{
				logger.warn("Entry #{}: entity {} matched no biomes, skipping", i, entityClass.getSimpleName());
				continue;
			}

			// Remove existing spawns
			EntityRegistry.removeSpawn(entityClass, EnumCreatureType.MONSTER, ALL_BIOMES);

			// Add new spawn
			if (entry.weight == null)
				throw new IllegalArgumentException("entry is missing weight");
			if (entry.group_size == null)
				throw new IllegalArgumentException("entry is missing group_size");
			if (entry.group_size.size() < 2)
				throw new IllegalArgumentException("group_size needs 2 integers");

			EntityRegistry.addSpawn(
				entityClass,
				entry.weight,
				entry.group_size.get(0),
				entry.group_size.get(1),
				EnumCreatureType.MONSTER,
				biomesForEntity);

			logger.info(
				"Entry #{}: applied {} for {} in {} biomes",
				i,
				entry,
				entityClass.getName(),
				biomesForEntity.length);
		}
	}

	@SuppressWarnings({
		"unchecked", "deprecation"
	})
	private static Collection<Class<? extends EntityLiving>> getEntitiesForMod(final String modId)
	{
		return ForgeRegistries.ENTITIES.getValues()
			.stream()
			.filter(e -> e.getRegistryName().getNamespace().equals(modId))
			.map(EntityEntry::getEntityClass)
			.filter(c -> c != null && EntityLiving.class.isAssignableFrom(c))
			.map(c -> (Class<? extends EntityLiving>) c)
			.collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	private static Class<? extends EntityLiving> getEntityClass(final String registryName)
	{
		final var entry = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(registryName));
		if (entry == null)
			return null;
		final var clazz = entry.getEntityClass();
		if (!EntityLiving.class.isAssignableFrom(clazz))
			return null;
		return (Class<? extends EntityLiving>) clazz;
	}

	private static Biome[] resolveBiomes(final List<String> biomeNames)
	{
		final var matched = new HashSet<Biome>();
		for (final var name : biomeNames)
		{
			if (name.equals("*"))
				return ALL_BIOMES;

			if (name.endsWith(":*"))
			{
				final var domain = name.substring(0, name.length() - 2);
				Stream.of(ALL_BIOMES)
					.filter(b -> b.getRegistryName().getNamespace().equals(domain))
					.forEach(matched::add);
			}
			else
			{
				final var b = ForgeRegistries.BIOMES.getValue(new ResourceLocation(name));
				if (b != null)
					matched.add(b);
			}
		}
		return matched.toArray(new Biome[0]);
	}

	private static Biome[] getCurrentSpawnBiomes(final Class<? extends EntityLiving> entityClass)
	{
		return Stream.of(ALL_BIOMES)
			.filter(
				b -> b.getSpawnableList(EnumCreatureType.MONSTER)
					.stream()
					.anyMatch(e -> e.entityClass.equals(entityClass)))
			.toArray(Biome[]::new);
	}
}
