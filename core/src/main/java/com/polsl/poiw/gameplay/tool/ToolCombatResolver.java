package com.polsl.poiw.gameplay.tool;

import com.polsl.poiw.engine.actor.Actor;
import com.polsl.poiw.gameplay.actor.AbstractCreatureActor;
import com.polsl.poiw.gameplay.actor.CropActor;
import com.polsl.poiw.gameplay.actor.MineableActor;
import com.polsl.poiw.gameplay.actor.TrainingDummyActor;
import com.polsl.poiw.gameplay.actor.TreeActor;

public final class ToolCombatResolver {

    private ToolCombatResolver() {
    }

    public static int resolveDamage(PlayerToolType toolType, Actor target, int baseDamage) {
        PlayerToolType resolvedTool = toolType != null ? toolType : PlayerToolType.SWORD;
        int clampedBaseDamage = Math.max(1, baseDamage);

        if (target instanceof TreeActor) {
            return resolvedTool == PlayerToolType.AXE ? clampedBaseDamage : 0;
        }
        if (target instanceof CropActor) {
            return resolvedTool == PlayerToolType.HOE ? clampedBaseDamage : 0;
        }
        if (target instanceof MineableActor) {
            return resolvedTool == PlayerToolType.PICKAXE ? clampedBaseDamage : 0;
        }
        if (target instanceof AbstractCreatureActor || target instanceof TrainingDummyActor) {
            return switch (resolvedTool) {
                case SWORD -> clampedBaseDamage;
                case AXE -> Math.max(1, Math.round(clampedBaseDamage * 0.7f));
                case PICKAXE -> Math.max(1, Math.round(clampedBaseDamage * 0.5f));
                case HOE -> Math.max(1, Math.round(clampedBaseDamage * 0.3f));
                case WATERING_CAN -> 1;
            };
        }

        return clampedBaseDamage;
    }
}