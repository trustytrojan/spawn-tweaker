package dev.trustytrojan.spawn_tweaker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.entity.EntityLiving;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

public class OnJoinConfig
{
    private static final Logger logger = LogManager.getLogger();

    public final java.util.Map<Integer, Double> minHealthChance;

    public OnJoinConfig(java.util.Map<Integer, Double> minHealthChance)
    {
        this.minHealthChance = (minHealthChance != null ? new java.util.LinkedHashMap<>(minHealthChance) : new java.util.LinkedHashMap<>());
    }

    public Double getChanceForHealth(int health)
    {
        int chosenThreshold = Integer.MIN_VALUE;
        Double chosenChance = null;
        for (java.util.Map.Entry<Integer, Double> e : minHealthChance.entrySet())
        {
            int thr = e.getKey();
            if (health >= thr && thr > chosenThreshold)
            {
                chosenThreshold = thr;
                chosenChance = e.getValue();
            }
        }
        return chosenChance;
    }

    public boolean shouldAllowOnJoin(EntityLiving el, java.util.Random rand)
    {
        // Check chunk-generation exemption
        try
        {
            if (el.world != null)
            {
                BlockPos pos = new BlockPos(el);
                Chunk chunk = el.world.getChunkFromBlockCoords(pos);
                if (chunk != null && !chunk.isTerrainPopulated())
                {
                    logger.info("on_join: entity {} allowed due to chunk generation (chunk not populated yet)", el.getName());
                    return true;
                }
            }
        }
        catch (Throwable th)
        {
            logger.debug("on_join: could not determine chunk population state for entity {}: {}", el.getName(), th.getMessage());
        }

        // Health-based checks
        int health = (int) Math.ceil(el.getMaxHealth());
        Double chance = getChanceForHealth(health);
        if (chance != null)
        {
            double roll = rand.nextDouble();
            boolean allow = roll <= chance;
            logger.debug("on_join health_check: entity=\"{}\", maxHealth={}, thresholdChance={}, roll={}, allow={}", el.getName(), health, chance, roll, allow);
            return allow;
        }

        logger.debug("on_join: non-boss entity \"{}\" allowed (no threshold matched for health={})", el.getName(), health);
        return true;
    }
}
