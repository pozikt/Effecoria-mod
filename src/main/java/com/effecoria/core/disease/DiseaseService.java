package com.effecoria.core.disease;

import com.effecoria.config.BalanceConfig;
import com.effecoria.content.ModMobEffects;
import com.effecoria.core.psi.ModAttachments;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;
import com.effecoria.world.DeadWastelandService;
import com.effecoria.world.OmegaScarService;
import com.effecoria.world.PhiRadiationService;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/** Infect / progress / cure / tick Φ-diseases. */
public final class DiseaseService {
    private DiseaseService() {}

    public static DiseaseProfile get(Player player) {
        return player.getData(ModAttachments.DISEASE_PROFILE.get());
    }

    public static void set(Player player, DiseaseProfile profile) {
        player.setData(ModAttachments.DISEASE_PROFILE.get(), profile);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.syncData(ModAttachments.DISEASE_PROFILE.get());
        }
    }

    public static void sync(ServerPlayer player) {
        player.syncData(ModAttachments.DISEASE_PROFILE.get());
    }

    /**
     * Infect or raise stage. Returns true if state changed.
     * @param stage desired stage (clamped); if already infected, uses max(current, stage)
     */
    public static boolean infect(ServerPlayer player, PhiDisease disease, int stage) {
        DiseaseProfile profile = get(player);
        if (disease == PhiDisease.CRYSTAL_FEVER && profile.crystalFeverImmunity()) {
            return false;
        }
        int target = Mth.clamp(stage, 1, disease.maxStage());
        DiseaseInstance existing = profile.get(disease);
        if (existing != null) {
            if (existing.stage() >= target) {
                return false;
            }
            int prev = existing.stage();
            existing.setStage(target);
            set(player, profile);
            announceStage(player, disease, prev, target);
            return true;
        }
        DiseaseInstance inst = DiseaseInstance.of(target);
        if (disease == PhiDisease.GHOST_ECHO) {
            inst.setEchoHostile(player.getRandom().nextBoolean());
        }
        profile.put(disease, inst);
        set(player, profile);
        player.displayClientMessage(
                Component.translatable("message.effecoria.disease_infected", Component.translatable(disease.translationKey())),
                true);
        announceStage(player, disease, 0, target);
        return true;
    }

    public static boolean infect(ServerPlayer player, PhiDisease disease) {
        return infect(player, disease, 1);
    }

    public static boolean cure(ServerPlayer player, PhiDisease disease) {
        DiseaseProfile profile = get(player);
        if (!profile.has(disease)) {
            return false;
        }
        if (disease == PhiDisease.ORKANUMN_ATROPHY) {
            DiseaseInstance atrophy = profile.get(disease);
            if (atrophy != null && atrophy.stage() >= 3) {
                atrophy.setStage(2);
                atrophy.setTicksActive(0);
                set(player, profile);
                player.displayClientMessage(
                        Component.translatable(
                                "message.effecoria.disease_improving",
                                Component.translatable(disease.translationKey())),
                        true);
                return true;
            }
        }
        if (disease == PhiDisease.ESSENCE_BURN) {
            DiseaseInstance burn = profile.get(disease);
            if (burn != null && burn.stage() >= 3) {
                profile.setOrkanumnScar(true);
            }
        }
        if (disease == PhiDisease.CRYSTAL_FEVER) {
            profile.setCrystalFeverImmunity(true);
        }
        profile.remove(disease);
        set(player, profile);
        player.displayClientMessage(
                Component.translatable("message.effecoria.disease_cured", Component.translatable(disease.translationKey())),
                true);
        return true;
    }

    public static int cureAll(ServerPlayer player) {
        DiseaseProfile profile = get(player);
        int count = profile.diseases().size();
        // Grant fever immunity if curing crystal fever specifically through clear — clearAll keeps immunity.
        boolean hadFever = profile.has(PhiDisease.CRYSTAL_FEVER);
        profile.clearAll();
        if (hadFever) {
            profile.setCrystalFeverImmunity(true);
        }
        set(player, profile);
        if (count > 0) {
            player.displayClientMessage(Component.translatable("message.effecoria.disease_cleared"), true);
        }
        return count;
    }

    public static void clearAll(ServerPlayer player) {
        DiseaseProfile profile = get(player);
        profile.clearAll();
        set(player, profile);
        player.displayClientMessage(Component.translatable("message.effecoria.disease_cleared"), true);
    }

    public static void setClearOnDeath(ServerPlayer player, boolean value) {
        DiseaseProfile profile = get(player);
        profile.setClearOnDeath(value);
        set(player, profile);
    }

    /** Called every 10 server ticks for initiated players (and lightly for exposure on all). */
    public static void tick(ServerPlayer player) {
        DiseaseProfile profile = get(player);
        boolean dirty = false;

        // Progress exposures even for non-initiated (dust / radiation / barrenness admin-only).
        dirty |= tickExposures(player, profile);

        if (!profile.diseases().isEmpty()) {
            for (DiseaseInstance inst : profile.diseases().values()) {
                inst.addTicks(10);
            }
            dirty = true;
            tryProgressStages(player, profile);
            DiseaseEffects.applyPresentation(player, profile);
            tickDiseaseSideEffects(player, profile);
        }

        if (dirty) {
            set(player, profile);
        }
    }

    private static boolean tickExposures(ServerPlayer player, DiseaseProfile profile) {
        boolean dirty = false;
        PhiRadiationService.Shield shield = PhiRadiationService.evaluate(player);
        float remain = shield.remaining();

        // High radiation → essence burn / essentocytosis counters
        if (remain >= BalanceConfig.DISEASE_RAD_BURN_REMAIN.get().floatValue()) {
            if (!player.hasEffect(ModMobEffects.PHI_RESISTANCE)
                    && !player.hasEffect(ModMobEffects.LEAD_SATURATION)) {
                int next = profile.highRadExposure() + 10;
                profile.setHighRadExposure(next);
                dirty = true;
                int burnThresh = BalanceConfig.DISEASE_RAD_BURN_TICKS.get();
                if (next >= burnThresh && !profile.has(PhiDisease.ESSENCE_BURN)) {
                    infectQuiet(profile, PhiDisease.ESSENCE_BURN, 1);
                    profile.setHighRadExposure(0);
                    player.displayClientMessage(
                            Component.translatable(
                                    "message.effecoria.disease_infected",
                                    Component.translatable(PhiDisease.ESSENCE_BURN.translationKey())),
                            true);
                }
                int cancerThresh = BalanceConfig.DISEASE_RAD_CANCER_TICKS.get();
                if (next >= cancerThresh && !profile.has(PhiDisease.ESSENTOCYTOSIS)) {
                    infectQuiet(profile, PhiDisease.ESSENTOCYTOSIS, 1);
                    player.displayClientMessage(
                            Component.translatable(
                                    "message.effecoria.disease_infected",
                                    Component.translatable(PhiDisease.ESSENTOCYTOSIS.translationKey())),
                            true);
                }
            }
        } else if (profile.highRadExposure() > 0) {
            profile.setHighRadExposure(Math.max(0, profile.highRadExposure() - 5));
            dirty = true;
        }

        // Low Φ / dead wasteland → atrophy
        boolean lowPhi = DeadWastelandService.isIn(player.level(), player.position());
        if (lowPhi) {
            int next = profile.lowPhiExposure() + 10;
            profile.setLowPhiExposure(next);
            dirty = true;
            if (next >= BalanceConfig.DISEASE_ATROPHY_TICKS.get() && !profile.has(PhiDisease.ORKANUMN_ATROPHY)) {
                infectQuiet(profile, PhiDisease.ORKANUMN_ATROPHY, 1);
                profile.setLowPhiExposure(0);
                player.displayClientMessage(
                        Component.translatable(
                                "message.effecoria.disease_infected",
                                Component.translatable(PhiDisease.ORKANUMN_ATROPHY.translationKey())),
                        true);
            }
        } else if (profile.lowPhiExposure() > 0) {
            // Recover slowly outside zero-flux
            profile.setLowPhiExposure(Math.max(0, profile.lowPhiExposure() - 8));
            dirty = true;
        }

        // Omega scar → omega sickness
        if (OmegaScarService.isBiome(player.level(), player.blockPosition())
                && !profile.has(PhiDisease.OMEGA_SICKNESS)
                && player.tickCount % 200 == 0
                && player.getRandom().nextFloat() < BalanceConfig.DISEASE_OMEGA_SCAR_CHANCE.get().floatValue()) {
            infectQuiet(profile, PhiDisease.OMEGA_SICKNESS, 1);
            dirty = true;
            player.displayClientMessage(
                    Component.translatable(
                            "message.effecoria.disease_infected",
                            Component.translatable(PhiDisease.OMEGA_SICKNESS.translationKey())),
                    true);
        }

        // High entropy sustained
        PlayerPsiData data = PsiHelper.get(player);
        float threshold = BalanceConfig.ENTROPY_THRESHOLD.get().floatValue();
        if (data.initiated()
                && data.entropyB() >= threshold * BalanceConfig.DISEASE_OMEGA_ENTROPY_RATIO.get().floatValue()
                && !profile.has(PhiDisease.OMEGA_SICKNESS)
                && player.tickCount % 100 == 0
                && player.getRandom().nextFloat() < 0.12f) {
            infectQuiet(profile, PhiDisease.OMEGA_SICKNESS, 1);
            dirty = true;
            player.displayClientMessage(
                    Component.translatable(
                            "message.effecoria.disease_infected",
                            Component.translatable(PhiDisease.OMEGA_SICKNESS.translationKey())),
                    true);
        }

        return dirty;
    }

    private static void infectQuiet(DiseaseProfile profile, PhiDisease disease, int stage) {
        DiseaseInstance inst = DiseaseInstance.of(Mth.clamp(stage, 1, disease.maxStage()));
        if (disease == PhiDisease.GHOST_ECHO) {
            inst.setEchoHostile(true);
        }
        profile.put(disease, inst);
    }

    private static void tryProgressStages(ServerPlayer player, DiseaseProfile profile) {
        for (var entry : profile.diseases().entrySet()) {
            PhiDisease disease = entry.getKey();
            DiseaseInstance inst = entry.getValue();
            if (inst.stage() >= disease.maxStage()) {
                continue;
            }
            int need = BalanceConfig.DISEASE_STAGE_TICKS.get() * inst.stage();
            // Atrophy / cancer / omega progress slower without continued exposure
            if (inst.ticksActive() >= need && player.getRandom().nextFloat() < 0.08f) {
                int prev = inst.stage();
                inst.setStage(prev + 1);
                inst.setTicksActive(0);
                announceStage(player, disease, prev, inst.stage());
                if (disease == PhiDisease.ESSENCE_BURN && inst.stage() >= 3) {
                    profile.setOrkanumnScar(true);
                }
            }
        }
    }

    private static void tickDiseaseSideEffects(ServerPlayer player, DiseaseProfile profile) {
        DiseaseInstance cancer = profile.get(PhiDisease.ESSENTOCYTOSIS);
        if (cancer != null && cancer.stage() >= 2 && player.tickCount % 40 == 0) {
            boolean crash = ((cancer.ticksActive() / 200) % 2) != 0;
            if (crash) {
                player.hurt(player.damageSources().magic(), 0.5f * cancer.stage());
            }
        }
        DiseaseInstance omega = profile.get(PhiDisease.OMEGA_SICKNESS);
        if (omega != null && player.tickCount % 40 == 0) {
            PlayerPsiData data = PsiHelper.get(player);
            data.setEntropyB(data.entropyB() + 0.01f * omega.stage());
            PsiHelper.set(player, data);
            if (omega.stage() >= 2 && player.getRandom().nextFloat() < 0.15f) {
                player.displayClientMessage(Component.translatable("message.effecoria.disease_omega_whisper"), true);
            }
        }
        DiseaseInstance echo = profile.get(PhiDisease.GHOST_ECHO);
        if (echo != null && player.tickCount % 80 == 0 && player.getRandom().nextFloat() < 0.25f) {
            String key = echo.echoHostile()
                    ? "message.effecoria.disease_echo_hostile"
                    : "message.effecoria.disease_echo_friendly";
            player.displayClientMessage(Component.translatable(key), true);
        }
        DiseaseInstance rot = profile.get(PhiDisease.OMEGA_ROT);
        if (rot != null && player.tickCount % 60 == 0) {
            player.hurt(player.damageSources().magic(), 0.5f + 0.5f * rot.stage());
        }
        DiseaseInstance fever = profile.get(PhiDisease.CRYSTAL_FEVER);
        if (fever != null) {
            // Auto-resolve after ~7 minutes of active ticks (~8400 at 10-tick cadence ≈ 7 min wall)
            int lifespan = BalanceConfig.DISEASE_FEVER_DURATION_TICKS.get();
            if (fever.ticksActive() >= lifespan) {
                cure(player, PhiDisease.CRYSTAL_FEVER);
            }
        }
    }

    private static void announceStage(ServerPlayer player, PhiDisease disease, int from, int to) {
        if (to <= from) {
            return;
        }
        player.displayClientMessage(
                Component.translatable(
                        "message.effecoria.disease_stage",
                        Component.translatable(disease.translationKey()),
                        to),
                true);
    }

    public static void onBacklash(ServerPlayer player) {
        DiseaseProfile profile = get(player);
        if (profile.has(PhiDisease.ESSENCE_BURN)) {
            DiseaseInstance burn = profile.get(PhiDisease.ESSENCE_BURN);
            if (burn != null && burn.stage() < PhiDisease.ESSENCE_BURN.maxStage()
                    && player.getRandom().nextFloat() < 0.45f) {
                infect(player, PhiDisease.ESSENCE_BURN, burn.stage() + 1);
            }
        } else if (player.getRandom().nextFloat() < BalanceConfig.DISEASE_BACKLASH_BURN_CHANCE.get().floatValue()) {
            infect(player, PhiDisease.ESSENCE_BURN, 1);
        }
    }

    public static void onDustInhale(ServerPlayer player, int amount) {
        if (hasRespirator(player)) {
            return;
        }
        DiseaseProfile profile = get(player);
        int next = profile.dustExposure() + amount;
        profile.setDustExposure(next);
        if (next >= BalanceConfig.DISEASE_DUST_TICKS.get()) {
            if (!profile.has(PhiDisease.DUST_LUNG)) {
                infect(player, PhiDisease.DUST_LUNG, 1);
            } else {
                DiseaseInstance dust = profile.get(PhiDisease.DUST_LUNG);
                if (dust != null && dust.stage() < 3 && player.getRandom().nextFloat() < 0.3f) {
                    infect(player, PhiDisease.DUST_LUNG, dust.stage() + 1);
                }
            }
            profile.setDustExposure(0);
        }
        set(player, profile);
    }

    public static boolean hasRespirator(Player player) {
        for (var stack : player.getInventory().items) {
            if (stack.is(com.effecoria.content.ModItems.LEAD_FILTER.get())
                    || stack.is(com.effecoria.content.ModItems.LEAD_CLOAK.get())) {
                return true;
            }
        }
        return player.getOffhandItem().is(com.effecoria.content.ModItems.LEAD_FILTER.get())
                || player.getOffhandItem().is(com.effecoria.content.ModItems.LEAD_CLOAK.get())
                || player.getMainHandItem().is(com.effecoria.content.ModItems.LEAD_FILTER.get())
                || player.getMainHandItem().is(com.effecoria.content.ModItems.LEAD_CLOAK.get());
    }

    public static void onCrystalCrabHit(ServerPlayer player) {
        if (get(player).crystalFeverImmunity()) {
            return;
        }
        if (player.getRandom().nextFloat() < BalanceConfig.DISEASE_CRAB_FEVER_CHANCE.get().floatValue()) {
            infect(player, PhiDisease.CRYSTAL_FEVER, 1);
        }
    }

    public static void onOmegaWoundTick(ServerPlayer player) {
        if (player.hasEffect(ModMobEffects.OMEGA_WOUND)
                && !get(player).has(PhiDisease.OMEGA_ROT)
                && player.getRandom().nextFloat() < BalanceConfig.DISEASE_OMEGA_ROT_CHANCE.get().floatValue()) {
            infect(player, PhiDisease.OMEGA_ROT, 1);
        }
    }

    public static void onCorruptionPresent(ServerPlayer player) {
        if (!get(player).has(PhiDisease.CURSE_ROT)
                && player.getRandom().nextFloat() < BalanceConfig.DISEASE_CURSE_ROT_CHANCE.get().floatValue()) {
            infect(player, PhiDisease.CURSE_ROT, 1);
        }
    }

    public static void onGhostEchoRisk(ServerPlayer player) {
        if (!get(player).has(PhiDisease.GHOST_ECHO)
                && player.getRandom().nextFloat() < BalanceConfig.DISEASE_GHOST_ECHO_CHANCE.get().floatValue()) {
            infect(player, PhiDisease.GHOST_ECHO, 1);
        }
    }

    public static void onSoulDissonanceRisk(ServerPlayer player) {
        if (!get(player).has(PhiDisease.SOUL_DISSONANCE)
                && player.getRandom().nextFloat() < BalanceConfig.DISEASE_DISSONANCE_CHANCE.get().floatValue()) {
            infect(player, PhiDisease.SOUL_DISSONANCE, 1);
        }
    }

    /** Passive healing progress for essence burn while shielded / resting. */
    public static void tryNaturalBurnRecovery(ServerPlayer player) {
        DiseaseProfile profile = get(player);
        DiseaseInstance burn = profile.get(PhiDisease.ESSENCE_BURN);
        if (burn == null) {
            return;
        }
        boolean shielded = player.hasEffect(ModMobEffects.PHI_RESISTANCE)
                || player.hasEffect(ModMobEffects.LEAD_SATURATION)
                || hasRespirator(player);
        if (!shielded) {
            return;
        }
        if (burn.ticksActive() >= BalanceConfig.DISEASE_BURN_RECOVER_TICKS.get() && burn.stage() <= 2) {
            if (burn.stage() <= 1) {
                cure(player, PhiDisease.ESSENCE_BURN);
            } else {
                burn.setStage(burn.stage() - 1);
                burn.setTicksActive(0);
                set(player, profile);
                player.displayClientMessage(
                        Component.translatable(
                                "message.effecoria.disease_improving",
                                Component.translatable(PhiDisease.ESSENCE_BURN.translationKey())),
                        true);
            }
        }
    }
}
