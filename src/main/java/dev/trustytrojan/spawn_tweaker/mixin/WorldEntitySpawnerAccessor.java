package dev.trustytrojan.spawn_tweaker.mixin;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldEntitySpawner;

@Mixin(WorldEntitySpawner.class)
public interface WorldEntitySpawnerAccessor
{
	// So CompiledRule can access it for counts
	@Accessor("eligibleChunksForSpawning")
	Set<ChunkPos> getEligibleChunks();
}
