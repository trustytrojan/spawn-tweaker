package dev.trustytrojan.spawn_tweaker.rule;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import net.minecraft.entity.EntityLiving;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.eventhandler.Event;

public abstract class Rule<E extends Event, R>
{
    // Store closures directly instead of data objects
    public final List<Predicate<ConditionChecker<E>>> selectorChecks = new ArrayList<>();
    public final List<Function<ConditionChecker<E>, R>> conditionChecks = new ArrayList<>();
    public R thenResult; // "then"
    public R elseResult; // "else"
    protected final ConditionChecker<E> checker;

    protected Rule(final IEventQuery<E> query)
    {
        this.checker = new ConditionChecker<>(query);
    }

    /**
     * Helper to add a selector check if the value is non-null.
     */
    protected void addSelectorIf(final Object value, final Predicate<ConditionChecker<E>> check)
    {
        if (value != null)
            selectorChecks.add(check);
    }

    /**
     * Helper to add a condition check if the value is non-null. Returns elseResult (or defaultResult) if the check
     * fails.
     */
    protected void addConditionIf(final Object value, final Predicate<ConditionChecker<E>> check)
    {
        if (value != null)
        {
            final var defaultResult = getDefaultResult();
            conditionChecks
                .add(checker -> check.test(checker) ? null : (elseResult != null ? elseResult : defaultResult));
        }
    }

    /**
     * Evaluate selector checks - returns true if all pass.
     */
    protected boolean matchSelectors(final E event, final IEventQuery<E> query)
    {
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

    /**
     * Evaluate condition checks - returns null if all pass, or the first non-null result.
     */
    protected R evaluateConditions(final E event, final IEventQuery<E> query)
    {
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
                final var defaultResult = getDefaultResult();
                return elseResult != null ? elseResult : defaultResult;
            }
        }

        if (!conditionChecks.isEmpty())
            return thenResult != null ? thenResult : getDefaultResult();
        return null;
    }

    /**
     * Return the default result for this rule (e.g. Event.Result.DEFAULT or false).
     */
    public abstract R getDefaultResult();

    /**
     * Return the result of evaluating this rule against the provided event.
     */
    public abstract R check(E event);

    /**
     * A convenience helper to cast and validate that the event's entity is an EntityLiving. Returns the living entity
     * or null if not present.
     */
    protected EntityLiving getLiving(final E event, final IEventQuery<E> query)
    {
        final var ent = query.getEntity(event);
        if (!(ent instanceof EntityLiving))
            return null;
        return (EntityLiving) ent;
    }

    /**
     * Return whether a registry entry exists for the given living entity and has a registry name.
     */
    protected boolean hasRegistryEntry(final EntityLiving living)
    {
        final var entry = EntityRegistry.getEntry(living.getClass());
        return entry != null && entry.getRegistryName() != null;
    }
}
