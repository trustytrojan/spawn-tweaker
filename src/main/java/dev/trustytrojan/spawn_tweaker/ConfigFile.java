package dev.trustytrojan.spawn_tweaker;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;

public abstract class ConfigFile extends File
{
	private static final Logger logger = LogManager.getLogger();

	private int tickCounter;
	private long prevLastModified;

	public ConfigFile(final File parent, final String child)
	{
		super(parent, child);
		prevLastModified = lastModified();
	}

	public abstract void load();

	@SubscribeEvent
	public void onServerTick(final ServerTickEvent event)
	{
		if (event.phase != TickEvent.Phase.END)
			return;
		if (++tickCounter % 20 != 0)
			return;

		final var currentLastModified = lastModified();

		if (currentLastModified == 0)
		{
			logger.warn("Config file {} does not exist or error occurred", getPath());
			return;
		}

		if (prevLastModified < currentLastModified)
		{
			logger.warn("File {} changed, reloading config", getPath());
			prevLastModified = currentLastModified;
			load();
		}
	}
}
