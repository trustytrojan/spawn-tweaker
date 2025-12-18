package dev.trustytrojan.spawn_tweaker.spawn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.common.eventhandler.Event.Result;

public class NaturalSpawnAlgorithm
{
	/**
	 * The radius/distance in chunks from the player to search for eligible chunks for spawning.
	 */
	private static final int ELIGIBLE_CHUNK_RADIUS = 7;

	/**
	 * The radius/distance in blocks from the player to prevent natural entity spawns in.
	 */
	private static final int PLAYER_SAFETY_RADIUS = 24;

	/**
	 * The radius/distance in blocks from world spawn to prevent natural entity spawns in.
	 */
	private static final int WORLD_SPAWN_SAFETY_RADIUS = 576;

	/**
	 * The number of attempts to try spawning an entity's maximum pack size in a chunk.
	 */
	private static final int SPAWN_PACK_ATTEMPTS = 3;

	/**
	 * The vanilla value is 17^2 = 289. Not sure if this has anything to do with the
	 * {@link #ELIGIBLE_CHUNK_RADIUS}.
	 */
	private static final int MOB_COUNT_DIV = 289;

	private static BlockPos getRandomBlockInChunk(final World worldIn, final int x, final int z)
	{
		final var chunk = worldIn.getChunk(x, z);
		final var i = x * 16 + worldIn.rand.nextInt(16);
		final var j = z * 16 + worldIn.rand.nextInt(16);
		final var k = MathHelper.roundUp(chunk.getHeight(new BlockPos(i, 0, j)) + 1, 16);
		final var l = worldIn.rand.nextInt(k > 0 ? k : chunk.getTopFilledSegment() + 16 - 1);
		return new BlockPos(i, l, j);
	}

	private final Set<ChunkPos> eligibleChunks = new HashSet<>();
	private WorldServer ws;
	private final List<ChunkPos> shuffledEligibleChunks = new ArrayList<>();
	private final MutableBlockPos mutableSpawnPos = new MutableBlockPos();

	public int run(
		final WorldServer ws,
		final boolean spawnHostileMobs,
		final boolean spawnPeacefulMobs,
		final boolean spawnOnSetTickRate)
	{
		this.ws = ws;
		collectEligibleChunks();

		var totalSpawned = 0;

		for (final var type : EnumCreatureType.values())
		{
			if (!shouldSpawnCreatureType(
				type,
				spawnHostileMobs,
				spawnPeacefulMobs,
				spawnOnSetTickRate))
				continue;

			shuffledEligibleChunks.clear();
			shuffledEligibleChunks.addAll(eligibleChunks);
			Collections.shuffle(shuffledEligibleChunks);

			for (final var chunkPos : shuffledEligibleChunks)
			{
				try
				{
					totalSpawned += spawnInChunk(type, chunkPos);
				}
				catch (final Exception e)
				{
					e.printStackTrace();
					break;
				}
			}
		}

		return totalSpawned;
	}

	private void collectEligibleChunks()
	{
		eligibleChunks.clear();

		for (final var player : ws.playerEntities)
		{
			if (player.isSpectator())
				continue;

			final int playerChunkX = MathHelper.floor(player.posX / 16.0D);
			final int playerChunkZ = MathHelper.floor(player.posZ / 16.0D);

			for (int dx = -ELIGIBLE_CHUNK_RADIUS; dx <= ELIGIBLE_CHUNK_RADIUS; ++dx)
			{
				for (int dz = -ELIGIBLE_CHUNK_RADIUS; dz <= ELIGIBLE_CHUNK_RADIUS; ++dz)
				{
					final var chunkPos = new ChunkPos(dx + playerChunkX, dz + playerChunkZ);

					if (isEligibleChunk(chunkPos))
						eligibleChunks.add(chunkPos);
				}
			}
		}
	}

	private boolean isEligibleChunk(final ChunkPos pos)
	{
		if (eligibleChunks.contains(pos) || !ws.getWorldBorder().contains(pos))
			return false;
		final var playerChunkEntry = ws.getPlayerChunkMap().getEntry(pos.x, pos.z);
		if (playerChunkEntry == null || !playerChunkEntry.isSentToPlayers())
			return false;
		return true;
	}

	private boolean shouldSpawnCreatureType(
		final EnumCreatureType type,
		final boolean spawnHostileMobs,
		final boolean spawnPeacefulMobs,
		final boolean spawnOnSetTickRate)
	{
		if (type.getPeacefulCreature() && !spawnPeacefulMobs)
			return false;
		if (!type.getPeacefulCreature() && !spawnHostileMobs)
			return false;
		if (type.getAnimal() && !spawnOnSetTickRate)
			return false;

		final int count = ws.countEntities(type, true);
		final int max = type.getMaxNumberOfCreature() * eligibleChunks.size() / MOB_COUNT_DIV;

		if (count > max)
			return false;

		return true;
	}

