package dev.trustytrojan.spawn_tweaker;

import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent.CheckSpawn;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ForgeEventHandler
{
    @SubscribeEvent
    public void onCheckSpawn(final CheckSpawn event)
    {
        final var rules = SpawnRules.get();
        if (rules == null)
            return;

        // 1. Create the simplified context wrapper
        final var context = new CheckSpawnWrapper(event);

        // 2. Iterate through our compiled rules
        for (final var rule : SpawnRules.get())
        {
            // 3. Evaluate the rule
            @SuppressWarnings("unchecked")
            final var result = ((CompiledRule<CheckSpawn>) rule).evaluate(context);

            // 4. If the rule returned a definitive result, apply it and stop
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

    @SubscribeEvent
    public void onJoinWorld(final EntityJoinWorldEvent event)
    {
        final var rules = SpawnRules.get();
        if (rules == null)
            return;

        final var context = new JoinWorldWrapper(event);

        for (final var rule : SpawnRules.get())
        {
            if (!rule.isOnJoin())
                continue;

            @SuppressWarnings("unchecked")
            final var result = ((CompiledRule<EntityJoinWorldEvent>) rule).evaluate(context);

            if (result != null)
            {
                event.setResult(result);
                return;
            }
        }
    }
}
