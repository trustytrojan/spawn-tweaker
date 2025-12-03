package dev.trustytrojan.spawn_tweaker;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    /**
     * Import monster spawn data from either YAML or JSON file.
     * Tries YAML first (.yml), then falls back to JSON (.json).
     */
    public static void importMonsterSpawnData()
    {
        logger.info("Importing monster spawn data...");

        File dataDir = new File("spawn_tweaker");
        File yamlFile = new File(dataDir, "monster_spawns.yml");
        File jsonFile = new File(dataDir, "monster_spawns.json");

        List<SpawnRule> rules = null;

        // Try YAML first
        if (yamlFile.exists())
        {
            rules = YamlHandler.readRules(yamlFile);
        }
        // Fall back to JSON
        else if (jsonFile.exists())
        {
            rules = JsonHandler.readRules(jsonFile);
        }
        else
        {
            logger.warn("No monster spawn data file found (tried .yml and .json)");
            return;
        }

        if (rules == null || rules.isEmpty())
        {
            logger.warn("No valid spawn rules to import");
            return;
        }

        applySpawnRules(rules);
        logger.info("Monster spawn data imported successfully");
    }

    /**
     * Apply a list of spawn rules to the game.
     * 
     * @param rules The spawn rules to apply
     */
    private static void applySpawnRules(List<SpawnRule> rules)
    {
        for (int ruleIdx = 0; ruleIdx < rules.size(); ruleIdx++)
        {
            SpawnRule rule = rules.get(ruleIdx);
            
            if (rule.forSelector.entities == null || rule.forSelector.entities.isEmpty())
            {
                logger.error("Rule #{}: missing or empty 'entities', skipping", ruleIdx + 1);
                continue;
            }

            if (rule.forSelector.biomes == null || rule.forSelector.biomes.isEmpty())
            {
                logger.error("Rule #{}: missing or empty 'biomes', skipping", ruleIdx + 1);
                continue;
            }

            // Resolve target biomes from patterns
            Biome[] targetBiomes = resolveBiomesFromGlobs(rule.forSelector.biomes);
            if (targetBiomes.length == 0)
            {
                logger.warn("Rule #{}: no biomes matched from patterns {} — skipping", 
                    ruleIdx + 1, rule.forSelector.biomes);
                continue;
            }

            // Match entities and apply spawn settings
            int totalMatched = 0;
            for (String entityPattern : rule.forSelector.entities)
            {
                Pattern pattern = Pattern.compile(globToRegex(entityPattern));
                int matchedCount = 0;

                for (ResourceLocation rl : ForgeRegistries.ENTITIES.getKeys())
                {
                    String key = rl.toString();
                    if (!pattern.matcher(key).matches())
                        continue;

                    EntityEntry entityEntry = ForgeRegistries.ENTITIES.getValue(rl);
                    if (entityEntry == null)
                        continue;

                    @SuppressWarnings("unchecked")
                    Class<? extends EntityLiving> entityClass = 
                        (Class<? extends EntityLiving>) entityEntry.getEntityClass();

                    // Remove old spawns for this entity across all biomes, then add for matched biomes
                    EntityRegistry.removeSpawn(entityClass, EnumCreatureType.MONSTER, ALL_BIOMES);
                    EntityRegistry.addSpawn(
                        entityClass, rule.spawn.weight, rule.spawn.minGroupSize, rule.spawn.maxGroupSize,
                        EnumCreatureType.MONSTER, targetBiomes
                    );

                    matchedCount++;
                }

                if (matchedCount == 0)
                {
                    logger.warn("Rule #{}: pattern \"{}\" matched zero entities", ruleIdx + 1, entityPattern);
                }
                else
                {
                    totalMatched += matchedCount;
                }
            }

            if (totalMatched > 0)
            {
                logger.info("Rule #{} applied for {} entities in {} biomes",
                    ruleIdx + 1, totalMatched, rule.forSelector.biomes.size());
            }
        }
    }

    /**
     * Export monster spawn data to both YAML and JSON formats.
     * 
     * @param patterns Entity patterns to export (e.g., "modid:*", "*")
     */
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

        // Convert spawn data to rules
        List<SpawnRule> rules = new ArrayList<>();
        java.util.Map<String, EntitySpawnInfo> entitySpawnMap = new java.util.LinkedHashMap<>();

        for (Biome biome : Biome.REGISTRY)
        {
            String biomeName = biome.getRegistryName().toString();

            for (Biome.SpawnListEntry entry : biome.getSpawnableList(EnumCreatureType.MONSTER))
            {
                EntityRegistry.EntityRegistration registration = 
                    EntityRegistry.instance().lookupModSpawn(entry.entityClass, true);
                if (registration == null)
                    continue;

                String entityKey = registration.getRegistryName().toString();
                if (!(matchAll || matchesAnyCompiled(entityKey, compiled)))
                    continue;

                // Get or create entity entry
                EntitySpawnInfo spawnInfo = entitySpawnMap.get(entityKey);
                if (spawnInfo == null)
                {
                    spawnInfo = new EntitySpawnInfo(entry.itemWeight, entry.minGroupCount, entry.maxGroupCount);
                    entitySpawnMap.put(entityKey, spawnInfo);
                }

                // Add biome to the list
                spawnInfo.biomes.add(biomeName);
            }
        }

        // Convert to SpawnRule format
        for (java.util.Map.Entry<String, EntitySpawnInfo> entry : entitySpawnMap.entrySet())
        {
            List<String> entities = new ArrayList<>();
            entities.add(entry.getKey());
            
            SpawnRule.ForSelector forSelector = new SpawnRule.ForSelector(entities, entry.getValue().biomes);
            SpawnRule.SpawnConfig spawnConfig = new SpawnRule.SpawnConfig(
                entry.getValue().weight,
                entry.getValue().minGroupSize,
                entry.getValue().maxGroupSize
            );
            
            rules.add(new SpawnRule(forSelector, spawnConfig));
        }

        // Export to both formats
        File dataDir = new File("spawn_tweaker");
        if (!dataDir.exists())
        {
            dataDir.mkdirs();
        }

        YamlHandler.writeRules(new File(dataDir, "monster_spawns_export.yml"), rules);
        JsonHandler.writeRules(new File(dataDir, "monster_spawns_export.json"), rules);
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

    /**
     * Resolve biome patterns (with wildcards) to actual Biome instances.
     * 
     * @param biomeKeys List of biome patterns (e.g., "minecraft:plains", "biomesoplenty:*")
     * @return Array of matched biomes
     */
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
        LinkedHashSet<Biome> matched = new LinkedHashSet<>();

        // Pre-compile patterns for efficiency
        List<Pattern> patterns = new ArrayList<>();
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
