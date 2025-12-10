# Spawn Tweaker
Spawn Tweaker is a Forge 1.12.2 mod allowing you to change the spawn weight and minimum/maximum group size for any modded mobs classifying as monsters in any biomes without needing to deal with tons of different mod configuration files.

## In-Game Commands

- `/spawntweaker reload`
  - Reloads the spawn configuration from `config/spawn_tweaker.yml`.
  - Use this after editing your spawn configuration to apply changes without restarting the game.
  - Examples:
    - `/spawntweaker reload` - Reloads configuration from `config/spawn_tweaker.yml` and reapplies the configured spawn rules.
    - `/spawntweaker killall` - Removes all monsters from active worlds.
  - Use `config/spawn_tweaker.yml` and `examples/prototype-v2.yml` as references when editing your configuration.
  
    Note: The export feature has been removed. Use `config/spawn_tweaker.yml` and `examples/prototype-v2.yml` as references when editing your configuration.

## Configuration Files

Spawn Tweaker supports **YAML** (`.yml`) format. YAML is recommended for its readability.

### File Location
Configuration files are located in the `config` folder in your game directory:
- `spawn_tweaker.yml` (preferred)

### YAML Format (Recommended)
*Please remember that in YAML leading whitespace **is** significant. **Do not** report parse errors as issues.*

**Structure (new prototype-v2 format):**
```yaml
# Rule 1
- mobs:
    - modid:entity_name
    - modX:* # Matches all entities in modX
  biomes:
    - minecraft:plains
    - biomesoplenty:* # Matches all biomes in biomesoplenty
  spawn:
    weight: 100
    group_size: [4, 4]

# Rule 2
- mobs:
    - another_mod:creature
  biomes:
    - '*' # Matches ALL biomes registered to Forge
  spawn:
    weight: 50
    group_size: [1, 3]
```

<!-- JSON support removed; YAML is the preferred format. -->

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
[Client thread/INFO] [dev.trustytrojan.spawn_tweaker.YamlHandler]: Loaded 1 spawn rules from YAML: spawn_tweaker.yml
[Client thread/INFO] [dev.trustytrojan.spawn_tweaker.SpawnTweaker]: Rule #1 applied for 62 entities in 1 biomes
[Client thread/INFO] [dev.trustytrojan.spawn_tweaker.SpawnTweaker]: Configuration applied successfully
```

### Notes

- **Pattern Matching**: Both entity and biome fields support glob patterns using `*` (matches any characters) and `?` (matches single character).
- **Multiple Rules**: You can define multiple rules to configure different entities & biomes with different spawn settings.
-- **Auto-Reload**: On startup, the mod reads `config/spawn_tweaker.yml` (YAML only).
- **Validation**: If a pattern matches no entities or no biomes, the entry is skipped and a warning is logged.
