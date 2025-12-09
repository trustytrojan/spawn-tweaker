package dev.trustytrojan.spawn_tweaker;

import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;

// No extra imports required

public class OnJoinRule
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

		@Override
		public DamageSource getSource(final EntityJoinWorldEvent o)
		{
			return null;
		}

		@Override
		public Entity getAttacker(final EntityJoinWorldEvent o)
		{
			return null;
		}

		@Override
		public EntityPlayer getPlayer(final EntityJoinWorldEvent o)
		{
			return null;
		}

		@Override
		public ItemStack getItem(final EntityJoinWorldEvent o)
		{
			return ItemStack.EMPTY;
		}
	};
	public String mob;
	public Object mod; // String or List<String>
	public String count;

	public boolean check(EntityJoinWorldEvent event)
	{
		if (!(event.getEntity() instanceof EntityLiving))
			return false;
		EntityLiving entity = (EntityLiving) event.getEntity();
		final var entry = EntityRegistry.getEntry(entity.getClass());
		if (entry == null || entry.getRegistryName() == null)
			return false;
		ResourceLocation registryName = entry.getRegistryName();
		final var world = event.getWorld();
		final var pos = entity.getPosition();

		if (mob != null)
		{
			// Simple glob matching or direct match
			if (!registryName.toString().matches(GlobUtils.globToRegex(mob)))
				return false;
		}

		if (mod != null)
		{
			final var domain = registryName.toString().split(":")[0];
			if (mod instanceof String)
			{
				if (!domain.matches(GlobUtils.globToRegex((String) mod)))
					return false;
			}
			else if (mod instanceof List)
			{
				boolean matched = false;
				for (final var m : (List<?>) mod)
				{
					if (m == null)
						continue;
					if (domain.matches(GlobUtils.globToRegex(m.toString())))
					{
						matched = true;
						break;
					}
				}
				if (!matched)
					return false;
			}
		}

		if (count != null)
		{
			// Parse count condition
			// e.g. ">=1,perchunk"
			// This requires counting entities in the world/chunk.
			// This is expensive.
			final var parts = count.split(",");
			final var cond = parts[0].trim();
			final var perchunk = parts.length > 1 && parts[1].trim().equalsIgnoreCase("perchunk");
			int cnt = 0;
			if (mob != null)
			{
				if (perchunk)
				{
					final var chunkX = pos.getX() >> 4;
					final var chunkZ = pos.getZ() >> 4;
					cnt = RuleUtils.countEntitiesMatchingGlobInChunk(world, mob, chunkX, chunkZ);
				}
				else
				{
					cnt = RuleUtils.countEntitiesMatchingGlob(world, mob);
				}
			}
			else if (mod != null)
			{
				if (mod instanceof String)
				{
					final var modStr = (String) mod;
					if (perchunk)
					{
						final var chunkX = pos.getX() >> 4;
						final var chunkZ = pos.getZ() >> 4;
						cnt = RuleUtils.countEntitiesMatchingModInChunk(world, modStr, chunkX, chunkZ);
					}
					else
					{
						cnt = RuleUtils.countEntitiesMatchingMod(world, modStr);
					}
				}
				else if (mod instanceof List)
				{
					int total = 0;
					for (final var m : (List<?>) mod)
					{
						if (m == null)
							continue;
						final var modStr = m.toString();
						if (perchunk)
						{
							final var chunkX = pos.getX() >> 4;
							final var chunkZ = pos.getZ() >> 4;
							total += RuleUtils.countEntitiesMatchingModInChunk(world, modStr, chunkX, chunkZ);
						}
						else
						{
							total += RuleUtils.countEntitiesMatchingMod(world, modStr);
						}
					}
					cnt = total;
				}
			}
			else
			{
				cnt = world.loadedEntityList.size();
			}
			if (!RuleUtils.compareNumberCondition(cond, cnt))
				return false;
		}

		return true; // If all conditions match, we return true (meaning the rule matches, so we should deny)
	}
}
