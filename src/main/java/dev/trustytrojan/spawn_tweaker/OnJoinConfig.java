package dev.trustytrojan.spawn_tweaker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.entity.EntityLiving;
import net.minecraft.util.math.BlockPos;

public class OnJoinConfig
{
    private static final Logger logger = LogManager.getLogger();

    public final java.util.Map<Integer, Double> minHealthChance;

    public OnJoinConfig(final java.util.Map<Integer, Double> minHealthChance)
    {
        this.minHealthChance = (minHealthChance != null ? new java.util.LinkedHashMap<>(minHealthChance) : new java.util.LinkedHashMap<>());
    }

    public Double getChanceForHealth(final int health)
    {
        var chosenThreshold = Integer.MIN_VALUE;
        Double chosenChance = null;
        for (java.util.Map.Entry<Integer, Double> e : minHealthChance.entrySet())
        {
            final var thr = e.getKey();
            if (health >= thr && thr > chosenThreshold)
            {
                chosenThreshold = thr;
                chosenChance = e.getValue();
            }
        }
        return chosenChance;
    }

    public boolean shouldAllowOnJoin(final EntityLiving el, final  java.util.Random rand)
    {
        // Check chunk-generation exemption
        try
        {
            if (el.world != null)
            {
                final var pos = new BlockPos(el);
                final var chunk = el.world.getChunk(pos);
                if (chunk != null && !chunk.isTerrainPopulated())
                {
                    logger.info("on_join: entity {} allowed due to chunk generation (chunk not populated yet)", el.getName());
                    return true;
                }
            }
        }
        catch (final Throwable th)
        {
            logger.debug("on_join: could not determine chunk population state for entity {}: {}", el.getName(), th.getMessage());
        }

        // Health-based checks
        final var health = (int) Math.ceil(el.getMaxHealth());
        final var chance = getChanceForHealth(health);
        if (chance != null)
        {
            final var roll = rand.nextDouble();
            final var allow = roll <= chance;
            logger.debug("on_join health_check: entity=\"{}\", maxHealth={}, thresholdChance={}, roll={}, allow={}", el.getName(), health, chance, roll, allow);
            return allow;
        }

        logger.debug("on_join: non-boss entity \"{}\" allowed (no threshold matched for health={})", el.getName(), health);
        return true;
    }
}
