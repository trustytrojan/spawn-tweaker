# Notes on troublesome mods
There are a few mods that may not play nicely with what Spawn Tweaker provides.

## Scape and Run: Parasites
Scape and Run: Parasites uses its own spawning algorithm. Unfortunately it does not respect Forge `CheckSpawn` or `EntityJoinWorldEvent` results unless they deny the spawn. **Do NOT have an `allow` or `default` result** otherwise their spawning algorithm will forcefully spawn everything it can. You should only write rules that `deny` spawns.
