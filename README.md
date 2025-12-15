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

## Documentation
See documentation [here](/docs).
