package dev.trustytrojan.spawn_tweaker.mixin;

import java.util.Collections;
import java.util.Random;
import java.util.Set;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.google.common.collect.Lists;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.common.eventhandler.Event;

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
	@Final
	private Set<ChunkPos> eligibleChunksForSpawning;

	@Shadow
	private static BlockPos getRandomChunkPosition(World worldIn, int x, int z)
	{
		throw new AssertionError();
	}

	@Shadow
	public static boolean canCreatureTypeSpawnAtLocation(
		EntityLiving.SpawnPlacementType spawnPlacementTypeIn,
		World worldIn,
		BlockPos pos)
	{
		throw new AssertionError();
	}

	@Unique
	private WorldServer st$worldServer;

	@Unique
	private Random st$wsRand;

	@Unique
	private int st$eligibleChunkCount;

	@Overwrite
	public int findChunksForSpawning(
		WorldServer worldServerIn,
		boolean spawnHostileMobs,
		boolean spawnPeacefulMobs,
		boolean spawnOnSetTickRate)
	{
		if (!spawnHostileMobs && !spawnPeacefulMobs)
			return 0;

		this.st$worldServer = worldServerIn;
		this.st$wsRand = worldServerIn.rand;

		spawnTweaker$collectEligibleChunks();

		var totalSpawned = 0;

		for (final var creatureType : EnumCreatureType.values())
		{
			if (creatureType.getPeacefulCreature() && !spawnPeacefulMobs)
				continue;
			if (!creatureType.getPeacefulCreature() && !spawnHostileMobs)
				continue;
			if (creatureType.getAnimal() && !spawnOnSetTickRate)
				continue;

			totalSpawned += spawnTweaker$spawnCreatureType(creatureType);
		}

		return totalSpawned;
	}

	@Unique
	private void spawnTweaker$collectEligibleChunks()
	{
		eligibleChunksForSpawning.clear();
		st$eligibleChunkCount = 0;

		for (final var player : st$worldServer.playerEntities)
		{
			if (player.isSpectator())
				continue;

			final var playerChunkX = MathHelper.floor(player.posX / 16.0D);
			final var playerChunkZ = MathHelper.floor(player.posZ / 16.0D);

			for (var dx = -8; dx <= 8; ++dx)
			{
				for (var dz = -8; dz <= 8; ++dz)
				{
					final var isBorderChunk = dx == -8 || dx == 8 || dz == -8 || dz == 8;
					final var chunkPos = new ChunkPos(dx + playerChunkX, dz + playerChunkZ);

					if (eligibleChunksForSpawning.contains(chunkPos))
						continue;

					++st$eligibleChunkCount;

					if (isBorderChunk)
						continue;
					if (!st$worldServer.getWorldBorder().contains(chunkPos))
						continue;

					final var playerChunkEntry =
						st$worldServer.getPlayerChunkMap().getEntry(chunkPos.x, chunkPos.z);
					if (playerChunkEntry != null && playerChunkEntry.isSentToPlayers())
						eligibleChunksForSpawning.add(chunkPos);
				}
			}
		}
	}

	@Unique
	private int spawnTweaker$spawnCreatureType(EnumCreatureType creatureType)
	{
		final var currentEntityCount = st$worldServer.countEntities(creatureType, true);
		final var maxEntities =
			creatureType.getMaxNumberOfCreature() * st$eligibleChunkCount / MOB_COUNT_DIV;
		if (currentEntityCount > maxEntities)
			return 0;

		final var shuffledEligibleChunks = Lists.newArrayList(eligibleChunksForSpawning);
		Collections.shuffle(shuffledEligibleChunks);

		var spawnedTotal = 0;

		for (final var eligibleChunk : shuffledEligibleChunks)
		{
			spawnedTotal += spawnTweaker$spawnInChunk(eligibleChunk, creatureType);
		}

		return spawnedTotal;
	}

	@Unique
	@SuppressWarnings("deprecation")
	private int spawnTweaker$spawnInChunk(ChunkPos eligibleChunk, EnumCreatureType creatureType)
	{
		final var randomChunkPos =
			getRandomChunkPosition(st$worldServer, eligibleChunk.x, eligibleChunk.z);
		final var baseX = randomChunkPos.getX();
		final var baseY = randomChunkPos.getY();
		final var baseZ = randomChunkPos.getZ();

		final var baseState = st$worldServer.getBlockState(randomChunkPos);
		if (baseState.isNormalCube())
			return 0;

		var spawnedThisChunk = 0;
		final var mutableSpawnPos = new BlockPos.MutableBlockPos();

		for (var packAttempt = 0; packAttempt < 3; ++packAttempt)
		{
			var sx = baseX;
			var sy = baseY;
			var sz = baseZ;
			Biome.SpawnListEntry spawnListEntry = null;
			IEntityLivingData livingData = null;
			final var groupAttempts = MathHelper.ceil(Math.random() * 4.0D);

			for (int groupAttempt = 0; groupAttempt < groupAttempts; ++groupAttempt)
			{
				sx += st$wsRand.nextInt(6) - st$wsRand.nextInt(6);
				sy += st$wsRand.nextInt(1) - st$wsRand.nextInt(1);
				sz += st$wsRand.nextInt(6) - st$wsRand.nextInt(6);

				mutableSpawnPos.setPos(sx, sy, sz);
				final var spawnX = (float) sx + 0.5F;
				final var spawnZ = (float) sz + 0.5F;

				if (st$worldServer
					.isAnyPlayerWithinRangeAt((double) spawnX, (double) sy, (double) spawnZ, 24.0D))
					continue;
				if (st$worldServer.getSpawnPoint()
					.distanceSq((double) spawnX, (double) sy, (double) spawnZ) < 576.0D)
					continue;

				if (spawnListEntry == null)
				{
					spawnListEntry =
						st$worldServer.getSpawnListEntryForTypeAt(creatureType, mutableSpawnPos);
					if (spawnListEntry == null)
						break;
				}

				final var placement =
					EntitySpawnPlacementRegistry.getPlacementForEntity(spawnListEntry.entityClass);
				if (!st$worldServer
					.canCreatureTypeSpawnHere(creatureType, spawnListEntry, mutableSpawnPos)
					|| !canCreatureTypeSpawnAtLocation(placement, st$worldServer, mutableSpawnPos))
					continue;

				final EntityLiving entityLiving;
				try
				{
					entityLiving = spawnListEntry.newInstance(st$worldServer);
				}
				catch (Exception exception)
				{
					exception.printStackTrace();
					return spawnedThisChunk;
				}

				entityLiving.setLocationAndAngles(
					(double) spawnX,
					(double) sy,
					(double) spawnZ,
					st$worldServer.rand.nextFloat() * 360.0F,
					0.0F);

				final var canSpawn = ForgeEventFactory
					.canEntitySpawn(entityLiving, st$worldServer, spawnX, sy, spawnZ, false);

				final var vanillaSpawnAllowed = (canSpawn == Event.Result.DEFAULT
					&& (entityLiving.getCanSpawnHere() && entityLiving.isNotColliding()));
				final var spawnAllowed = canSpawn == Event.Result.ALLOW || vanillaSpawnAllowed;
				if (!spawnAllowed)
					continue;

				if (!ForgeEventFactory
					.doSpecialSpawn(entityLiving, st$worldServer, spawnX, sy, spawnZ))
				{
					livingData = entityLiving.onInitialSpawn(
						st$worldServer.getDifficultyForLocation(new BlockPos(entityLiving)),
						livingData);
				}

				if (entityLiving.isNotColliding())
				{
					++spawnedThisChunk;
					st$worldServer.spawnEntity(entityLiving);
				}
				else
				{
					entityLiving.setDead();
				}

				if (spawnedThisChunk >= ForgeEventFactory.getMaxSpawnPackSize(entityLiving))
					return spawnedThisChunk;
			}
		}
		return spawnedThisChunk;
	}
}
