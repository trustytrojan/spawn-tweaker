package dev.trustytrojan.spawn_tweaker;

import java.io.File;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.Logger;

@Mod(
	modid = "spawn_tweaker",
	name = "Spawn Tweaker",
	version = "1.1.1",
	acceptableRemoteVersions = "*"
)
public class SpawnTweakerMod
{
	private static Logger logger;
	private static File configDir;

	@EventHandler
	public void preInit(final FMLPreInitializationEvent event)
	{
		logger = event.getModLog();
		configDir = event.getModConfigurationDirectory();
	}

	@EventHandler
	public void postInit(final FMLPostInitializationEvent event)
	{
		// Register the event handler
		MinecraftForge.EVENT_BUS.register(new ForgeEventHandler());

		// Load the config
		SpawnRuleManager.load(new File(configDir, "check-spawn.yml"));
		SpawnEntryManager.init();
		SpawnEntryManager.load(new File(configDir, "spawn-entries.yml"));

		logger.info("Registered SpawnEventHandler and loaded config");
	}

	@EventHandler
	public void serverStarting(final FMLServerStartingEvent event)
	{
		event.registerServerCommand(new CommandSpawnTweaker());
		logger.info("Registered /spawntweaker command");
	}
}
