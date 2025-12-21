package dev.trustytrojan.spawn_tweaker;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.reflect.TypeToken;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome.SpawnListEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public final class OriginalEntries
{
	private OriginalEntries()
	{}

	private static File file;

	public static void init(final File configDir)
	{
		file = new File(configDir, "original-entries.json");
		save();
	}

	public static void save()
	{
		final class SpawnListEntrySerializer implements JsonSerializer<SpawnListEntry>
		{
			@Override
			public JsonElement serialize(
				final SpawnListEntry src,
				final Type typeOfSrc,
				final JsonSerializationContext ctx)
			{
				final var o = new JsonObject();
				o.add("entity", ctx.serialize(EntityList.getKey(src.entityClass).toString()));
				o.add("weight", ctx.serialize(src.itemWeight));
				o.add(
					"group_size",
					ctx.serialize(Arrays.asList(src.minGroupCount, src.maxGroupCount)));
				return o;
			}
		}

		final var m = ForgeRegistries.BIOMES.getValuesCollection()
			.stream()
			.collect(
				Collectors.toMap(
					b -> b.getRegistryName().toString(),
					b -> b.getSpawnableList(EnumCreatureType.MONSTER)));

		final var gson = new GsonBuilder()
			.registerTypeAdapter(SpawnListEntry.class, new SpawnListEntrySerializer())
			.create();

		try (final var w = new FileWriter(file))
		{
			gson.toJson(m, w);
		}
		catch (final IOException e)
		{
			e.printStackTrace();
		}
	}

	public static void restore()
	{
		final class SpawnListEntryDeserializer implements JsonDeserializer<SpawnListEntry>
		{
			@Override
			public SpawnListEntry deserialize(
				JsonElement json,
				Type typeOfT,
				JsonDeserializationContext context) throws JsonParseException
			{
				final var o = json.getAsJsonObject();
				final var rl = new ResourceLocation(o.get("entity").getAsString());
				@SuppressWarnings("unchecked")
				final var clazz = (Class<? extends EntityLiving>) EntityList.getClass(rl);
				if (clazz == null)
					return null;
				final var weight = o.get("weight").getAsInt();
				final var groupSize = o.get("group_size").getAsJsonArray();
				final var minGroupSize = groupSize.get(0).getAsInt();
				final var maxGroupSize = groupSize.get(1).getAsInt();
				return new SpawnListEntry(clazz, weight, minGroupSize, maxGroupSize);
			}
		}

		final var gson = new GsonBuilder()
			.registerTypeAdapter(SpawnListEntry.class, new SpawnListEntryDeserializer())
			.create();

		final Map<String, List<SpawnListEntry>> m;

		try (final var r = new FileReader(file))
		{
			m = gson.fromJson(r, new TypeToken<Map<String, List<SpawnListEntry>>>()
			{}.getType());
		}
		catch (final IOException e)
		{
			e.printStackTrace();
			return;
		}

		for (final var entry : m.entrySet())
		{
			final var biome = ForgeRegistries.BIOMES.getValue(new ResourceLocation(entry.getKey()));
			if (biome == null)
				continue;
			final var spawnableList = biome.getSpawnableList(EnumCreatureType.MONSTER);
			spawnableList.clear();
			spawnableList.addAll(entry.getValue());
		}
	}
}
