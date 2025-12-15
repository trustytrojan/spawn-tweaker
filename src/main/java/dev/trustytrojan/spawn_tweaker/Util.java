package dev.trustytrojan.spawn_tweaker;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
	 * @param <T>    Type of stream element
	 * @param stream The stream to consume
	 * @return An {@link Iterable} using the provided stream's iterator.
	 * @see {@link Stream#iterator()}
	 * @apiNote <b>Do not reuse</b> the returned {@link Iterable} in another loop. The stream is consumed by the iterable's
	 *          first pass of elements.
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
		final var registry = GameRegistry.findRegistry(entryClass);
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
					final var entry = registry.getValue(rl);

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

	public static Stream<Class<? extends EntityLiving>> resolveEntityClasses(
		final Map<String, Object> mobs)
	{
		if (mobs == null)
			return Stream.empty();

		return Util.resolveRegistryEntries(EntityEntry.class, mobs)
			.map(Util::getEntityLivingClass)
			.filter(Objects::nonNull) // this erases the <? extends EntityLiving>, we have to cast it back 🤷‍♂️
			.map(c -> (Class<? extends EntityLiving>) c);
	}

	public static Stream<Biome> resolveBiomes(final Map<String, Object> biomes)
	{
		if (biomes == null)
			return Stream.empty();

		return Util.resolveRegistryEntries(Biome.class, biomes);
	}
}
