package dev.trustytrojan.spawn_tweaker;

import org.apache.logging.log4j.Logger;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

@Mod(
    modid = "spawn_tweaker",
    name = "Spawn Tweaker",
    version = "1.1"
)
public class SpawnTweakerMod
{
    private static Logger logger;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event)
    {
        event.registerServerCommand(new CommandSpawnTweaker());
        logger.info("Registered /spawntweaker command");
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event)
    {
        SpawnTweaker.init();
    }
}
