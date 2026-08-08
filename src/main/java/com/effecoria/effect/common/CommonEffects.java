package com.effecoria.effect.common;

import javax.annotation.Nullable;

import com.effecoria.content.ModItems;
import com.effecoria.content.ModParticleTypes;
import com.effecoria.content.PhiHarnessItems;
import com.effecoria.core.formula.BreathDebuffs;
import com.effecoria.core.magic.SpellEffectEntry;
import com.effecoria.core.progression.ExhaustionService;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;
import com.effecoria.effect.mental.MentalityService;
import com.effecoria.world.ModDimensions;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Universal techniques shared by every initiated school (encyclopedia §2.2–2.6). */
public final class CommonEffects {
    private CommonEffects() {}

    /** §2.2 Ψ-adrenaline — muscle/nerve surge; burns hard. */
    public static void psiAdrenaline(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = scaleTicks(effect, power, "duration_ticks", 120);
        int amp = Mth.clamp(Math.round(power / 55f), 0, 2);
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, amp, false, true, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, amp, false, true, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.DIG_SPEED, duration, Math.max(0, amp), false, true, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.JUMP, duration, 0, false, true, true));
        // Fast burn — hunger spike + mild exhaustion.
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.HUNGER, Math.min(100, duration), 1, true, false, true));
        PlayerPsiData data = PsiHelper.get(caster);
        ExhaustionService.addExhaustion(data, 4f + power * 0.04f);
        PsiHelper.set(caster, data);

        ServerLevel level = caster.serverLevel();
        Vec3 at = caster.position().add(0, 1, 0);
        level.sendParticles(ModParticleTypes.PHI_SPARK.get(), at.x, at.y, at.z, 14, 0.35, 0.4, 0.35, 0.02);
        level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 10, 0.3, 0.35, 0.3, 0.15);
        level.playSound(null, caster.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.55f, 1.6f);
        caster.displayClientMessage(Component.translatable("message.effecoria.common.adrenaline"), true);
    }

    /**
     * Φ-thrust — shove yourself along look by pushing against the Φ-field.
     * Primary locomotion in hyperspace weightlessness; short hop elsewhere.
     */
    public static void phiThrust(ServerPlayer caster, SpellEffectEntry effect, float power) {
        float strength = effect.params().has("strength") ? effect.params().get("strength").getAsFloat() : 1.35f;
        strength *= 0.9f + power / 140f;
        if (ModDimensions.isSubspace(caster.level())) {
            strength *= 1.45f;
        }

        Vec3 look = caster.getLookAngle().normalize();
        Vec3 impulse = look.scale(strength);
        // Slight upward bias in realspace so the cast reads as a hop, not a floor scrape.
        if (!ModDimensions.isSubspace(caster.level()) && look.y < 0.15) {
            impulse = impulse.add(0, 0.22, 0);
        }
        caster.setDeltaMovement(caster.getDeltaMovement().add(impulse));
        caster.hurtMarked = true;
        caster.hasImpulse = true;
        caster.fallDistance = 0f;

        ServerLevel level = caster.serverLevel();
        Vec3 at = caster.position().add(0, 1.0, 0);
        Vec3 wake = look.scale(-0.55);
        level.sendParticles(
                ModParticleTypes.PHI_SPARK.get(),
                at.x + wake.x,
                at.y + wake.y,
                at.z + wake.z,
                16,
                0.15,
                0.2,
                0.15,
                0.04);
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 6, 0.12, 0.15, 0.12, 0.02);
        level.playSound(null, caster.blockPosition(), SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 0.55f, 1.55f);
        caster.displayClientMessage(Component.translatable("message.effecoria.common.thrust"), true);
    }

    /** §2.3 Φ-illumination — cold Cherenkov glow. */
    public static void phiGlow(ServerPlayer caster, SpellEffectEntry effect, float power) {
        int duration = scaleTicks(effect, power, "duration_ticks", 400);
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0, false, false, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.GLOWING, duration, 0, false, true, true));

        ServerLevel level = caster.serverLevel();
        Vec3 at = caster.position().add(0, 1.1, 0);
        level.sendParticles(ModParticleTypes.PHI_SPARK.get(), at.x, at.y, at.z, 18, 0.4, 0.5, 0.4, 0.01);
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 8, 0.25, 0.35, 0.25, 0.01);
        level.playSound(null, caster.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7f, 1.4f);
        caster.displayClientMessage(Component.translatable("message.effecoria.common.glow"), true);
    }

    /** §2.4 Ψ-charge — pour Ψ into a held / inventory Phi Cell. */
    public static void psiCharge(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ItemStack cell = findChargeablePhiCell(caster);
        if (cell.isEmpty()) {
            caster.displayClientMessage(Component.translatable("message.effecoria.common.charge_need_cell"), true);
            return;
        }
        float before = PhiHarnessItems.cellCharge(cell);
        if (before >= 0.999f) {
            caster.displayClientMessage(Component.translatable("message.effecoria.common.charge_full"), true);
            return;
        }
        float gain = Mth.clamp(0.18f + power * 0.004f, 0.15f, 0.55f);
        if (effect.params().has("charge")) {
            gain = effect.params().get("charge").getAsFloat();
        }
        float after = Mth.clamp(before + gain, 0f, 1f);
        PhiHarnessItems.setCellCharge(cell, after);

        ServerLevel level = caster.serverLevel();
        Vec3 at = caster.getEyePosition().add(caster.getLookAngle().scale(0.4));
        level.sendParticles(ModParticleTypes.PHI_SPARK.get(), at.x, at.y, at.z, 12, 0.15, 0.15, 0.15, 0.02);
        level.playSound(null, caster.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.45f, 1.5f);
        caster.displayClientMessage(
                Component.translatable("message.effecoria.phi_cell_charged", Math.round(after * 100f)), true);
    }

    /** §2.5 Ψ-link — emotional impulse to another mind. */
    public static void psiLink(
            ServerPlayer caster, SpellEffectEntry effect, float power, @Nullable LivingEntity target) {
        if (target == null || target == caster) {
            caster.displayClientMessage(Component.translatable("message.effecoria.common.link_need_target"), true);
            return;
        }
        MentalityService.Kind kind = MentalityService.of(target);
        if (kind == MentalityService.Kind.CONSTRUCT) {
            caster.displayClientMessage(Component.translatable("message.effecoria.common.link_no_mind"), true);
            return;
        }

        String signal = effect.params().has("signal")
                ? effect.params().get("signal").getAsString()
                : "danger";
        String signalKey = switch (signal) {
            case "help" -> "help";
            case "here" -> "here";
            default -> "danger";
        };

        if (target instanceof ServerPlayer peer) {
            peer.displayClientMessage(
                    Component.translatable(
                            "message.effecoria.common.link_receive_" + signalKey, caster.getDisplayName()),
                    true);
            BreathDebuffs.apply(peer, new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, true, true));
        } else {
            BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, true, true));
            if ("danger".equals(signalKey)) {
                BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false, true));
            }
        }

        caster.displayClientMessage(
                Component.translatable(
                        "message.effecoria.common.link_sent_" + signalKey, target.getDisplayName()),
                true);

        ServerLevel level = caster.serverLevel();
        Vec3 from = caster.position().add(0, 1.2, 0);
        Vec3 to = target.position().add(0, 1.2, 0);
        for (int i = 0; i <= 8; i++) {
            double t = i / 8.0;
            double x = Mth.lerp(t, from.x, to.x);
            double y = Mth.lerp(t, from.y, to.y);
            double z = Mth.lerp(t, from.z, to.z);
            level.sendParticles(ModParticleTypes.MENTAL_SYNAPSE.get(), x, y, z, 1, 0.02, 0.02, 0.02, 0.0);
        }
        level.playSound(null, caster.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6f, 1.2f);
    }

    /** §2.6 Ψ-ward — aura buffer vs corruption / mental; continuous Ψ drain. */
    public static void psiWard(ServerPlayer caster, SpellEffectEntry effect, float power) {
        long now = caster.level().getGameTime();
        if (CommonWardService.hasWard(caster, now)) {
            CommonWardService.clear(caster);
            caster.displayClientMessage(Component.translatable("message.effecoria.common.ward_off"), true);
            return;
        }
        int duration = scaleTicks(effect, power, "duration_ticks", 300);
        CommonWardService.activate(caster, now + duration);
        BreathDebuffs.apply(
                caster, new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, true, true));
        // Mild mental fog resistance stand-in.
        MentalityService.setShield(caster, now + duration);

        ServerLevel level = caster.serverLevel();
        Vec3 at = caster.position().add(0, 1, 0);
        level.sendParticles(ModParticleTypes.MENTAL_WARD.get(), at.x, at.y, at.z, 16, 0.4, 0.5, 0.4, 0.02);
        level.playSound(null, caster.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.4f, 1.35f);
        caster.displayClientMessage(Component.translatable("message.effecoria.common.ward_on"), true);
    }

    public static boolean canChargePhiCell(ServerPlayer caster) {
        return !findChargeablePhiCell(caster).isEmpty();
    }

    private static ItemStack findChargeablePhiCell(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.is(ModItems.PHI_CELL.get()) && PhiHarnessItems.cellCharge(main) < 0.999f) {
            return main;
        }
        ItemStack off = player.getOffhandItem();
        if (off.is(ModItems.PHI_CELL.get()) && PhiHarnessItems.cellCharge(off) < 0.999f) {
            return off;
        }
        ItemStack best = ItemStack.EMPTY;
        float bestCharge = 2f;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.is(ModItems.PHI_CELL.get())) {
                continue;
            }
            float c = PhiHarnessItems.cellCharge(stack);
            if (c < 0.999f && c < bestCharge) {
                best = stack;
                bestCharge = c;
            }
        }
        return best;
    }

    private static int scaleTicks(SpellEffectEntry effect, float power, String key, int fallback) {
        int base = effect.params().has(key) ? effect.params().get(key).getAsInt() : fallback;
        return Math.max(40, Math.round(base * (0.85f + power / 100f)));
    }
}
