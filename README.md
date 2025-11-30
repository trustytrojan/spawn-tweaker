# spawn-tweaker
Spawn Tweaker is a Forge 1.12.2 mod allowing you to change the spawn weight, minimum/maximum group size, and biome(s) of any modded mobs classifying as monsters without needing to deal with tons of different mod configuration files.

## In-Game Commands

- `/spawntweaker import`
  - Reloads the spawn configuration from the `spawn_tweaker/monster_spawns.json` file.
  - Use this after editing the JSON file to apply changes without restarting the game.
  - **Note:** If a `spawn_tweaker/monster_spawns.json` file exists, the mod will automatically import it on Minecraft startup.

- `/spawntweaker export <glob> [<glob> ...]`
  - Exports current spawn data for entities matching the provided glob patterns to `spawn_tweaker/monster_spawns.json`.
  - **Note:** This overwrites the existing file!
  - Examples:
    - `/spawntweaker export *` - Exports spawn data for ALL registered monster entities.
    - `/spawntweaker export mod1:*` - Exports only monsters from the mod `mod1`.
    - `/spawntweaker export thermalfoundation:* enderzoo:enderminy` - Exports Thermal Foundation mobs and the specific Enderminy entity.

## `spawn_tweaker/monster_spawns.json` File

This file is generated in the `spawn_tweaker` folder in your game directory. It contains the spawn rules for monsters.

**Structure:**
```json
{
  "modid:entity_name": {
    "weight": 100,
    "minGroupSize": 4,
    "maxGroupSize": 4,
    "biomes": [
      "minecraft:plains",
      "minecraft:desert",
      "biomesoplenty:origin_valley"
    ]
  }
}
```

- **Key (`modid:entity_name`)**: The registry name of the entity (e.g., `minecraft:zombie`).
- **weight**: The spawn weight. Higher numbers mean the entity spawns more frequently.
- **minGroupSize**: The minimum number of entities that spawn in a group.
- **maxGroupSize**: The maximum number of entities that spawn in a group.
- **biomes**: A list of biome registry names where this spawn rule applies.

