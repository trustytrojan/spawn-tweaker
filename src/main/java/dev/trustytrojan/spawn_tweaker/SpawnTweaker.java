package dev.trustytrojan.spawn_tweaker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class SpawnTweaker
{
    private static final Logger logger = LogManager.getLogger();
    private static Biome[] ALL_BIOMES;

    public static void init()
    {
        ALL_BIOMES = new Biome[Biome.REGISTRY.getKeys().size()];
        int i = 0;
        for (Biome b : Biome.REGISTRY)
            ALL_BIOMES[i++] = b;

        importMonsterSpawnData();
    }

    public static void importMonsterSpawnData()
    {
        logger.info("Importing monster spawn data...");

        File dataDir = new File("spawn_tweaker");
        File inputFile = new File(dataDir, "monster_spawns.json");

        if (!inputFile.exists())
        {
            logger.warn("Monster spawn data file not found: {}", inputFile.getAbsolutePath());
            return;
        }

        try (java.io.FileReader reader = new java.io.FileReader(inputFile))
        {
            Gson gson = new Gson();
            java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<Map<String, EntitySpawnInfo>>(){}.getType();
            Map<String, EntitySpawnInfo> monsterData = gson.fromJson(reader, type);

            for (Map.Entry<String, EntitySpawnInfo> entry : monsterData.entrySet())
            {
                String entityKey = entry.getKey();
                EntitySpawnInfo spawnInfo = entry.getValue();

                if (spawnInfo == null)
                {
                    logger.error("{}: spawnInfo is null, skipping", entityKey);
                    continue;
                }

                if (spawnInfo.biomes == null || spawnInfo.biomes.isEmpty())
                {
                    logger.error("{}: biomes is null or empty, skipping", entityKey);
                    continue;
                }

                // Get the class of the entity
                EntityEntry entityEntry = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(entityKey));
                @SuppressWarnings("unchecked")
                Class<? extends EntityLiving> entityClass = (Class<? extends EntityLiving>) entityEntry.getEntityClass();

                // Remove old spawns
                EntityRegistry.removeSpawn(entityClass, EnumCreatureType.MONSTER, ALL_BIOMES);

                // Add new spawns
                EntityRegistry.addSpawn(
                    entityClass, spawnInfo.weight, spawnInfo.minGroupSize, spawnInfo.maxGroupSize,
                    EnumCreatureType.MONSTER,
                    spawnInfo.biomes.stream()
                        .map(ResourceLocation::new)
                        .map(Biome.REGISTRY::getObject)
                        .toArray(Biome[]::new)
                );

                logger.info("Changed {} spawn to weight={} min={} max={} for {} biomes", entityKey, spawnInfo.weight, spawnInfo.minGroupSize, spawnInfo.maxGroupSize, spawnInfo.biomes.size());
            }

            logger.info("Monster spawn data imported successfully");
        }
        catch (IOException e)
        {
            logger.error("Failed to read monster spawn data", e);
        }
    }

    public static void exportMonsterSpawnData(java.util.List<String> patterns)
    {
        logger.info("Exporting monster spawn data for patterns: {}", patterns);

        // Precompile patterns once
        boolean matchAll = false;
        List<Pattern> compiled = new ArrayList<>();
        if (patterns != null)
        {
            for (String p : patterns)
            {
                if ("*".equals(p)) { matchAll = true; break; }
                compiled.add(Pattern.compile(globToRegex(p)));
            }
        }

        // Structure: "modid:entity" -> spawn data
        Map<String, EntitySpawnInfo> monsterData = new HashMap<>();

        for (Biome biome : Biome.REGISTRY)
        {
            String biomeName = biome.getRegistryName().toString();

            for (Biome.SpawnListEntry entry : biome.getSpawnableList(net.minecraft.entity.EnumCreatureType.MONSTER))
            {
                EntityRegistry.EntityRegistration registration = EntityRegistry.instance().lookupModSpawn(entry.entityClass, true);
                if (registration == null)
                    continue;

                String entityKey = registration.getRegistryName().toString();
                if (!(matchAll || matchesAnyCompiled(entityKey, compiled)))
                    continue;

                // Get or create entity entry
                EntitySpawnInfo spawnInfo = monsterData.get(entityKey);
                if (spawnInfo == null)
                {
                    spawnInfo = new EntitySpawnInfo(entry.itemWeight, entry.minGroupCount, entry.maxGroupCount);
                    monsterData.put(entityKey, spawnInfo);
                }

                // Add biome to the list
                spawnInfo.biomes.add(biomeName);
            }
        }

        // Write to JSON file
        File dataDir = new File("spawn_tweaker");
        if (!dataDir.exists())
        {
            dataDir.mkdirs();
        }

        File outputFile = new File(dataDir, "monster_spawns.json");

        try (FileWriter writer = new FileWriter(outputFile))
        {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(monsterData, writer);
            logger.info("Monster spawn data exported to {}", outputFile.getAbsolutePath());
        }
        catch (IOException e)
        {
            logger.error("Failed to write monster spawn data", e);
        }
    }

    // Backward-compatible overload: single modid or '*' handled as a single glob
    public static void exportMonsterSpawnData(String modid)
    {
        java.util.List<String> patterns = java.util.Collections.singletonList(modid);
        exportMonsterSpawnData(patterns);
    }

    private static boolean matchesAnyCompiled(String key, java.util.List<Pattern> patterns)
    {
        if (patterns == null || patterns.isEmpty()) return false;
        for (Pattern pat : patterns)
        {
            if (pat.matcher(key).matches()) return true;
        }
        return false;
    }

    private static String globToRegex(String glob)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < glob.length(); i++)
        {
            char c = glob.charAt(i);
            switch (c)
            {
                case '*': sb.append(".*"); break;
                case '?': sb.append('.'); break;
                case '.': sb.append("\\."); break;
                case '\\': sb.append("\\\\"); break;
                case '+': case '(': case ')': case '|': case '^': case '$': case '[': case ']': case '{': case '}':
                    sb.append('\\').append(c); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    // Helper class to store entity spawn information
    private static class EntitySpawnInfo
    {
        int weight;
        int minGroupSize;
        int maxGroupSize;
        List<String> biomes;

        EntitySpawnInfo(int weight, int minGroupSize, int maxGroupSize)
        {
            this.weight = weight;
            this.minGroupSize = minGroupSize;
            this.maxGroupSize = maxGroupSize;
            this.biomes = new ArrayList<>();
        }
    }
}
