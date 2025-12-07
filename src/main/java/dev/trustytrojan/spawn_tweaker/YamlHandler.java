package dev.trustytrojan.spawn_tweaker;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Handles reading and writing spawn rules in YAML format.
 */
public class YamlHandler
{
    private static final Logger logger = LogManager.getLogger();
    private static final Yaml YAML = new Yaml();

    /**
     * Read spawn rules from a YAML file.
     * 
     * @param file The YAML file to read from
     * @return List of spawn rules, or null if file doesn't exist or an error occurs
     */
    public static List<SpawnRule> readRules(File file)
    {
        if (!file.exists())
        {
            logger.warn("YAML file not found: {}", file.getAbsolutePath());
            return null;
        }

        try (FileReader reader = new FileReader(file))
        {
            // YAML now has a root mapping: it contains keys like 'on_join' and 'rules'
            Object loaded = YAML.load(reader);
            if (loaded == null)
            {
                logger.warn("No spawn rules found in YAML file");
                return new ArrayList<>();
            }

            // Parse 'on_join' if present
            if (loaded instanceof Map)
            {
                @SuppressWarnings("unchecked")
                Map<String, Object> root = (Map<String, Object>) loaded;
                try
                {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> onJoinRaw = (Map<String, Object>) root.get("on_join");
                    if (onJoinRaw != null)
                    {
                        logger.debug("Parsing on_join section from YAML");
                        parseOnJoin(onJoinRaw);
                    }
                    else
                    {
                        // Clear previous on_join config if YAML doesn't contain it
                        SpawnTweaker.setOnJoinConfig(null);
                        logger.debug("No on_join section found in YAML; cleared on_join config");
                    }
                }
                catch (ClassCastException e)
                {
                    logger.error("Malformed 'on_join' section in YAML - expected mapping", e);
                }

                // Parse the 'rules' list
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> rawRules = (List<Map<String, Object>>) root.get("rules");
                List<SpawnRule> rules = new ArrayList<>();
                if (rawRules != null)
                {
                    for (int i = 0; i < rawRules.size(); i++)
                    {
                        Map<String, Object> rawRule = rawRules.get(i);
                        SpawnRule rule = parseRule(rawRule, i + 1);
                        if (rule != null)
                        {
                            rules.add(rule);
                        }
                    }
                }

                logger.info("Loaded {} spawn rules from YAML: {}", rules.size(), file.getName());
                return rules;
            }

            // Fallback to legacy format (plain list of rules at root)
            if (loaded instanceof List)
            {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> rawRules = (List<Map<String, Object>>) loaded;
                if (rawRules == null || rawRules.isEmpty())
                {
                    logger.warn("No spawn rules found in YAML file");
                    return new ArrayList<>();
                }

                List<SpawnRule> rules = new ArrayList<>();
                for (int i = 0; i < rawRules.size(); i++)
                {
                    Map<String, Object> rawRule = rawRules.get(i);
                    SpawnRule rule = parseRule(rawRule, i + 1);
                    if (rule != null)
                    {
                        rules.add(rule);
                    }
                }

                logger.info("Loaded {} spawn rules from YAML: {}", rules.size(), file.getName());
                // Clear on-join config (legacy file won't have it)
                SpawnTweaker.setOnJoinConfig(null);
                return rules;
            }

            logger.warn("YAML file contains invalid structure; expected mapping with 'rules' or list of rules");
            return new ArrayList<>();
        }
        catch (IOException e)
        {
            logger.error("Failed to read YAML file: " + file.getAbsolutePath(), e);
            return null;
        }
        catch (Exception e)
        {
            logger.error("Failed to parse YAML file: " + file.getAbsolutePath(), e);
            return null;
        }
    }

    private static void parseOnJoin(Map<String, Object> onJoinRaw)
    {
        try
        {
            // bossChance is removed: we only support min_health_chance thresholds now

            java.util.Map<Integer, Double> minHealthChance = new java.util.LinkedHashMap<>();
            @SuppressWarnings("unchecked")
            Map<Object, Object> rawMin = (Map<Object, Object>) onJoinRaw.get("min_health_chance");
            if (rawMin != null)
            {
                for (java.util.Map.Entry<Object, Object> e : rawMin.entrySet())
                {
                    try
                    {
                        int key = Integer.parseInt(e.getKey().toString());
                        Object val = e.getValue();
                        if (val instanceof Number)
                        {
                            minHealthChance.put(key, ((Number) val).doubleValue());
                        }
                    }
                    catch (NumberFormatException nfe)
                    {
                        logger.warn("Skipping invalid min_health_chance key: {}", e.getKey());
                    }
                }
            }
            OnJoinConfig cfg = new OnJoinConfig(minHealthChance);
            SpawnTweaker.setOnJoinConfig(cfg);
            logger.info("Loaded on_join config: min_health_chance entries={}", minHealthChance.size());

            // Log each threshold for visibility
            for (java.util.Map.Entry<Integer, Double> e : minHealthChance.entrySet())
            {
                logger.debug("on_join: min_health_chance threshold {} -> {}", e.getKey(), e.getValue());
            }
        }
        catch (Exception e)
        {
            logger.error("Failed to parse on_join section", e);
        }
    }

