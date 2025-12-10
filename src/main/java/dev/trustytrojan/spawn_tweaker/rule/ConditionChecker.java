package dev.trustytrojan.spawn_tweaker.rule;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.trustytrojan.spawn_tweaker.GlobUtils;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.registry.EntityRegistry;

public final class ConditionChecker<E extends Event>
{
	private static final Logger logger = LogManager.getLogger();
	private final IEventQuery<E> eventQuery;
	private E event;

	public ConditionChecker(IEventQuery<E> eq)
	{
		eventQuery = eq;
	}

	public void setEvent(E e)
	{
		event = e;
	}

	public boolean dimensionMatches(int d)
	{
		return eventQuery.getWorld(event).provider.getDimension() == d;
	}

	// === Helpers translated from ConditionCheckers ===
	private boolean checkLiving(final Predicate<EntityLiving> predicate)
	{
		final var ent = eventQuery.getEntity(event);
		return ent instanceof EntityLiving && predicate.test((EntityLiving) ent);
	}

	private boolean checkRegistryName(final Predicate<ResourceLocation> predicate)
	{
		return checkLiving(ent ->
		{
			final var entry = EntityRegistry.getEntry(ent.getClass());
			if (entry == null)
				return false;
			final var name = entry.getRegistryName();
			return name != null && predicate.test(name);
		});
	}

	// === Selector checks ===
	public boolean dimension(final int dim)
	{
		return eventQuery.getWorld(event).provider.getDimension() == dim;
	}

	public boolean mod(final Object modParam)
	{
		return checkRegistryName(name ->
		{
			final var domain = name.toString().split(":")[0];
			if (modParam instanceof String)
				return domain.matches(GlobUtils.globToRegex((String) modParam));
			if (modParam instanceof List)
			{
				for (final var m : (List<?>) modParam)
					if (m != null && domain.matches(GlobUtils.globToRegex(m.toString())))
					return true;
			}
			return false;
		});
	}

	public boolean mobs(final Map<String, List<String>> mobs)
	{
		return checkRegistryName(name ->
		{
			final var full = name.toString();
			final var parts = full.split(":");
			final var domain = parts[0];
			final var path = parts.length > 1 ? parts[1] : "";
			final var allowed = mobs.get(domain);
			return allowed != null && allowed.contains(path);
		});
	}

	public boolean mob(final String mob)
	{
		return checkRegistryName(name -> name.toString().matches(GlobUtils.globToRegex(mob)));
	}

	public boolean health(final NumberCondition condition)
	{
		return checkLiving(ent -> RuleUtils.compareNumberCondition(condition, ent.getMaxHealth()));
	}

	// === Condition checks ===
	public boolean random(final double chance)
	{
		return eventQuery.getWorld(event).rand.nextDouble() <= chance;
	}

	public boolean light(final int min, final int max)
	{
		final var pos = eventQuery.getPos(event);
		final var light = eventQuery.getWorld(event).getLightFromNeighbors(pos);
		return light >= min && light <= max;
	}

	public boolean height(final NumberCondition condition)
	{
		final var y = eventQuery.getY(event);
		final var result = RuleUtils.compareNumberCondition(condition, y);
		logger.debug("    height check: y={}, condition={}, result={}", y, condition, result);
		return result;
	}

	public boolean count(final CountCondition condition, final Map<String, List<String>> mobs, final Object mod)
	{
		if (condition == null)
			return true;

		final var pos = eventQuery.getPos(event);
		final var chunkX = pos.getX() >> 4;
		final var chunkZ = pos.getZ() >> 4;
		final var world = eventQuery.getWorld(event);
		final var perChunk = condition.isPerChunk();
		final var entityCount = computeCountForSelector(world, mobs, mod, perChunk, chunkX, chunkZ);
		return RuleUtils.compareNumberCondition(condition, entityCount);
	}

	private int computeCountForSelector(
		final World world,
		final Map<String, List<String>> mobs,
		final Object mod,
		final boolean perChunk,
		final int chunkX,
		final int chunkZ)
	{
		if (mobs != null && !mobs.isEmpty())
		{
			return countByMobs(world, mobs, perChunk, chunkX, chunkZ);
		}
		if (mod != null)
		{
			return countByMod(world, mod, perChunk, chunkX, chunkZ);
		}
		return countAll(world, perChunk, chunkX, chunkZ);
	}

	private int countByMobs(
		final World world,
		final Map<String, List<String>> mobs,
		final boolean perChunk,
		final int chunkX,
		final int chunkZ)
	{
		int count = 0;
		for (final var domain : mobs.keySet())
		{
			final var list = mobs.get(domain);
			if (list == null)
				continue;
			for (final var path : list) count += countEntities(world, domain + ":" + path, perChunk, chunkX, chunkZ);
		}
		return count;
	}

	private int countByMod(
		final World world,
		final Object mod,
		final boolean perChunk,
		final int chunkX,
		final int chunkZ)
	{
		if (mod instanceof String)
			return countEntitiesByMod(world, (String) mod, perChunk, chunkX, chunkZ);
		if (mod instanceof List)
		{
			int total = 0;
			for (final var m : (List<?>) mod) if (m != null)
				total += countEntitiesByMod(world, m.toString(), perChunk, chunkX, chunkZ);
			return total;
		}
		return 0;
	}

	// Helper to reduce duplication
	private int countEntities(
		final World world,
		final String glob,
		final boolean perChunk,
		final int chunkX,
		final int chunkZ)
	{
		return perChunk
			? RuleUtils.countEntitiesMatchingGlobInChunk(world, glob, chunkX, chunkZ)
			: RuleUtils.countEntitiesMatchingGlob(world, glob);
	}

	private int countEntitiesByMod(
		final World world,
		final String mod,
		final boolean perChunk,
		final int chunkX,
		final int chunkZ)
	{
		return perChunk
			? RuleUtils.countEntitiesMatchingModInChunk(world, mod, chunkX, chunkZ)
			: RuleUtils.countEntitiesMatchingMod(world, mod);
	}

	private int countAll(final World world, final boolean perChunk, final int chunkX, final int chunkZ)
	{
		return perChunk
			? RuleUtils.countEntitiesMatchingGlobInChunk(world, "*:*", chunkX, chunkZ)
			: world.loadedEntityList.size();
	}
}
