package dev.trustytrojan.spawn_tweaker.coremod;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import zone.rong.mixinbooter.IEarlyMixinLoader;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Early mixin loader coremod to register vanilla/Forge-targeting mixins.
 * Kept in its own package to avoid transformer exclusion overlapping mixin classes.
 */
@IFMLLoadingPlugin.TransformerExclusions({"dev.trustytrojan.spawn_tweaker.coremod"})
public class CoremodLoadingPlugin implements IFMLLoadingPlugin, IEarlyMixinLoader
{
	@Override
	public String[] getASMTransformerClass()
	{
		return new String[0];
	}

	@Override
	public String getModContainerClass()
	{
		return null;
	}

	@Override
	public String getSetupClass()
	{
		return null;
	}

	@Override
	public void injectData(Map<String, Object> data)
	{
		// No-op for mixin registration
	}

	@Override
	public String getAccessTransformerClass()
	{
		return null;
	}

	// IEarlyMixinLoader
	@Override
	public List<String> getMixinConfigs()
	{
		return Arrays.asList("mixins.spawn_tweaker.json");
	}
}
