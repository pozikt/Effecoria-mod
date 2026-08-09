package com.effecoria.effect;

import com.effecoria.core.formula.SpellCombat;

import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.formula.BreathDebuffs;
import com.effecoria.effect.elemental.AirHandService;
import com.effecoria.effect.elemental.ElementalCraftEffects;
import com.effecoria.effect.elemental.ElementalEffects;
import com.effecoria.effect.common.CommonEffects;
import com.effecoria.effect.corruption.CorruptionEffects;
import com.effecoria.effect.mental.MentalEffects;
import com.effecoria.effect.necromancy.NecromancyEffects;
import com.effecoria.effect.spatial.SpatialEffects;
import com.effecoria.effect.spatial.SpatialVfx;
import com.effecoria.effect.organic.OrganicEffects;
import com.effecoria.entity.RootCageEntity;
import com.effecoria.effect.organic.OrganicSpikeWaveService;
import com.effecoria.core.magic.SpellDefinition;
import com.effecoria.core.magic.SpellEffectEntry;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;
import com.effecoria.core.seal.SealPlaceOutcome;
import com.effecoria.core.seal.SealPlaceResult;
import com.effecoria.core.seal.SealService;
import com.effecoria.core.seal.SealTypes;
import com.effecoria.magic.CastAim;
import com.effecoria.magic.CastDelivery;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

import java.util.Set;

public final class SpellEffectExecutor {
    /** Organism-only: no living under aim ⇒ WHIFF_NO_TARGET. */
    private static final Set<String> LIVING_REQUIRED_EFFECTS = Set.of(
            "air_hand",
            "ice_prison",
            "vacuum_cage",
            "root_bind",
            "diagnostic_glimpse",
            "genetic_lock",
            "dimensional_anchor",
            "warp_exchange",
            "neural_lock",
            "mind_probe",
            "locus_echo",
            "mind_terror",
            "mind_depress",
            "mind_blank",
            "cliff_urge",
            "drown_urge",
            "psychic_frenzy",
            "psi_whisper",
            "mind_dominate",
            "false_memory",
            "dream_lock",
            "death_mark",
            "thrall_focus",
            "death_shadow",
            "soul_shackle",
            "grave_bind",
            "curse_of_frailty",
            "haunting_visage",
            "soul_anchor",
            "corrupt_mark",
            "binding_seal",
            "festering_wound",
            "decay_bind",
            "prey_mark",
            "psi_link");

    /** Prefer living ally; else apply to caster (never whiff on dirt). */
    private static final Set<String> HEAL_SELF_FALLBACK_EFFECTS = Set.of(
            "vital_infusion",
            "soothing_sap",
            "symbiotic_graft",
            "vital_ward",
            "adrenal_gift",
            "gene_engineering");

    /** Combat / utility that always resolves an aim point; FULL cost even with no living hit. */
    private static final Set<String> AIM_EFFECTS = Set.of(
            "telekinesis",
            "mind_sting",
            "soul_drain",
            "wither_touch",
            "rift_yank",
            "bio_strike",
            "foreign_agent",
            "parasite_seed",
            "muscle_spasm",
            "parasitic_infection",
            "metabolic_shock",
            "immune_suppression",
            "organic_necrosis",
            "scorched_earth",
            "biological_plague",
            "biological_cleaving",
            "bone_chill",
            "grave_whisper",
            "life_tap",
            "death_coil",
            "necrotic_bolt",
            "corpse_burst",
            "bone_volley",
            "soul_reaper",
            "warp_bolt",
            "fold_repulse",
            "rift_slash",
            "void_lance",
            "rift_burst",
            "spatial_singularity",
            "mind_bolt",
            "thought_lance",
            "telekinetic_crush",
            "synaptic_overload",
            "psychic_drain",
            "thought_bomb",
            "mind_servitude",
            "rot_touch",
            "entropy_lash",
            "plague_bolt",
            "tainted_leech");

    private static final Set<String> BLOCK_TARGET_EFFECTS = Set.of(
            "place_trap_seal",
            "place_fortify_seal",
            "place_glow_seal",
            "place_snare_seal",
            "place_repulse_seal",
            "ore_smelt");

    private static final Set<String> HELD_COOK_EFFECTS = Set.of("sear");
    private static final Set<String> HELD_CHARGE_EFFECTS = Set.of("psi_charge");

    private SpellEffectExecutor() {}

    /** True if any effect strictly needs a living entity (whiff without one). */
    public static boolean requiresTarget(SpellDefinition spell) {
        for (SpellEffectEntry effect : spell.effects()) {
            if (LIVING_REQUIRED_EFFECTS.contains(effect.type().getPath())) {
                return true;
            }
        }
        return false;
    }

    public static boolean requiresBlockTarget(SpellDefinition spell) {
        for (SpellEffectEntry effect : spell.effects()) {
            if (BLOCK_TARGET_EFFECTS.contains(effect.type().getPath())) {
                return true;
            }
        }
        return false;
    }

    public static boolean requiresCookableHeld(SpellDefinition spell) {
        for (SpellEffectEntry effect : spell.effects()) {
            if (HELD_COOK_EFFECTS.contains(effect.type().getPath())) {
                return true;
            }
        }
        return false;
    }

