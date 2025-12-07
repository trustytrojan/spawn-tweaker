package dev.trustytrojan.spawn_tweaker;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

import org.junit.Test;

public class YamlHandlerTest {

    @Test
    public void testReadRulesRejectsExportFormat() throws Exception {
        String exportYaml = "- entity: testmod:mob\n  weight: 10\n  minGroupSize: 1\n  maxGroupSize: 1\n  biomes:\n    minecraft:\n      - plains\n";
        File tmp = File.createTempFile("spawntweaker-test", ".yml");
        tmp.deleteOnExit();
        try (FileWriter writer = new FileWriter(tmp)) {
            writer.write(exportYaml);
        }

        List<SpawnRule> rules = YamlHandler.readRules(tmp);
        assertNotNull("readRules should return a non-null list even if none found", rules);
        assertTrue("Export-format YAML should not be accepted as main config rules", rules.isEmpty());
    }
}
