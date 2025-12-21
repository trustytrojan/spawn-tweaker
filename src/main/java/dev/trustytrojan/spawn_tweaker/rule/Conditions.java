package dev.trustytrojan.spawn_tweaker.rule;

import java.util.Map;

public class Conditions
{
	public Map<String, Object> mobs;
	public Integer dimension;
	public Range health;
	public Range light;
	public Range height;
	public Double random;
	public Count count;

	public void compile()
	{
		// @formatter:off
		if (health != null) health.compile();
		if (light != null) light.compile();
		if (height != null) height.compile();
		if (count != null) count.compile();
		// @formatter:on
	}
}
