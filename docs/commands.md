# In-Game Commands
All commands can only be run by server admins/operators (permission level 2).

The root command is `/spawntweaker`, with an alias `/st`. There are various **subcommands** below. Subcommands and any named arguments can be tab-autocompleted while typing them.

## `reload <rules|entries>`
Reloads either the `rules.yml` or `entries.yml` file.

Reloading the rules simply replaces all loaded rules with the contents of `rules.yml`.

Reloading the entries first restores all the original spawn entries from when all mods finished initializing, then loads and applies the entries (which you can also refer to as modifications to the original entries) from `entries.yml`.

## `algorithm [<name> [<value>]]`
Display or modify properties of the custom spawn algorithm. Possible names are: `packAttempts`, `packEntityMaxDistance`, `varyY`, and `spawnRadiusRange`.

## `killall`
Forcefully removes all non-player entities from the world, bypassing their death animations which would show with a normal `/kill` command.
