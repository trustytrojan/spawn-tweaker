package dev.trustytrojan.spawn_tweaker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.yaml.snakeyaml.Yaml;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Shared helpers for loading YAML config files into typed lists.
 */
public final class YamlLoader
{
	private static final Gson GSON = new Gson();
	private static final Yaml YAML = new Yaml();

	private YamlLoader()
	{}

	public static <T> List<T> loadListFromYaml(final File configFile, final Class<T> elementClass) throws IOException
	{
		try (final var ios = new FileInputStream(configFile))
		{
			final var loaded = YAML.load(ios);
			if (loaded == null)
				return Collections.emptyList();
			final var jsonTree = GSON.toJsonTree(loaded);
			final var listType = TypeToken.getParameterized(List.class, elementClass).getType();
			final List<T> parsed = GSON.fromJson(jsonTree, listType);
			return (parsed != null) ? parsed : Collections.emptyList();
		}
		catch (final IOException e)
		{
			throw e;
		}
	}
}
