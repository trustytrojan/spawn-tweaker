package dev.trustytrojan.spawn_tweaker;

import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.monster.IMob;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ForgeEventHandler
{
    private static final Logger logger = LogManager.getLogger();

    @SubscribeEvent
    public void onEntityJoinWorld(final EntityJoinWorldEvent event)
	{
        // filter to only monsters, only run this on a server
        if (!(event.getEntity() instanceof IMob) || event.getWorld().isRemote)
            return;

		final var el = (EntityLiving) event.getEntity();

        // Do not touch behavior of vanilla mobs (namespace 'minecraft') — only affect modded monsters
        final var rl = EntityList.getKey(el);
        if (rl != null && "minecraft".equals(rl.getNamespace()))
        {
            logger.trace("on_join: skipping vanilla entity {} ({})", el.getName(), rl);
            return;
        }

        // Check spawn rules tied to 'on_join'
        final var allow = SpawnTweaker.shouldAllowOnJoin(el, new Random());
        if (!allow)
        {
            // Canceling the event prevents the entity from being added to the world
            event.setCanceled(true);
        }

        logger.debug("on_join: spawn {} for entity=\"{}\" ({})", allow ? "allowed" : "canceled", el.getName(), el.getClass().getName());
    }
}
