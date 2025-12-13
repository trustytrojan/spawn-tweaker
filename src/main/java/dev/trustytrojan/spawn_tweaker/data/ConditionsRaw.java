package dev.trustytrojan.spawn_tweaker.data;

import java.util.List;

// Unified conditions class (used by both 'for' and 'if')
public class ConditionsRaw
{
	// modid string
	public String mod;

	// List of modid strings
	public List<String> mods;

	// 'modid:entity' string
	public String mob;

	/**
	 * Either a {@code List<String>} of {@code modid:entity} strings, or a {@code Map<String, List<String>>} in the form:
	 * 
	 * <pre>
	 * { modid: [entity1, entity2, ...], modid2: [...], ... }
	 * </pre>
	 */
	public Object mobs;

	public Integer dimension;
	public RangeRaw health;
	public RangeRaw light;
	public RangeRaw height;
	public Double random;
	public CountRaw count;
}
