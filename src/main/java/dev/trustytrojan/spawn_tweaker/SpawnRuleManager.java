package dev.trustytrojan.spawn_tweaker;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.trustytrojan.spawn_tweaker.data.SpawnRuleRaw;
import net.minecraftforge.event.entity.EntityEvent;

public class SpawnRuleManager
{
	private static final Logger logger = LogManager.getLogger();
	private static List<CompiledRule<? extends EntityEvent>> activeRules;
	private static File lastConfigFile;

	public static void load(final File configFile)
	{
		lastConfigFile = configFile;
		try
		{
			activeRules = YamlLoader
				.loadListFromYaml(configFile, SpawnRuleRaw.class)
				.stream()
				.map(CompiledRule::new)
				.collect(Collectors.toList());
			logger.info("Rules loaded!");
		}
		catch (final IOException e)
		{
			logger.error("Error occurred loading rules: ", e);
		}
	}

	public static void reload()
	{
		if (lastConfigFile != null)
			load(lastConfigFile);
	}

	public static List<CompiledRule<? extends EntityEvent>> getRules()
	{
		return activeRules;
	}
}
