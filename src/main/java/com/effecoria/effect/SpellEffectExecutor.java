package com.effecoria.effect;

import com.effecoria.content.ModParticleTypes;
import com.effecoria.effect.elemental.AirHandService;
import com.effecoria.effect.elemental.ElementalEffects;
import com.effecoria.effect.corruption.CorruptionEffects;
import com.effecoria.effect.mental.MentalEffects;
import com.effecoria.effect.necromancy.NecromancyEffects;
import com.effecoria.effect.spatial.SpatialEffects;
import com.effecoria.effect.organic.OrganicEffects;
import com.effecoria.core.magic.SpellDefinition;
import com.effecoria.core.magic.SpellEffectEntry;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;
import com.effecoria.core.seal.SealPlaceResult;
import com.effecoria.core.seal.SealService;
import com.effecoria.core.seal.SealTypes;
import com.effecoria.effect.necromancy.NecroSummonService;
import com.effecoria.magic.CastDelivery;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.windcharge.WindCharge;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

public final class SpellEffectExecutor {
    private static final Set<String> TARGETED_EFFECTS = Set.of(
            "telekinesis",
            "mind_sting",
            "soul_drain",
            "wither_touch",
            "shade_summon",
            "root_bind",
            "rift_yank",
            "corrupt_mark",
            "binding_seal",
            "water_prison",
            "air_hand",
            "ice_prison",
            "diagnostic_glimpse",
            "bio_strike",
            "foreign_agent",
            "muscle_spasm",
            "parasitic_infection",
            "metabolic_shock",
            "immune_suppression",
            "organic_necrosis",
            "scorched_earth",
            "biological_plague",
            "bone_chill",
            "grave_whisper",
            "life_tap",
            "soul_shackle",
            "raise_skeleton",
            "shade_brood",
            "death_coil",
            "symbiotic_graft",
            "genetic_lock",
            "biological_cleaving",
            "necrotic_bolt",
            "grave_bind",
            "curse_of_frailty",
            "haunting_visage",
            "corpse_burst",
            "raise_zombie",
            "bone_volley",
            "soul_anchor",
            "soul_reaper",
            "army_of_dead",
            "warp_bolt",
            "fold_repulse",
            "rift_slash",
            "dimensional_anchor",
            "void_lance",
            "warp_exchange",
            "rift_burst",
            "spatial_singularity",
            "mind_bolt",
            "thought_lance",
            "neural_lock",
            "telekinetic_crush",
            "mind_probe",
            "synaptic_overload",
            "psychic_drain",
            "thought_bomb",
            "rot_touch",
            "entropy_lash",
            "plague_bolt",
            "festering_wound",
            "decay_bind",
            "tainted_leech");

    private static final Set<String> BLOCK_SEAL_EFFECTS = Set.of(
            "place_trap_seal",
            "place_fortify_seal",
            "place_glow_seal",
            "place_snare_seal",
            "place_repulse_seal");

    private SpellEffectExecutor() {}

    public static boolean requiresTarget(SpellDefinition spell) {
        for (SpellEffectEntry effect : spell.effects()) {
            if (TARGETED_EFFECTS.contains(effect.type().getPath())) {
                return true;
            }
        }
        return false;
    }

    public static boolean requiresBlockTarget(SpellDefinition spell) {
        for (SpellEffectEntry effect : spell.effects()) {
            if (BLOCK_SEAL_EFFECTS.contains(effect.type().getPath())) {
                return true;
            }
        }
        return false;
    }

