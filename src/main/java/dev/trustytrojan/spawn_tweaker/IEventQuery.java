package dev.trustytrojan.spawn_tweaker;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface IEventQuery<T>
{
	World getWorld(T e);
	BlockPos getPos(T e);
	BlockPos getValidBlockPos(T e);
	int getY(T e);
	Entity getEntity(T e);
	DamageSource getSource(T e);
	Entity getAttacker(T e);
	EntityPlayer getPlayer(T e);
	ItemStack getItem(T e);
}
