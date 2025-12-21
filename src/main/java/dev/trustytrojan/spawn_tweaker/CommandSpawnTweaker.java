package dev.trustytrojan.spawn_tweaker;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.CommandException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

public final class CommandSpawnTweaker extends CommandBase
{
	@Override
	public String getName()
	{
		return "spawntweaker";
	}

	@Override
	public List<String> getAliases()
	{
		return Arrays.asList("st");
	}

	@Override
	public String getUsage(final ICommandSender sender)
	{
		return "Subcommands:\n- reload <rules|entries>\n- killall\n- algorithm [<name> [<value>]]";
	}

	@Override
	public void execute(
		final MinecraftServer server,
		final ICommandSender sender,
		final String[] args) throws CommandException
	{
		final Consumer<String> reply = msg -> sender.sendMessage(new TextComponentString(msg));

		if (args.length < 1)
		{
			reply.accept(getUsage(sender));
			return;
		}

		switch (args[0].toLowerCase())
		{
		case "reload" ->
		{
			if (args.length < 2)
			{
				reply.accept("/spawntweaker reload: Must specify 'rules' or 'entries'");
				return;
			}

			switch (args[1].toLowerCase())
			{
			case "rules" ->
			{
				SpawnRules.load();
				reply.accept("Rules loaded.");
			}

			case "entries" ->
			{
				SpawnEntries.load();
				reply.accept("Entries loaded and applied.");
			}

			default -> reply.accept("/spawntweaker reload: Must specify 'rules' or 'entries'");
			}
		}

		case "algorithm" ->
		{
			final int extra = args.length - 1; // number of args after 'algorithm'
			if (extra == 0)
			{
				reply.accept(
					"packAttempts=" + SpawnAlgorithmConfig.packAttempts + ", packEntityMaxDistance="
						+ SpawnAlgorithmConfig.packEntityMaxDistance + ", varyY="
						+ SpawnAlgorithmConfig.varyY + ", spawnRadiusRange=["
						+ SpawnAlgorithmConfig.spawnRadiusRange[0] + ","
						+ SpawnAlgorithmConfig.spawnRadiusRange[1] + "]");
				return;
			}

			if (extra == 1)
			{
				switch (args[1])
				{
				case "spawnRadiusRange" -> reply.accept(
					"spawnRadiusRange=[" + SpawnAlgorithmConfig.spawnRadiusRange[0] + ","
						+ SpawnAlgorithmConfig.spawnRadiusRange[1] + "]");
				case "packAttempts" ->
					reply.accept("packAttempts=" + SpawnAlgorithmConfig.packAttempts);
				case "packEntityMaxDistance" -> reply
					.accept("packEntityMaxDistance=" + SpawnAlgorithmConfig.packEntityMaxDistance);
				case "varyY" -> reply.accept("varyY=" + SpawnAlgorithmConfig.varyY);
				default -> reply.accept("Unknown field: " + args[1]);
				}
				return;
			}

			if (extra >= 2)
			{
				switch (args[1])
				{
				case "packAttempts" ->
				{
					SpawnAlgorithmConfig.packAttempts = parseInt(args[2]);
					reply.accept("packAttempts set to " + SpawnAlgorithmConfig.packAttempts);
				}
				case "spawnRadiusRange" ->
				{
					int min, max;
					if (args.length >= 4)
					{
						min = parseInt(args[2]);
						max = parseInt(args[3]);
					}
					else
					{
						final String[] parts = args[2].split(",", -1);
						if (parts.length != 2)
							throw new CommandException(
								"Usage: /spawntweaker algorithm spawnRadiusRange <min> <max> or <min,max>");
						min = parseInt(parts[0].trim());
						max = parseInt(parts[1].trim());
					}
					SpawnAlgorithmConfig.spawnRadiusRange[0] = min;
					SpawnAlgorithmConfig.spawnRadiusRange[1] = max;
					reply.accept("spawnRadiusRange set to [" + min + "," + max + "]");
				}
				case "packEntityMaxDistance" ->
				{
					SpawnAlgorithmConfig.packEntityMaxDistance = parseInt(args[2]);
					reply.accept(
						"packEntityMaxDistance set to "
							+ SpawnAlgorithmConfig.packEntityMaxDistance);
				}
				case "varyY" ->
				{
					final boolean val = parseBoolean(args[2]);
					SpawnAlgorithmConfig.varyY = val;
					reply.accept("varyY set to " + SpawnAlgorithmConfig.varyY);
				}
				default -> reply.accept("Unknown field: " + args[1]);
				}
				return;
			}

			reply.accept("Usage: /spawntweaker algorithm [<name> [<value>]]");
		}

		case "killall" ->
		{
			// may want to limit to just the sender's world?
			for (final var world : server.worlds)
			{
				// have to make a copy to avoid comodification exception
				Stream.of(world.loadedEntityList.toArray(new Entity[0]))
					.filter(e -> !(e instanceof EntityPlayer))
					.forEach(world::removeEntityDangerously); // skips death animation & particles
			}
			reply.accept("All entities removed.");
		}

		default -> reply.accept("Unknown subcommand: " + args[0]);
		}
	}

	@Override
	public int getRequiredPermissionLevel()
	{
		return 2; // OP only
	}

	@Override
	public List<String> getTabCompletions(
		MinecraftServer server,
		ICommandSender sender,
		String[] args,
		@Nullable BlockPos targetPos)
	{
		if (args.length == 1)
		{
			return getListOfStringsMatchingLastWord(
				args,
				"reload",
				"restore_original_spawns",
				"killall",
				"algorithm");
		}

		if (args.length == 2)
		{
			switch (args[0].toLowerCase())
			{
			case "reload":
				return getListOfStringsMatchingLastWord(args, "rules", "entries");
			case "algorithm":
				return getListOfStringsMatchingLastWord(
					args,
					"packAttempts",
					"packEntityMaxDistance",
					"varyY",
					"spawnRadiusRange");
			default:
				return java.util.Collections.emptyList();
			}
		}

		if (args.length == 3 && "algorithm".equalsIgnoreCase(args[0]))
		{
			if ("varyY".equalsIgnoreCase(args[1]))
				return getListOfStringsMatchingLastWord(args, "true", "false");
			return java.util.Collections.emptyList();
		}

		return java.util.Collections.emptyList();
	}
}
