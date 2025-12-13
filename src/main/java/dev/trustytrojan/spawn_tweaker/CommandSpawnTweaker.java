package dev.trustytrojan.spawn_tweaker;

import java.util.function.Consumer;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.monster.IMob;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

public class CommandSpawnTweaker extends CommandBase
{
	@Override
	public String getName()
	{
		return "spawntweaker";
	}

	@Override
	public String getUsage(final ICommandSender sender)
	{
		return "Subcommands:\n- reload (rules|entries)\n- restore_original_spawns\n- killall";
	}

	@Override
	public void execute(
		final MinecraftServer server,
		final ICommandSender sender,
		final String[] args)
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
			}
		}

		case "restore_original_spawns" ->
		{
			OriginalEntries.restore();
			reply.accept("Original spawn entries restored.");
		}

		case "killall" ->
		{
			for (final var world : server.worlds)
			{
				world.loadedEntityList.stream()
					.filter(e -> e instanceof IMob)
					.forEach(world::removeEntityDangerously);
			}
			reply.accept("All monsters removed.");
		}

		default -> reply.accept("Unknown subcommand: " + args[0]);
		}
	}

	@Override
	public int getRequiredPermissionLevel()
	{
		return 2; // OP only
	}
}
