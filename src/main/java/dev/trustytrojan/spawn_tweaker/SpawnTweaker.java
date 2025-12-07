package dev.trustytrojan.spawn_tweaker;

import java.io.File;
import java.util.ArrayList;
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
    // Configuration applied on entity join events
    private static volatile OnJoinConfig onJoinConfig = null;

    public static void init()
    {
        ALL_BIOMES = new Biome[Biome.REGISTRY.getKeys().size()];
        int i = 0;
        for (Biome b : Biome.REGISTRY)
            ALL_BIOMES[i++] = b;

        importMonsterSpawnData();
    }

    /**
     * Import monster spawn data from YAML file (`monster_spawns.yml`).
     */
    public static void importMonsterSpawnData()
    {
        logger.info("Importing monster spawn data...");

        File dataDir = new File("spawn_tweaker");
        File yamlFile = new File(dataDir, "monster_spawns.yml");
        // JSON support removed; only YAML is used

        List<SpawnRule> rules = null;

        // Try YAML first
        if (yamlFile.exists())
        {
            rules = YamlHandler.readRules(yamlFile);
        }
        // (JSON support removed - prefer YAML)
        else
        {
            logger.warn("No monster spawn data file found (tried .yml)");
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

    // on_join config is now provided by `OnJoinConfig` (top-level class)

    public static void setOnJoinConfig(OnJoinConfig cfg)
    {
        onJoinConfig = cfg;
    }

    public static OnJoinConfig getOnJoinConfig()
    {
        return onJoinConfig;
    }

    /**
     * Evaluate whether an entity's spawn should be allowed at join, per configured settings.
     */
    public static boolean shouldAllowOnJoin(net.minecraft.entity.EntityLiving el, java.util.Random rand)
    {
        OnJoinConfig cfg = onJoinConfig;
        if (cfg == null) return true; // no config => allow
        return cfg.shouldAllowOnJoin(el, rand);
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

            // Note: 'biomes' on a rule is optional. If absent, we will use the
            // set of biomes where each matched entity is currently listed to spawn.

            // Match entities and apply spawn settings
            int totalMatched = 0;
            java.util.LinkedHashSet<Biome> affectedBiomes = new java.util.LinkedHashSet<>();
            for (String entityPattern : rule.forSelector.entities)
            {
                Pattern pattern = Pattern.compile(GlobUtils.globToRegex(entityPattern));
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

                    Biome[] targetBiomesForEntity = getTargetBiomesForEntity(rule, entityClass, rl, ruleIdx, key);
                    if (targetBiomesForEntity == null || targetBiomesForEntity.length == 0)
                    {
                        // getTargetBiomesForEntity logs warning when needed
                        continue;
                    }

                    applySpawnForEntity(entityClass, rule.spawn, targetBiomesForEntity);
                    for (Biome b : targetBiomesForEntity) affectedBiomes.add(b);

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
                    ruleIdx + 1, totalMatched, affectedBiomes.size());
            }
        }
    }

    private static Biome[] getTargetBiomesForEntity(SpawnRule rule, Class<? extends EntityLiving> entityClass, ResourceLocation rl, int ruleIdx, String entityKey)
    {
        if (rule.forSelector.biomes == null || rule.forSelector.biomes.isEmpty())
        {
            java.util.List<Biome> biomesForEntity = new java.util.ArrayList<>();
            for (Biome b : Biome.REGISTRY)
            {
                for (Biome.SpawnListEntry entry : b.getSpawnableList(EnumCreatureType.MONSTER))
                {
                    if (entry.entityClass.equals(entityClass))
                    {
                        biomesForEntity.add(b);
                        break;
                    }
                }
            }
            if (biomesForEntity.isEmpty())
            {
                logger.warn("Rule #{}: entity {} currently registered in zero biomes, skipping", ruleIdx + 1, entityKey);
                return new Biome[0];
            }
            return biomesForEntity.toArray(new Biome[biomesForEntity.size()]);
        }

        Biome[] targetBiomesGlob = GlobUtils.resolveBiomesFromGlobs(rule.forSelector.biomes);
        if (targetBiomesGlob.length == 0)
        {
            logger.warn("Rule #{}: no biomes matched from patterns {} — skipping", ruleIdx + 1, rule.forSelector.biomes);
            return new Biome[0];
        }
        return targetBiomesGlob;
    }

    private static void applySpawnForEntity(Class<? extends EntityLiving> entityClass, SpawnRule.SpawnConfig spawn, Biome[] targetBiomesForEntity)
    {
        EntityRegistry.removeSpawn(entityClass, EnumCreatureType.MONSTER, ALL_BIOMES);
        EntityRegistry.addSpawn(entityClass, spawn.weight, spawn.minGroupSize, spawn.maxGroupSize,
            EnumCreatureType.MONSTER, targetBiomesForEntity);
    }

    /**
     * Export monster spawn data to YAML format.
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
                compiled.add(Pattern.compile(GlobUtils.globToRegex(p)));
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
                if (!(matchAll || GlobUtils.matchesAnyCompiled(entityKey, compiled)))
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
    }

    // Backward-compatible overload: single modid or '*' handled as a single glob
    public static void exportMonsterSpawnData(String modid)
    {
        java.util.List<String> patterns = java.util.Collections.singletonList(modid);
        exportMonsterSpawnData(patterns);
    }

    // matchesAnyCompiled moved to GlobUtils

    // globToRegex moved to GlobUtils

    /**
     * Resolve biome patterns (with wildcards) to actual Biome instances.
     * 
     * @param biomeKeys List of biome patterns (e.g., "minecraft:plains", "biomesoplenty:*")
     * @return Array of matched biomes
     */
    // resolveBiomesFromGlobs moved to GlobUtils

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
