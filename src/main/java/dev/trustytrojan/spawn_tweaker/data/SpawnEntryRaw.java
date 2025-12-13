package dev.trustytrojan.spawn_tweaker.data;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.trustytrojan.spawn_tweaker.Util;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.world.WorldEvent.PotentialSpawns;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityRegistry;

public class SpawnEntryRaw
{
	private static final Logger logger = LogManager.getLogger();

	// matching conditions
	public Map<String, /* String | List<String> */ Object> mobs;
	public Map<String, /* String | List<String> */ Object> biomes;

	// whether to remove all existing entries before adding ours
	public String remove; // "existing" | "specified"

	// actual entry data. optional in case an entry is just meant to remove spawns
	public Integer weight;
	public List<Integer> group_size;

	private int index;

	// for logging purposes
	public void setIndex(final int i)
	{
		index = i;
	}

	@Override
	public String toString()
	{
		return String.format("weight=%s group_size=%s", weight, group_size);
	}

	public Stream<Class<? extends EntityLiving>> resolveEntityClasses()
	{
		if (mobs == null)
		{
			logger.warn("Entry #{}: 'mobs' not provided, resolving zero entities", index);
			return Stream.empty();
		}

		return Util.resolveRegistryEntries(EntityEntry.class, mobs)
			.map(Util::getEntityLivingClass)
			.filter(Objects::nonNull) // this erases the <? extends EntityLiving>, we have to cast it back 🤷‍♂️
			.map(c -> (Class<? extends EntityLiving>) c);
	}

	public Stream<Biome> resolveBiomes()
	{
		if (biomes == null)
		{
			logger.warn("Entry #{}: 'biomes' not provided, resolving zero biomes", index);
			return Stream.empty();
		}

		return Util.resolveRegistryEntries(Biome.class, biomes);
	}

	public void apply()
	{
		final var canAddSpawn = weight != null && group_size != null && group_size.size() >= 2;

		if (!canAddSpawn && remove == null)
		{
			logger.warn("Entry #{}: NOT applying: weight/group_size/remove missing");
			return;
		}

		logger.info("Entry #{}: Applying {}", index, this);
		final var biomes = resolveBiomes().toArray(Biome[]::new);

		if (biomes.length == 0)
			logger.info("Entry #{}: No biomes specified, defaulting to existing entity biomes", index);

		for (final var entityClass : Util.iterableStream(resolveEntityClasses()))
		{
			final var currentBiomes = Util.getCurrentSpawnBiomes(entityClass);

			if (remove.equals("existing") && currentBiomes.length > 0)
			{
				EntityRegistry.removeSpawn(entityClass, EnumCreatureType.MONSTER, currentBiomes);
				logger.info("Entry #{}: Removed {} existing biomes for {}", index, currentBiomes.length, entityClass);
			}

			final var targetBiomes = (biomes.length > 0) ? biomes : currentBiomes;

			if (targetBiomes.length == 0)
			{
				logger.warn("Entry #{}: no target biomes for {}, skipping", index, entityClass);
				continue;
			}

			if (remove.equals("specified"))
			{
				EntityRegistry.removeSpawn(entityClass, EnumCreatureType.MONSTER, targetBiomes);
				logger.info("Entry #{}: Removed {} specified biomes for {}", index, targetBiomes.length, entityClass);
			}

			if (canAddSpawn)
			{
				EntityRegistry.addSpawn(
					entityClass,
					weight,
					group_size.get(0),
					group_size.get(1),
					EnumCreatureType.MONSTER,
					targetBiomes);
				logger.info("Entry #{}: Added/updated {} biomes for {}", index, targetBiomes.length, entityClass);
			}
		}
	}
}
