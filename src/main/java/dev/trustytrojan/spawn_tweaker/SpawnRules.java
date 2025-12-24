package dev.trustytrojan.spawn_tweaker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.trustytrojan.spawn_tweaker.rule.SpawnRule;

public final class SpawnRules extends ConfigFile
{
	private static final Logger logger = LogManager.getLogger();

	public static final List<SpawnRule> spawnRules = new ArrayList<>(),
		joinRules = new ArrayList<>();

	public SpawnRules(final File configDir)
	{
		super(configDir, "rules.yml");
	}

	@Override
	public void load()
	{
		spawnRules.clear();
		joinRules.clear();
		try
		{
			for (final var rule : YamlLoader.loadListFromYaml(this, SpawnRule.class))
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
}
