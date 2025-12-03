# spawn-tweaker
Spawn Tweaker is a Forge 1.12.2 mod allowing you to change the spawn weight, minimum/maximum group size, and biome(s) of any modded mobs classifying as monsters without needing to deal with tons of different mod configuration files.

## In-Game Commands

- `/spawntweaker import`
  - Reloads the spawn configuration from the `spawn_tweaker/monster_spawns.json` file.
  - Use this after editing the JSON file to apply changes without restarting the game.
  - **Note:** If a `spawn_tweaker/monster_spawns.json` file exists, the mod will automatically import it on Minecraft startup.

- `/spawntweaker export <glob> [<glob> ...]`
  - Exports current spawn data for entities matching the provided glob patterns to `spawn_tweaker/monster_spawns_exports.json`.
  - Examples:
    - `/spawntweaker export *` - Exports spawn data for ALL registered monster entities.
    - `/spawntweaker export mod1:*` - Exports only monsters from the mod `mod1`.
    - `/spawntweaker export thermalfoundation:* enderzoo:enderminy` - Exports Thermal Foundation mobs and the specific Enderminy entity.
  - You should use the exported spawn data to decide what to write for `monster_spawns.json`.

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

- **Key (`modid:entity_name`)**: The registry name of the entity (e.g., `minecraft:zombie`). You can also use glob patterns to target multiple entities, e.g., `"mymod:*"` to affect all entities from `mymod`.
- **weight**: The spawn weight. Higher numbers mean the entity spawns more frequently.
- **minGroupSize**: The minimum number of entities that spawn in a group.
- **maxGroupSize**: The maximum number of entities that spawn in a group.
- **biomes**: A list of biome registry names where this spawn rule applies. Entries support glob patterns as well, e.g., `"minecraft:*"` or `"minecraft:*hills*"`.

### Biome/Entity Glob Support
For example:
```
{
  "five_nights_at_freddycraft:*": {
    "weight": 1,
    "minGroupSize": 1,
    "maxGroupSize": 1,
    "biomes": ["*"]
  }
}
```

This JSON should yield this output in your logs upon startup:
```
[17:03:31] [Client thread/INFO] [dev.trustytrojan.spawn_tweaker.SpawnTweaker]: Importing monster spawn data...
[17:03:31] [Client thread/INFO] [dev.trustytrojan.spawn_tweaker.SpawnTweaker]: "five_nights_at_freddycraft:*" matched 62 entities; set spawn weight=1 min=1 max=1 for all biomes
[17:03:31] [Client thread/INFO] [dev.trustytrojan.spawn_tweaker.SpawnTweaker]: Monster spawn data imported successfully
```

If not, definitely leave an issue about it!

**Note:** If a pattern matches no entities or no biomes, the entry is skipped and a warning is logged.
