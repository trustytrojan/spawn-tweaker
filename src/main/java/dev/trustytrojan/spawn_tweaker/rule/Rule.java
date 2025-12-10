package dev.trustytrojan.spawn_tweaker.rule;

import java.util.List;
import java.util.Map;

import net.minecraft.entity.EntityLiving;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.eventhandler.Event;

public abstract class Rule<E extends Event, R>
{
	public static class Selector
	{
		public Object mod; // String or List<String>
		public String health;
		public Integer dimension;
		public Map<String, List<String>> mobs;
	}

	public static class Conditions
	{
		public Double random;
		public List<Integer> light; // [min, max]
		public String height;
		public String health;
		public String count;
	}

	public Selector selector; // "for"
	public Conditions conditions; // "if"
	public R thenResult; // "then"
	public R elseResult; // "else"
	protected RuleEvaluator<E, R> evaluator;

	public void buildEvaluator()
	{
		this.evaluator = RuleEvaluator.buildChecksFor(this, getDefaultResult());
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
     * A convenience helper to cast and validate that the event's entity is an EntityLiving.
     * Returns the living entity or null if not present.
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
