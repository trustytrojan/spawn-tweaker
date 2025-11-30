package dev.trustytrojan.spawn_tweaker;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import org.apache.logging.log4j.Logger;

@Mod(modid = SpawnTweakerMod.MODID, name = SpawnTweakerMod.NAME, version = SpawnTweakerMod.VERSION)
public class SpawnTweakerMod
{
    public static final String MODID = "spawn_tweaker";
    public static final String NAME = "Spawn Tweaker";
    public static final String VERSION = "1.0";

    private static Logger logger;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        logger.info("Spawn Tweaker initialized");
    }

    @EventHandler
    public void serverStarting(net.minecraftforge.fml.common.event.FMLServerStartingEvent event)
    {
        event.registerServerCommand(new CommandSpawnTweaker());
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event)
    {
        SpawnTweaker.init();
    }
}
