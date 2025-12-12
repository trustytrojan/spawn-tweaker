package dev.trustytrojan.spawn_tweaker;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.trustytrojan.spawn_tweaker.data.SpawnRuleRaw;

public class SpawnRuleManager
{
	private static final Logger logger = LogManager.getLogger();
	private static List<CompiledRule> activeRules;
	private static File lastConfigFile;

	public static void load(final File configFile)
	{
		lastConfigFile = configFile;
		activeRules = YamlLoader
			.loadListFromYaml(configFile, SpawnRuleRaw.class, e -> logger.error("Failed to load spawn rules", e))
			.stream()
			.map(CompiledRule::new)
			.collect(Collectors.toList());
		logger.info("Rules loaded!");
	}

	public static void reload()
	{
		if (lastConfigFile != null)
			load(lastConfigFile);
	}

	public static List<CompiledRule> getRules()
	{
		return activeRules;
	}
}
