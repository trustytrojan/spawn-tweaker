package dev.trustytrojan.spawn_tweaker.rule;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import net.minecraftforge.fml.common.eventhandler.Event;

public class RuleEvaluator<E extends Event, R>
{
	public final List<Predicate<ConditionChecker<E>>> selectorChecks = new ArrayList<>();
	public final List<Function<ConditionChecker<E>, R>> conditionChecks = new ArrayList<>();
	public R thenResult;
	public R elseResult;
	public R defaultResult;

	public boolean match(final E event, final IEventQuery<E> q)
	{
		final var checker = new ConditionChecker<E>(q);
		checker.setEvent(event);
		for (final var c : selectorChecks)
		{
			try
			{
				if (!Boolean.TRUE.equals(c.test(checker)))
					return false;
			}
			catch (final Throwable t)
			{
				return false;
			}
		}
		return true;
	}

	// Returns `null` if the rule should be skipped
	public R evaluate(final E event, final IEventQuery<E> q)
	{
		final var checker = new ConditionChecker<E>(q);
		checker.setEvent(event);
		for (final var c : conditionChecks)
		{
			try
			{
				final var r = c.apply(checker);
				if (r != null)
					return r;
			}
			catch (final Throwable t)
			{
				return elseResult != null ? elseResult : defaultResult;
			}
		}

		if (!conditionChecks.isEmpty())
			return thenResult != null ? thenResult : defaultResult;
		return null;
	}

	public static <E extends Event, R> RuleEvaluator<E, R> buildChecksFor(final Rule<E, R> rule, R defaultResult)
	{
		final var rc = new RuleEvaluator<E, R>();
		rc.thenResult = rule.thenResult;
		rc.elseResult = rule.elseResult;
		rc.defaultResult = defaultResult;

		final var sel = rule.selector;
		if (sel != null)
		{
			if (sel.dimension != null)
				rc.selectorChecks.add((checker) -> checker.dimension(sel.dimension));

			if (sel.mod != null)
				rc.selectorChecks.add((checker) -> checker.mod(sel.mod));

			if (sel.mobs != null && !sel.mobs.isEmpty())
				rc.selectorChecks.add((checker) -> checker.mobs(sel.mobs));

			if (sel.health != null)
				rc.selectorChecks.add((checker) -> checker.health(sel.health));
		}

		final var cond = rule.conditions;
		if (cond != null)
		{
			if (cond.random != null)
				rc.conditionChecks.add((checker) -> checker.random(cond.random)
					? null
					: (rc.elseResult != null ? rc.elseResult : rc.defaultResult));

			if (cond.health != null)
				rc.conditionChecks.add((checker) -> checker.health(cond.health)
					? null
					: (rc.elseResult != null ? rc.elseResult : rc.defaultResult));

			if (cond.light != null && cond.light.size() == 2)
				rc.conditionChecks
					.add((checker) -> checker.light(cond.light.get(0), cond.light.get(1))
						? null
						: (rc.elseResult != null ? rc.elseResult : rc.defaultResult));

			if (cond.height != null)
				rc.conditionChecks.add((checker) -> checker.height(cond.height)
					? null
					: (rc.elseResult != null ? rc.elseResult : rc.defaultResult));

			if (cond.count != null)
				rc.conditionChecks
					.add((checker) -> checker.count(cond.count, rule.selector)
						? null
						: (rc.elseResult != null ? rc.elseResult : rc.defaultResult));
		}

		return rc;
	}
}
