package dev.trustytrojan.spawn_tweaker;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import org.yaml.snakeyaml.Yaml;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import dev.trustytrojan.spawn_tweaker.data.SpawnRuleRaw;

public class SpawnRuleManager
{
	private static final List<CompiledRule> activeRules = new ArrayList<>();
	private static File lastConfigFile;

	public static void load(final File configFile)
	{
		lastConfigFile = configFile;
		activeRules.clear();
		final var gson = new Gson();
		final var yaml = new Yaml();

		try (final var ios = new FileInputStream(configFile))
		{
			// 1. YAML -> Generic Java Objects
			final var rawYaml = yaml.load(ios);

			// 2. Generic Java Objects -> Gson Tree
			final var jsonTree = gson.toJsonTree(rawYaml);

			// 3. Gson Tree -> Raw POJOs
			final List<SpawnRuleRaw> rawRules =
				gson.fromJson(jsonTree, new TypeToken<List<SpawnRuleRaw>>() {}.getType());

			// 4. Raw POJOs -> Compiled Functional Rules
			if (rawRules != null)
			{
				for (final var raw : rawRules)
					activeRules.add(new CompiledRule(raw));
			}

		}
		catch (Exception e)
		{
			// Log error to Forge logger
			e.printStackTrace();
		}
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
