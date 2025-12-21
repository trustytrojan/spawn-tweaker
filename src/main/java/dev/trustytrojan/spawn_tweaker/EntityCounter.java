package dev.trustytrojan.spawn_tweaker;

import java.util.HashMap;
import java.util.Map;

import dev.trustytrojan.spawn_tweaker.event.VanillaWorldEventListener;
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
		world.addEventListener(new VanillaWorldEventListener());
	}

	public static void registerSpawn(
		final World world,
		final Class<? extends EntityLiving> entityClass)
	{
		final var entityCountMap = WORLD_TO_ENTITY_COUNT.get(world);
		if (entityCountMap == null)
			return;
		final var newCount = entityCountMap.getOrDefault(entityClass, 0) + 1;
		entityCountMap.put(entityClass, newCount);
	}

	public static void registerDespawn(
		final World world,
		final Class<? extends EntityLiving> entityClass)
	{
		final var entityCountMap = WORLD_TO_ENTITY_COUNT.get(world);
		if (entityCountMap == null)
			return;
		final var newCount = Math.max(0, entityCountMap.getOrDefault(entityClass, 0) - 1);
		entityCountMap.put(entityClass, newCount);
	}

	public static int getCount(final World world, final Class<? extends EntityLiving> entityClass)
	{
		final var entityCountMap = WORLD_TO_ENTITY_COUNT.get(world);
		if (entityCountMap == null)
			return 0;
		return entityCountMap.get(entityClass);
	}
}
