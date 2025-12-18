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

/**
 * A (hopefully) logically equivalent refactoring and breakdown of the vanilla natural spawning
 * algorithm found in {@link WorldEntitySpawner#findChunksForSpawning}.
 * 
 * Will be configurable as soon as I can assert it is as close to the original algorithm as
 * possible.
 * 
 * This will be kept logically equivalent as a means of preservation of vanilla behavior. Soon I
 * will write an algorithm that attempts to achieve the same spawning with less code.
 */
public final class VanillaNaturalSpawnAlgorithm
{
	/**
	 * The radius/distance in chunks from the player to search for eligible chunks for spawning.
	 */
	private static final int ELIGIBLE_CHUNK_RADIUS = 8;

	/**
	 * The radius/distance in blocks from the player to prevent natural entity spawns in.
	 */
	private static final int PLAYER_SAFETY_RADIUS = 24;

	/**
	 * The radius/distance in blocks from world spawn point to prevent natural entity spawns in.
	 */
	private static final int SPAWN_SAFETY_RADIUS = 576;

	/**
	 * The number of attempts to try spawning an entity's maximum pack size in a chunk.
	 */
	private static final int PACK_ATTEMPTS = 3;

	/**
	 * The maximum number of entities to spawn in a pack. Pack size is randomly chosen between [1,
	 * {@link #PACK_SIZE_MAX}].
	 */
	private static final int PACK_SIZE_MAX = 4;

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

	private WorldServer ws;

	/**
	 * Same as {@link WorldEntitySpawner#eligibleChunksForSpawning}.
	 */
	private final Set<ChunkPos> eligibleChunks = new HashSet<>();

	/**
	 * Same as {@code mutableSpawnPos} from original code, kept here to avoid reallocation.
	 */
	private final List<ChunkPos> shuffledEligibleChunks = new ArrayList<>();

	/**
	 * Same as {@code mutableSpawnPos} from original code, kept here to avoid reallocation.
	 */
	private final MutableBlockPos mutableSpawnPos = new MutableBlockPos();

	/**
	 * Needed to communicate from {@link #spawnPack} to {@link #spawnInChunk} without convoluted
	 * return semantics.
	 */
	private boolean packSizeExceeded;

