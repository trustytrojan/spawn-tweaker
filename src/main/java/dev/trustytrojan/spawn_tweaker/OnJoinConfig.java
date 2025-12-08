package dev.trustytrojan.spawn_tweaker;

import java.util.Map;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.entity.EntityLiving;
import net.minecraft.util.math.BlockPos;

public class OnJoinConfig
{
    private static final Logger logger = LogManager.getLogger();

    public final Map<Integer, Double> minHealthChance;

    public OnJoinConfig(final Map<Integer, Double> minHealthChance)
    {
        this.minHealthChance = (minHealthChance != null)
            ? new java.util.LinkedHashMap<>(minHealthChance)
            : new java.util.LinkedHashMap<>();
    }

    public Double getChanceForHealth(final int health)
    {
        var chosenThreshold = Integer.MIN_VALUE;
        Double chosenChance = null;
        for (final var e : minHealthChance.entrySet())
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

    public boolean shouldAllowJoin(final EntityLiving el, final Random rand)
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
                    logger.debug("on_join: entity {} allowed due to chunk generation (chunk not populated yet)", el.getName());
                    return true;
                }
            }
        }
        catch (final Throwable th)
        {
            logger.warn("on_join: could not determine chunk population state for entity {}: {}", el.getName(), th.getMessage());
        }

        // Health-based checks
        final var health = (int) Math.ceil(el.getMaxHealth());
        final var chance = getChanceForHealth(health);
        if (chance != null)
        {
            final var roll = rand.nextDouble();
            final var allow = roll <= chance;
            logger.info("on_join health_check: entity=\"{}\", maxHealth={}, chance={}, roll={:.2f}, allow={}", el.getName(), health, chance, roll, allow);
            return allow;
        }

        logger.debug("on_join: entity \"{}\" allowed (no threshold matched for health={})", el.getName(), health);
        return true;
    }
}
