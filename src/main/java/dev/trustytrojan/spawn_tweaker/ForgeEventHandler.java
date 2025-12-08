package dev.trustytrojan.spawn_tweaker;

import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.entity.EntityLiving;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ForgeEventHandler
{
    private static final Logger logger = LogManager.getLogger();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEntityJoinWorld(final EntityJoinWorldEvent event)
	{
        // filter to only monsters, only run this on a server
        if (!(event.getEntity() instanceof final EntityLiving el) || event.getWorld().isRemote)
            return;

        // Check spawn rules tied to 'on_join'
        final var allow = SpawnTweaker.shouldAllowOnJoin(el, new Random());
        if (!allow)
        {
            // Canceling the event prevents the entity from being added to the world
            event.setCanceled(true);
        }

        logger.debug("on_join: spawn {} for entity=\"{}\" ({})", allow ? "allowed" : "canceled", el.getName(), el.getClass().getName());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEntitySpawnEvent(LivingSpawnEvent.CheckSpawn event)
    {
    }
}
