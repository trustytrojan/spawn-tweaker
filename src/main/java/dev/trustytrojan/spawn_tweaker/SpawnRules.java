package dev.trustytrojan.spawn_tweaker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.trustytrojan.spawn_tweaker.data.SpawnRuleRaw;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent.CheckSpawn;

public class SpawnRules
{
	private static final Logger logger = LogManager.getLogger();
	private static File file;

	public static final List<CompiledRule<CheckSpawn>> spawnRules = new ArrayList<>();
	public static final List<CompiledRule<EntityJoinWorldEvent>> joinRules = new ArrayList<>();

	public static void init(final File configDir)
	{
		file = new File(configDir, "rules.yml");
	}

	public static void load()
	{
		spawnRules.clear();
		joinRules.clear();
		try
		{
			for (final var rawRule : YamlLoader.loadListFromYaml(file, SpawnRuleRaw.class))
			{
				if (rawRule.on == null || rawRule.on.equals("spawn"))
					spawnRules.add(new CompiledRule<>(rawRule));
				else if (rawRule.on.equals("join"))
					joinRules.add(new CompiledRule<>(rawRule));
			}
			logger.info("Rules loaded!");
		}
		catch (final IOException e)
		{
			logger.error("Error occurred loading rules: ", e);
		}
	}
}
