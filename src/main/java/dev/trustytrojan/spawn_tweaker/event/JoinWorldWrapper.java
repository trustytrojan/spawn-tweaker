package dev.trustytrojan.spawn_tweaker.event;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

public class JoinWorldWrapper extends EntityEventWrapper<EntityJoinWorldEvent>
{
	public JoinWorldWrapper(final EntityJoinWorldEvent event)
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
		return getWorld().getChunk(getPosition());
	}

	@Override
	public BlockPos getPosition()
	{
		return getEntity().getPosition();
	}

	@Override
	public int getHeight()
	{
		return getPosition().getY();
	}
}
