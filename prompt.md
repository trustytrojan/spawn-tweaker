great work. we should start splitting up code in #file:SpawnTweaker.java , it's getting bloated.
- take all of the glob-related functions and put them into a new class `GlobUtils`. put `OnJoinConfig` into it's own file.
- #sym:shouldAllowOnJoin(EntityLiving, Random) might as well be a method in #sym:OnJoinConfig itself.
- there is a lot of nesting in #sym:applySpawnRules(List<SpawnRule>) , we can certainly take parts out into logical functions for clarity. 
- remove all json related code.
- change the write methods of #sym:YamlHandler to simply output a structure like this:
```yaml
- entity:
  weight:
  minGroupSize:
  maxGroupSize:
  biomes:
    resourceDomain1: [biome1, biome2, ...]
    resourceDomain2: [...]
- ...
```
also tell the yaml writer not to break lines if they are too long.