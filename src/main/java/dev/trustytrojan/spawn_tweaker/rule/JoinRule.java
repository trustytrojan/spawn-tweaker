package dev.trustytrojan.spawn_tweaker.rule;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
// No functional BiFunction required - we use RuleEvaluator instead

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
// EntityLiving and EntityRegistry are handled by Rule helpers

// No extra imports required

public class JoinRule extends Rule<EntityJoinWorldEvent, Boolean>
{
	public static final IEventQuery<EntityJoinWorldEvent> EVENT_QUERY_JOIN = new IEventQuery<EntityJoinWorldEvent>() {
		@Override
		public World getWorld(final EntityJoinWorldEvent o)
		{
			return o.getWorld();
		}

		@Override
		public BlockPos getPos(final EntityJoinWorldEvent o)
		{
			return o.getEntity().getPosition();
		}

		@Override
		public BlockPos getValidBlockPos(final EntityJoinWorldEvent o)
		{
			return o.getEntity().getPosition().down();
		}

		@Override
		public int getY(final EntityJoinWorldEvent o)
		{
			return o.getEntity().getPosition().getY();
		}

		@Override
		public Entity getEntity(final EntityJoinWorldEvent o)
		{
			return o.getEntity();
		}
	};

	public String mob;
	public Object mod; // String or List<String>
	public String count;

	public void buildChecks()
	{
		// Map the lightweight JoinRule fields into the generic Rule selector/conditions format
		this.selector = null;
		this.conditions = null;

		if (mob != null || mod != null)
		{
			this.selector = new Rule.Selector();
			if (mob != null)
			{
				final var parts = mob.split(":", 2);
				final var domain = parts[0];
				final var path = parts.length > 1 ? parts[1] : "";
				final var map = new HashMap<String, List<String>>();
				final var list = new ArrayList<String>();
				list.add(path);
				map.put(domain, list);
				this.selector.mobs = map;
			}
			if (mod != null)
			{
				this.selector.mod = mod;
			}
		}

		if (count != null)
		{
			this.conditions = new Rule.Conditions();
			this.conditions.count = count;
		}

		// Use Rule.buildEvaluator() to translate selectors/conditions into a RuleEvaluator
		buildEvaluator();
	}

	@Override
	public Boolean getDefaultResult()
	{
		return false;
	}

	@Override
	public Boolean check(EntityJoinWorldEvent event)
	{
		final var living = getLiving(event, EVENT_QUERY_JOIN);
		if (living == null)
			return false;
		if (!hasRegistryEntry(living))
			return false;

		if (evaluator == null)
			buildEvaluator();
		if (!evaluator.match(event, EVENT_QUERY_JOIN))
			return false;

		final var evalResult = evaluator.evaluate(event, EVENT_QUERY_JOIN);
		if (evalResult != null)
			return evalResult;

		// If there were no explicit conditions, a selector match should be considered a match
		if (conditions == null)
			return true;

		// Otherwise return then/default result (probably false for JoinRule)
		return thenResult != null ? thenResult : getDefaultResult();
	}
}
