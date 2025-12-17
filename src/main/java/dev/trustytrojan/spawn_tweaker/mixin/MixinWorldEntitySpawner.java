package dev.trustytrojan.spawn_tweaker.mixin;

import java.util.Set;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.entity.EntityLiving;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;

/**
 * Mixin for {@link WorldEntitySpawner} to override the natural spawning algorithm.
 * 
 * <p>
 * <b>WARNING:</b> This uses @Overwrite which completely replaces the original method. This approach is necessary
 * because the spawning logic is too complex for injectors, but it means this mod may conflict with other mods that
 * modify the same method.
 * </p>
 * 
 * <p>
 * <b>Maintenance:</b> This overwrite must be kept in sync with the target Minecraft version. If the vanilla
 * implementation changes, this method must be updated accordingly.
 * </p>
 * 
 * @see WorldEntitySpawner#findChunksForSpawning(WorldServer, boolean, boolean, boolean)
 */
@Mixin(WorldEntitySpawner.class)
public abstract class MixinWorldEntitySpawner
{
	@Shadow
	@Final
	private static int MOB_COUNT_DIV;

	@Shadow
	private Set<ChunkPos> eligibleChunksForSpawning;

	@Shadow
	protected static BlockPos getRandomChunkPosition(
		net.minecraft.world.World worldIn,
		int x,
		int z)
	{
		throw new AssertionError();
	}

	@Shadow
	public static boolean canCreatureTypeSpawnAtLocation(
		EntityLiving.SpawnPlacementType spawnPlacementTypeIn,
		net.minecraft.world.World worldIn,
		BlockPos pos)
	{
		throw new AssertionError();
	}

	@Overwrite
	public int findChunksForSpawning(
		WorldServer worldServerIn,
		boolean spawnHostileMobs,
		boolean spawnPeacefulMobs,
		boolean spawnOnSetTickRate)
	{
		throw new RuntimeException("exception from our mixin!!!!");
	}
}
