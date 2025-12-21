# Rule Conditions

This page documents the available conditions you can use inside `for:` and `if:`.

## `count`
Checks entity population counts tracked by Spawn Tweaker.

### Fields

- `at_least` / `at_most` / `between`: same as other range conditions.
- `per`: scales the threshold by an environment factor.
  - `chunk`: scales by eligible chunk count (roughly “per chunk” behavior)
  - `player`: scales by player count
  - omitted: no scaling
- `mobs`: a mob selector table. When present, `count` will sum the population of the listed mob types. When **not** present, `count` will sum the population of **the mob that is trying to be spawned.**

### Examples

Allow up to 5 zombies to be in the world at a time:
```yaml
- for:
    mobs: { minecraft: [zombie] }
  if:
    count: { at_least: 5 }
  then: deny
```

The same rule, but with inverted logic:
```yaml
- for:
    mobs: { minecraft: [zombie] }
  if:
    count: { at_most: 5 }
  then: default
  else: deny
```

Allow up to 5 zombies **per player**. If there are 2 players, this rule allows 10 zombies:
```yaml
- for:
    mobs: { minecraft: [zombie] }
  if:
    count: { at_least: 5 }
    per: player
  then: deny
```

Allow up to 5 zombies **scaled by loaded chunks** using vanilla's mob cap formula. If there are 64 loaded chunks, this rules allows `5 * 64 / 289 = 1` zombie. (Good idea on Mojang's part, because we don't want `5 * 64 = 320` zombies...)
```yaml
- for:
    mobs: { minecraft: [zombie] }
  if:
    count: { at_least: 5 }
    per: chunk
  then: deny
```

Allow up to 5 skeletons and 5 zombies **separately**, for a total of 10 zombies and skeletons combined.
```yaml
- for:
    mobs: { minecraft: [zombie, skeleton] }
  if:
    count: { at_least: 5 }
  then: deny
```

Allow up to 5 zombies and skeletons **combined**. The comments should reduce the confusion, but if you're still confused, read the next section of this page.
```yaml
- for:
    mobs: # only allows these mobs to move onto the next condition
      minecraft: [zombie, skeleton]
  if:
    count:
      at_least: 5
      mobs: # counts the combined population of the these mobs
        minecraft: [zombie, skeleton]
  then: deny
```

### Important: what is being counted?
By default, `count` will count the mob that is **currently being evaluated by this rule.** That means rules like these:

```yaml
- for: { mobs: { modX: '*' } }
  if: { count: { at_least: 20 } }
  then: deny

- for:
    mobs: { modX: '*' }
    count: { at_least: 20 }
  then: deny

- if:
    mobs: { modX: '*' }
    count: { at_least: 20 }
  then: deny
```

…only deny **one specific `modX`** mob when **that specific mob's** count reaches 20.
It does **not** cap the *total* of all `modX` mobs to 20.

For example, say there are 10 of `modX:mobY` and 20 of `modX:mobZ` in the world. So in total there are 30 `modX` mobs in the world. Let's dissect what happens in the first rule from above when a `modX:mobY` is trying to spawn:

1. `for: { mobs: { modX: '*' } }`: `modX:mobY` passes the `mobs` check because `'*'` matches any mob from `modX`. So it passes all selectors and moves onto the `if` conditions.
2. `if: { count: { at_least: 20 } }`: Since `modX:mobY` is the mob currently being processed in this rule, `count` will only count how many `modX:mobY` mobs there are in the world. **This does not include the 20 `modX:mobZ` mobs in the world.** Since `10 < 20`, it fails, failing all `if` conditions.
3. The `then` result is not applied, letting vanilla decide the spawn.

The same logic applies to the `for`-only and `if`-only rules from above.

To get the intended "mod-capping" behavior, **specify to `count` what to count:**

```yaml
for: { mobs: { modX: '*' } }
if:
  count:
    at_least: 20
    mobs: { modX: '*' }
then: deny
```
