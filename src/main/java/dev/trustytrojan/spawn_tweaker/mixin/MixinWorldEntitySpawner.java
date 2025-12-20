package dev.trustytrojan.spawn_tweaker.mixin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLiving.SpawnPlacementType;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.common.eventhandler.Event;

@Mixin(WorldEntitySpawner.class)
abstract class MixinWorldEntitySpawner
{
	@Shadow
	@Final
	private static int MOB_COUNT_DIV;

	@Shadow
	@Final
	private Set<ChunkPos> eligibleChunksForSpawning;

	@Unique
	private final List<ChunkPos> shuffledEligibleChunks = new ArrayList<>();

	@Shadow
	private static BlockPos getRandomChunkPosition(final World worldIn, final int x, final int z)
	{
		throw new AssertionError();
	}

	@Shadow
	public static boolean canCreatureTypeSpawnAtLocation(
		final SpawnPlacementType spawnPlacementTypeIn,
		final World worldIn,
		final BlockPos pos)
	{
		throw new AssertionError();
	}

	static class ExecutionTimer
	{
		String name;

		ExecutionTimer(String name)
		{
			this.name = name;
		}

		long sum, totalRuns, start;

		void start()
		{
			start = System.nanoTime();
		}

		void end()
		{
			final var elapsed = System.nanoTime() - start;
			sum += elapsed;
			++totalRuns;
			final var avg = sum / totalRuns;
			System.out.printf("%s: curr=%dus avg=%dus\n", name, elapsed / 1_000, avg / 1_000);
		}
	}

	private static final ExecutionTimer myAlgorithm = new ExecutionTimer("myAlgorithm"),
		original = new ExecutionTimer("original");

	@Inject(
		method = "findChunksForSpawning(Lnet/minecraft/world/WorldServer;ZZZ)I",
		at = @At("HEAD"),
		cancellable = true
	)
	private void beforeFindChunks(
		WorldServer ws,
		boolean spawnHostileMobs,
		boolean spawnPeacefulMobs,
		boolean spawnOnSetTickRate,
		CallbackInfoReturnable<Integer> cir)
	{
		if (ws.playerEntities.isEmpty())
			return;
		myAlgorithm.start();
		myAlgorithm(ws, spawnHostileMobs, spawnPeacefulMobs, spawnOnSetTickRate);
		myAlgorithm.end();
		original.start();
	}

	@Inject(
		method = "findChunksForSpawning(Lnet/minecraft/world/WorldServer;ZZZ)I",
		at = @At("RETURN")
	)
	private void afterFindChunks(
		WorldServer ws,
		boolean spawnHostileMobs,
		boolean spawnPeacefulMobs,
		boolean spawnOnSetTickRate,
		CallbackInfoReturnable<Integer> cir)
	{
		if (ws.playerEntities.isEmpty())
			return;
		original.end();
	}

	// /**
	// * @author trustytrojan
	// * @reason Delegate to myAlgorithm for simplified one-per-tick spawning
	// */
	// @Overwrite
	// public int findChunksForSpawning(
	// final WorldServer ws,
	// final boolean spawnHostileMobs,
	// final boolean spawnPeacefulMobs,
	// final boolean spawnOnSetTickRate)
	// {
	// if (ws.playerEntities.isEmpty())
	// return 0;
	// myAlgorithm.start();
	// myAlgorithm(ws, spawnHostileMobs, spawnPeacefulMobs, spawnOnSetTickRate);
	// myAlgorithm.end();
	// return 0;
	// }

	private static final MutableBlockPos mutablePos = new MutableBlockPos();