    /**
     * Write spawn rules to a YAML file.
     * 
     * @param file The YAML file to write to
     * @param rules The spawn rules to write
     * @return true if successful, false otherwise
     */
    public static boolean writeRules(File file, List<SpawnRule> rules)
    {
        // Ensure parent directory exists
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists())
        {
            parentDir.mkdirs();
        }

        try (FileWriter writer = new FileWriter(file))
        {
            // Build entity-centric export list: one top-level list item per entity
            List<Map<String, Object>> entityList = new ArrayList<>();

            for (SpawnRule rule : rules)
            {
                if (rule == null || rule.forSelector == null || rule.spawn == null) continue;
                java.util.List<String> entities = rule.forSelector.entities;
                java.util.List<String> biomes = rule.forSelector.biomes;
                for (String entityKey : entities)
                {
                    Map<String, Object> ent = new LinkedHashMap<>();
                    ent.put("entity", entityKey);
                    ent.put("weight", rule.spawn.weight);
                    ent.put("minGroupSize", rule.spawn.minGroupSize);
                    ent.put("maxGroupSize", rule.spawn.maxGroupSize);

                    // Group biomes by resource domain
                    Map<String, List<String>> domainMap = new LinkedHashMap<>();
                    if (biomes != null)
                    {
                        for (String biome : biomes)
                        {
                            String domain = "minecraft";
                            String name = biome;
                            int idx = biome.indexOf(':');
                            if (idx >= 0)
                            {
                                domain = biome.substring(0, idx);
                                name = biome.substring(idx + 1);
                            }
                            domainMap.computeIfAbsent(domain, k -> new ArrayList<>()).add(name);
                        }
                    }
                    ent.put("biomes", domainMap);
                    entityList.add(ent);
                }
            }

            // Prepare YAML dumper options to avoid line wrapping and use block style
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setWidth(Integer.MAX_VALUE);
            Yaml yaml = new Yaml(options);
            yaml.dump(entityList, writer);
            logger.info("Exported {} spawn rules to YAML: {}", rules.size(), file.getAbsolutePath());
            return true;
        }
        catch (IOException e)
        {
            logger.error("Failed to write YAML file: " + file.getAbsolutePath(), e);
            return false;
        }
    }

    private static SpawnRule parseRule(Map<String, Object> rawRule, int ruleIndex)
    {
        try
        {
            @SuppressWarnings("unchecked")
            Map<String, Object> forSection = (Map<String, Object>) rawRule.get("for");
            if (forSection == null)
            {
                logger.error("Rule #{}: missing 'for' section, skipping", ruleIndex);
                return null;
            }

            @SuppressWarnings("unchecked")
            List<String> entities = (List<String>) forSection.get("entities");
                @SuppressWarnings("unchecked")
                List<String> biomes = (List<String>) forSection.get("biomes");
                if (entities == null || entities.isEmpty())
                {
                    logger.error("Rule #{}: missing or empty 'entities' in 'for' section, skipping", ruleIndex);
                    return null;
                }
                if (biomes == null || biomes.isEmpty())
                {
                    // Biomes is optional; we will treat this as 'use entity's current biomes' at apply-time
                    logger.debug("Rule #{}: no 'biomes' specified; will use entity's current spawn biomes", ruleIndex);
                    biomes = null;
                }

            @SuppressWarnings("unchecked")
            Map<String, Object> spawnSection = (Map<String, Object>) rawRule.get("spawn");
            if (spawnSection == null)
            {
                logger.error("Rule #{}: missing 'spawn' section, skipping", ruleIndex);
                return null;
            }

            int weight = getIntValue(spawnSection, "weight", 1);
            int minGroupSize = getIntValue(spawnSection, "minGroupSize", 1);
            int maxGroupSize = getIntValue(spawnSection, "maxGroupSize", 1);

            SpawnRule.ForSelector forSelector = new SpawnRule.ForSelector(entities, biomes);
            SpawnRule.SpawnConfig spawnConfig = new SpawnRule.SpawnConfig(weight, minGroupSize, maxGroupSize);
            
            return new SpawnRule(forSelector, spawnConfig);
        }
        catch (Exception e)
        {
            logger.error("Rule #{}: failed to parse rule", ruleIndex, e);
            return null;
        }
    }

    private static Map<String, Object> serializeRule(SpawnRule rule)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        
        // Create 'for' section
        Map<String, Object> forSection = new LinkedHashMap<>();
        forSection.put("entities", rule.forSelector.entities);
        if (rule.forSelector.biomes != null && !rule.forSelector.biomes.isEmpty())
        {
            forSection.put("biomes", rule.forSelector.biomes);
        }
        
        // Create 'spawn' section
        Map<String, Object> spawnSection = new LinkedHashMap<>();
        spawnSection.put("weight", rule.spawn.weight);
        spawnSection.put("minGroupSize", rule.spawn.minGroupSize);
        spawnSection.put("maxGroupSize", rule.spawn.maxGroupSize);
        
        result.put("for", forSection);
        result.put("spawn", spawnSection);
        
        return result;
    }

    private static int getIntValue(Map<String, Object> map, String key, int defaultValue)
    {
        Object value = map.get(key);
        if (value instanceof Number)
        {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
}
