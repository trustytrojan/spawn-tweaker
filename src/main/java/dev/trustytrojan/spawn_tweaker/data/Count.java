package dev.trustytrojan.spawn_tweaker.data;

import java.util.function.Function;

import dev.trustytrojan.spawn_tweaker.event.EntityEventWrapper;
import dev.trustytrojan.spawn_tweaker.mixin.WorldEntitySpawnerAccessor;
import dev.trustytrojan.spawn_tweaker.mixin.WorldServerAccessor;

public class Count extends Range
{
	// Raw data
	public String per; // 'chunk' | null

	// Compiled data
	public transient Function<EntityEventWrapper<?>, Integer> scaler;

	@Override
	public void compile()
	{
		super.compile();

		if ("chunk".equalsIgnoreCase(per))
		{
			scaler = ctx -> {
				final var world = ctx.getWorld();
				final var entitySpawner = ((WorldServerAccessor) world).getEntitySpawner();
				final var eligibleChunks =
					((WorldEntitySpawnerAccessor) (Object) entitySpawner).getEligibleChunks();
				return eligibleChunks.size() / 289;
			};
		}
		else if ("player".equalsIgnoreCase(per))
		{
			scaler = ctx -> ctx.getWorld().playerEntities.size();
		}
		else
		{
			scaler = ctx -> 1;
		}
	}
}
