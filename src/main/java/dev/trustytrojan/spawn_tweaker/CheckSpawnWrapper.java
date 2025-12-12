package dev.trustytrojan.spawn_tweaker;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.entity.living.LivingSpawnEvent.CheckSpawn;

public class CheckSpawnWrapper extends EntityEventWrapper<CheckSpawn>
{
	public CheckSpawnWrapper(final CheckSpawn event)
	{
		super(event);
	}

	@Override
	public World getWorld()
	{
		return event.getWorld();
	}

	@Override
	public Chunk getChunk()
	{
		return getWorld().getChunk((int) event.getX() >> 4, (int) event.getZ() >> 4);
	}

	@Override
	public BlockPos getPosition()
	{
		return new BlockPos(event.getX(), event.getY(), event.getZ());
	}

	@Override
	public int getHeight()
	{
		return (int) event.getY();
	}
}
