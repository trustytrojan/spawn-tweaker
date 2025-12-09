package dev.trustytrojan.spawn_tweaker;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import net.minecraftforge.event.entity.living.LivingSpawnEvent.CheckSpawn;
import net.minecraftforge.fml.common.eventhandler.Event;

public class RuleEvaluator<T>
{
	public final List<BiFunction<T, IEventQuery<T>, Boolean>> selectorChecks = new ArrayList<>();
	public final List<BiFunction<T, IEventQuery<T>, Event.Result>> conditionChecks = new ArrayList<>();
	public Event.Result thenResult;
	public Event.Result elseResult;

	public boolean match(final T event, final IEventQuery<T> q)
	{
		for (final var c : selectorChecks)
		{
			try
			{
				if (!Boolean.TRUE.equals(c.apply(event, q)))
					return false;
			}
			catch (final Throwable t)
			{
				return false;
			}
		}
		return true;
	}

	public Event.Result evaluate(final T event, final IEventQuery<T> q)
	{
		for (final var c : conditionChecks)
		{
			try
			{
				final var r = c.apply(event, q);
				if (r != null)
					return r;
			}
			catch (final Throwable t)
			{
				return elseResult != null ? elseResult : Event.Result.DEFAULT;
			}
		}

		if (!conditionChecks.isEmpty())
			return thenResult != null ? thenResult : Event.Result.DEFAULT;
		return null;
	}

	public static RuleEvaluator<CheckSpawn> buildChecksFor(final CheckSpawnRule rule)
	{
		final var rc = new RuleEvaluator<CheckSpawn>();
		rc.thenResult = rule.thenResult;
		rc.elseResult = rule.elseResult;

		final var sel = rule.selector;
		if (sel != null)
		{
			if (sel.dimension != null)
				rc.selectorChecks.add(ConditionCheckers.dimension(sel.dimension));

			if (sel.mod != null)
				rc.selectorChecks.add(ConditionCheckers.mod(sel.mod));

			if (sel.mobs != null && !sel.mobs.isEmpty())
				rc.selectorChecks.add(ConditionCheckers.mobs(sel.mobs));

			if (sel.health != null)
				rc.selectorChecks.add(ConditionCheckers.health(sel.health));
		}

		final var cond = rule.conditions;
		if (cond != null)
		{
			if (cond.random != null)
				rc.conditionChecks.add((e, q) -> ConditionCheckers.<CheckSpawn>random(cond.random).apply(e, q)
					? null
					: (rc.elseResult != null ? rc.elseResult : Event.Result.DEFAULT));

			if (cond.health != null)
				rc.conditionChecks.add((e, q) -> ConditionCheckers.<CheckSpawn>health(cond.health).apply(e, q)
					? null
					: (rc.elseResult != null ? rc.elseResult : Event.Result.DEFAULT));

			if (cond.light != null && cond.light.size() == 2)
				rc.conditionChecks
					.add((e, q) -> ConditionCheckers.<CheckSpawn>light(cond.light.get(0), cond.light.get(1)).apply(e, q)
						? null
						: (rc.elseResult != null ? rc.elseResult : Event.Result.DEFAULT));

			if (cond.height != null)
				rc.conditionChecks.add((e, q) -> ConditionCheckers.<CheckSpawn>height(cond.height).apply(e, q)
					? null
					: (rc.elseResult != null ? rc.elseResult : Event.Result.DEFAULT));

			if (cond.count != null)
				rc.conditionChecks
					.add((e, q) -> ConditionCheckers.<CheckSpawn>count(cond.count, rule.selector).apply(e, q)
						? null
						: (rc.elseResult != null ? rc.elseResult : Event.Result.DEFAULT));
		}

		return rc;
	}
}
