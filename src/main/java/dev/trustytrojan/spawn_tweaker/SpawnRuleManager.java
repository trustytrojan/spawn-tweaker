package dev.trustytrojan.spawn_tweaker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.reflect.TypeToken;

import dev.trustytrojan.spawn_tweaker.data.SpawnRuleRaw;

public class SpawnRuleManager
{
	private static final Logger logger = LogManager.getLogger();
	private static final List<CompiledRule> activeRules = new ArrayList<>();
	private static File lastConfigFile;

	public static void load(final File configFile)
	{
		lastConfigFile = configFile;
		activeRules.clear();

		final List<SpawnRuleRaw> rawRules = ConfigLoader.loadListFromYaml(
			configFile,
			new TypeToken<List<SpawnRuleRaw>>() {}.getType(),
			e -> logger.error("Failed to load spawn rules", e));

		for (final var raw : rawRules)
			activeRules.add(new CompiledRule(raw));
	}

	public static void reload()
	{
		if (lastConfigFile != null)
		{
			load(lastConfigFile);
		}
	}

	public static List<CompiledRule> getRules()
	{
		return activeRules;
	}
}
