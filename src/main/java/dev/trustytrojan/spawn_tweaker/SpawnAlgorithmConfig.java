package dev.trustytrojan.spawn_tweaker;

public final class SpawnAlgorithmConfig
{
	private SpawnAlgorithmConfig()
	{}

	public static int packAttempts = 3;
	public static int packEntityMaxDistance = 5;
	public static boolean varyY = false;

	// @formatter:off
	public static final int[] spawnRadiusRange = new int[] { 2, 7 };
	// @formatter:on
}
