package dev.trustytrojan.spawn_tweaker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.monster.IMob;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ForgeEventHandler
{
    private static final Logger logger = LogManager.getLogger();

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event)
	{
        // filter to only monsters, only run this on a server
        if (!(event.getEntity() instanceof IMob) || event.getWorld().isRemote)
            return;

		EntityLiving el = (EntityLiving) event.getEntity();
        logger.debug("on_join event: entity=\"{}\", class={}, maxHealth={}, isNonBoss={}", el.getName(), el.getClass().getName(), el.getMaxHealth(), el.isNonBoss());

        // Do not touch behavior of vanilla mobs (namespace 'minecraft') — only affect modded monsters
        ResourceLocation rl = EntityList.getKey(el);
        if (rl != null && "minecraft".equals(rl.getNamespace()))
        {
            logger.debug("on_join: skipping vanilla entity {} ({})", el.getName(), rl);
            return;
        }

        // Check spawn rules tied to 'on_join'
        boolean allow = SpawnTweaker.shouldAllowOnJoin(el, event.getWorld().rand);
        if (!allow)
        {
            // Canceling the event prevents the entity from being added to the world
            event.setCanceled(true);
            logger.debug("on_join: spawn canceled for entity=\"{}\" ({}) by on_join rules", el.getName(), el.getClass().getName());
            return;
        }
        else
        {
            logger.debug("on_join: spawn allowed for entity=\"{}\" ({})", el.getName(), el.getClass().getName());
        }
    }
}
