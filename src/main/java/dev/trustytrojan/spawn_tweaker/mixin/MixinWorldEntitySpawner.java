package dev.trustytrojan.spawn_tweaker.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import dev.trustytrojan.spawn_tweaker.spawn.VanillaNaturalSpawnAlgorithm;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;

@Mixin(WorldEntitySpawner.class)
final class MixinWorldEntitySpawner
{
	@Unique
	private final VanillaNaturalSpawnAlgorithm naturalSpawnAlgorithm = new VanillaNaturalSpawnAlgorithm();

	@Overwrite
	public int findChunksForSpawning(
		final WorldServer worldServer,
		final boolean spawnHostileMobs,
		final boolean spawnPeacefulMobs,
		final boolean spawnOnSetTickRate)
	{
		if (!spawnHostileMobs && !spawnPeacefulMobs)
			return 0;

		return naturalSpawnAlgorithm
			.run(worldServer, spawnHostileMobs, spawnPeacefulMobs, spawnOnSetTickRate);
	}
}
