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
	modid = "${mod_id}",
	name = "${mod_name}",
	version = "${mod_version}",
	// does not depend on clients, so accept any version
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

		// Ensure mod config subdir
		final File modConfigDir = new File(configDir, "spawn_tweaker");
		if (!modConfigDir.exists())
			modConfigDir.mkdirs();

		// Load the config from (minecraft config dir)/spawn_tweaker/
		SpawnRuleManager.load(new File(modConfigDir, "rules.yml"));
		SpawnEntryManager.init();
		SpawnEntryManager.load(new File(modConfigDir, "entries.yml"));
	}

	@EventHandler
	public void serverStarting(final FMLServerStartingEvent event)
	{
		event.registerServerCommand(new CommandSpawnTweaker());
		logger.info("Registered /spawntweaker command");
	}
}
