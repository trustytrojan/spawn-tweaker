package dev.trustytrojan.spawn_tweaker;

import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import dev.trustytrojan.spawn_tweaker.rule.SpawnRule;
import dev.trustytrojan.spawn_tweaker.rule.JoinRule;

/**
 * Handles reading and writing spawn rules in YAML format.
 */
public class YamlHandler
{
	private static final Logger logger = LogManager.getLogger();
	private static final Yaml YAML = new Yaml();

	/**
	 * Read the new configuration format.
	 */
	@SuppressWarnings("unchecked")
	public static Configuration readConfiguration(final File file)
	{
		if (!file.exists())
			return null;
		try (final var reader = new FileReader(file))
		{
			final var loaded = YAML.load(reader);
			if (!(loaded instanceof Map))
				return null;

			final var root = (Map<String, Object>) loaded;
			final var config = new Configuration();

		// Parse on_join_deny
		if (root.containsKey("on_join_deny"))
		{
			final var list = (List<Map<String, Object>>) root.get("on_join_deny");
			for (final var raw : list)
			{
				final var rule = JoinRule.fromYaml(raw);
				if (rule != null)
					config.onJoinDeny.add(rule);
			}
		}

		// Parse check_spawn
		if (root.containsKey("check_spawn"))
		{
			final var list = (List<Map<String, Object>>) root.get("check_spawn");
			for (final var raw : list)
			{
				final var rule = SpawnRule.fromYaml(raw);
				if (rule != null)
					config.checkSpawn.add(rule);
			}
		}

		// Parse spawn
		if (root.containsKey("spawn"))
		{
			final var list = (List<Map<String, Object>>) root.get("spawn");
			for (var i = 0; i < list.size(); i++)
			{
				final var rule = SpawnEntryCombination.fromYaml(list.get(i), i);
				if (rule != null)
					config.spawn.add(rule);
			}
		}			return config;
		}
		catch (Exception e)
		{
			logger.error("Failed to read configuration", e);
			return null;
		}
	}
}
