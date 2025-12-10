package dev.trustytrojan.spawn_tweaker.rule;

import java.util.regex.Pattern;

import dev.trustytrojan.spawn_tweaker.GlobUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.registry.EntityRegistry;

public final class RuleUtils
{
	private RuleUtils()
	{}

	public static boolean compareNumberCondition(final String expr, final double value)
	{
		if (expr == null || expr.isEmpty())
			return true;
		final var s = expr.trim();
		try
		{
			if (s.startsWith(">="))
				return value >= Double.parseDouble(s.substring(2).trim());
			if (s.startsWith("<="))
				return value <= Double.parseDouble(s.substring(2).trim());
			if (s.startsWith(">"))
				return value > Double.parseDouble(s.substring(1).trim());
			if (s.startsWith("<"))
				return value < Double.parseDouble(s.substring(1).trim());
			if (s.startsWith("="))
				return value == Double.parseDouble(s.substring(1).trim());
			return value == Double.parseDouble(s);
		}
		catch (final Exception e)
		{
			return false;
		}
	}

	public static Event.Result parseActionResult(final String s)
	{
		if (s == null)
			return null;
		final var ss = s.trim().toLowerCase();
		if (ss.equals("default") || ss.startsWith("def"))
			return Event.Result.DEFAULT;
		if (ss.equals("allow") || ss.equals("true"))
			return Event.Result.ALLOW;
		if (ss.equals("deny") || ss.equals("false"))
			return Event.Result.DENY;
		return null;
	}

	public static int countEntitiesMatching(final World world, final String registryName)
	{
		if (world == null || registryName == null)
			return 0;
		int count = 0;
		for (final var ent : world.loadedEntityList)
		{
			if (!(ent instanceof EntityLiving))
				continue;
			final Entity e = (Entity) ent;
			final var eReg = EntityRegistry.getEntry(e.getClass());
			if (eReg == null)
				continue;
			final var name = eReg.getRegistryName();
			if (name == null)
				continue;
			if (name.toString().equalsIgnoreCase(registryName))
				count++;
		}
		return count;
	}

	public static int countEntitiesMatchingInChunk(
		final World world,
		final String registryName,
		final int chunkX,
		final int chunkZ)
	{
		if (world == null || registryName == null)
			return 0;
		int count = 0;
		for (final var ent : world.loadedEntityList)
		{
			if (!(ent instanceof EntityLiving))
				continue;
			final Entity e = (Entity) ent;
			final var eReg = EntityRegistry.getEntry(e.getClass());
			if (eReg == null)
				continue;
			final var name = eReg.getRegistryName();
			if (name == null)
				continue;
			if (!name.toString().equalsIgnoreCase(registryName))
				continue;
			final var eChunkX = (int) Math.floor(e.posX / 16.0);
			final var eChunkZ = (int) Math.floor(e.posZ / 16.0);
			if (eChunkX == chunkX && eChunkZ == chunkZ)
				count++;
		}
		return count;
	}

	public static int countEntitiesMatchingMod(final World world, final String modGlob)
	{
		if (world == null || modGlob == null)
			return 0;
		final var pat = Pattern.compile(GlobUtils.globToRegex(modGlob));
		int count = 0;
		for (final var ent : world.loadedEntityList)
		{
			if (!(ent instanceof EntityLiving))
				continue;
			final Entity e = (Entity) ent;
			final var eReg = EntityRegistry.getEntry(e.getClass());
			if (eReg == null)
				continue;
			final var name = eReg.getRegistryName();
			if (name == null)
				continue;
			final var domain = name.toString().split(":")[0];
			if (pat.matcher(domain).matches())
				count++;
		}
		return count;
	}

	public static int countEntitiesMatchingModInChunk(
		final World world,
		final String modGlob,
		final int chunkX,
		final int chunkZ)
	{
		if (world == null || modGlob == null)
			return 0;
		final var pat = Pattern.compile(GlobUtils.globToRegex(modGlob));
		int count = 0;
		for (final var ent : world.loadedEntityList)
		{
			if (!(ent instanceof EntityLiving))
				continue;
			final Entity e = (Entity) ent;
			final var eReg = EntityRegistry.getEntry(e.getClass());
			if (eReg == null)
				continue;
			final var name = eReg.getRegistryName();
			if (name == null)
				continue;
			final var domain = name.toString().split(":")[0];
			if (!pat.matcher(domain).matches())
				continue;
			final var eChunkX = (int) Math.floor(e.posX / 16.0);
			final var eChunkZ = (int) Math.floor(e.posZ / 16.0);
			if (eChunkX == chunkX && eChunkZ == chunkZ)
				count++;
		}
		return count;
	}

	public static int countEntitiesMatchingGlob(final World world, final String glob)
	{
		if (world == null || glob == null)
			return 0;
		final var pat = Pattern.compile(GlobUtils.globToRegex(glob));
		int count = 0;
		for (final var ent : world.loadedEntityList)
		{
			if (!(ent instanceof EntityLiving))
				continue;
			final Entity e = (Entity) ent;
			final var eReg = EntityRegistry.getEntry(e.getClass());
			if (eReg == null)
				continue;
			final var name = eReg.getRegistryName();
			if (name == null)
				continue;
			if (pat.matcher(name.toString()).matches())
				count++;
		}
		return count;
	}

	public static int countEntitiesMatchingGlobInChunk(
		final World world,
		final String glob,
		final int chunkX,
		final int chunkZ)
	{
		if (world == null || glob == null)
			return 0;
		final var pat = Pattern.compile(GlobUtils.globToRegex(glob));
		int count = 0;
		for (final var ent : world.loadedEntityList)
		{
			if (!(ent instanceof EntityLiving))
				continue;
			final Entity e = (Entity) ent;
			final var eReg = EntityRegistry.getEntry(e.getClass());
			if (eReg == null)
				continue;
			final var name = eReg.getRegistryName();
			if (name == null)
				continue;
			if (!pat.matcher(name.toString()).matches())
				continue;
			final var eChunkX = (int) Math.floor(e.posX / 16.0);
			final var eChunkZ = (int) Math.floor(e.posZ / 16.0);
			if (eChunkX == chunkX && eChunkZ == chunkZ)
				count++;
		}
		return count;
	}
}
