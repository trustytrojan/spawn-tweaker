package dev.trustytrojan.spawn_tweaker;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;

public final class SpawnEntries
{
	private SpawnEntries()
	{}

	private static final Logger logger = LogManager.getLogger();
	private static File file;

	public static void init(final File configDir)
	{
		file = new File(configDir, "entries.yml");
		prevLastModified = file.lastModified();
	}

	public static void load()
	{
		try
		{
			final var entries = YamlLoader.loadListFromYaml(file, SpawnEntry.class);

			if (entries.isEmpty())
			{
				logger.warn("No entries loaded from file. Restoring original entries.");
				OriginalEntries.restore();
			}

			apply(YamlLoader.loadListFromYaml(file, SpawnEntry.class));
		}
		catch (final IOException t)
		{
			logger.error("Error occurred loading entries: ", t);
			return;
		}
	}

	public static void apply(final List<SpawnEntry> entries)
	{
		var i = 1;
		for (final var entry : entries)
		{
			entry.setIndex(i++);
			try
			{
				entry.apply();
			}
			catch (final Throwable t)
			{
				logger.error("Error occurred when applying entry #{}: {}", i, t);
			}
		}
		logger.info("Entries applied!");
	}

	private static int tickCounter;
	private static long prevLastModified;

	@SubscribeEvent
	public static void onServerTick(final ServerTickEvent event)
	{
		if (event.phase != TickEvent.Phase.END)
			return;
		if (++tickCounter % 20 != 0)
			return;

		final var currentLastModified = file.lastModified();

		if (currentLastModified == 0)
		{
			logger.warn("Config file {} does not exist or error occurred", file);
			return;
		}

		if (prevLastModified < currentLastModified)
		{
			logger.warn("File {} changed, reloading config", file);
			prevLastModified = currentLastModified;
			load();
		}
	}
}
