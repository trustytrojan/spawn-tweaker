package dev.trustytrojan.spawn_tweaker.event;

import dev.trustytrojan.spawn_tweaker.SpawnRules;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent.CheckSpawn;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * @implNote {@link CheckSpawn} happens before {@link EntityJoinWorldEvent}.
 * @see net.minecraft.world.WorldEntitySpawner#findChunksForSpawning
 */
public final class ForgeEventSubscriber
{
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onCheckSpawn(final CheckSpawn event)
    {
        final var context = new CheckSpawnWrapper(event);
        final var rules = SpawnRules.spawnRules;

        for (int i = 0, n = rules.size(); i < n; ++i)
        {
            final var result = rules.get(i).evaluate(context);

            if (result != null)
            {
                event.setResult(result);
                return;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onJoinWorld(final EntityJoinWorldEvent event)
    {
        final var context = new JoinWorldWrapper(event);
        final var rules = SpawnRules.joinRules;

        for (int i = 0, n = rules.size(); i < n; ++i)
        {
            final var result = rules.get(i).evaluate(context);

            if (result == Result.DENY)
            {
                event.setCanceled(true);
                return;
            }
        }
    }
}
