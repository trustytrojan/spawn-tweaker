package dev.trustytrojan.spawn_tweaker;

import java.util.ArrayList;
import java.util.List;

import dev.trustytrojan.spawn_tweaker.rule.SpawnRule;
import dev.trustytrojan.spawn_tweaker.rule.JoinRule;

public class Configuration
{
	public List<JoinRule> onJoinDeny = new ArrayList<>();
	public List<SpawnRule> checkSpawn = new ArrayList<>();
	public List<SpawnEntryCombination> spawn = new ArrayList<>();
}