    public static boolean requiresChargeableCell(SpellDefinition spell) {
        for (SpellEffectEntry effect : spell.effects()) {
            if (HELD_CHARGE_EFFECTS.contains(effect.type().getPath())) {
                return true;
            }
        }
        return false;
    }

    public static CastDelivery applyAll(ServerPlayer caster, SpellDefinition spell, float power) {
        return applyAll(caster, spell, power, null);
    }

    public static CastDelivery applyAll(
            ServerPlayer caster, SpellDefinition spell, float power, @Nullable LivingEntity forcedTarget) {
        BreathDebuffs.beginCast(caster, forcedTarget != null && forcedTarget == caster);
        try {
            double range = resolveCastRange(caster, spell);
            CastAim.Result aim = CastAim.resolve(caster, range);
            LivingEntity living = forcedTarget != null ? forcedTarget : aim.living();
            Vec3 aimPoint = forcedTarget != null ? forcedTarget.getBoundingBox().getCenter() : aim.point();

            boolean needsLiving = requiresTarget(spell);
            boolean airHandRelease = isAirHandRelease(caster, spell);
            if (needsLiving && !airHandRelease && living == null) {
                return CastDelivery.WHIFF_NO_TARGET;
            }

            if (requiresCookableHeld(spell) && !ElementalCraftEffects.canSearHeld(caster)) {
                caster.displayClientMessage(Component.translatable("message.effecoria.sear.need_raw"), true);
                return CastDelivery.WHIFF_NO_TARGET;
            }

            if (requiresChargeableCell(spell) && !CommonEffects.canChargePhiCell(caster)) {
                caster.displayClientMessage(
                        Component.translatable("message.effecoria.common.charge_need_cell"), true);
                return CastDelivery.WHIFF_NO_TARGET;
            }

            BlockPos blockTarget = null;
            if (requiresBlockTarget(spell)) {
                blockTarget = resolveBlockTarget(caster, spell);
                if (blockTarget == null) {
                    return CastDelivery.WHIFF_NO_BLOCK;
                }
                if (hasEffect(spell, "ore_smelt")
                        && !ElementalCraftEffects.canSmeltBlock(caster.serverLevel(), blockTarget)) {
                    caster.displayClientMessage(
                            Component.translatable("message.effecoria.ore_smelt.no_recipe"), true);
                    return CastDelivery.WHIFF_NO_BLOCK;
                }
            }

            for (SpellEffectEntry effect : spell.effects()) {
                String path = effect.type().getPath();
                LivingEntity effectTarget = living;
                if (HEAL_SELF_FALLBACK_EFFECTS.contains(path) && effectTarget == null) {
                    effectTarget = caster;
                }
                apply(caster, effect, power, effectTarget, blockTarget, aimPoint);
            }
            return CastDelivery.FULL;
        } finally {
            BreathDebuffs.endCast();
        }
    }

