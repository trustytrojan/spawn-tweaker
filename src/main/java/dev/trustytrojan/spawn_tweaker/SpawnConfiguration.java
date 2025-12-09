package dev.trustytrojan.spawn_tweaker;

import java.util.ArrayList;
import java.util.List;

public class SpawnConfiguration
{
	public List<OnJoinRule> onJoinDeny = new ArrayList<>();
	public List<CheckSpawnRule> checkSpawn = new ArrayList<>();
	public List<SpawnEntryCombination> spawn = new ArrayList<>();
}
