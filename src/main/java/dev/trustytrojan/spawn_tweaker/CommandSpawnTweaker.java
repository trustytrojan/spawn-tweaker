package dev.trustytrojan.spawn_tweaker;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
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
		return "/spawntweaker reload | /spawntweaker killall";
	}

	@Override
	public void execute(final MinecraftServer server, final ICommandSender sender, final String[] args)
	{
		if (args.length < 1)
		{
			sender.sendMessage(new TextComponentString("Usage: /spawntweaker reload | /spawntweaker killall"));
			return;
		}

		switch (args[0].toLowerCase())
		{
		case "reload" ->
		{
			SpawnTweaker.loadConfiguration();
			sender.sendMessage(new TextComponentString("Configuration reloaded from config/spawn_tweaker.yml."));
		}

		case "killall" ->
		{
			for (final var world : server.worlds)
			{
				for (final var e : world.loadedEntityList.toArray())
				{
					if (e instanceof IMob)
						world.removeEntityDangerously((Entity) e);
				}
			}
			sender.sendMessage(new TextComponentString("All monsters removed."));
		}

		default -> sender.sendMessage(new TextComponentString("Unknown subcommand: " + args[0]));
		}
	}

	@Override
	public int getRequiredPermissionLevel()
	{
		return 2; // OP only
	}
}
