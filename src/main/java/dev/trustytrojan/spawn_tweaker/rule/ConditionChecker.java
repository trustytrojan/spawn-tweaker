package dev.trustytrojan.spawn_tweaker.rule;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import dev.trustytrojan.spawn_tweaker.GlobUtils;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.registry.EntityRegistry;

public final class ConditionChecker<E extends Event>
{
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
			final var domain = name.toString().split(":" )[0];
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
			final var parts = full.split(":" );
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

	public boolean health(final String expr)
	{
		return checkLiving(ent -> RuleUtils.compareNumberCondition(expr, ent.getMaxHealth()));
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

	public boolean height(final String expr)
	{
		return RuleUtils.compareNumberCondition(expr, eventQuery.getY(event));
	}

	public boolean count(final String spec, final Rule.Selector selector)
	{
		final var parts = spec.split(",");
		final var condStr = parts[0].trim();
		final var perChunk = parts.length > 1 && parts[1].trim().equalsIgnoreCase("perchunk");
		final var pos = eventQuery.getPos(event);
		final var chunkX = pos.getX() >> 4;
		final var chunkZ = pos.getZ() >> 4;
		final var world = eventQuery.getWorld(event);
		final var count = computeCountForSelector(world, selector, perChunk, chunkX, chunkZ);
		return RuleUtils.compareNumberCondition(condStr, count);
	}

	private int computeCountForSelector(
		final World world,
		final Rule.Selector selector,
		final boolean perChunk,
		final int chunkX,
		final int chunkZ)
	{
		if (selector != null && selector.mobs != null && !selector.mobs.isEmpty())
		{
			return countByMobs(world, selector.mobs, perChunk, chunkX, chunkZ);
		}
		if (selector != null && selector.mod != null)
		{
			return countByMod(world, selector.mod, perChunk, chunkX, chunkZ);
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
			for (final var path : list)
			{
				final var full = domain + ":" + path;
				count += perChunk
					? RuleUtils.countEntitiesMatchingGlobInChunk(world, full, chunkX, chunkZ)
					: RuleUtils.countEntitiesMatchingGlob(world, full);
			}
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
		{
			final var modStr = (String) mod;
			return perChunk
				? RuleUtils.countEntitiesMatchingModInChunk(world, modStr, chunkX, chunkZ)
				: RuleUtils.countEntitiesMatchingMod(world, modStr);
		}
		int total = 0;
		if (mod instanceof List)
		{
			for (final var m : (List<?>) mod)
			{
				if (m == null)
					continue;
				final var modStr = m.toString();
				total += perChunk
					? RuleUtils.countEntitiesMatchingModInChunk(world, modStr, chunkX, chunkZ)
					: RuleUtils.countEntitiesMatchingMod(world, modStr);
			}
		}
		return total;
	}

	private int countAll(final World world, final boolean perChunk, final int chunkX, final int chunkZ)
	{
		return perChunk
			? RuleUtils.countEntitiesMatchingGlobInChunk(world, "*:*", chunkX, chunkZ)
			: world.loadedEntityList.size();
	}
}
