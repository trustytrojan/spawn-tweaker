package dev.trustytrojan.spawn_tweaker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.trustytrojan.spawn_tweaker.rule.SpawnRule;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;

public final class SpawnRules
{
	private SpawnRules()
	{}

	private static final Logger logger = LogManager.getLogger();
	private static File file;

	public static final List<SpawnRule> spawnRules = new ArrayList<>();
	public static final List<SpawnRule> joinRules = new ArrayList<>();

	public static void init(final File configDir)
	{
		file = new File(configDir, "rules.yml");
		prevLastModified = file.lastModified();
	}

	public static void load()
	{
		spawnRules.clear();
		joinRules.clear();
		try
		{
			for (final var rule : YamlLoader.loadListFromYaml(file, SpawnRule.class))
			{
				rule.compile();
				if (rule.on == null || rule.on.equals("spawn"))
					spawnRules.add(rule);
				else if (rule.on.equals("join"))
					joinRules.add(rule);
			}
			logger.info("Rules loaded!");
		}
		catch (final IOException e)
		{
			logger.error("Error occurred loading rules: ", e);
		}
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
