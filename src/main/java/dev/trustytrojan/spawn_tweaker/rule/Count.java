package dev.trustytrojan.spawn_tweaker.rule;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import dev.trustytrojan.spawn_tweaker.Util;
import dev.trustytrojan.spawn_tweaker.event.EntityEventWrapper;
import dev.trustytrojan.spawn_tweaker.mixin.WorldEntitySpawnerAccessor;
import dev.trustytrojan.spawn_tweaker.mixin.WorldServerAccessor;

public class Count extends Range
{
	// Raw data
	public String per; // 'chunk' | null
	public Map<String, Object> mobs; // when present, count this explicit selector group

	// Compiled data
	public transient Function<EntityEventWrapper<?>, Integer> scaler;
	public transient boolean isGroupCount = false;
	public transient Set<Class<? extends net.minecraft.entity.EntityLiving>> mobClasses;

	@Override
	public void compile()
	{
		super.compile();

		if (mobs != null)
		{
			isGroupCount = true;
			mobClasses = Util.resolveEntityClasses(mobs).collect(Collectors.toSet());
		}

		if ("chunk".equalsIgnoreCase(per))
		{
			scaler = ctx ->
			{
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
