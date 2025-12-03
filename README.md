# Spawn Tweaker
Spawn Tweaker is a Forge 1.12.2 mod allowing you to change the spawn weight and minimum/maximum group size for any modded mobs classifying as monsters in any biomes without needing to deal with tons of different mod configuration files.

## In-Game Commands

- `/spawntweaker import`
  - Reloads the spawn configuration from the `spawn_tweaker` directory.
  - Use this after editing your spawn configuration to apply changes without restarting the game.
  - **Note:** On Minecraft startup, the mod will automatically import spawn data if either `monster_spawns.yml` or `monster_spawns.json` exists (YAML is tried first).

- `/spawntweaker export <glob> [<glob> ...]`
  - Exports current spawn data for entities matching the provided glob patterns to both `spawn_tweaker/monster_spawns_export.yml` and `spawn_tweaker/monster_spawns_export.json`.
  - Examples:
    - `/spawntweaker export *` - Exports spawn data for ALL registered monster entities.
    - `/spawntweaker export mod1:*` - Exports only monsters from the mod `mod1`.
    - `/spawntweaker export thermalfoundation:* enderzoo:enderminy` - Exports Thermal Foundation mobs and the specific Enderminy entity.
  - You should use the exported spawn data as a starting point to decide what to write for your configuration.

## Configuration Files

Spawn Tweaker supports both **YAML** (`.yml`) and **JSON** (`.json`) formats. YAML is recommended for its readability.

### File Location
Configuration files are located in the `spawn_tweaker` folder in your game directory:
- `monster_spawns.yml` (preferred) or `monster_spawns.json`

### YAML Format (Recommended)
*Please remember that in YAML leading whitespace **is** significant. **Do not** report parse errors as issues.*

**Structure:**
```yaml
# Rule 1
- for:
    entities:
      - modid:entity_name
      - modX:* # Matches all entities in modX
    biomes:
      - minecraft:plains
      - biomesoplenty:* # Matches all biomes in biomesoplenty
  spawn:
    weight: 100
    minGroupSize: 4
    maxGroupSize: 4

# Rule 2
- for:
    entities:
      - another_mod:creature
    biomes:
      - '*' # Matches ALL biomes registered to Forge
  spawn:
    weight: 50
    minGroupSize: 1
    maxGroupSize: 3
```

### JSON Format (Also Supported)

**Structure:**
```json
[
  {
    "for": {
      "entities": [
        "modid:entity_name",
        "modX:*"
      ],
      "biomes": [
        "minecraft:plains",
        "biomesoplenty:*"
      ]
    },
    "spawn": {
      "weight": 100,
      "minGroupSize": 4,
      "maxGroupSize": 4
    }
  }
]
```

### Configuration Fields

Each rule consists of two sections:

#### `for` Section
- **entities**: List of entity registry names or glob patterns (e.g., `minecraft:zombie`, `mymod:*`)
- **biomes**: List of biome registry names or glob patterns (e.g., `minecraft:plains`, `biomesoplenty:*`, `*` for all biomes)

#### `spawn` Section
- **weight**: The spawn weight. Higher numbers mean the entity spawns more frequently.
- **minGroupSize**: The minimum number of entities that spawn in a group.
- **maxGroupSize**: The maximum number of entities that spawn in a group.

### Example Configuration

Here's a YAML example that sets all Five Nights at Freddycraft entities to spawn with low frequency in all biomes:

```yaml
- for:
    entities:
      - five_nights_at_freddycraft:*
    biomes:
      - '*'
  spawn:
    weight: 1
    minGroupSize: 1
    maxGroupSize: 1
```

This configuration should yield output similar to the following in your logs upon startup:
```
[Client thread/INFO] [dev.trustytrojan.spawn_tweaker.SpawnTweaker]: Importing monster spawn data...
[Client thread/INFO] [dev.trustytrojan.spawn_tweaker.YamlHandler]: Loaded 1 spawn rules from YAML: monster_spawns.yml
[Client thread/INFO] [dev.trustytrojan.spawn_tweaker.SpawnTweaker]: Rule #1 applied for 62 entities in 1 biomes
[Client thread/INFO] [dev.trustytrojan.spawn_tweaker.SpawnTweaker]: Monster spawn data imported successfully
```

### Notes

- **Pattern Matching**: Both entity and biome fields support glob patterns using `*` (matches any characters) and `?` (matches single character).
- **Multiple Rules**: You can define multiple rules to configure different entities & biomes with different spawn settings.
- **Auto-Detection**: On import, the mod tries YAML first (`.yml`), then falls back to JSON (`.json`).
- **Export**: The export command generates both YAML and JSON files for your convenience.
- **Validation**: If a pattern matches no entities or no biomes, the entry is skipped and a warning is logged.
