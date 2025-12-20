package dev.trustytrojan.spawn_tweaker.mixin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.entity.EntityLiving;
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
final class MixinWorldEntitySpawner
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
		throw new AssertionError("Mixin shadow");
	}

	/* CODE FOR BENCHMARKING AGAINST VANILLA */
	/* @formatter:off
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
	@formatter:on */

	/**
	 * @author trustytrojan
	 * @reason Delegate to my spawn algorithm
	 * @return Zero, since {@link WorldServer#tick} doesn't do anything with the return value
	 */
	@Overwrite
	public int findChunksForSpawning(
		final WorldServer ws,
		final boolean spawnHostileMobs,
		final boolean spawnPeacefulMobs,
		final boolean spawnOnSetTickRate)
	{
		myAlgorithm(ws, spawnHostileMobs, spawnPeacefulMobs, spawnOnSetTickRate);
		return 0;
	}

	private static final MutableBlockPos mutablePos = new MutableBlockPos();

	@SuppressWarnings("deprecation")
	public void myAlgorithm(
		final WorldServer ws,
		final boolean spawnHostileMobs,
		final boolean spawnPeacefulMobs,
		final boolean spawnOnSetTickRate)
	{
		if (ws.playerEntities.isEmpty())
			return;

		final var rand = ws.rand;

		// Gather eligible chunks.
		// Same idea as vanilla, but save time by looking at a "hollow-square" radius around each
		// player. This lets us safely skip "too-close-to-player" and "too-close-to-spawn" checks in
		// the entity spawning loop.
		eligibleChunksForSpawning.clear();
		for (final var player : ws.playerEntities)
		{
			if (player.isSpectator())
				continue;

			final var px = MathHelper.floor(player.posX / 16);
			final var pz = MathHelper.floor(player.posZ / 16);

			// @formatter:off
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

		// Creature type loop
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

			// Chunk loop
			nextChunk:
			for (final var chunk : shuffledEligibleChunks)
			{
				// Pick a random block in our chunk
				final var startPos = getRandomChunkPosition(ws, chunk.x, chunk.z);
				if (ws.getBlockState(startPos).isNormalCube())
					continue;
				var spawnedThisChunk = 0;

				// Pack attempt loop (more like: spawn entry picking loop)
				for (int p = 0; p < 3; ++p)
				{
					// Fire a Forge PotentialSpawns event.
					// Get a spawn entry *before* entering the entity spawning loop.
					// Vanilla for some reason delays this until after finding a suitable block
					// in the entity spawning loop, and breaks it if the entry is null.
					// What we do here is logically equivalent with less code.
					final var spawnEntry = ws.getSpawnListEntryForTypeAt(creatureType, startPos);
					if (spawnEntry == null)
						continue;
					final var placement =
						EntitySpawnPlacementRegistry.getPlacementForEntity(spawnEntry.entityClass);

					// canCreatureTypeSpawnAtLocation() wastes 2 whole calls doing nothing.
					// canCreatureTypeSpawnBody() directly contains the desired logic.
					if (!WorldEntitySpawner.canCreatureTypeSpawnBody(placement, ws, startPos))
						continue;

					// Use the spawn entry's group sizes. Newer minecraft versions do this;
					// 1.12.2 just picks a random integer in [1, 4].
					final var packSize =
						MathHelper.getInt(rand, spawnEntry.minGroupCount, spawnEntry.maxGroupCount);

					// Persistent loop data
					var x = startPos.getX();
					var y = startPos.getY();
					var z = startPos.getZ();
					IEntityLivingData livingData = null;

					// Entity spawning loop
					for (var s = 0; s < packSize; ++s)
					{
						// Have each entity's position in the pack branch off from the start
						x += rand.nextInt(6) - rand.nextInt(6);
						// newer minecraft versions don't vary Y... maybe we should mimic that
						y += rand.nextInt(1) - rand.nextInt(1);
						z += rand.nextInt(6) - rand.nextInt(6);
						mutablePos.setPos(x, y, z);

						// Removed the redundant ws.canCreatureTypeSpawnHere() call.
						// It just checks if the passed in spawn entry is in the biome's list,
						// which we know is true, since we got it from the same list...
						// This saves a whole PotentialSpawns event from running.

						// Moved the canCreatureTypeSpawnAtLocation() call next to spawnEntry.
						// If startPos isn't valid, why waste time randomly searching for a valid
						// block when it's likely the whole chunk is invalid?

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

						// "Max spawn pack size" actually means "Limit per chunk".
						// Vanilla skips to the next chunk if just *one* entity type reaches its
						// chunk limit. Meaning there can be 3 different kinds of mobs in the same
						// chunk. We will keep this behavior.
						if (spawnedThisChunk >= ForgeEventFactory.getMaxSpawnPackSize(entity))
							continue nextChunk;

						// Respect the creature type cap at all times!
						if (creatureTypeEntityCount + spawnedThisChunk > maxCreatureTypeAllowed)
							continue nextCreatureType;
					}
				}
			}
		}
	}
}
