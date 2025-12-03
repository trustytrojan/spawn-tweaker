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
            List<Map<String, Object>> rawRules = YAML.load(reader);
            
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
            return rules;
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
            List<Map<String, Object>> rawRules = new ArrayList<>();
            for (SpawnRule rule : rules)
            {
                rawRules.add(serializeRule(rule));
            }

            YAML.dump(rawRules, writer);
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
                logger.error("Rule #{}: missing or empty 'biomes' in 'for' section, skipping", ruleIndex);
                return null;
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
        forSection.put("biomes", rule.forSelector.biomes);
        
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
