package dev.trustytrojan.spawn_tweaker.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;

@Mixin(WorldServer.class)
public interface WorldServerAccessor
{
	@Accessor("entitySpawner")
	WorldEntitySpawner getEntitySpawner();
}
