package dev.trustytrojan.spawn_tweaker;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.trustytrojan.spawn_tweaker.data.SpawnEntryRaw;

public class SpawnEntryManager
{
	private static final Logger logger = LogManager.getLogger();
	private static File lastConfigFile;

	public static void load(final File configFile)
	{
		lastConfigFile = configFile;

		try
		{
			applyEntries(YamlLoader.loadListFromYaml(configFile, SpawnEntryRaw.class));
		}
		catch (final IOException t)
		{
			logger.error("Error occurred loading entries: ", t);
		}
	}

	public static void applyEntries(final List<SpawnEntryRaw> entries)
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

	public static void reload()
	{
		if (lastConfigFile != null)
			load(lastConfigFile);
	}
}