	/**
	 * My algorithm's philosophy: at 20 tps, spawning one pack per tick is completely sufficient to
	 * mimic natural spawning. Pick one random player, one random creature type, one chunk, and
	 * spawn one pack. This is dramatically simpler than vanilla's exhaustive per-player, per-chunk,
	 * per-type loops.
	 */
	@SuppressWarnings("deprecation")
	public void myAlgorithm(
		final WorldServer ws,
		final boolean spawnHostileMobs,
		final boolean spawnPeacefulMobs,
		final boolean spawnOnSetTickRate)
	{
		final var rand = ws.rand;

		// Gather eligible chunks
		eligibleChunksForSpawning.clear();
		for (final var player : ws.playerEntities)
		{
			if (player.isSpectator())
				continue;

			final var px = MathHelper.floor(player.posX / 16);
			final var pz = MathHelper.floor(player.posZ / 16);

			// @formatter:off
			// TODO: make hollow-square radius configurable
			for (var dx = 3; dx <= 8; ++dx)
			for (var dz = 3; dz <= 8; ++dz)
			for (var sx = -1; sx <= 1; sx += 2)
			for (var sz = -1; sz <= 1; sz += 2)
			{
				final var chunkPos = new ChunkPos(px + sx * dx, pz + sz * dz);
				if (!ws.getWorldBorder().contains(chunkPos))
					continue;
				final var playerChunkEntry = ws.getPlayerChunkMap().getEntry(chunkPos.x, chunkPos.z);
				if (playerChunkEntry == null || !playerChunkEntry.isSentToPlayers())
					continue;
				eligibleChunksForSpawning.add(chunkPos);
			}
			// @formatter:on
		}
		final var eligibleChunkCount = eligibleChunksForSpawning.size();
		if (eligibleChunkCount == 0)
			return;
		shuffledEligibleChunks.clear();
		shuffledEligibleChunks.addAll(eligibleChunksForSpawning);

		nextCreatureType:
		for (final var creatureType : EnumCreatureType.values())
		{
			if (creatureType.getPeacefulCreature() && !spawnPeacefulMobs)
				continue;
			if (!creatureType.getPeacefulCreature() && !spawnHostileMobs)
				continue;
			if (creatureType.getAnimal() && !spawnOnSetTickRate)
				continue;
			final var creatureTypeEntityCount = ws.countEntities(creatureType, true);
			final var maxCreatureTypeAllowed =
				creatureType.getMaxNumberOfCreature() * eligibleChunkCount / MOB_COUNT_DIV;
			if (creatureTypeEntityCount > maxCreatureTypeAllowed)
				continue;

			Collections.shuffle(shuffledEligibleChunks);

			for (final var chunk : shuffledEligibleChunks)
			{
				// Pick a random block in our chunk
				final var startPos = getRandomChunkPosition(ws, chunk.x, chunk.z);
				if (ws.getBlockState(startPos).isNormalCube())
					continue;
				var spawnedThisChunk = 0;

				for (int p = 0; p < 3; ++p)
				{
					// Forge PotentialSpawns event
					final var spawnEntry = ws.getSpawnListEntryForTypeAt(creatureType, startPos);
					if (spawnEntry == null)
						continue;
					final var placement =
						EntitySpawnPlacementRegistry.getPlacementForEntity(spawnEntry.entityClass);

					// TODO: Consider making this configurable: either respect biome ranges OR Forge
					final var packSize =
						MathHelper.getInt(rand, spawnEntry.minGroupCount, spawnEntry.maxGroupCount);

					// Persistent loop data
					var x = startPos.getX();
					var y = startPos.getY();
					var z = startPos.getZ();
					IEntityLivingData livingData = null;

					for (var i = 0; i < packSize; ++i)
					{
						// Have each entity's position in the pack branch off from the start
						x += rand.nextInt(6) - rand.nextInt(6);
						y += rand.nextInt(1) - rand.nextInt(1);
						z += rand.nextInt(6) - rand.nextInt(6);
						mutablePos.setPos(x, y, z);

						// Make sure it doesn't rain zombies one day
						if (!canCreatureTypeSpawnAtLocation(placement, ws, mutablePos))
							continue;

						final EntityLiving entity;
						try
						{
							entity = spawnEntry.newInstance(ws);
						}
						catch (final Exception e)
						{
							e.printStackTrace();
							break;
						}

						// This does the centering so we don't have to
						entity.moveToBlockPosAndAngles(mutablePos, rand.nextFloat() * 360, 0);

						// Collision check earlier than vanilla does to avoid calling twice
						if (!entity.isNotColliding())
						{
							entity.setDead();
							continue;
						}

						// Forge CheckSpawn event: where our spawn rules run
						final var checkSpawnResult =
							ForgeEventFactory.canEntitySpawn(entity, ws, x, y, z, false);
						final var shouldSpawn = checkSpawnResult == Event.Result.ALLOW
							|| (checkSpawnResult == Event.Result.DEFAULT
								&& entity.getCanSpawnHere());
						if (!shouldSpawn)
							continue;

						// Forge SpecialSpawn event
						// Despite Forge documentation saying so, the original code does *not*
						// cancel the spawn if the event was canceled; we will keep that behavior
						// for now.
						if (!ForgeEventFactory.doSpecialSpawn(entity, ws, x, y, z))
						{
							livingData = entity.onInitialSpawn(
								ws.getDifficultyForLocation(mutablePos),
								livingData);
						}

						// Finally, spawn the entity. This makes it *join* the world, firing Forge's
						// EntityJoinWorldEvent, where our join rules run.
						ws.spawnEntity(entity);
						++spawnedThisChunk;

						// Respect the creature type cap at all times!
						if (creatureTypeEntityCount + spawnedThisChunk > maxCreatureTypeAllowed)
							continue nextCreatureType;
					}
				}
			}
		}
	}
}
