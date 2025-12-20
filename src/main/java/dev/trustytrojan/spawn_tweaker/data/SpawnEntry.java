package dev.trustytrojan.spawn_tweaker.data;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.trustytrojan.spawn_tweaker.Util;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.registry.EntityRegistry;

public class SpawnEntry
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
	private Biome[] resolvedBiomes;

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

	public void apply()
	{
		final var canAddSpawn = weight != null && group_size != null && group_size.size() >= 2;

		if (!canAddSpawn && remove == null)
		{
			logger.warn("Entry #{}: NOT applying: weight/group_size/remove missing", index);
			return;
		}

		logger.info("Entry #{}: Applying {}", index, this);
		resolvedBiomes = Util.resolveBiomes(biomes).toArray(Biome[]::new);

		if (resolvedBiomes.length == 0)
			logger.info(
				"Entry #{}: No biomes specified, defaulting to existing entity biomes",
				index);

		Util.resolveEntityClasses(mobs).forEach(this::processEntity);
	}

	private void processEntity(final Class<? extends EntityLiving> clazz)
	{
		final var currentBiomes = Util.getCurrentSpawnBiomes(clazz);

		if ("existing".equals(remove) && currentBiomes.length > 0)
		{
			EntityRegistry.removeSpawn(clazz, EnumCreatureType.MONSTER, currentBiomes);
			logger.info(
				"Entry #{}: Removed {} existing biomes for {}",
				index,
				currentBiomes.length,
				clazz);
		}

		final var targetBiomes = (resolvedBiomes.length > 0) ? resolvedBiomes : currentBiomes;

		if (targetBiomes.length == 0)
		{
			logger.warn("Entry #{}: no target biomes for {}, skipping", index, clazz);
			return;
		}

		if ("specified".equals(remove))
		{
			EntityRegistry.removeSpawn(clazz, EnumCreatureType.MONSTER, targetBiomes);
			logger.info(
				"Entry #{}: Removed {} specified biomes for {}, skipping add",
				index,
				targetBiomes.length,
				clazz);
			return;
		}

		EntityRegistry.addSpawn(
			clazz,
			weight,
			group_size.get(0),
			group_size.get(1),
			EnumCreatureType.MONSTER,
			targetBiomes);
	}
}
