package dev.trustytrojan.spawn_tweaker;

import java.util.HashMap;
import java.util.Map;

import dev.trustytrojan.spawn_tweaker.event.EntityCounterWorldEventListener;
import net.minecraft.entity.EntityLiving;
import net.minecraft.world.World;

public final class EntityCounter
{
	private EntityCounter()
	{}

	private static final Map<World, Map<Class<? extends EntityLiving>, Integer>> WORLD_TO_ENTITY_COUNT =
		new HashMap<>();

	public static void init(final World world)
	{
		if (WORLD_TO_ENTITY_COUNT.containsKey(world))
			return;
		WORLD_TO_ENTITY_COUNT.put(world, new HashMap<>());
		world.addEventListener(new EntityCounterWorldEventListener());
	}

	public static void registerSpawn(
		final World world,
		final Class<? extends EntityLiving> entityClass)
	{
		final var entityCountMap =
			WORLD_TO_ENTITY_COUNT.computeIfAbsent(world, w -> new HashMap<>());
		final var newCount = entityCountMap.getOrDefault(entityClass, 0) + 1;
		entityCountMap.put(entityClass, newCount);
	}

	public static void registerDespawn(
		final World world,
		final Class<? extends EntityLiving> entityClass)
	{
		final var entityCountMap =
			WORLD_TO_ENTITY_COUNT.computeIfAbsent(world, w -> new HashMap<>());
		final var newCount = Math.max(0, entityCountMap.getOrDefault(entityClass, 0) - 1);
		entityCountMap.put(entityClass, newCount);
	}

	public static int getCount(final World world, final Class<? extends EntityLiving> entityClass)
	{
		final var entityCountMap = WORLD_TO_ENTITY_COUNT.get(world);
		if (entityCountMap == null)
			return 0;
		return entityCountMap.getOrDefault(entityClass, 0);
	}

	public static int getCount(
		final World world,
		final Iterable<Class<? extends EntityLiving>> entityClasses)
	{
		final var entityCountMap = WORLD_TO_ENTITY_COUNT.get(world);
		if (entityCountMap == null)
			return 0;

		var sum = 0;
		for (final var entityClass : entityClasses)
		{
			if (entityClass == null)
				continue;
			sum += entityCountMap.getOrDefault(entityClass, 0);
		}
		return sum;
	}
}
