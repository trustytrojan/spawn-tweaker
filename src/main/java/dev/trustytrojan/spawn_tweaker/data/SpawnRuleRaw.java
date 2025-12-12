package dev.trustytrojan.spawn_tweaker.data;

import com.google.gson.annotations.SerializedName;

// The root mapping for a single rule entry in the YAML list
public class SpawnRuleRaw
{
	public String on; // "join", "spawn" by default

	@SerializedName("for")
	public ConditionsRaw forParams;

	@SerializedName("if")
	public ConditionsRaw ifParams;

	public String then; // "allow", "deny", "default"

	@SerializedName("else")
	public String elseAction; // "allow", "deny", "default"
}
