package dev.trustytrojan.spawn_tweaker.event;

import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.registry.EntityRegistry;

public abstract class EntityEventWrapper<E extends EntityEvent>
{
	protected final E event;

	protected EntityEventWrapper(final E event)
	{
		this.event = event;
	}

	public void setResult(final Result result)
	{
		event.setResult(result);
	}

	public abstract WorldServer getWorld();
	public abstract Chunk getChunk();
	public abstract BlockPos getPosition();
	public abstract int getHeight();

	public EntityLiving getEntity()
	{
		return (EntityLiving) event.getEntity();
	}

	public ResourceLocation getEntityRl()
	{
		return EntityRegistry.getEntry(getEntity().getClass()).getRegistryName();
	}

	public int getDimension()
	{
		return getWorld().provider.getDimension();
	}

	public int getLightLevel()
	{
		return getWorld().getLight(getPosition());
	}
}
