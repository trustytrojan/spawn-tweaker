package dev.trustytrojan.spawn_tweaker;

import java.util.List;
import java.util.Map;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingSpawnEvent.CheckSpawn;
import net.minecraftforge.fml.common.eventhandler.Event;

// No extra imports required here

public class CheckSpawnRule
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

		@Override
		public DamageSource getSource(final CheckSpawn o)
		{
			return null;
		}

		@Override
		public Entity getAttacker(final CheckSpawn o)
		{
			return null;
		}

		@Override
		public EntityPlayer getPlayer(final CheckSpawn o)
		{
			return null;
		}

		@Override
		public ItemStack getItem(final CheckSpawn o)
		{
			return ItemStack.EMPTY;
		}
	};

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
	public Event.Result thenResult; // "then"
	public Event.Result elseResult; // "else"
	private RuleEvaluator<CheckSpawn> evaluator;

	public void buildEvaluator()
	{
		this.evaluator = RuleEvaluator.buildChecksFor(this);
	}

	public Event.Result check(CheckSpawn event)
	{
		if (!(event.getEntity() instanceof EntityLiving))
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

	// checkCountCondition removed; count checks are handled by RuleEvaluator
}
