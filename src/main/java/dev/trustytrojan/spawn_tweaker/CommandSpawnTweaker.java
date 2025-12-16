package dev.trustytrojan.spawn_tweaker;

import java.util.function.Consumer;
import java.util.stream.Stream;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
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
		return "Subcommands:\n- reload <rules|entries>\n- restore_original_spawns\n- killall";
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

		case "restore_original_spawns" ->
		{
			OriginalEntries.restore();
			reply.accept("Original spawn entries restored.");
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
}
