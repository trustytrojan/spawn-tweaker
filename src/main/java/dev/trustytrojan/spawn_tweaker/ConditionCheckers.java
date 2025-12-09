package dev.trustytrojan.spawn_tweaker;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import net.minecraft.entity.EntityLiving;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.EntityRegistry;

public final class ConditionCheckers
{
	private ConditionCheckers()
	{}

	// Selector checks (returning BiFunction<T, IEventQuery<T>, Boolean>)
	public static <T> BiFunction<T, IEventQuery<T>, Boolean> dimension(final int dim)
	{
		return (e, q) -> q.getWorld(e).provider.getDimension() == dim;
	}

	public static <T> BiFunction<T, IEventQuery<T>, Boolean> mod(final Object modParam)
	{
		return (e, q) ->
		{
			final var ent = q.getEntity(e);
			if (!(ent instanceof EntityLiving))
				return false;
			final var entry = EntityRegistry.getEntry(ent.getClass());
			if (entry == null || entry.getRegistryName() == null)
				return false;
			final var domain = entry.getRegistryName().toString().split(":")[0];
			if (modParam instanceof String)
				return domain.matches(GlobUtils.globToRegex((String) modParam));
			if (modParam instanceof List)
			{
				for (final var m : (List<?>) modParam)
					if (m != null && domain.matches(GlobUtils.globToRegex(m.toString())))
					return true;
			}
			return false;
		};
	}

	public static <T> BiFunction<T, IEventQuery<T>, Boolean> mobs(final Map<String, List<String>> mobs)
	{
		return (e, q) ->
		{
			final var ent = q.getEntity(e);
			if (!(ent instanceof EntityLiving))
				return false;
			final var entry = EntityRegistry.getEntry(ent.getClass());
			if (entry == null || entry.getRegistryName() == null)
				return false;
			final var full = entry.getRegistryName().toString();
			final var parts = full.split(":");
			final var domain = parts[0];
			final var path = parts.length > 1 ? parts[1] : "";
			final var allowed = mobs.get(domain);
			return allowed != null && allowed.contains(path);
		};
	}

	public static <T> BiFunction<T, IEventQuery<T>, Boolean> health(final String expr)
	{
		return (e, q) ->
		{
			final var ent = q.getEntity(e);
			if (!(ent instanceof EntityLiving))
				return false;
			return RuleUtils.compareNumberCondition(expr, ((EntityLiving) ent).getMaxHealth());
		};
	}

	// Condition checks (returning BiFunction<T, IEventQuery<T>, Boolean>) - true == pass, false == fail
	public static <T> BiFunction<T, IEventQuery<T>, Boolean> random(final double chance)
	{
		return (e, q) -> q.getWorld(e).rand.nextDouble() <= chance;
	}

	// healthCondition removed; use health(expr) for both selector and condition checks

	public static <T> BiFunction<T, IEventQuery<T>, Boolean> light(final int min, final int max)
	{
		return (e, q) ->
		{
			final var pos = q.getPos(e);
			final var light = q.getWorld(e).getLightFromNeighbors(pos);
			return light >= min && light <= max;
		};
	}

	public static <T> BiFunction<T, IEventQuery<T>, Boolean> height(final String expr)
	{
		return (e, q) -> RuleUtils.compareNumberCondition(expr, q.getY(e));
	}

	public static <T> BiFunction<T, IEventQuery<T>, Boolean> count(
		final String spec,
		final CheckSpawnRule.Selector selector)
	{
		return (e, q) ->
		{
			final var parts = spec.split(",");
			final var condStr = parts[0].trim();
			final var perChunk = parts.length > 1 && parts[1].trim().equalsIgnoreCase("perchunk");
			final var pos = q.getPos(e);
			final var chunkX = pos.getX() >> 4;
			final var chunkZ = pos.getZ() >> 4;
			final var world = q.getWorld(e);
			final var count = computeCountForSelector(world, selector, perChunk, chunkX, chunkZ);
			return RuleUtils.compareNumberCondition(condStr, count);
		};
	}

	private static int computeCountForSelector(
		final World world,
		final CheckSpawnRule.Selector selector,
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

	private static int countByMobs(
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

	private static int countByMod(
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

	private static int countAll(final World world, final boolean perChunk, final int chunkX, final int chunkZ)
	{
		return perChunk
			? RuleUtils.countEntitiesMatchingGlobInChunk(world, "*:*", chunkX, chunkZ)
			: world.loadedEntityList.size();
	}
}
