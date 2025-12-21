# Custom Spawn Algorithm

This mod replaces Minecraft 1.12.2's `WorldEntitySpawner#findChunksForSpawning` algorithm with a custom implementation (`myAlgorithm`) that keeps Forge behavior intact while changing *where* work happens and what is considered “eligible” earlier.

The goals are:

- Respect biome spawn entry group sizes (`minGroupCount` / `maxGroupCount`).
- Reduce wasted work (fewer redundant checks and fewer expensive operations on positions that can’t possibly spawn).
- Incorporate spawning ideas from newer Minecraft versions where practical.

## Configuration

The algorithm uses the following runtime-tunable knobs from [src/main/java/dev/trustytrojan/spawn_tweaker/SpawnAlgorithmConfig.java](src/main/java/dev/trustytrojan/spawn_tweaker/SpawnAlgorithmConfig.java):

- `spawnRadiusRange = {min, max}`: controls the hollow-square ring of chunk distances around each player.
- `packAttempts`: number of “pack attempts” per chunk (vanilla uses 3).
- `packEntityMaxDistance`: maximum random-walk distance per entity from the pack origin (vanilla effectively uses 5).
- `varyY`: whether the pack random-walk varies Y (newer versions do not; vanilla 1.12.2 varies Y by ±0).

## Algorithm Overview

The algorithm is structured as three nested loops:

1. Creature type loop (`EnumCreatureType`)
2. Eligible chunk loop (shuffled)
3. Pack attempt loop, then per-entity loop within the chosen pack size

### Step 0: Early exit

If there are no players, do nothing.

### Step 1: Gather eligible chunks (player-centric ring)

For every non-spectator player:

- Convert the player’s position to a chunk coordinate `(px, pz)`.
- Add chunks that fall in a *hollow square ring* around the player:
	- `abs(dx)` in `[min..max]` and `abs(dz)` in `[min..max]`.
	- Only chunks inside the world border and “sent to players” via the `PlayerChunkMap` are considered.

This differs from vanilla, which scans a full `(17 x 17)` square and then filters by edge flags and later distance checks.

### Step 2: Creature type gating (caps & tick-rate flags)

For each `EnumCreatureType`:

- Respect the vanilla booleans (`spawnHostileMobs`, `spawnPeacefulMobs`, `spawnOnSetTickRate`).
- Count entities of that creature type in the world.
- Compute the maximum allowed count based on the eligible chunk count (same formula as vanilla):

	`maxAllowed = type.getMaxNumberOfCreature() * eligibleChunkCount / MOB_COUNT_DIV`

If the world is already over cap, skip that creature type.

Note: In vanilla 1.12.2, spawning is invoked from `WorldServer#tick()` every tick when `doMobSpawning` is enabled. The `spawnOnSetTickRate` flag is only `true` once every 400 ticks, which affects `EnumCreatureType#getAnimal()` spawns.

### Step 3: Chunk iteration

Shuffle the eligible chunk list and iterate.

For each chunk:

- Choose one random block position in the chunk via `getRandomChunkPosition`.
- If that block is a “normal cube” (solid), skip the chunk.

### Step 4: Pack attempt loop (spawn entry selection)

For `packAttempts` iterations:

- Select the spawn list entry **before** spawning entities: `ws.getSpawnListEntryForTypeAt(creatureType, startPos)`.
	- If null, continue.
- Resolve spawn placement: `EntitySpawnPlacementRegistry.getPlacementForEntity(entry.entityClass)`.
- Determine the pack size using the entry’s configured group sizes:

	`packSize = randInt(minGroupCount..maxGroupCount)`

This mirrors modern behavior and fixes vanilla 1.12.2’s hard-coded `1..4` pack size.

### Step 5: Entity loop (random walk + spawn checks)

For each entity in the pack:

- Random-walk the spawn position from the pack origin (bounded by `packEntityMaxDistance`).
- Check block validity via `WorldEntitySpawner.canCreatureTypeSpawnBody(placement, world, pos)`.
	- This consolidates the “body” checks (ground block + headroom) in one place.
	- Note: in 1.12.2 this method allocates temporary `BlockPos` objects internally (`pos.up()` / `pos.down()`).
- Instantiate the entity from the spawn entry.
- Position/orient it.
- Early collision check (`isNotColliding()`).
- Fire Forge `CheckSpawn` and then `SpecialSpawn` (keeping Forge semantics consistent with vanilla).
- Spawn into the world (`spawnEntity`).
- Respect Forge’s “max spawn pack size” as a **per-chunk limit** (same behavior as vanilla).

## Differences From Vanilla 1.12.2 (Intentional)

### Chunk eligibility is stricter and earlier

Vanilla uses a broad chunk scan and then relies on per-candidate distance checks (`isAnyPlayerWithinRangeAt` and spawn-point distance) later.

This algorithm instead selects a ring around players up-front, which is intended to:

- Avoid spawning too close to players.
- Avoid spending time on “obviously invalid” chunks.
- Reduce the need for later distance checks.

### Spawn entry group sizes are respected

Vanilla ignores `minGroupCount`/`maxGroupCount` and uses a random size in `[1..4]`.
This algorithm uses the biome entry’s configured group sizes.

### Redundant checks are removed

- The `canCreatureTypeSpawnHere` check is not repeated after selecting the entry from the same list.
- The “body” spawnability check is done prior to entity creation to avoid costly instantiation when the position can’t ever work.

## Notes & Caveats

- Because `canCreatureTypeSpawnBody` allocates in 1.12.2, it can be a hotspot in “all-valid” terrains (e.g., superflat). Consider profiling with a sampler if superflat is a primary target.
- The cap guard is only as accurate as the initial `countEntities` snapshot plus entities spawned by this algorithm during the pass.

