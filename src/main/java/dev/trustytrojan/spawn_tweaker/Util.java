package dev.trustytrojan.spawn_tweaker;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

public final class Util
{
	private static final Logger logger = LogManager.getLogger();

	/**
	 * <b>This terminates the stream!!!!!!!!</b>
	 */
	public static <T> Iterable<T> iterableStream(final Stream<T> stream)
	{
		return new Iterable<>() {
			@Override
			public Iterator<T> iterator()
			{
				return stream.iterator();
			}
		};
	}

	/**
	 * @param entry The entity entry
	 * @return The class, or {@code null} if not found or isn't {@code EntityLiving}
	 */
	@SuppressWarnings("unchecked")
	public static Class<? extends EntityLiving> getEntityLivingClass(final EntityEntry entry)
	{
		final var clazz = entry.getEntityClass();
		if (clazz == null || !EntityLiving.class.isAssignableFrom(clazz))
			return null;
		return (Class<? extends EntityLiving>) clazz;
	}

	public static <T extends IForgeRegistryEntry<T>> Stream<T> getRegistryEntriesForMod(
		final Class<T> entryClass,
		final String modId)
	{
		return GameRegistry.findRegistry(entryClass)
			.getValuesCollection()
			.stream()
			.filter(e -> e.getRegistryName().getNamespace().equals(modId));
	}

	@SuppressWarnings("unchecked")
	public static <T extends IForgeRegistryEntry<T>> Stream<T> resolveRegistryEntries(
		final Class<T> entryClass,
		final Map<String, Object> selectorTable)
	{
		final var streamBuilder = Stream.<Stream<T>>builder();

		for (final var selector : selectorTable.entrySet())
		{
			final var modId = selector.getKey();
			final var value = selector.getValue();

			if (value.equals("*"))
				streamBuilder.add(Util.getRegistryEntriesForMod(entryClass, modId));
			else if (value instanceof List)
				for (final var mobName : ((List<String>) value))
				{
					final var rl = new ResourceLocation(modId + ':' + mobName);
					final var entry = GameRegistry.findRegistry(entryClass).getValue(rl);

					if (entry == null)
					{
						logger.warn("Entry not found for name \"{}\"", rl);
						continue;
					}

					streamBuilder.add(Stream.of(entry));
				}
			else
				throw new IllegalArgumentException(
					"Values in selectorTable must be a list of names or the string '*'");
		}

		return streamBuilder.build().flatMap(s -> s);
	}

	public static Biome[] getCurrentSpawnBiomes(final Class<? extends EntityLiving> entityClass)
	{
		return ForgeRegistries.BIOMES.getValuesCollection()
			.stream()
			.filter(
				b -> b.getSpawnableList(EnumCreatureType.MONSTER)
					.stream()
					.anyMatch(e -> e.entityClass.equals(entityClass)))
			.toArray(Biome[]::new);
	}
}
