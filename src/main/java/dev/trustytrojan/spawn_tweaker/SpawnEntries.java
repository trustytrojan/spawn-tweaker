package dev.trustytrojan.spawn_tweaker;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class SpawnEntries extends ConfigFile
{
	private static final Logger logger = LogManager.getLogger();

	public SpawnEntries(final File configDir)
	{
		super(configDir, "entries.yml");
	}

	@Override
	public void load()
	{
		OriginalEntries.restore();
		logger.info("Restored original entries. Now loading entries.yml");

		try
		{
			final var entries = YamlLoader.loadListFromYaml(this, SpawnEntry.class);

			if (entries.isEmpty())
			{
				logger.warn("No entries loaded from file!");
				return;
			}

			apply(entries);
		}
		catch (final IOException t)
		{
			logger.error("Error occurred loading entries: ", t.getMessage());
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
