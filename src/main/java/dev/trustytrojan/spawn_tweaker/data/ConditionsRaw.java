package dev.trustytrojan.spawn_tweaker.data;

import java.util.Map;

// Unified conditions class (used by both 'for' and 'if')
public class ConditionsRaw
{
	public Map<String, /* String | List<String> */ Object> mobs;
	public Integer dimension;
	public RangeRaw health;
	public RangeRaw light;
	public RangeRaw height;
	public Double random;
	public CountRaw count;
}
