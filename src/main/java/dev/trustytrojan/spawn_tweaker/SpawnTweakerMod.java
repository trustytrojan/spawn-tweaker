package dev.trustytrojan.spawn_tweaker;

import java.io.File;

import dev.trustytrojan.spawn_tweaker.event.ForgeEventSubscriber;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

@Mod(
	modid = "${mod_id}",
	name = "${mod_name}",
	version = "${mod_version}",
	// does not depend on clients, so accept any version
	acceptableRemoteVersions = "*"
)
public final class SpawnTweakerMod
{
	public static SpawnEntries spawnEntries;
	public static SpawnRules spawnRules;

	@EventHandler
	public void preInit(final FMLPreInitializationEvent event)
	{
		final var configDir = new File(event.getModConfigurationDirectory(), "spawn_tweaker");
		if (!configDir.exists())
			configDir.mkdir();
		OriginalEntries.init(configDir);
		spawnRules = new SpawnRules(configDir);
		spawnEntries = new SpawnEntries(configDir);
	}

	@EventHandler
	public void postInit(final FMLPostInitializationEvent event)
	{
		MinecraftForge.EVENT_BUS.register(new ForgeEventSubscriber());
		MinecraftForge.EVENT_BUS.register(spawnRules);
		MinecraftForge.EVENT_BUS.register(spawnEntries);
		spawnRules.load();
		OriginalEntries.save();
		spawnEntries.load();
	}

	@EventHandler
	public void serverStarting(final FMLServerStartingEvent event)
	{
		event.registerServerCommand(new CommandSpawnTweaker());
	}
}
