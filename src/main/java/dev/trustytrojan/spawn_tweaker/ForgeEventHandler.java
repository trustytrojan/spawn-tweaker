package dev.trustytrojan.spawn_tweaker;

import net.minecraftforge.event.entity.living.LivingSpawnEvent.CheckSpawn;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ForgeEventHandler
{
    @SubscribeEvent
    public void onCheckSpawn(final CheckSpawn event)
    {
        // 1. Create the simplified context wrapper
        final var context = new SpawnContext(event);

        // 2. Iterate through our compiled rules
        for (final var rule : SpawnRuleManager.getRules())
        {
            // 3. Evaluate the rule
            final var result = rule.evaluate(context); // 4. If the rule returned a definitive result, apply it and stop
            if (result != null)
            {
                // In Forge 1.12.2, ALLOW forces spawn, DENY stops it, DEFAULT lets vanilla handle it.
                event.setResult(result);
                return;
            }

            // If result was null, the rule conditions weren't met (and no 'else' clause existed),
            // so we continue to the next rule in the list.
        }
    }
}
