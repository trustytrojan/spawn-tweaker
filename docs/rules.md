# Rules
Inspired by [In Control!](https://github.com/McJtyMods/InControl), the rule system lets you allow, deny, or let vanilla decide an entity spawn/join event.

## YAML Object Structure
The general structure of a rule object is below. Anything inside angle-brackets (`<>`) signifies a required parameter.
```yaml
on: <join | spawn (default)>
for:
  <condition>: <parameters...>
  ...
if:
  <condition>: <parameters...>
  ...
then: <allow | deny | default> # 'then' is required for all rules
else: <allow | deny | default (default)> # 'default' is the default if 'else' is not specified
```

### `on` Clause
Specifies the type of event this rule should evaluate on. Currently allows `join` or `spawn` as arguments. `spawn` is the default. `join` will make the rule run in Forge's [`EntityJoinWorldEvent`](https://skmedix.github.io/ForgeJavaDocs/javadoc/forge/1.12.2-14.23.5.2859/net/minecraftforge/event/entity/EntityJoinWorldEvent.html) event listener, which happens *after* Forge's [`CheckSpawn`](https://skmedix.github.io/ForgeJavaDocs/javadoc/forge/1.12.2-14.23.5.2859/net/minecraftforge/event/entity/living/LivingSpawnEvent.CheckSpawn.html) event. This is because in [Minecraft natural spawning code](https://github.com/MinecraftForge/MinecraftForge/blob/3effde4f1fc9d14d6ed1dbf6bebc39c2b18780e1/patches/minecraft/net/minecraft/world/WorldEntitySpawner.java.patch#L36-L46), an entity is checked whether it can spawn before it actually spawns. So specifying `on: join` is the last line of defense in case your `on: spawn` rule isn't behaving as expected.

### `for` Clause
The set of conditions to match the event on for either:
- the `if` clause to evaluate, or
- the `then` action to be taken, given there is no `if` clause.

You typically want to use this as a "selector" for an event to move onto the `if` clause, which has its own set of conditions. Essentially, if you write this rule:
```yaml
for:
  mobs: { weird_mod: [weird_mob] }
if:
  health: { at_least: 20 }
then: allow
else: deny
```
Only events with a `weird_mod:weird_mob` mob will check the mob's health and make a decision, and all other events will skip this rule.

### `if` clause
As described in the last section, this is another set of conditions that are **only evaluated if the `for` conditions pass.** Once the `if` conditions can evaluate, then the rule will inevitably set a result on the event, specified by the `then` and/or `else` clauses.

Taking the last section's example: if a `weird_mod:weird_mob` has 20 or more health, its spawn will be **allowed**, even if vanilla behavior would otherwise prevent it. If it has less than 20 health, its spawn will be **denied**, even if vanilla behavior would allow it.

### `then` clause
Specifies whether to allow/deny the event when the `for` **and/or** `if` conditions have all passed. It is analogous to the `result` attribute of In Control! rule objects.

### `else` clause
Specifies whether to allow/deny the event when the `if` conditions have **not** all passed. Note that unlike `then`, the `else` action is **ignored if there is no `if` clause**. It is meant to be used only with `if` so that the rule reads well.
