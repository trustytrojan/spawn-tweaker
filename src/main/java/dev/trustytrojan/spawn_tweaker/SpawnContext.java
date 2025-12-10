package dev.trustytrojan.spawn_tweaker;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.entity.living.LivingSpawnEvent.CheckSpawn;
import net.minecraftforge.fml.common.registry.EntityRegistry;

public class SpawnContext
{
	private final CheckSpawn event;

	public SpawnContext(final CheckSpawn event)
	{
		this.event = event;
	}

	public EntityLivingBase getEntity()
	{
		return event.getEntityLiving();
	}

	public ResourceLocation getEntityRl()
	{
		return EntityRegistry.getEntry(getEntity().getClass()).getRegistryName();
	}

	public World getWorld()
	{
		return event.getWorld();
	}

	public Chunk getChunk()
	{
		return getWorld().getChunk((int) getX() >> 4, (int) getZ() >> 4);
	}

	public int getDimension()
	{
		return getWorld().provider.getDimension();
	}

	public BlockPos getBlockPos()
	{
		return new BlockPos(getX(), getY(), getZ());
	}

	public float getX()
	{
		return event.getX();
	}

	public float getY()
	{
		return event.getY();
	}

	public float getZ()
	{
		return event.getZ();
	}

	public int getLightLevel()
	{
		return getWorld().getLight(getBlockPos());
	}
}
