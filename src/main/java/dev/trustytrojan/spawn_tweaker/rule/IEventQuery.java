package dev.trustytrojan.spawn_tweaker.rule;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.Event;

public interface IEventQuery<T extends Event>
{
	World getWorld(T e);
	BlockPos getPos(T e);
	BlockPos getValidBlockPos(T e);
	int getY(T e);
	Entity getEntity(T e);
}