	private int spawnInChunk(final EnumCreatureType type, final ChunkPos pos) throws Exception
	{
		final var bp = getRandomBlockInChunk(ws, pos.x, pos.z);
		final float x = bp.getX(), y = bp.getY(), z = bp.getZ();

		if (ws.getBlockState(bp).isNormalCube())
			return 0;

		var spawnCount = 0;

		for (var i = 0; i < SPAWN_PACK_ATTEMPTS; ++i)
		{
			final var result = spawnPack(spawnCount, type, x, y, z);
			spawnCount = result.newSpawnCount;
			if (result.entityPackSizeExceeded)
				break;
		}

		return spawnCount;
	}

	private static class SpawnPackResult
	{
		boolean entityPackSizeExceeded;
		int newSpawnCount;

		SpawnPackResult(final int spawnedThisChunk)
		{
			this.newSpawnCount = spawnedThisChunk;
		}
	}

	/**
	 * @param spawnedThisChunk Current entity count in chunk
	 * @param type             Creature type
	 * @throws Exception if entity instantiation fails
	 * @returns {@code -1} if entity pack size exceeded, otherwise new entity count in chunk
	 */
	@SuppressWarnings("deprecation")
	private SpawnPackResult spawnPack(
		final int spawnedThisChunk,
		final EnumCreatureType type,
		float x,
		float y,
		float z) throws Exception
	{
		final int groupAttempts = MathHelper.ceil(Math.random() * 4.0D);
		Biome.SpawnListEntry entry = null;
		IEntityLivingData livingData = null;

		final var result = new SpawnPackResult(spawnedThisChunk);

		for (var i = 0; i < groupAttempts; ++i)
		{
			x += ws.rand.nextInt(6) - ws.rand.nextInt(6);
			y += ws.rand.nextInt(1) - ws.rand.nextInt(1);
			z += ws.rand.nextInt(6) - ws.rand.nextInt(6);

			mutableSpawnPos.setPos(x, y, z);
			final var sx = x + 0.5f;
			final var sz = z + 0.5f;

			if (anyPlayerInRange(sx, y, sz) || tooCloseToSpawn(sx, y, sz))
				continue;

			if (entry == null)
			{
				entry = ws.getSpawnListEntryForTypeAt(type, mutableSpawnPos);
				if (entry == null)
					break; // the group attempt
			}

			final var placement =
				EntitySpawnPlacementRegistry.getPlacementForEntity(entry.entityClass);

			if (!ws.canCreatureTypeSpawnHere(type, entry, mutableSpawnPos))
				continue;
			if (!WorldEntitySpawner.canCreatureTypeSpawnAtLocation(placement, ws, mutableSpawnPos))
				continue;

			final var entityLiving = entry.newInstance(ws);
			entityLiving.setLocationAndAngles(sx, y, sz, ws.rand.nextFloat() * 360.0F, 0.0F);

			final var canSpawn = ForgeEventFactory
				.canEntitySpawn(entityLiving, ws, (float) sx, (float) y, (float) sz, false);
			final var shouldSpawn = (canSpawn == Result.ALLOW
				|| canSpawn == Result.DEFAULT && entityLiving.getCanSpawnHere())
				&& entityLiving.isNotColliding();
			if (!shouldSpawn)
				continue;

			if (!ForgeEventFactory.doSpecialSpawn(entityLiving, ws, sx, y, sz))
			{
				livingData = entityLiving.onInitialSpawn(
					ws.getDifficultyForLocation(new BlockPos(entityLiving)),
					livingData);
			}

			// skip colliding check since it is enforced above
			ws.spawnEntity(entityLiving);

			if (++result.newSpawnCount >= ForgeEventFactory.getMaxSpawnPackSize(entityLiving))
			{
				result.entityPackSizeExceeded = true;
				break;
			}
		}

		return result;
	}

	private boolean anyPlayerInRange(final double x, final double y, final double z)
	{
		return ws.isAnyPlayerWithinRangeAt(x, y, z, PLAYER_SAFETY_RADIUS);
	}

	private boolean tooCloseToSpawn(final double x, final double y, final double z)
	{
		return ws.getSpawnPoint().distanceSq(x, y, z) < WORLD_SPAWN_SAFETY_RADIUS;
	}
}