    public static CastDelivery applyAll(ServerPlayer caster, SpellDefinition spell, float power) {
        LivingEntity target = null;
        if (requiresTarget(spell)) {
            boolean airHandRelease = isAirHandRelease(caster, spell);
            if (!airHandRelease) {
                target = resolveTarget(caster, spell);
                if (target == null) {
                    return CastDelivery.WHIFF_NO_TARGET;
                }
            }
        }

        BlockPos blockTarget = null;
        if (requiresBlockTarget(spell)) {
            blockTarget = resolveBlockTarget(caster, spell);
            if (blockTarget == null) {
                return CastDelivery.WHIFF_NO_BLOCK;
            }
        }

        for (SpellEffectEntry effect : spell.effects()) {
            apply(caster, effect, power, target, blockTarget);
        }
        return CastDelivery.FULL;
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

    private static LivingEntity resolveTarget(ServerPlayer caster, SpellDefinition spell) {
        double range = 12;
        for (SpellEffectEntry effect : spell.effects()) {
            if (!TARGETED_EFFECTS.contains(effect.type().getPath())) {
                continue;
            }
            if (effect.params().has("range")) {
                range = Math.max(range, effect.params().get("range").getAsDouble());
            }
        }
        return findSpellTarget(caster, range);
    }

    private static BlockPos resolveBlockTarget(ServerPlayer caster, SpellDefinition spell) {
        double range = 8;
        for (SpellEffectEntry effect : spell.effects()) {
            if (!BLOCK_SEAL_EFFECTS.contains(effect.type().getPath())) {
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
            BlockPos blockTarget) {
        switch (effect.type().getPath()) {
            case "telekinesis" -> telekinesis(caster, effect, power, target);
            case "mind_sting" -> mindSting(caster, effect, power, target);
            case "phi_sense" -> phiSense(caster, effect);
            case "fireball" -> ElementalEffects.weakFireball(caster, effect, power);
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
            case "water_prison" -> ElementalEffects.waterPrison(caster, effect, power, target);
            case "vacuum_cage" -> ElementalEffects.vacuumCage(
                    caster, effect, power, findSpellTarget(caster, 10));
            case "ice_prison" -> ElementalEffects.icePrison(caster, effect, power, target);
            case "shockwave" -> ElementalEffects.shockwave(caster, effect, power);
            case "ice_sheet" -> ElementalEffects.iceSheet(caster, effect, power);
            case "breath_bubble" -> ElementalEffects.breathBubble(
                    caster, effect, power, findSpellTarget(caster, 8));
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
            case "bio_strike" -> OrganicEffects.bioStrike(caster, effect, power, target);
            case "bone_needle" -> OrganicEffects.boneNeedle(caster, effect, power);
            case "foreign_agent" -> OrganicEffects.foreignAgent(caster, effect, power, target);
            case "muscle_spasm" -> OrganicEffects.muscleSpasm(caster, effect, power, target);
            case "chitin_plates" -> OrganicEffects.chitinPlates(caster, effect, power);
            case "acid_gland" -> OrganicEffects.acidGland(caster, effect, power);
            case "parasitic_infection" -> OrganicEffects.parasiticInfection(caster, effect, power, target);
            case "metabolic_shock" -> OrganicEffects.metabolicShock(caster, effect, power, target);
            case "biological_field" -> OrganicEffects.biologicalField(caster, effect, power);
            case "bone_spur" -> OrganicEffects.boneSpur(caster, effect, power);
            case "sense_sharpening" -> OrganicEffects.senseSharpening(caster, effect, power);
            case "pain_inhibitor" -> OrganicEffects.painInhibitor(caster, effect, power);
            case "poison_thorns" -> OrganicEffects.poisonThorns(caster, effect, power);
            case "bio_mimicry" -> OrganicEffects.bioMimicry(caster, effect, power);
            case "organism_adaptation" -> OrganicEffects.organismAdaptation(caster, effect, power);
            case "immune_suppression" -> OrganicEffects.immuneSuppression(caster, effect, power, target);
            case "metabolic_boost" -> OrganicEffects.metabolicBoost(caster, effect, power);
            case "organic_necrosis" -> OrganicEffects.organicNecrosis(caster, effect, power, target);
            case "full_restructuring" -> OrganicEffects.fullRestructuring(caster, effect, power);
            case "scorched_earth" -> OrganicEffects.scorchedEarth(caster, effect, power, target);
            case "bio_fission" -> OrganicEffects.bioFission(caster, effect, power);
            case "super_regeneration" -> OrganicEffects.superRegeneration(caster, effect, power);
            case "population_control" -> OrganicEffects.populationControl(caster, effect, power);
            case "biological_plague" -> OrganicEffects.biologicalPlague(caster, effect, power, target);
            case "living_armor" -> OrganicEffects.livingArmor(caster, effect, power);
            case "beast_form" -> OrganicEffects.beastForm(caster, effect, power);
            case "bio_cataclysm" -> OrganicEffects.bioCataclysm(caster, effect, power);
            case "absolute_regeneration" -> OrganicEffects.absoluteRegeneration(caster, effect, power);
            case "cellular_dominion" -> OrganicEffects.cellularDominion(caster, effect, power);
            case "evolutionary_leap" -> OrganicEffects.evolutionaryLeap(caster, effect, power);
            case "symbiotic_graft" -> OrganicEffects.symbioticGraft(caster, effect, power, target);
            case "limb_regeneration" -> OrganicEffects.limbRegeneration(caster, effect, power);
            case "verdant_bloom" -> OrganicEffects.verdantBloom(caster, effect, power);
            case "genetic_lock" -> OrganicEffects.geneticLock(caster, effect, power, target);
            case "biological_cleaving" -> OrganicEffects.biologicalCleaving(caster, effect, power, target);
            case "full_transformation" -> OrganicEffects.fullTransformation(caster, effect, power);
            case "spore_storm" -> OrganicEffects.sporeStorm(caster, effect, power);
            case "biological_singularity" -> OrganicEffects.biologicalSingularity(caster, effect, power);
            case "life_creation" -> OrganicEffects.lifeCreation(caster, effect, power);
            case "biological_immortality" -> OrganicEffects.biologicalImmortality(caster, effect, power);
            case "evoker_fangs" -> evokerFangs(caster, effect, power);
            case "root_bind" -> rootBind(caster, effect, power, target);
            case "soul_drain" -> soulDrain(caster, effect, power, target);
            case "wither_touch" -> witherTouch(caster, effect, power, target);
            case "shade_summon" -> shadeSummon(caster, effect, power, target);
            case "bone_chill" -> NecromancyEffects.boneChill(caster, effect, power, target);
            case "death_sense" -> NecromancyEffects.deathSense(caster, effect, power);
            case "grave_whisper" -> NecromancyEffects.graveWhisper(caster, effect, power, target);
            case "siphon_pulse" -> NecromancyEffects.siphonPulse(caster, effect, power);
            case "bone_armor" -> NecromancyEffects.boneArmor(caster, effect, power);
            case "life_tap" -> NecromancyEffects.lifeTap(caster, effect, power, target);
            case "wither_wave" -> NecromancyEffects.witherWave(caster, effect, power);
            case "dark_pact" -> NecromancyEffects.darkPact(caster, effect, power);
            case "soul_shackle" -> NecromancyEffects.soulShackle(caster, effect, power, target);
            case "phantom_step" -> NecromancyEffects.phantomStep(caster, effect, power);
            case "grave_field" -> NecromancyEffects.graveField(caster, effect, power);
            case "raise_skeleton" -> NecromancyEffects.raiseSkeleton(caster, effect, power, target);
            case "shade_brood" -> NecromancyEffects.shadeBrood(caster, effect, power, target);
            case "lich_ward" -> NecromancyEffects.lichWard(caster, effect, power);
            case "death_coil" -> NecromancyEffects.deathCoil(caster, effect, power, target);
            case "soul_cataclysm" -> NecromancyEffects.soulCataclysm(caster, effect, power);
            case "death_apotheosis" -> NecromancyEffects.deathApotheosis(caster, effect, power);
            case "necrotic_bolt" -> NecromancyEffects.necroticBolt(caster, effect, power, target);
            case "grave_bind" -> NecromancyEffects.graveBind(caster, effect, power, target);
            case "curse_of_frailty" -> NecromancyEffects.curseOfFrailty(caster, effect, power, target);
            case "haunting_visage" -> NecromancyEffects.hauntingVisage(caster, effect, power, target);
            case "corpse_burst" -> NecromancyEffects.corpseBurst(caster, effect, power, target);
            case "raise_zombie" -> NecromancyEffects.raiseZombie(caster, effect, power, target);
            case "bone_volley" -> NecromancyEffects.boneVolley(caster, effect, power, target);
            case "necrotic_aura" -> NecromancyEffects.necroticAura(caster, effect, power);
            case "soul_anchor" -> NecromancyEffects.soulAnchor(caster, effect, power, target);
            case "army_of_dead" -> NecromancyEffects.armyOfDead(caster, effect, power, target);
            case "death_gate" -> NecromancyEffects.deathGate(caster, effect, power);
            case "soul_reaper" -> NecromancyEffects.soulReaper(caster, effect, power, target);
            case "phylactery_surge" -> NecromancyEffects.phylacterySurge(caster, effect, power);
            case "lich_ascension" -> NecromancyEffects.lichAscension(caster, effect, power);
            case "warp_bolt" -> SpatialEffects.warpBolt(caster, effect, power, target);
            case "spatial_ward" -> SpatialEffects.spatialWard(caster, effect, power);
            case "fold_repulse" -> SpatialEffects.foldRepulse(caster, effect, power, target);
            case "rift_slash" -> SpatialEffects.riftSlash(caster, effect, power, target);
            case "gravity_snare" -> SpatialEffects.gravitySnare(caster, effect, power);
            case "gravity_field" -> SpatialEffects.gravityField(caster, effect, power);
            case "dimensional_anchor" -> SpatialEffects.dimensionalAnchor(caster, effect, power, target);
            case "void_lance" -> SpatialEffects.voidLance(caster, effect, power, target);
            case "warp_exchange" -> SpatialEffects.warpExchange(caster, effect, power, target);
            case "spatial_surge" -> SpatialEffects.spatialSurge(caster, effect, power);
            case "far_blink" -> SpatialEffects.farBlink(caster, effect, power);
            case "rift_burst" -> SpatialEffects.riftBurst(caster, effect, power, target);
            case "spatial_singularity" -> SpatialEffects.spatialSingularity(caster, effect, power, target);
            case "absolute_fold" -> SpatialEffects.absoluteFold(caster, effect, power);
            case "mind_bolt" -> MentalEffects.mindBolt(caster, effect, power, target);
            case "psychic_scream" -> MentalEffects.psychicScream(caster, effect, power);
            case "thought_lance" -> MentalEffects.thoughtLance(caster, effect, power, target);
            case "neural_lock" -> MentalEffects.neuralLock(caster, effect, power, target);
            case "telekinetic_crush" -> MentalEffects.telekineticCrush(caster, effect, power, target);
            case "mass_confusion" -> MentalEffects.massConfusion(caster, effect, power);
            case "psychic_barrier" -> MentalEffects.psychicBarrier(caster, effect, power);
            case "mind_probe" -> MentalEffects.mindProbe(caster, effect, power, target);
            case "synaptic_overload" -> MentalEffects.synapticOverload(caster, effect, power, target);
            case "psychic_drain" -> MentalEffects.psychicDrain(caster, effect, power, target);
            case "mental_fortress" -> MentalEffects.mentalFortress(caster, effect, power);
            case "thought_bomb" -> MentalEffects.thoughtBomb(caster, effect, power, target);
            case "psychic_storm" -> MentalEffects.psychicStorm(caster, effect, power);
            case "psychic_amplify" -> MentalEffects.psychicAmplify(caster, effect, power);
            case "omega_mind" -> MentalEffects.omegaMind(caster, effect, power);
            case "blink" -> SpatialEffects.standardBlink(caster, effect, power);
            case "rift_yank" -> riftYank(caster, effect, power, target);
            case "phase_veil" -> phaseVeil(caster, effect, power);
            case "corrupt_mark" -> CorruptionEffects.corruptMark(caster, effect, power, target);
            case "binding_seal" -> CorruptionEffects.bindingSeal(caster, effect, power, target);
            case "blight_pulse" -> CorruptionEffects.blightPulse(caster, effect, power);
            case "rot_touch" -> CorruptionEffects.rotTouch(caster, effect, power, target);
            case "entropy_lash" -> CorruptionEffects.entropyLash(caster, effect, power, target);
            case "plague_bolt" -> CorruptionEffects.plagueBolt(caster, effect, power, target);
            case "festering_wound" -> CorruptionEffects.festeringWound(caster, effect, power, target);
            case "miasma_cloak" -> CorruptionEffects.miasmaCloak(caster, effect, power);
            case "blight_surge" -> CorruptionEffects.blightSurge(caster, effect, power);
            case "decay_bind" -> CorruptionEffects.decayBind(caster, effect, power, target);
            case "blight_field" -> CorruptionEffects.blightField(caster, effect, power);
            case "entropy_aegis" -> CorruptionEffects.entropyAegis(caster, effect, power);
            case "tainted_leech" -> CorruptionEffects.taintedLeech(caster, effect, power, target);
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
                : 600;
        float strength = power;
        CompoundTag sealParams = sealParamsFromEffect(effect);
        SealPlaceResult result =
                SealService.place(level, blockTarget, typeId, caster.getUUID(), strength, duration, sealParams);

        spawnSealPlaceParticles(level, blockTarget, typeId);
        level.playSound(null, blockTarget, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.8f, 1.2f);
        Component sealName = Component.translatable("seal.effecoria." + typeId.getPath());
        Component message = switch (result) {
            case STACKED -> Component.translatable("message.effecoria.seal_stacked", sealName);
            case REPLACED_OFFENSIVE -> Component.translatable("message.effecoria.seal_replaced_offensive", sealName);
            case REPLACED_SAME -> Component.translatable(
                    duration < 0 ? "message.effecoria.seal_placed_permanent" : "message.effecoria.seal_refreshed",
                    sealName);
            case PLACED -> Component.translatable(
                    duration < 0 ? "message.effecoria.seal_placed_permanent" : "message.effecoria.seal_placed",
                    sealName);
        };
        caster.displayClientMessage(message, true);
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

    private static void telekinesis(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        float force = effect.params().get("force").getAsFloat();
        Vec3 look = caster.getLookAngle().normalize();
        double strength = force * (power / 50f);
        target.setDeltaMovement(target.getDeltaMovement().add(look.scale(strength)));
        target.hurtMarked = true;
        spawnMindParticles(caster.serverLevel(), target.position());
    }

    private static void mindSting(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        float damage = effect.params().get("damage").getAsFloat();
        int slowTicks = effect.params().get("slow_duration_ticks").getAsInt();
        float scaledDamage = damage * (power / 50f);
        DamageSource source = caster.level().damageSources().magic();
        target.hurt(source, scaledDamage);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowTicks, 1));
        spawnMindParticles(caster.serverLevel(), target.position());
    }

    private static void phiSense(ServerPlayer caster, SpellEffectEntry effect) {
        int duration = effect.params().get("duration_ticks").getAsInt();
        PlayerPsiData data = PsiHelper.get(caster);
        data.setPhiSenseUntil(caster.level().getGameTime() + duration);
        PsiHelper.set(caster, data);
        caster.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("message.effecoria.phi_sense_active"), true);
        caster.serverLevel().sendParticles(
                ModParticleTypes.PHI_SPARK.get(),
                caster.getX(),
                caster.getEyeY(),
                caster.getZ(),
                24,
                0.4,
                0.4,
                0.4,
                0.02);
    }


    /** Evoker-style fang line along the caster's look vector. */
    private static void evokerFangs(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        int count = effect.params().has("count") ? effect.params().get("count").getAsInt() : 8;
        double spacing = effect.params().has("spacing") ? effect.params().get("spacing").getAsDouble() : 0.9;
        int warmup = effect.params().has("warmup_ticks") ? effect.params().get("warmup_ticks").getAsInt() : 15;
        int stagger = effect.params().has("stagger_ticks") ? effect.params().get("stagger_ticks").getAsInt() : 2;
        count = Math.min(20, Math.max(3, Math.round(count * (0.85f + power / 120f))));

        Vec3 look = caster.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0, look.z);
        if (horizontal.lengthSqr() < 1.0E-4) {
            horizontal = new Vec3(caster.getLookAngle().x, 0, caster.getLookAngle().z);
        }
        horizontal = horizontal.normalize();
        float yRot = (float) (Math.atan2(horizontal.x, horizontal.z) * (180.0 / Math.PI));

        for (int i = 0; i < count; i++) {
            double along = (i + 1) * spacing;
            double x = caster.getX() + horizontal.x * along;
            double z = caster.getZ() + horizontal.z * along;
            double y = findGroundY(level, x, caster.getY(), z);
            EvokerFangs fangs = new EvokerFangs(level, x, y, z, yRot, warmup + i * stagger, caster);
            level.addFreshEntity(fangs);
        }

        level.playSound(null, caster.blockPosition(), SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.PLAYERS, 1f, 1f);
        level.playSound(null, caster.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.8f, 1.1f);
    }

    /** Drain life from a target into the caster — external Ψ siphon. */
    private static void soulDrain(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        float damage = effect.params().get("damage").getAsFloat();
        float healRatio = effect.params().has("heal_ratio") ? effect.params().get("heal_ratio").getAsFloat() : 0.5f;
        float scaledDamage = damage * (power / 50f);
        target.hurt(caster.level().damageSources().magic(), scaledDamage);
        caster.heal(scaledDamage * healRatio);
        spawnNecroParticles(level, target.position().add(0, 1, 0));
        level.playSound(null, target.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.7f, 0.8f);
    }

    /** Withering touch — necrotic damage over time. */
    private static void witherTouch(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        float damage = effect.params().get("damage").getAsFloat();
        int witherTicks = effect.params().get("wither_ticks").getAsInt();
        float scaledDamage = damage * (power / 50f);
        target.hurt(caster.level().damageSources().wither(), scaledDamage);
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, witherTicks, 0));
        spawnNecroParticles(level, target.position().add(0, 1, 0));
        level.playSound(null, target.blockPosition(), SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 0.7f, 1.2f);
    }

    /** Summon a permanent shade (vex relay) bound to the necromancer — reserves Ψ. */
    private static void shadeSummon(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        if (!NecroSummonService.canAffordAnother(caster)) {
            caster.displayClientMessage(
                    Component.translatable(
                            "message.effecoria.necro.summon_psi_reserve",
                            (int) com.effecoria.config.BalanceConfig.NECRO_SUMMON_PSI_RESERVE.get().floatValue()),
                    true);
            return;
        }
        Vec3 look = caster.getLookAngle().normalize();
        double spawnX = caster.getX() + look.x * 1.5;
        double spawnZ = caster.getZ() + look.z * 1.5;
        double spawnY = caster.getY() + 1.0;

        Vex shade = EntityType.VEX.create(level);
        if (shade == null) {
            return;
        }
        shade.moveTo(spawnX, spawnY, spawnZ, caster.getYRot(), 0f);
        shade.setAggressive(true);
        level.addFreshEntity(shade);
        if (!NecroSummonService.register(shade, caster, target)) {
            return;
        }

        spawnNecroParticles(level, new Vec3(spawnX, spawnY, spawnZ));
        level.playSound(null, caster.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 1f, 0.85f);
    }

    /** Fold space and yank the target to the caster. */
    private static void riftYank(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        if (target == null) {
            return;
        }
        ServerLevel level = caster.serverLevel();
        float damage = effect.params().has("damage") ? effect.params().get("damage").getAsFloat() : 3f;
        float scaledDamage = damage * (power / 50f);

        Vec3 dest = caster.position().add(caster.getLookAngle().normalize().scale(1.2));
        spawnSpatialParticles(level, target.position().add(0, 1, 0));
        target.teleportTo(dest.x, dest.y, dest.z);
        target.hurt(caster.level().damageSources().magic(), scaledDamage);
        target.hurtMarked = true;
        spawnSpatialParticles(level, dest.add(0, 1, 0));
        level.playSound(null, caster.blockPosition(), SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1f, 0.8f);
    }

    /** Thin the operator across a fold — brief veil. */
    private static void phaseVeil(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        int duration = effect.params().has("duration_ticks") ? effect.params().get("duration_ticks").getAsInt() : 80;
        duration = Math.round(duration * (0.85f + power / 100f));

        caster.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, duration, 0, false, true));
        caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 0, false, false));
        caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, duration, 0, false, false));
        spawnSpatialParticles(level, caster.position().add(0, 1, 0));
        level.playSound(null, caster.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    private static void spawnSpatialParticles(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.SPATIAL_RIFT.get(), pos.x, pos.y, pos.z, 16, 0.35, 0.5, 0.35, 0.03);
        level.sendParticles(ModParticleTypes.SPATIAL_WARP.get(), pos.x, pos.y, pos.z, 10, 0.25, 0.35, 0.25, 0.02);
    }

    /** Raycast + cone fallback — works without pixel-perfect crosshair on entity. */
    private static LivingEntity findSpellTarget(ServerPlayer caster, double range) {
        LivingEntity direct = raycastLivingAlongLook(caster, range);
        if (direct != null) {
            return direct;
        }
        return findLivingInLookCone(caster, range, 0.65);
    }

    private static LivingEntity raycastLivingAlongLook(ServerPlayer caster, double range) {
        Vec3 start = caster.getEyePosition();
        Vec3 end = start.add(caster.getLookAngle().scale(range));
        AABB search = caster.getBoundingBox().expandTowards(caster.getLookAngle().scale(range)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                caster.level(),
                caster,
                start,
                end,
                search,
                entity -> entity instanceof LivingEntity living
                        && living.isAlive()
                        && living != caster
                        && !living.isSpectator());
        if (hit != null && hit.getEntity() instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    private static LivingEntity findLivingInLookCone(ServerPlayer caster, double range, double minDot) {
        Vec3 look = caster.getLookAngle().normalize();
        Vec3 eye = caster.getEyePosition();
        AABB box = new AABB(eye, eye).inflate(range);
        LivingEntity best = null;
        double bestDist = range + 1;
        for (LivingEntity entity : caster.serverLevel().getEntitiesOfClass(
                LivingEntity.class, box, e -> e != caster && e.isAlive() && !e.isSpectator())) {
            Vec3 toEntity = entity.getBoundingBox().getCenter().subtract(eye);
            double dist = toEntity.length();
            if (dist > range || dist < 0.5) {
                continue;
            }
            double dot = toEntity.normalize().dot(look);
            if (dot < minDot) {
                continue;
            }
            if (dist < bestDist) {
                bestDist = dist;
                best = entity;
            }
        }
        return best;
    }

    private static double findGroundY(ServerLevel level, double x, double referenceY, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int startY = (int) Math.floor(referenceY) + 2;
        for (int dy = 0; dy < 12; dy++) {
            pos.set((int) Math.floor(x), startY - dy, (int) Math.floor(z));
            if (!level.getBlockState(pos).isAir() && level.getBlockState(pos).isSolidRender(level, pos)) {
                return pos.getY() + 1;
            }
        }
        return referenceY;
    }

    /** Root a target in place and optionally bloom nearby crops. */
    private static void rootBind(ServerPlayer caster, SpellEffectEntry effect, float power, LivingEntity target) {
        ServerLevel level = caster.serverLevel();
        int rootTicks = effect.params().get("root_ticks").getAsInt();
        boolean bloom = !effect.params().has("bloom") || effect.params().get("bloom").getAsBoolean();
        int scaledTicks = Math.round(rootTicks * (0.8f + power / 100f));

        if (target != null) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, scaledTicks, 4));
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, scaledTicks, 1));
            spawnOrganicRoots(level, target.position().add(0, 0.2, 0));
            level.playSound(null, target.blockPosition(), SoundEvents.AZALEA_PLACE, SoundSource.PLAYERS, 1f, 0.7f);
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
        level.sendParticles(ModParticleTypes.ORGANIC_LEAF.get(), pos.x, pos.y + 0.5, pos.z, 8, 0.35, 0.4, 0.35, 0.02);
        level.sendParticles(ModParticleTypes.ORGANIC_ROOT.get(), pos.x, pos.y, pos.z, 4, 0.2, 0.05, 0.2, 0.01);
        level.sendParticles(ModParticleTypes.ORGANIC_FOG.get(), pos.x, pos.y + 0.8, pos.z, 6, 0.25, 0.3, 0.25, 0.01);
    }

    private static void spawnOrganicRoots(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.ORGANIC_ROOT.get(), pos.x, pos.y, pos.z, 10, 0.25, 0.05, 0.25, 0.015);
        level.sendParticles(ModParticleTypes.ORGANIC_LEAF.get(), pos.x, pos.y + 0.3, pos.z, 4, 0.2, 0.2, 0.2, 0.01);
    }

    private static void spawnNecroParticles(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.NECRO_SHADOW.get(), pos.x, pos.y, pos.z, 10, 0.3, 0.35, 0.3, 0.01);
        level.sendParticles(ModParticleTypes.NECRO_FOG.get(), pos.x, pos.y + 0.5, pos.z, 8, 0.25, 0.4, 0.25, 0.008);
    }

    /** Brief psychic fog over the target's head. */
    private static void spawnMindParticles(ServerLevel level, Vec3 pos) {
        level.sendParticles(ModParticleTypes.MENTAL_FOG.get(), pos.x, pos.y + 1.6, pos.z, 8, 0.25, 0.15, 0.25, 0.005);
        level.sendParticles(ModParticleTypes.MENTAL_FOG.get(), pos.x, pos.y + 1.2, pos.z, 4, 0.15, 0.1, 0.15, 0.003);
    }
}
