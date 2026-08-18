package com.effecoria.effect.organic.gene;

import java.util.Locale;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;

/** Body region used by the gene editor. Graft effects stay global; slots only group the UI. */
public enum GeneAnatomySlot {
    HEAD,
    TORSO,
    FORE,
    HIND,
    TAIL,
    DORSUM;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component title() {
        return Component.translatable("gui.effecoria.gene_editor.slot." + id());
    }

    public static boolean presentOn(LivingEntity entity, GeneAnatomySlot slot) {
        if (slot != TAIL) {
            return true;
        }
        return isQuadruped(entity);
    }

    public static boolean isQuadruped(LivingEntity entity) {
        if (entity instanceof Player
                || entity instanceof IronGolem
                || entity instanceof AbstractVillager
                || entity instanceof AbstractIllager
                || entity instanceof Zombie
                || entity instanceof AbstractSkeleton
                || entity instanceof Witch
                || entity instanceof Vex
                || entity instanceof Bat
                || entity instanceof Silverfish) {
            return false;
        }
        return entity.getBbHeight() <= 1.5f || entity.getBbWidth() >= entity.getBbHeight() * 0.55f;
    }
}
