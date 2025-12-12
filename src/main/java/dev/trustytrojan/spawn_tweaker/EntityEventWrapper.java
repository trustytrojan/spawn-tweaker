package dev.trustytrojan.spawn_tweaker;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;

public abstract class EntityEventWrapper<E extends EntityEvent>
{
	protected final E event;

	protected EntityEventWrapper(final E event)
	{
		this.event = event;
	}

	public abstract World getWorld();
	public abstract Chunk getChunk();
	public abstract BlockPos getPosition();
	public abstract int getHeight();

	public EntityLivingBase getEntity()
	{
		return (EntityLivingBase) event.getEntity();
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
