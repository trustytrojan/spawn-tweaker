package dev.trustytrojan.spawn_tweaker;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import dev.trustytrojan.spawn_tweaker.data.SpawnEntryRaw;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public class SpawnEntryManager
{
	private static final Logger logger = LogManager.getLogger();
	private static File lastConfigFile;
	private static Biome[] ALL_BIOMES;

	public static void init()
	{
		ALL_BIOMES = new Biome[Biome.REGISTRY.getKeys().size()];
		var i = 0;
		for (final var b : ForgeRegistries.BIOMES)
			ALL_BIOMES[i++] = b;
	}

	public static void load(final File configFile)
	{
		lastConfigFile = configFile;
		final var gson = new Gson();
		final var yaml = new Yaml();

		try (final var ios = new FileInputStream(configFile))
		{
			// 1. YAML -> Generic Java Objects
			final var loaded = yaml.load(ios);
			if (loaded == null)
				return;

			// 2. Generic Java Objects -> Gson Tree
			final var jsonTree = gson.toJsonTree(loaded);

			// 3. Gson Tree -> Raw POJOs
			final List<SpawnEntryRaw> rawEntries =
				gson.fromJson(jsonTree, new TypeToken<List<SpawnEntryRaw>>() {}.getType());

			// 4. Apply entries
			if (rawEntries != null)
			{
				applyEntries(rawEntries);
			}

		}
		catch (final Exception e)
		{
			logger.error("Failed to load spawn entries", e);
		}
	}

	public static void reload()
	{
		if (lastConfigFile != null)
		{
			load(lastConfigFile);
		}
	}

	private static void applyEntries(final List<SpawnEntryRaw> entries)
	{
		final var allBiomes = getAllBiomes();

		for (final var entry : entries)
		{
			final var matchedEntities = new HashSet<Class<? extends EntityLiving>>();

			// 1. Resolve entities
			if (entry.mod != null)
			{
				matchedEntities.addAll(getEntitiesForMod(entry.mod));
			}

			if (entry.mobs != null)
			{
				for (final var mob : entry.mobs)
				{
					if (mob.endsWith(":*"))
					{
						final var modId = mob.substring(0, mob.length() - 2);
						matchedEntities.addAll(getEntitiesForMod(modId));
					}
					else
					{
						final var clazz = getEntityClass(mob);
						if (clazz != null)
						{
							matchedEntities.add(clazz);
						}
					}
				}
			}

			if (matchedEntities.isEmpty())
			{
				continue;
			}

			// 2. Resolve biomes
			final Biome[] targetBiomes;
			if (entry.biomes != null && !entry.biomes.isEmpty())
			{
				targetBiomes = resolveBiomes(entry.biomes, allBiomes);
			}
			else
			{
				targetBiomes = null;
			}

			// 3. Apply changes
			for (final var entityClass : matchedEntities)
			{
				final var biomesForEntity =
					(targetBiomes != null) ? targetBiomes : getCurrentSpawnBiomes(entityClass, allBiomes);

				if (biomesForEntity.length == 0)
				{
					continue;
				}

				// Remove existing spawns
				EntityRegistry.removeSpawn(entityClass, EnumCreatureType.MONSTER, allBiomes);

				// Add new spawn
				if (entry.weight == null)
					throw new IllegalArgumentException("entry is missing weight");
				if (entry.group_size == null)
					throw new IllegalArgumentException("entry is missing group_size");
				if (entry.group_size.size() < 2)
					throw new IllegalArgumentException("group_size needs 2 integers");

				final var minGroup = entry.group_size.get(0);
				final var maxGroup = entry.group_size.get(1);

				EntityRegistry.addSpawn(entityClass, entry.weight, minGroup, maxGroup, EnumCreatureType.MONSTER,
					biomesForEntity);
				logger.info("Applied spawn entry for {} in {} biomes", entityClass.getSimpleName(),
					biomesForEntity.length);
			}
		}
	}

	private static Biome[] getAllBiomes()
	{
		final var list = new ArrayList<Biome>();
		for (final var b : ForgeRegistries.BIOMES)
		{
			list.add(b);
		}
		return list.toArray(new Biome[list.size()]);
	}

	private static Set<Class<? extends EntityLiving>> getEntitiesForMod(final String modId)
	{
		final var set = new HashSet<Class<? extends EntityLiving>>();
		for (final var entry : ForgeRegistries.ENTITIES)
		{
			final var key = entry.getRegistryName();
			if (key != null && key.getNamespace().equals(modId))
			{
				if (EntityLiving.class.isAssignableFrom(entry.getEntityClass()))
				{
					@SuppressWarnings("unchecked")
					final var clazz = (Class<? extends EntityLiving>) entry.getEntityClass();
					set.add(clazz);
				}
			}
		}
		return set;
	}

	private static Class<? extends EntityLiving> getEntityClass(final String registryName)
	{
		final var key = new ResourceLocation(registryName);
		final var entry = ForgeRegistries.ENTITIES.getValue(key);
		if (entry != null && EntityLiving.class.isAssignableFrom(entry.getEntityClass()))
		{
			@SuppressWarnings("unchecked")
			final var clazz = (Class<? extends EntityLiving>) entry.getEntityClass();
			return clazz;
		}
		return null;
	}

	private static Biome[] resolveBiomes(final List<String> biomeNames, final Biome[] allBiomes)
	{
		final var matched = new HashSet<Biome>();
		for (final var name : biomeNames)
		{
			if (name.equals("*"))
			{
				return allBiomes;
			}
			if (name.endsWith(":*"))
			{
				final var domain = name.substring(0, name.length() - 2);
				for (final var b : allBiomes)
				{
					final var key = b.getRegistryName();
					if (key != null && key.getNamespace().equals(domain))
					{
						matched.add(b);
					}
				}
			}
			else
			{
				final var b = ForgeRegistries.BIOMES.getValue(new ResourceLocation(name));
				if (b != null)
				{
					matched.add(b);
				}
			}
		}
		return matched.toArray(new Biome[0]);
	}

	private static Biome[] getCurrentSpawnBiomes(
		final Class<? extends EntityLiving> entityClass,
		final Biome[] allBiomes)
	{
		final var list = new ArrayList<Biome>();
		for (final var b : allBiomes)
		{
			final var spawns = b.getSpawnableList(EnumCreatureType.MONSTER);
			for (final var entry : spawns)
			{
				if (entry.entityClass.equals(entityClass))
				{
					list.add(b);
					break;
				}
			}
		}
		return list.toArray(new Biome[0]);
	}
}