    private static boolean isAirHandRelease(ServerPlayer caster, SpellDefinition spell) {
        if (!AirHandService.isHolding(caster)) {
            return false;
        }
        for (SpellEffectEntry effect : spell.effects()) {
            if ("air_hand".equals(effect.type().getPath())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasEffect(SpellDefinition spell, String path) {
        for (SpellEffectEntry effect : spell.effects()) {
            if (path.equals(effect.type().getPath())) {
                return true;
            }
        }
        return false;
    }

    private static double resolveCastRange(ServerPlayer caster, SpellDefinition spell) {
        double range = 12;
        for (SpellEffectEntry effect : spell.effects()) {
            String path = effect.type().getPath();
            if (!LIVING_REQUIRED_EFFECTS.contains(path)
                    && !AIM_EFFECTS.contains(path)
                    && !HEAL_SELF_FALLBACK_EFFECTS.contains(path)
                    && !BLOCK_TARGET_EFFECTS.contains(path)
                    && !HELD_COOK_EFFECTS.contains(path)
                    && !HELD_CHARGE_EFFECTS.contains(path)) {
                continue;
            }
            if (effect.params().has("range")) {
                range = Math.max(range, effect.params().get("range").getAsDouble());
            }
        }
        return range;
    }

    private static BlockPos resolveBlockTarget(ServerPlayer caster, SpellDefinition spell) {
        double range = 8;
        for (SpellEffectEntry effect : spell.effects()) {
            if (!BLOCK_TARGET_EFFECTS.contains(effect.type().getPath())) {
                continue;
            }
            if (effect.params().has("range")) {
                range = Math.max(range, effect.params().get("range").getAsDouble());
            }
        }
        HitResult hit = caster.pick(range, 0f, false);
        if (hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult blockHit)) {
            return null;
        }
        BlockPos pos = blockHit.getBlockPos();
        if (caster.level().getBlockState(pos).isAir()) {
            return null;
        }
        return pos;
    }

    private static void apply(
            ServerPlayer caster,
            SpellEffectEntry effect,
            float power,
            LivingEntity target,
            BlockPos blockTarget,
            Vec3 aim) {
        switch (effect.type().getPath()) {
            case "telekinesis" -> telekinesis(caster, effect, power, target, aim);
            case "phi_sense" -> MentalEffects.empathicScan(caster, effect, power);
            case "mind_sting" -> mindSting(caster, effect, power, target, aim);
            case "fireball" -> ElementalEffects.weakFireball(caster, effect, power);
            case "sear" -> ElementalCraftEffects.sear(caster, effect, power);
            case "ore_smelt" -> ElementalCraftEffects.oreSmelt(caster, effect, power, blockTarget);
            case "psi_adrenaline" -> CommonEffects.psiAdrenaline(caster, effect, power);
            case "phi_thrust" -> CommonEffects.phiThrust(caster, effect, power);
            case "phi_glow" -> CommonEffects.phiGlow(caster, effect, power);
            case "psi_charge" -> CommonEffects.psiCharge(caster, effect, power);
            case "psi_link" -> CommonEffects.psiLink(caster, effect, power, target);
            case "psi_ward" -> CommonEffects.psiWard(caster, effect, power);
            case "wind_charge" -> ElementalEffects.windCharge(caster, effect, power);
            case "weak_breeze" -> ElementalEffects.weakBreeze(caster, effect, power);
            case "hyper_cooling" -> ElementalEffects.hyperCooling(caster, effect, power);
            case "water_stream" -> ElementalEffects.waterLash(caster, effect, power);
            case "steam_jet" -> ElementalEffects.steamJet(caster, effect, power);
            case "steam_veil" -> ElementalEffects.steamVeil(caster, effect, power);
            case "ice_shard" -> ElementalEffects.iceShard(caster, effect, power);
            case "frost_bastion" -> ElementalEffects.frostBastion(caster, effect, power);
            case "plasma_bolt" -> ElementalEffects.plasmaBolt(caster, effect, power);
            case "hydro_slice" -> ElementalEffects.hydroSlice(caster, effect, power);
            case "great_fireball" -> ElementalEffects.greatFireball(caster, effect, power);
            case "steam_flight" -> ElementalEffects.steamFlight(caster, effect, power);
            case "air_hand" -> ElementalEffects.airHand(caster, effect, power, target);
            case "vacuum_cage" -> ElementalEffects.vacuumCage(caster, effect, power, target);
            case "ice_prison" -> ElementalEffects.icePrison(caster, effect, power, target);
            case "shockwave" -> ElementalEffects.shockwave(caster, effect, power);
            case "ice_sheet" -> ElementalEffects.iceSheet(caster, effect, power);
            case "breath_bubble" -> ElementalEffects.breathBubble(
                    caster, effect, power, target != null ? target : findSpellTarget(caster, 8));
            case "water_shield" -> ElementalEffects.waterShield(caster, effect, power);
            case "sonic_lance" -> ElementalEffects.sonicLance(caster, effect, power);
            case "air_ionization" -> ElementalEffects.airIonization(caster, effect, power);
            case "mirage" -> ElementalEffects.mirage(caster, effect, power);
            case "tornado" -> ElementalEffects.tornado(caster, effect, power);
            case "ion_storm" -> ElementalEffects.ionStorm(caster, effect, power);
            case "lightning_spear" -> ElementalEffects.lightningSpear(caster, effect, power);
            case "water_shroud" -> ElementalEffects.waterShroud(caster, effect, power);
            case "air_shroud" -> ElementalEffects.airShroud(caster, effect, power);
            case "atmospheric_pressure" -> ElementalEffects.atmosphericPressure(caster, effect, power);
            case "cryo_wave" -> ElementalEffects.cryoWave(caster, effect, power);
            case "air_form" -> ElementalEffects.airForm(caster, effect, power);
            case "hurricane_storm" -> ElementalEffects.hurricaneStorm(caster, effect, power);
            case "elemental_supremacy" -> ElementalEffects.elementalSupremacy(caster, effect, power);
            case "thermonuclear_pulse" -> ElementalEffects.thermonuclearPulse(caster, effect, power);
            case "absolute_zero" -> ElementalEffects.absoluteZero(caster, effect, power);
            case "meteorological_cataclysm" -> ElementalEffects.meteorologicalCataclysm(caster, effect, power);
            case "quasar" -> ElementalEffects.quasar(caster, effect, power);
            case "plasma_barrage" -> ElementalEffects.plasmaBarrage(caster, effect, power);
            case "vitality" -> OrganicEffects.applyVitality(caster, effect, power, target);
            case "diagnostic_glimpse" -> OrganicEffects.diagnosticGlimpse(caster, effect, power, target);
            case "blood_stasis" -> OrganicEffects.bloodStasis(caster, effect, power);
            case "life_sense" -> OrganicEffects.lifeSense(caster, effect, power);
            case "bio_strike" -> OrganicEffects.bioStrike(caster, effect, power, target, aim);
            case "bone_needle" -> OrganicEffects.boneNeedle(caster, effect, power);
            case "foreign_agent" -> OrganicEffects.foreignAgent(caster, effect, power, target, aim);
            case "parasite_seed" -> OrganicEffects.parasiteSeed(caster, effect, power, target, aim);
            case "spore_burst" -> OrganicEffects.sporeBurst(caster, effect, power);
            case "muscle_spasm" -> OrganicEffects.muscleSpasm(caster, effect, power, target, aim);
            case "chitin_plates" -> OrganicEffects.chitinPlates(caster, effect, power);
            case "acid_gland" -> OrganicEffects.acidGland(caster, effect, power);
            case "parasitic_infection" -> OrganicEffects.parasiticInfection(caster, effect, power, target, aim);
            case "metabolic_shock" -> OrganicEffects.metabolicShock(caster, effect, power, target, aim);
            case "biological_field" -> OrganicEffects.biologicalField(caster, effect, power);
            case "bone_spur" -> OrganicEffects.boneSpur(caster, effect, power);
            case "sense_sharpening" -> OrganicEffects.senseSharpening(caster, effect, power);
            case "pain_inhibitor" -> OrganicEffects.painInhibitor(caster, effect, power);
            case "poison_thorns" -> OrganicEffects.poisonThorns(caster, effect, power);
            case "bio_mimicry" -> OrganicEffects.bioMimicry(caster, effect, power);
            case "organism_adaptation" -> OrganicEffects.organismAdaptation(caster, effect, power);
            case "immune_suppression" -> OrganicEffects.immuneSuppression(caster, effect, power, target, aim);
            case "metabolic_boost" -> OrganicEffects.metabolicBoost(caster, effect, power);
            case "organic_necrosis" -> OrganicEffects.organicNecrosis(caster, effect, power, target, aim);
            case "full_restructuring" -> OrganicEffects.fullRestructuring(caster, effect, power);
            case "scorched_earth" -> OrganicEffects.scorchedEarth(caster, effect, power, target, aim);
            case "bio_fission" -> OrganicEffects.bioFission(caster, effect, power);
            case "super_regeneration" -> OrganicEffects.superRegeneration(caster, effect, power);
            case "population_control" -> OrganicEffects.populationControl(caster, effect, power);
            case "biological_plague" -> OrganicEffects.biologicalPlague(caster, effect, power, target, aim);
            case "living_armor" -> OrganicEffects.livingArmor(caster, effect, power);
            case "beast_form" -> OrganicEffects.beastForm(caster, effect, power);
            case "bio_cataclysm" -> OrganicEffects.bioCataclysm(caster, effect, power);
            case "absolute_regeneration" -> OrganicEffects.absoluteRegeneration(caster, effect, power);
            case "cellular_dominion" -> OrganicEffects.cellularDominion(caster, effect, power);
            case "evolutionary_leap" -> OrganicEffects.evolutionaryLeap(caster, effect, power);
            case "symbiotic_graft" -> OrganicEffects.symbioticGraft(caster, effect, power, target);
            case "vital_infusion" -> OrganicEffects.vitalInfusion(caster, effect, power, target);
            case "soothing_sap" -> OrganicEffects.soothingSap(caster, effect, power, target);
            case "vital_ward" -> OrganicEffects.vitalWard(caster, effect, power, target);
            case "adrenal_gift" -> OrganicEffects.adrenalGift(caster, effect, power, target);
            case "limb_regeneration" -> OrganicEffects.limbRegeneration(caster, effect, power);
            case "verdant_bloom" -> OrganicEffects.verdantBloom(caster, effect, power);
            case "genetic_lock" -> OrganicEffects.geneticLock(caster, effect, power, target);
            case "gene_engineering" -> OrganicEffects.geneEngineering(caster, effect, power, target);
            case "biological_cleaving" -> OrganicEffects.biologicalCleaving(caster, effect, power, target, aim);
            case "full_transformation" -> OrganicEffects.fullTransformation(caster, effect, power);
            case "spore_storm" -> OrganicEffects.sporeStorm(caster, effect, power);
            case "biological_singularity" -> OrganicEffects.biologicalSingularity(caster, effect, power);
            case "life_creation" -> OrganicEffects.lifeCreation(caster, effect, power);
            case "biological_immortality" -> OrganicEffects.biologicalImmortality(caster, effect, power);
            case "root_spike_wave", "evoker_fangs" -> OrganicSpikeWaveService.launch(caster, effect, power);
            case "root_bind" -> rootBind(caster, effect, power, target);
            case "soul_drain" -> soulDrain(caster, effect, power, target, aim);
            case "wither_touch" -> witherTouch(caster, effect, power, target, aim);
            case "death_mark" -> NecromancyEffects.deathMark(caster, effect, power, target);
            case "thrall_focus" -> NecromancyEffects.thrallFocus(caster, effect, power, target);
            case "mark_reap" -> NecromancyEffects.markReap(caster, effect, power);
            case "death_shadow" -> NecromancyEffects.deathShadow(caster, effect, power, target);
            case "bone_chill" -> NecromancyEffects.boneChill(caster, effect, power, target, aim);
            case "death_sense" -> NecromancyEffects.deathSense(caster, effect, power);
            case "grave_whisper" -> NecromancyEffects.graveWhisper(caster, effect, power, target, aim);
            case "siphon_pulse" -> NecromancyEffects.siphonPulse(caster, effect, power);
            case "bone_armor" -> NecromancyEffects.boneArmor(caster, effect, power);
            case "life_tap" -> NecromancyEffects.lifeTap(caster, effect, power, target, aim);
            case "wither_wave" -> NecromancyEffects.witherWave(caster, effect, power);
            case "dark_pact" -> NecromancyEffects.darkPact(caster, effect, power);
            case "soul_shackle" -> NecromancyEffects.soulShackle(caster, effect, power, target);
            case "phantom_step" -> NecromancyEffects.phantomStep(caster, effect, power);
            case "grave_field" -> NecromancyEffects.graveField(caster, effect, power);
            case "lich_ward" -> NecromancyEffects.lichWard(caster, effect, power);
            case "death_coil" -> NecromancyEffects.deathCoil(caster, effect, power, target, aim);
            case "soul_cataclysm" -> NecromancyEffects.soulCataclysm(caster, effect, power);
            case "death_apotheosis" -> NecromancyEffects.deathApotheosis(caster, effect, power);
            case "necrotic_bolt" -> NecromancyEffects.necroticBolt(caster, effect, power, target, aim);
            case "grave_bind" -> NecromancyEffects.graveBind(caster, effect, power, target);
            case "curse_of_frailty" -> NecromancyEffects.curseOfFrailty(caster, effect, power, target);
            case "haunting_visage" -> NecromancyEffects.hauntingVisage(caster, effect, power, target);
            case "corpse_burst" -> NecromancyEffects.corpseBurst(caster, effect, power, target, aim);
            case "bone_volley" -> NecromancyEffects.boneVolley(caster, effect, power, target, aim);
            case "necrotic_aura" -> NecromancyEffects.necroticAura(caster, effect, power);
            case "soul_anchor" -> NecromancyEffects.soulAnchor(caster, effect, power, target);
            case "death_gate" -> NecromancyEffects.deathGate(caster, effect, power);
            case "soul_reaper" -> NecromancyEffects.soulReaper(caster, effect, power, target, aim);
            case "phylactery_surge" -> NecromancyEffects.phylacterySurge(caster, effect, power);
            case "lich_ascension" -> NecromancyEffects.lichAscension(caster, effect, power);
            case "raise_skeleton" -> NecromancyEffects.raiseSkeleton(caster, effect, power);
            case "shade_summon" -> NecromancyEffects.shadeSummon(caster, effect, power, target);
            case "army_of_dead" -> NecromancyEffects.armyOfDead(caster, effect, power);
            case "warp_bolt" -> SpatialEffects.warpBolt(caster, effect, power, target, aim);
            case "spatial_ward" -> SpatialEffects.spatialWard(caster, effect, power);
            case "fold_repulse" -> SpatialEffects.foldRepulse(caster, effect, power, target, aim);
            case "rift_slash" -> SpatialEffects.riftSlash(caster, effect, power, target, aim);
            case "gravity_snare" -> SpatialEffects.gravitySnare(caster, effect, power);
            case "gravity_field" -> SpatialEffects.gravityField(caster, effect, power);
            case "dimensional_anchor" -> SpatialEffects.dimensionalAnchor(caster, effect, power, target);
            case "void_lance" -> SpatialEffects.voidLance(caster, effect, power, target, aim);
            case "warp_exchange" -> SpatialEffects.warpExchange(caster, effect, power, target);
            case "spatial_surge" -> SpatialEffects.spatialSurge(caster, effect, power);
            case "far_blink" -> SpatialEffects.farBlink(caster, effect, power);
            case "rift_burst" -> SpatialEffects.riftBurst(caster, effect, power, target, aim);
            case "spatial_singularity" -> SpatialEffects.spatialSingularity(caster, effect, power, target, aim);
            case "absolute_fold" -> SpatialEffects.absoluteFold(caster, effect, power);
            case "subspace_voyage" -> SpatialEffects.subspaceVoyage(caster, effect, power);
            case "rift_excise" -> SpatialEffects.riftExcise(caster, effect, power);
            case "spatial_pocket" -> SpatialEffects.spatialPocket(caster, effect, power);
            case "mind_bolt" -> MentalEffects.mindBolt(caster, effect, power, target, aim);
            case "psychic_scream" -> MentalEffects.psychicScream(caster, effect, power);
            case "thought_lance" -> MentalEffects.thoughtLance(caster, effect, power, target, aim);
            case "neural_lock" -> MentalEffects.neuralLock(caster, effect, power, target);
            case "telekinetic_crush" -> MentalEffects.telekineticCrush(caster, effect, power, target, aim);
            case "mass_confusion" -> MentalEffects.massConfusion(caster, effect, power);
            case "psychic_barrier" -> MentalEffects.psychicBarrier(caster, effect, power);
            case "mind_probe" -> MentalEffects.mindProbe(caster, effect, power, target);
            case "locus_echo" -> MentalEffects.locusEcho(caster, effect, power, target);
            case "synaptic_overload" -> MentalEffects.synapticOverload(caster, effect, power, target, aim);
            case "psychic_drain" -> MentalEffects.psychicDrain(caster, effect, power, target, aim);
            case "mental_fortress" -> MentalEffects.mentalFortress(caster, effect, power);
            case "thought_bomb" -> MentalEffects.thoughtBomb(caster, effect, power, target, aim);
            case "psychic_storm" -> MentalEffects.psychicStorm(caster, effect, power);
            case "psychic_amplify" -> MentalEffects.psychicAmplify(caster, effect, power);
            case "omega_mind" -> MentalEffects.omegaMind(caster, effect, power);
            case "mind_terror" -> MentalEffects.mindTerror(caster, effect, power, target);
            case "mind_depress" -> MentalEffects.mindDepress(caster, effect, power, target);
            case "mind_blank" -> MentalEffects.mindBlank(caster, effect, power, target);
            case "cliff_urge" -> MentalEffects.cliffUrge(caster, effect, power, target);
            case "drown_urge" -> MentalEffects.drownUrge(caster, effect, power, target);
            case "psychic_frenzy" -> MentalEffects.psychicFrenzy(caster, effect, power, target);
            case "mass_hysteria" -> MentalEffects.massHysteria(caster, effect, power);
            case "psi_whisper" -> MentalEffects.psiWhisper(caster, effect, power, target);
            case "mind_illusion" -> MentalEffects.mindIllusion(caster, effect, power, target);
            case "mind_dominate" -> MentalEffects.mindDominate(caster, effect, power, target);
            case "mind_servitude" -> MentalEffects.mindServitude(caster, effect, power, target);
            case "false_memory" -> MentalEffects.falseMemory(caster, effect, power, target);
            case "dream_lock" -> MentalEffects.dreamLock(caster, effect, power, target);
            case "hive_mind" -> MentalEffects.hiveMind(caster, effect, power);
            case "psi_echo" -> MentalEffects.psiEcho(caster, effect, power);
            case "total_veil" -> MentalEffects.totalVeil(caster, effect, power);
            case "blink" -> SpatialEffects.standardBlink(caster, effect, power);
            case "phase_slip" -> SpatialEffects.phaseSlip(caster, effect, power);
            case "rift_yank" -> riftYank(caster, effect, power, target, aim);
            case "phase_veil" -> phaseVeil(caster, effect, power);
            case "corrupt_mark" -> CorruptionEffects.corruptMark(caster, effect, power, target);
            case "prey_mark" -> CorruptionEffects.preyMark(caster, effect, power, target);
            case "binding_seal" -> CorruptionEffects.bindingSeal(caster, effect, power, target);
            case "blight_pulse" -> CorruptionEffects.blightPulse(caster, effect, power);
            case "contagion_bloom" -> CorruptionEffects.contagionBloom(caster, effect, power);
            case "hunt_pulse" -> CorruptionEffects.huntPulse(caster, effect, power);
            case "rot_touch" -> CorruptionEffects.rotTouch(caster, effect, power, target, aim);
            case "entropy_lash" -> CorruptionEffects.entropyLash(caster, effect, power, target, aim);
            case "plague_bolt" -> CorruptionEffects.plagueBolt(caster, effect, power, target, aim);
            case "festering_wound" -> CorruptionEffects.festeringWound(caster, effect, power, target);
            case "miasma_cloak" -> CorruptionEffects.miasmaCloak(caster, effect, power);
            case "blight_surge" -> CorruptionEffects.blightSurge(caster, effect, power);
            case "decay_bind" -> CorruptionEffects.decayBind(caster, effect, power, target);
            case "blight_field" -> CorruptionEffects.blightField(caster, effect, power);
            case "entropy_aegis" -> CorruptionEffects.entropyAegis(caster, effect, power);
            case "tainted_leech" -> CorruptionEffects.taintedLeech(caster, effect, power, target, aim);
            case "virulent_wave" -> CorruptionEffects.virulentWave(caster, effect, power);
            case "plague_crown" -> CorruptionEffects.plagueCrown(caster, effect, power);
            case "omega_blight" -> CorruptionEffects.omegaBlight(caster, effect, power);
            case "place_trap_seal" -> placeSeal(caster, effect, power, blockTarget, SealTypes.DAMAGE_TRAP);
            case "place_fortify_seal" -> placeSeal(caster, effect, power, blockTarget, SealTypes.FORTIFY);
            case "place_glow_seal" -> placeSeal(caster, effect, power, blockTarget, SealTypes.GLOW);
            case "place_snare_seal" -> placeSeal(caster, effect, power, blockTarget, SealTypes.SNARE);
            case "place_repulse_seal" -> placeSeal(caster, effect, power, blockTarget, SealTypes.REPULSE);
            default -> {}
        }
    }

    private static void placeSeal(
            ServerPlayer caster,
            SpellEffectEntry effect,
            float power,
            BlockPos blockTarget,
            ResourceLocation typeId) {
        if (blockTarget == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        int duration = effect.params().has("duration_ticks")
                ? effect.params().get("duration_ticks").getAsInt()
                : 2400;
        float strength = Math.min(power, 64f);
        CompoundTag sealParams = sealParamsFromEffect(effect);
        SealPlaceOutcome outcome =
                SealService.place(level, blockTarget, typeId, caster.getUUID(), strength, duration, sealParams);

        spawnSealPlaceParticles(level, blockTarget, typeId);
        level.playSound(null, blockTarget, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.8f, 1.2f);
        caster.displayClientMessage(formatSealPlaceMessage(outcome, typeId, duration), true);
    }

    private static Component formatSealPlaceMessage(SealPlaceOutcome outcome, ResourceLocation typeId, int duration) {
        Component sealName = sealDisplayName(typeId);
        Component base = switch (outcome.result()) {
            case STACKED -> Component.translatable("message.effecoria.seal_stacked", sealName);
            case REPLACED_OFFENSIVE -> {
                Component previous = outcome.previousOffensive() != null
                        ? sealDisplayName(outcome.previousOffensive())
                        : Component.translatable("seal.effecoria.unknown");
                yield Component.translatable("message.effecoria.seal_replaced_offensive", previous, sealName);
            }
            case REPLACED_SAME -> Component.translatable(
                    duration < 0 ? "message.effecoria.seal_placed_permanent" : "message.effecoria.seal_refreshed",
                    sealName);
            case PLACED -> Component.translatable(
                    duration < 0 ? "message.effecoria.seal_placed_permanent" : "message.effecoria.seal_placed",
                    sealName);
        };
        if (outcome.layersAfter().size() <= 1) {
            return base;
        }
        Component layers = formatLayerList(outcome.layersAfter());
        return base.copy().append(Component.literal(" · ")).append(
                Component.translatable("message.effecoria.seal_layers", layers));
    }

    private static Component sealDisplayName(ResourceLocation typeId) {
        return Component.translatable("seal.effecoria." + typeId.getPath());
    }

    private static Component formatLayerList(java.util.List<ResourceLocation> layers) {
        net.minecraft.network.chat.MutableComponent joined = Component.empty();
        for (int i = 0; i < layers.size(); i++) {
            if (i > 0) {
                joined.append(Component.literal(" + "));
            }
            joined.append(sealDisplayName(layers.get(i)));
        }
        return joined;
    }

    private static void spawnSealPlaceParticles(ServerLevel level, BlockPos pos, ResourceLocation typeId) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.55;
        double z = pos.getZ() + 0.5;
        if (typeId.equals(SealTypes.GLOW)) {
            level.sendParticles(ModParticleTypes.SEAL_GLYPH.get(), x, y, z, 6, 0.15, 0.15, 0.15, 0.01);
            level.sendParticles(ModParticleTypes.SEAL_SPARK.get(), x, y + 0.1, z, 10, 0.2, 0.25, 0.2, 0.02);
        } else if (typeId.equals(SealTypes.DAMAGE_TRAP) || typeId.equals(SealTypes.SNARE) || typeId.equals(SealTypes.REPULSE)) {
            level.sendParticles(ModParticleTypes.CORRUPTION_RUNE.get(), x, y, z, 8, 0.2, 0.2, 0.2, 0.01);
            level.sendParticles(ModParticleTypes.CORRUPTION_POISON.get(), x, y + 0.2, z, 6, 0.15, 0.1, 0.15, 0.02);
        } else {
            level.sendParticles(ModParticleTypes.SEAL_GLYPH.get(), x, y, z, 12, 0.25, 0.25, 0.25, 0.015);
            level.sendParticles(ModParticleTypes.PHI_SPARK.get(), x, y + 0.15, z, 4, 0.1, 0.15, 0.1, 0.01);
        }
    }

    private static CompoundTag sealParamsFromEffect(SpellEffectEntry effect) {
        CompoundTag tag = new CompoundTag();
        if (effect.params().has("trap_damage_mult")) {
            tag.putFloat("trap_damage_mult", effect.params().get("trap_damage_mult").getAsFloat());
        }
        if (effect.params().has("slow_amplifier")) {
            tag.putInt("slow_amplifier", effect.params().get("slow_amplifier").getAsInt());
        }
        if (effect.params().has("repulse_force")) {
            tag.putFloat("repulse_force", effect.params().get("repulse_force").getAsFloat());
        }
        return tag;
    }

    private static void telekinesis(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target, Vec3 aim) {
        if (target != null) {
            float force = effect.params().get("force").getAsFloat();
            Vec3 look = caster.getLookAngle().normalize();
            double strength = force * (power / 50f);
            target.setDeltaMovement(target.getDeltaMovement().add(look.scale(strength)));
            target.hurtMarked = true;
        }
        MentalEffects.spawnForce(caster.serverLevel(), (target != null ? target.position() : aim).add(0, 1, 0));
    }

    private static void mindSting(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target, Vec3 aim) {
        if (target == null) {
            MentalEffects.spawnShard(caster.serverLevel(), aim.add(0, 0.2, 0));
            return;
        }
        int slowTicks = effect.params().has("slow_duration_ticks")
                ? effect.params().get("slow_duration_ticks").getAsInt()
                : 60;
        int fogTicks = effect.params().has("confusion_ticks")
                ? effect.params().get("confusion_ticks").getAsInt()
                : Math.max(slowTicks, 80);
        slowTicks = Math.round(slowTicks * (0.85f + power / 120f));
        fogTicks = Math.round(fogTicks * (0.85f + power / 120f));
        if (!com.effecoria.effect.mental.MentalityService.tryAfflict(caster, target, fogTicks)) {
            com.effecoria.effect.mental.MentalityService.notifyFail(caster, target);
            return;
        }
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.CONFUSION, fogTicks, 0));
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowTicks, 1));
        BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.WEAKNESS, fogTicks / 2, 0));
        MentalEffects.spawnShard(caster.serverLevel(), target.position().add(0, 1, 0));
    }

    private static void phiSense(ServerPlayer caster, SpellEffectEntry effect) {
        int duration = effect.params().get("duration_ticks").getAsInt();
        PlayerPsiData data = PsiHelper.get(caster);
        long bonus = com.effecoria.core.progression.RaceTraitsService.phiSenseDurationBonusTicks(caster);
        data.setPhiSenseUntil(caster.level().getGameTime() + duration + bonus);
        PsiHelper.set(caster, data);
        caster.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("message.effecoria.phi_sense_active"), true);
        MentalEffects.spawnSense(caster.serverLevel(), caster.position().add(0, caster.getEyeHeight() * 0.5, 0));
    }




    /** Drain life from a target into the caster — external Ψ siphon. */
    private static void soulDrain(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target, Vec3 aim) {
        ServerLevel level = caster.serverLevel();
        Vec3 hit = target != null ? target.position().add(0, 1, 0) : aim.add(0, 0.2, 0);
        if (target != null) {
            float damage = effect.params().get("damage").getAsFloat();
            float healRatio = effect.params().has("heal_ratio") ? effect.params().get("heal_ratio").getAsFloat() : 0.5f;
            float scaledDamage = damage * (power / 50f);
            target.hurt(SpellCombat.magic(caster), scaledDamage);
            caster.heal(scaledDamage * healRatio);
        }
        spawnNecroParticles(level, hit);
        level.playSound(null, BlockPos.containing(hit), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.7f, 0.8f);
    }

    /** Withering touch — necrotic damage over time. */
    private static void witherTouch(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target, Vec3 aim) {
        ServerLevel level = caster.serverLevel();
        Vec3 hit = target != null ? target.position().add(0, 1, 0) : aim.add(0, 0.2, 0);
        if (target != null) {
            float damage = effect.params().get("damage").getAsFloat();
            int witherTicks = effect.params().get("wither_ticks").getAsInt();
            float scaledDamage = damage * (power / 50f);
            target.hurt(SpellCombat.wither(caster), scaledDamage);
            BreathDebuffs.apply(target, new MobEffectInstance(MobEffects.WITHER, witherTicks, 0));
        }
        spawnNecroParticles(level, hit);
        level.playSound(null, BlockPos.containing(hit), SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 0.7f, 1.2f);
    }

    /** Fold space and yank the target to the caster. */
    private static void riftYank(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target, Vec3 aim) {
        ServerLevel level = caster.serverLevel();
        float damage = effect.params().has("damage") ? effect.params().get("damage").getAsFloat() : 3f;
        float scaledDamage = damage * (power / 50f);

        Vec3 dest = caster.position().add(caster.getLookAngle().normalize().scale(1.2));
        Vec3 from = target != null ? target.position().add(0, 1, 0) : aim.add(0, 0.2, 0);
        SpatialVfx.playCut(
                caster,
                from,
                dest.add(0, 1, 0),
                power / 70f,
                3,
                SpatialVfx.CutMode.LINE);
        if (target != null) {
            target.teleportTo(dest.x, dest.y, dest.z);
            target.hurt(SpellCombat.magic(caster), scaledDamage);
            target.hurtMarked = true;
        }
        level.playSound(null, caster.blockPosition(), SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1f, 0.8f);
    }

    /** Thin the operator across a fold — brief veil. */
    private static void phaseVeil(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 80;
        float scale = Math.min(2.0f, 0.85f + power / 100f);
        duration = Math.round(duration * scale);

        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.INVISIBILITY, duration, 0, false, true));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 0, false, false));
        BreathDebuffs.apply(caster, new MobEffectInstance(MobEffects.SLOW_FALLING, duration, 0, false, false));
        spawnSpatialParticles(level, caster.position().add(0, 1, 0));
        level.playSound(null, caster.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    private static void spawnSpatialParticles(ServerLevel level, Vec3 pos) {
        // Spatial school uses Veil distortion only — no particles
    }

    /** Raycast + cone fallback — works without pixel-perfect crosshair on entity. */
    private static LivingEntity findSpellTarget(ServerPlayer caster, double range) {
        return CastAim.resolve(caster, range).living();
    }

    /** Root a target in place and optionally bloom nearby crops. */
    private static void rootBind(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        ServerLevel level = caster.serverLevel();
        int rootTicks = effect.params().get("root_ticks").getAsInt();
        boolean bloom = !effect.params().has("bloom") || effect.params().get("bloom").getAsBoolean();
        int scaledTicks = Math.round(rootTicks * (0.8f + power / 100f));

        if (target != null) {
            RootCageEntity.bind(level, target, caster, scaledTicks, power);
        }

        if (bloom) {
            bloomNearby(level, BlockPos.containing(caster.position()), 3);
        }
    }

    private static void bloomNearby(ServerLevel level, BlockPos center, int radius) {
        RandomSource random = level.getRandom();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -1, -radius), center.offset(radius, 2, radius))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof BonemealableBlock growable
                    && growable.isValidBonemealTarget(level, pos, state)
                    && growable.isBonemealSuccess(level, random, pos, state)
                    && random.nextFloat() < 0.35f) {
                growable.performBonemeal(level, random, pos, state);
                level.sendParticles(
                        ModParticleTypes.ORGANIC_LEAF.get(),
                        pos.getX() + 0.5,
                        pos.getY() + 0.6,
                        pos.getZ() + 0.5,
                        3,
                        0.15,
                        0.15,
                        0.15,
                        0.01);
            }
        }
    }

    private static void spawnOrganicParticles(ServerLevel level, Vec3 pos) {
        OrganicEffects.spawnBloom(level, pos);
    }

    private static void spawnOrganicRoots(ServerLevel level, Vec3 pos) {
        OrganicEffects.spawnRoots(level, pos);
    }

    private static void spawnNecroParticles(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.NECRO_SHADOW.get(), pos.x, pos.y, pos.z, 10, 0.3, 0.35, 0.3, 0.01);
        level.sendParticles(ModParticleTypes.NECRO_FOG.get(), pos.x, pos.y + 0.5, pos.z, 8, 0.25, 0.4, 0.25, 0.008);
    }
}
