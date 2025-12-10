package dev.trustytrojan.spawn_tweaker.rule;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingSpawnEvent.CheckSpawn;
import net.minecraftforge.fml.common.eventhandler.Event;

// No extra imports required here

public class SpawnRule extends Rule<CheckSpawn, Event.Result>
{
	public static final IEventQuery<CheckSpawn> EVENT_QUERY = new IEventQuery<>() {
		@Override
		public World getWorld(final CheckSpawn o)
		{
			return o.getWorld();
		}

		@Override
		public BlockPos getPos(final CheckSpawn o)
		{
			return new BlockPos((int) o.getX(), (int) o.getY(), (int) o.getZ());
		}

		@Override
		public BlockPos getValidBlockPos(final CheckSpawn o)
		{
			return new BlockPos((int) o.getX(), (int) o.getY() - 1, (int) o.getZ());
		}

		@Override
		public int getY(final CheckSpawn o)
		{
			return (int) o.getY();
		}

		@Override
		public Entity getEntity(final CheckSpawn o)
		{
			return o.getEntity();
		}
	};

	@Override
	public Event.Result getDefaultResult()
	{
		return Event.Result.DEFAULT;
	}

	@Override
	public Event.Result check(CheckSpawn event)
	{
		final var living = getLiving(event, EVENT_QUERY);
		if (living == null)
			return Event.Result.DEFAULT;

		// Selector checks are delegated to the per-rule evaluator
		if (evaluator == null)
			buildEvaluator();
		if (!evaluator.match(event, EVENT_QUERY))
			return null; // PASS

		// Conditions checks are delegated to the per-rule evaluator; if it returns a result it means it decided on the
		// rule
		final var evalResult = evaluator.evaluate(event, EVENT_QUERY);
		if (evalResult != null)
			return evalResult;

		// If there were no conditions, we can't make a decision - treat as PASS (not handled)
		if (conditions == null)
			return null;

		return thenResult != null ? thenResult : Event.Result.DEFAULT;
	}
}
