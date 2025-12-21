package dev.trustytrojan.spawn_tweaker;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.trustytrojan.spawn_tweaker.data.SpawnEntry;

public final class SpawnEntries
{
	private SpawnEntries()
	{}

	private static final Logger logger = LogManager.getLogger();
	private static File file;

	public static void init(final File configDir)
	{
		file = new File(configDir, "entries.yml");
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
}
