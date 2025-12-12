package dev.trustytrojan.spawn_tweaker.data;

import java.util.List;

public class SpawnEntryRaw
{
	public String mob;
	public Object mobs;
	public String mod;
	public List<String> mods;
	public List<String> biomes;
	public Integer weight;
	public List<Integer> group_size;

	@Override
	public String toString()
	{
		return String.format("weight=%s group_size=%s", weight, group_size);
	}
}
