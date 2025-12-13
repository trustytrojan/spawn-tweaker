package dev.trustytrojan.spawn_tweaker;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.trustytrojan.spawn_tweaker.data.SpawnRuleRaw;
import net.minecraftforge.event.entity.EntityEvent;

public class SpawnRules
{
	private static final Logger logger = LogManager.getLogger();
	private static List<CompiledRule<? extends EntityEvent>> rules;
	private static File file;

	public static void init(final File configDir)
	{
		file = new File(configDir, "rules.yml");
	}

	public static void load()
	{
		try
		{
			rules = YamlLoader.loadListFromYaml(file, SpawnRuleRaw.class)
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

	public static List<CompiledRule<? extends EntityEvent>> get()
	{
		return rules;
	}
}
