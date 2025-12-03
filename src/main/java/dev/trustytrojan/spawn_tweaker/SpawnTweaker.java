package dev.trustytrojan.spawn_tweaker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

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
                String entityGlob = entry.getKey();
                EntitySpawnInfo spawnInfo = entry.getValue();

                if (spawnInfo == null)
                {
                    logger.error("{}: spawnInfo is null, skipping", entityGlob);
                    continue;
                }

                if (spawnInfo.biomes == null || spawnInfo.biomes.isEmpty())
                {
                    logger.error("{}: biomes is null or empty, skipping", entityGlob);
                    continue;
                }

                // Resolve target biomes from (possibly globbed) biome keys
                Biome[] targetBiomes = resolveBiomesFromGlobs(spawnInfo.biomes);
                if (targetBiomes.length == 0)
                {
                    logger.warn("{}: no biomes matched from patterns {} — skipping", entityGlob, spawnInfo.biomes);
                    continue;
                }

                // Treat the entity key as a glob and match against all registered entities
                Pattern entityPattern = Pattern.compile(globToRegex(entityGlob));
                int matchedCount = 0;
                for (ResourceLocation rl : ForgeRegistries.ENTITIES.getKeys())
                {
                    String key = rl.toString();
                    if (!entityPattern.matcher(key).matches())
                        continue;

                    EntityEntry entityEntry = ForgeRegistries.ENTITIES.getValue(rl);
                    if (entityEntry == null)
                        continue;

                    @SuppressWarnings("unchecked")
                    Class<? extends EntityLiving> entityClass = (Class<? extends EntityLiving>) entityEntry.getEntityClass();

                    // Remove old spawns for this entity across all biomes, then add for matched biomes
                    EntityRegistry.removeSpawn(entityClass, EnumCreatureType.MONSTER, ALL_BIOMES);
                    EntityRegistry.addSpawn(
                        entityClass, spawnInfo.weight, spawnInfo.minGroupSize, spawnInfo.maxGroupSize,
                        EnumCreatureType.MONSTER, targetBiomes
                    );

                    matchedCount++;
                }

                if (matchedCount == 0)
                {
                    logger.warn("\"{}\" matched zero entities — nothing changed", entityGlob);
                }
                else
                {
                    logger.info("\"{}\" matched {} entities; set spawn weight={} min={} max={} for {}",
                        entityGlob, matchedCount, spawnInfo.weight, spawnInfo.minGroupSize, spawnInfo.maxGroupSize,
                        (targetBiomes == ALL_BIOMES) ? "all biomes" : String.format("biomes=%s", spawnInfo.biomes));
                }
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

        File outputFile = new File(dataDir, "monster_spawns_export.json");

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

    private static Biome[] resolveBiomesFromGlobs(List<String> biomeKeys)
    {
        if (biomeKeys == null || biomeKeys.isEmpty())
            return new Biome[0];

        // Fast path: if "*" is present, match all biomes
        if (biomeKeys.contains("*"))
        {
            return ALL_BIOMES;
        }

        // Build a set to avoid duplicates when multiple patterns overlap
        java.util.LinkedHashSet<Biome> matched = new java.util.LinkedHashSet<>();

        // Pre-compile patterns for efficiency
        java.util.List<Pattern> patterns = new java.util.ArrayList<>();
        for (String key : biomeKeys)
        {
            patterns.add(Pattern.compile(globToRegex(key)));
        }

        for (ResourceLocation rl : Biome.REGISTRY.getKeys())
        {
            String name = rl.toString();
            for (Pattern p : patterns)
            {
                if (p.matcher(name).matches())
                {
                    Biome b = Biome.REGISTRY.getObject(rl);
                    if (b != null)
                        matched.add(b);
                    break; // no need to test other patterns once matched
                }
            }
        }

        return matched.toArray(new Biome[matched.size()]);
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