	/**
	 * Run the algorithm. The arguments are the same as that of
	 * {@link WorldEntitySpawner#findChunksForSpawning}.
	 * 
	 * @return Total number of entities spawned.
	 */
	public int run(
		final WorldServer ws,
		final boolean spawnHostileMobs,
		final boolean spawnPeacefulMobs,
		final boolean spawnOnSetTickRate)
	{
		this.ws = ws;
		final var mobCapChunkCount = collectEligibleChunks();
		shuffledEligibleChunks.clear();
		shuffledEligibleChunks.addAll(eligibleChunks);

		var totalSpawned = 0;

		for (final var type : EnumCreatureType.values())
		{
			if (!shouldSpawnCreatureType(
				type,
				mobCapChunkCount,
				spawnHostileMobs,
				spawnPeacefulMobs,
				spawnOnSetTickRate))
				continue;

			Collections.shuffle(shuffledEligibleChunks);

			for (final var chunkPos : shuffledEligibleChunks)
			{
				/**
				 * Instead of catching the entity instantiation exception at the pack-spawning loop
				 * and using convoluted return semantics to abort the entire algorithm, let it throw
				 * up here.
				 */
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

	/**
	 * Collects eligible chunks for spawning into {@link #eligibleChunks}, clearing it beforehand.
	 * 
	 * @return The number of "loaded chunks" to calculate mob caps with. This is <b>not</b> the same
	 *         as the size of {@link #eligibleChunks}.
	 */
	private int collectEligibleChunks()
	{
		eligibleChunks.clear();
		var mobCapChunkCount = 0;

		for (final var player : ws.playerEntities)
		{
			if (player.isSpectator())
				continue;

			final var playerChunkX = MathHelper.floor(player.posX / 16);
			final var playerChunkZ = MathHelper.floor(player.posZ / 16);

			for (var dx = -ELIGIBLE_CHUNK_RADIUS; dx <= ELIGIBLE_CHUNK_RADIUS; ++dx)
			{
				for (var dz = -ELIGIBLE_CHUNK_RADIUS; dz <= ELIGIBLE_CHUNK_RADIUS; ++dz)
				{
					final var chunkPos = new ChunkPos(playerChunkX + dx, playerChunkZ + dz);

					if (eligibleChunks.contains(chunkPos))
						continue;

					/**
					 * I don't know why the chunk count for mob caps counts more than what is
					 * actually used for spawning, but to preserve vanilla behavior as much as
					 * possible, I'm leaving this as is.
					 */
					++mobCapChunkCount;

					if (isEligibleChunkRadiusBorder(dx, dz))
						continue;

					if (isEligibleChunk(chunkPos))
						eligibleChunks.add(chunkPos);
				}
			}
		}

		return mobCapChunkCount;
	}

	private static boolean isEligibleChunkRadiusBorder(final int dx, final int dz)
	{
		return dx == ELIGIBLE_CHUNK_RADIUS || dx == -ELIGIBLE_CHUNK_RADIUS
			|| dz == ELIGIBLE_CHUNK_RADIUS || dz == -ELIGIBLE_CHUNK_RADIUS;
	}

	private boolean isEligibleChunk(final ChunkPos pos)
	{
		if (!ws.getWorldBorder().contains(pos))
			return false;
		final var playerChunkEntry = ws.getPlayerChunkMap().getEntry(pos.x, pos.z);
		if (playerChunkEntry == null || !playerChunkEntry.isSentToPlayers())
			return false;
		return true;
	}

	private boolean shouldSpawnCreatureType(
		final EnumCreatureType type,
		final int mobCapChunkCount,
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
		final int max = type.getMaxNumberOfCreature() * mobCapChunkCount / MOB_COUNT_DIV;

		/**
		 * Vanilla algorithm pitfall: this is only checked once, and never again, even during entity
		 * spawning!
		 */
		if (count > max)
			return false;

		return true;
	}

	/**
	 * Begin spawning entities in the specified chunk. Entities are spawned in <i>packs</i>.
	 * <p>
	 * If an entity's configured <i>max pack size</i> is <b>not</b> hit/exceeded during pack
	 * spawning, multiple packs of different entities <b>can</b> spawn. Otherwise, spawning for this
	 * chunk ends early.
	 * 
	 * @param type Creature type
	 * @param pos  Chunk position
	 * @return Number of entities spawned in the chunk
	 * @throws Exception if entity instantiation fails
	 */
	private int spawnInChunk(final EnumCreatureType type, final ChunkPos pos) throws Exception
	{
		final var bp = getRandomBlockInChunk(ws, pos.x, pos.z);
		final float x = bp.getX(), y = bp.getY(), z = bp.getZ();

		if (ws.getBlockState(bp).isNormalCube())
			return 0;

		var spawnCount = 0;

		for (var i = 0; i < PACK_ATTEMPTS; ++i)
		{
			/**
			 * We unfortunately need to resort to a global boolean as the original code used a
			 * labeled continue statement. This lets our return values consistently represent the
			 * numbers of entities spawned.
			 */
			packSizeExceeded = false;
			spawnCount += spawnPack(type, x, y, z);
			if (packSizeExceeded)
				break;
		}

		return spawnCount;
	}

	/**
	 * Probably the most complex part of the algorithm: spawn a "pack" of the same entity.
	 * 
	 * @param type Creature type
	 * @param x    Starting X coordinate
	 * @param y    Starting Y coordinate
	 * @param z    Starting Z coordinate
	 * @throws Exception if entity instantiation fails
	 * @return Number of entities spawned
	 */
	@SuppressWarnings("deprecation")
	private int spawnPack(final EnumCreatureType type, float x, float y, float z) throws Exception
	{
		final var packSize = MathHelper.ceil(Math.random() * PACK_SIZE_MAX);
		Biome.SpawnListEntry entry = null;
		IEntityLivingData livingData = null;

		var spawnCount = 0;

		for (var i = 0; i < packSize; ++i)
		{
			x += ws.rand.nextInt(6) - ws.rand.nextInt(6);
			y += ws.rand.nextInt(1) - ws.rand.nextInt(1);
			z += ws.rand.nextInt(6) - ws.rand.nextInt(6);
			mutableSpawnPos.setPos(x, y, z);

			// center the entity on the block, use these as actual spawn coordinates
			final var sx = x + 0.5f;
			final var sz = z + 0.5f;

			if (anyPlayerInRange(sx, y, sz) || tooCloseToSpawn(sx, y, sz))
				continue;

			if (entry == null)
			{
				entry = ws.getSpawnListEntryForTypeAt(type, mutableSpawnPos);
				if (entry == null)
					break;
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
			final var shouldSpawn = canSpawn == Result.ALLOW || (canSpawn == Result.DEFAULT
				&& entityLiving.getCanSpawnHere() && entityLiving.isNotColliding());
			if (!shouldSpawn)
				continue;

			if (!ForgeEventFactory.doSpecialSpawn(entityLiving, ws, sx, y, sz))
			{
				livingData = entityLiving.onInitialSpawn(
					ws.getDifficultyForLocation(new BlockPos(entityLiving)),
					livingData);
			}

			if (entityLiving.isNotColliding())
			{
				++spawnCount;
				ws.spawnEntity(entityLiving);
			}
			else
			{
				entityLiving.setDead();
			}

			if (spawnCount >= ForgeEventFactory.getMaxSpawnPackSize(entityLiving))
			{
				// unfortunately no better way to do this than with global state,
				// since original code did a labeled continue statement on the chunk loop.
				packSizeExceeded = true;
				break;
			}
		}

		return spawnCount;
	}

	private boolean anyPlayerInRange(final double x, final double y, final double z)
	{
		return ws.isAnyPlayerWithinRangeAt(x, y, z, PLAYER_SAFETY_RADIUS);
	}

	private boolean tooCloseToSpawn(final double x, final double y, final double z)
	{
		return ws.getSpawnPoint().distanceSq(x, y, z) < SPAWN_SAFETY_RADIUS;
	}
}
