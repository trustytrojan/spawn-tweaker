# Spawn Tweaker
Spawn Tweaker is a successor to [McJty/InControl](https://github.com/McJtyMods/InControl) for Forge 1.12.2, since 1.12.2 was abandoned by the developer.

Spawn Tweaker allows you to:
  - Modify spawn weight and minimum/maximum group size for any modded mobs classifying as monsters in any biomes
  - Hot reload spawn entries during world execution
  - Control mob spawns with a complex rule system

## In-Game Commands
Commands can only be run by server operators/admins.

- `/spawntweaker reload`
  - Reloads all files from `config/spawn_tweaker`. This includes both rules and entries.

- `/spawntweaker killall`
  - *Removes* all monsters from the world, which is quicker and more performant than killing them.

## Configuration Files

Spawn Tweaker uses **YAML** (`.yml`) files for configuration.
Configuration files are located in the `config/spawn_tweaker/` folder in your game directory.

There are two main configuration files:
1. `entries.yml` - For modifying spawn weights and group sizes of existing spawns.
2. `rules.yml` - For advanced control logic (allow/deny spawns based on conditions).

### 1. Spawn Entries (`entries.yml`)

This file is used to tweak the spawn properties (weight, group size) of mobs that are already registered to spawn in biomes.

**Structure:**
```yaml
- mob: minecraft:zombie
  biomes:
    - minecraft:plains
    - minecraft:forest
  weight: 100
  group_size: [4, 4]

- mod: lycanitesmobs
  biomes:
    - '*' # Matches all biomes
  weight: 10
  group_size: [1, 3]
```

**Fields:**
- **Target Selection** (Use one or more):
  - `mob`: Single entity registry name (e.g., `minecraft:zombie`).
  - `mobs`: List of entities or a map of modid to entities.
  - `mod`: Single mod ID (matches all entities from this mod).
  - `mods`: List of mod IDs.
- **biomes**: List of biome registry names. Supports `*` to match all biomes.
- **weight**: The spawn weight. Higher numbers mean the entity spawns more frequently.
- **group_size**: A list of two integers `[min, max]` defining the minimum and maximum number of entities per spawn.

### 2. Spawn Rules (`rules.yml`)

This file allows you to define complex rules to allow or deny spawns based on various conditions.

**Structure:**
```yaml
- on: spawn
  if:
    mob: minecraft:zombie
    light:
      at_least: 12
  then: deny
```

**Fields:**
- **on**: The event to trigger on. Currently supports `spawn` (CheckSpawn) and `join` (EntityJoinWorld). Default is `spawn`.
- **then**: Action to take if conditions match. Values: `allow`, `deny`, `default`.
- **else**: Action to take if conditions DO NOT match. Values: `allow`, `deny`, `default`.
- **for** / **if**: Conditions to check. Both keys work identically.

**Conditions:**
- `mob` / `mobs` / `mod` / `mods`: Same as in `entries.yml`.
- `dimension`: Dimension ID (integer).
- `health`: Range of health (e.g., `at_least: 10`, `between: [10, 20]`).
- `light`: Range of light level.
- `height`: Range of Y-coordinate.
- `random`: Chance (0.0 to 1.0).
- `count`: Check existing mob counts.
  - `per`: `chunk` or `world`.
  - `at_least`, `at_most`, `between`.

**Range Format:**
Ranges (`health`, `light`, `height`, `count`) can be specified as:
- `at_least: X`
- `at_most: X`
- `between: [Min, Max]`

### Examples

**entries.yml**:
```yaml
# Make zombies spawn more often in plains
- mob: minecraft:zombie
  biomes:
    - minecraft:plains
  weight: 200
  group_size: [4, 8]
```

**rules.yml**:
```yaml
# Prevent zombies from spawning in high light levels
- on: spawn
  if:
    mob: minecraft:zombie
    light:
      at_least: 12
  then: deny

# Limit total number of skeletons in the world
- on: spawn
  if:
    mob: minecraft:skeleton
    count:
      per: world
      at_least: 100
  then: deny
```
