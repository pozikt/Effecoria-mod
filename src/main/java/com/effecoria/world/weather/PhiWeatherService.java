package com.effecoria.world.weather;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.effecoria.config.BalanceConfig;
import com.effecoria.content.ModBlocks;
import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.formula.BreathDebuffs;
import com.effecoria.core.magic.MagicSchool;
import com.effecoria.core.progression.ExhaustionService;
import com.effecoria.core.psi.PlayerPsiData;
import com.effecoria.core.psi.PsiHelper;
import com.effecoria.network.ModNetworking;
import com.effecoria.world.DeadWastelandService;
import com.effecoria.world.EssencePlateauService;
import com.effecoria.world.PhiFogService;
import com.effecoria.world.PhiRadiationService;
import com.effecoria.world.VitrifiedWastesService;
import com.effecoria.world.WhisperingSpireService;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Unified Φ/Ω weather layer: timed storms/tornadoes/blood rain plus ambient rain/mist/omega
 * derived from biome, vanilla weather, fog density, and Whispering Spire zones.
 */
public final class PhiWeatherService {
    private PhiWeatherService() {}

    public record ActiveEvent(
            PhiWeatherKind kind, long untilTick, @Nullable BlockPos origin, double radius, float intensity) {
        boolean covers(BlockPos pos, long now) {
            if (now >= untilTick) {
                return false;
            }
            if (origin == null || radius <= 0) {
                return true;
            }
            double dx = pos.getX() + 0.5 - origin.getX();
            double dz = pos.getZ() + 0.5 - origin.getZ();
            return dx * dx + dz * dz <= radius * radius;
        }
    }

    public record Snapshot(PhiWeatherKind kind, float intensity, long untilTick) {
        public static final Snapshot CLEAR = new Snapshot(PhiWeatherKind.CLEAR, 0f, 0L);
    }

    private static final Map<ResourceKey<Level>, List<ActiveEvent>> EVENTS = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<Level>, Long> POST_STORM_PHI_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> RAIN_EXPOSURE = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> OMEGA_WHISPER_CD = new ConcurrentHashMap<>();

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        prune(level, now);

        if (now % 20 == 0) {
            maybeStartBiomeStorms(level, now);
            maybeStartOmegaBlood(level, now);
            maybePulseLightning(level, now);
            maybePulseTornado(level, now);
        }

        if (now % 10 == 0) {
            for (ServerPlayer player : level.players()) {
                syncTo(player);
            }
        }
    }

    public static void tickPlayer(ServerPlayer player) {
        Level level = player.level();
        if (level.isClientSide()) {
            return;
        }
        BlockPos pos = player.blockPosition();
        Snapshot snap = snapshotAt(level, pos);
        // Ambient weather is shown on the HUD — no action-bar spam.

        if (DeadWastelandService.isBiome(level, pos)) {
            RAIN_EXPOSURE.remove(player.getUUID());
            return;
        }

        boolean openSky = level.canSeeSky(pos);
        applyPlayerEffects(player, snap, openSky);

        if (snap.kind() == PhiWeatherKind.ESSENCE_TORNADO) {
            tickTornadoPull(player, snap);
        }
    }

    public static Snapshot snapshotAt(Level level, BlockPos pos) {
        if (DeadWastelandService.isBiome(level, pos)) {
            return Snapshot.CLEAR;
        }

        long now = level.getGameTime();
        PhiWeatherKind best = PhiWeatherKind.CLEAR;
        float intensity = 0f;
        long until = 0L;

        List<ActiveEvent> list = EVENTS.get(level.dimension());
        if (list != null) {
            synchronized (list) {
                for (ActiveEvent event : list) {
                    if (!event.covers(pos, now)) {
                        continue;
                    }
                    if (event.kind().priority() >= best.priority()) {
                        best = event.kind();
                        intensity = event.intensity();
                        until = event.untilTick();
                    }
                }
            }
        }

        // Ambient overlays (do not outrank timed severe events already chosen).
        WhisperingSpireService.Zone zone = WhisperingSpireService.zoneAt(level, pos);
        boolean omegaZone = zone == WhisperingSpireService.Zone.BLACK || zone == WhisperingSpireService.Zone.RED;
        boolean raining = level.isRaining() && level.canSeeSky(pos);
        boolean inPhiBiome = EssencePlateauService.isBiome(level, pos) || VitrifiedWastesService.isBiome(level, pos);

        if (best.priority() < PhiWeatherKind.OMEGA_RAIN.priority() && omegaZone && raining) {
            best = PhiWeatherKind.OMEGA_RAIN;
            intensity = Math.max(intensity, 0.85f);
        } else if (best.priority() < PhiWeatherKind.OMEGA_FOG.priority() && omegaZone) {
            best = PhiWeatherKind.OMEGA_FOG;
            intensity = Math.max(intensity, zone == WhisperingSpireService.Zone.BLACK ? 1f : 0.7f);
        }

        if (best.priority() < PhiWeatherKind.ESSENCE_STORM.priority() && isStormActive(level, pos)) {
            best = PhiWeatherKind.ESSENCE_STORM;
            intensity = Math.max(intensity, 1f);
            until = Math.max(until, stormUntil(level));
        }

        if (best.priority() < PhiWeatherKind.ESSENCE_RAIN.priority() && raining && inPhiBiome && !omegaZone) {
            best = PhiWeatherKind.ESSENCE_RAIN;
            intensity = Math.max(intensity, 0.65f);
        }

        PhiFogService.Density fog = PhiFogService.densityAt(level, pos);
        if (best.priority() < PhiWeatherKind.ESSENCE_MIST.priority()
                && fog != PhiFogService.Density.NONE
                && EssencePlateauService.isBiome(level, pos)) {
            best = fog == PhiFogService.Density.STORM ? PhiWeatherKind.ESSENCE_STORM : PhiWeatherKind.ESSENCE_MIST;
            intensity = Math.max(intensity, fog.level() / 3f);
        }

        long dayTime = level.getDayTime() % 24000L;
        boolean dewWindow = dayTime >= 0 && dayTime < BalanceConfig.PHI_WEATHER_DEW_WINDOW_TICKS.get();
        if (best == PhiWeatherKind.CLEAR
                && dewWindow
                && EssencePlateauService.isBiome(level, pos)
                && level.canSeeSky(pos)) {
            best = PhiWeatherKind.ESSENCE_DEW;
            intensity = 0.4f;
        }

        return new Snapshot(best, intensity, until);
    }

    public static PhiWeatherKind dominantKind(Level level, BlockPos pos) {
        return snapshotAt(level, pos).kind();
    }

    /** Vitrified / plateau essence storms (biome-wide timed events). */
    public static boolean isStormActive(Level level) {
        return isStormActive(level, null);
    }

    public static boolean isStormActive(Level level, @Nullable BlockPos pos) {
        long now = level.getGameTime();
        List<ActiveEvent> list = EVENTS.get(level.dimension());
        if (list == null) {
            return false;
        }
        synchronized (list) {
            for (ActiveEvent event : list) {
                if (event.kind() != PhiWeatherKind.ESSENCE_STORM || now >= event.untilTick()) {
                    continue;
                }
                if (pos == null || event.covers(pos, now)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static long stormUntil(Level level) {
        long now = level.getGameTime();
        long best = 0L;
        List<ActiveEvent> list = EVENTS.get(level.dimension());
        if (list == null) {
            return 0L;
        }
        synchronized (list) {
            for (ActiveEvent event : list) {
                if (event.kind() == PhiWeatherKind.ESSENCE_STORM && event.untilTick() > now) {
                    best = Math.max(best, event.untilTick());
                }
            }
        }
        return best;
    }

    public static float castChaosChance(Player player) {
        Snapshot snap = snapshotAt(player.level(), player.blockPosition());
        if (snap.kind() == PhiWeatherKind.ESSENCE_TORNADO) {
            return 1f;
        }
        if (snap.kind() == PhiWeatherKind.ESSENCE_STORM || snap.kind() == PhiWeatherKind.ESSENCE_LIGHTNING) {
            return BalanceConfig.PHI_WEATHER_STORM_CAST_CHAOS.get().floatValue();
        }
        if (snap.kind() == PhiWeatherKind.BLOOD_RAIN) {
            return 0.25f;
        }
        return 0f;
    }

    public static boolean blocksMagic(Player player) {
        return dominantKind(player.level(), player.blockPosition()) == PhiWeatherKind.ESSENCE_TORNADO;
    }

    public static float psiRegenBonus(Player player) {
        Snapshot snap = snapshotAt(player.level(), player.blockPosition());
        if (!player.level().canSeeSky(player.blockPosition())) {
            return 0f;
        }
        if (snap.kind() == PhiWeatherKind.ESSENCE_RAIN) {
            return BalanceConfig.PHI_WEATHER_RAIN_PSI_REGEN.get().floatValue();
        }
        if (snap.kind() == PhiWeatherKind.ESSENCE_MIST) {
            return BalanceConfig.PHI_WEATHER_MIST_PSI_REGEN.get().floatValue();
        }
        return 0f;
    }

    public static float phiEnvironmentBonus(Level level, BlockPos pos) {
        Snapshot snap = snapshotAt(level, pos);
        if (snap.kind() == PhiWeatherKind.BLOOD_RAIN) {
            return -1.5f; // drives ambient Φ toward dead under blood rain
        }
        long until = POST_STORM_PHI_UNTIL.getOrDefault(level.dimension(), 0L);
        if (level.getGameTime() < until
                && (EssencePlateauService.isBiome(level, pos) || VitrifiedWastesService.isBiome(level, pos))) {
            return BalanceConfig.PHI_WEATHER_POST_STORM_PHI.get().floatValue();
        }
        if (snap.kind() == PhiWeatherKind.ESSENCE_STORM) {
            return BalanceConfig.PHI_WEATHER_STORM_PHI_BONUS.get().floatValue() * 0.35f;
        }
        return 0f;
    }

    public static float necroPowerBonus(Player player) {
        if (dominantKind(player.level(), player.blockPosition()) == PhiWeatherKind.BLOOD_RAIN) {
            return BalanceConfig.PHI_WEATHER_BLOOD_NECRO_POWER.get().floatValue();
        }
        return 1f;
    }

    public static float necroCostFactor(Player player) {
        if (dominantKind(player.level(), player.blockPosition()) == PhiWeatherKind.BLOOD_RAIN) {
            return BalanceConfig.PHI_WEATHER_BLOOD_NECRO_COST.get().floatValue();
        }
        return 1f;
    }

    public static void startStorm(ServerLevel level, long durationTicks, @Nullable BlockPos origin, double radius) {
        long until = level.getGameTime() + durationTicks;
        addEvent(level, new ActiveEvent(PhiWeatherKind.ESSENCE_STORM, until, origin, radius, 1f));
        announceNear(level, origin, radius, "message.effecoria.weather.essence_storm_start");
    }

    public static void syncTo(ServerPlayer player) {
        Snapshot snap = snapshotAt(player.level(), player.blockPosition());
        PacketDistributor.sendToPlayer(
                player,
                new ModNetworking.PhiWeatherSyncPayload(snap.kind().id(), snap.intensity(), snap.untilTick()));
    }

    private static void maybeStartBiomeStorms(ServerLevel level, long now) {
        if (isStormActive(level)) {
            return;
        }
        boolean anyVitrified = false;
        boolean anyPlateau = false;
        for (ServerPlayer p : level.players()) {
            BlockPos pos = p.blockPosition();
            if (VitrifiedWastesService.isBiome(level, pos)) {
                anyVitrified = true;
            }
            if (EssencePlateauService.isBiome(level, pos)) {
                anyPlateau = true;
            }
        }
        if (anyVitrified) {
            float chance = BalanceConfig.VITRIFIED_STORM_CHANCE_PER_SECOND.get().floatValue();
            if (level.random.nextFloat() < chance) {
                startStorm(level, BalanceConfig.VITRIFIED_STORM_DURATION_TICKS.get(), null, 0);
                return;
            }
        }
        if (anyPlateau && level.isThundering()) {
            float chance = BalanceConfig.PHI_WEATHER_PLATEAU_STORM_CHANCE.get().floatValue();
            if (level.random.nextFloat() < chance) {
                startStorm(level, BalanceConfig.PHI_WEATHER_PLATEAU_STORM_DURATION.get(), null, 0);
            }
        }
    }

    private static void maybeStartOmegaBlood(ServerLevel level, long now) {
        if ((now % 100) != 0) {
            return;
        }
        boolean night = !com.effecoria.core.phi.PhiFieldService.isSolarDay(level);
        if (!night) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            BlockPos pos = player.blockPosition();
            WhisperingSpireService.Zone zone = WhisperingSpireService.zoneAt(level, pos);
            if (zone != WhisperingSpireService.Zone.BLACK && zone != WhisperingSpireService.Zone.RED) {
                continue;
            }
            if (hasEvent(level, PhiWeatherKind.BLOOD_RAIN)) {
                return;
            }
            PlayerPsiData data = PsiHelper.get(player);
            boolean necro = data.initiated() && data.school() == MagicSchool.NECROMANCY;
            boolean hotEntropy = data.entropyB() >= BalanceConfig.PHI_WEATHER_BLOOD_ENTROPY_MIN.get().floatValue();
            if (!(necro || hotEntropy)) {
                continue;
            }
            if (level.random.nextFloat() >= BalanceConfig.PHI_WEATHER_BLOOD_CHANCE.get().floatValue()) {
                continue;
            }
            long until = now + BalanceConfig.PHI_WEATHER_BLOOD_DURATION.get();
            BlockPos vent = WhisperingSpireService.nearestVent(level, pos);
            addEvent(
                    level,
                    new ActiveEvent(
                            PhiWeatherKind.BLOOD_RAIN,
                            until,
                            vent != null ? vent : pos,
                            BalanceConfig.PHI_WEATHER_BLOOD_RADIUS.get(),
                            1f));
            announceNear(
                    level,
                    vent != null ? vent : pos,
                    BalanceConfig.PHI_WEATHER_BLOOD_RADIUS.get(),
                    "message.effecoria.weather.blood_rain_start");
            return;
        }
    }

    private static void maybePulseLightning(ServerLevel level, long now) {
        if (!isStormActive(level) && !level.isThundering()) {
            return;
        }
        if (level.random.nextFloat() >= BalanceConfig.PHI_WEATHER_LIGHTNING_CHANCE.get().floatValue()) {
            return;
        }
        List<ServerPlayer> candidates = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            BlockPos pos = p.blockPosition();
            if (DeadWastelandService.isBiome(level, pos)) {
                continue;
            }
            if (EssencePlateauService.isBiome(level, pos)
                    || VitrifiedWastesService.isBiome(level, pos)
                    || WhisperingSpireService.zoneAt(level, pos) != WhisperingSpireService.Zone.NONE) {
                candidates.add(p);
            }
        }
        if (candidates.isEmpty()) {
            return;
        }
        ServerPlayer anchor = candidates.get(level.random.nextInt(candidates.size()));
        int ox = anchor.getBlockX() + level.random.nextInt(17) - 8;
        int oz = anchor.getBlockZ() + level.random.nextInt(17) - 8;
        int oy = level.getHeight(Heightmap.Types.MOTION_BLOCKING, ox, oz);
        BlockPos strike = new BlockPos(ox, oy, oz);
        strikeLightning(level, strike);
        long until = now + 40;
        addEvent(level, new ActiveEvent(PhiWeatherKind.ESSENCE_LIGHTNING, until, strike, 6.0, 1f));
    }

    private static void maybePulseTornado(ServerLevel level, long now) {
        if (!isStormActive(level)) {
            return;
        }
        if (hasEvent(level, PhiWeatherKind.ESSENCE_TORNADO)) {
            return;
        }
        if (level.random.nextFloat() >= BalanceConfig.PHI_WEATHER_TORNADO_CHANCE.get().floatValue()) {
            return;
        }
        List<ServerPlayer> inStorm = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            if (isStormActive(level, p.blockPosition())) {
                inStorm.add(p);
            }
        }
        if (inStorm.isEmpty()) {
            return;
        }
        ServerPlayer anchor = inStorm.get(level.random.nextInt(inStorm.size()));
        BlockPos origin = anchor.blockPosition();
        double radius = BalanceConfig.PHI_WEATHER_TORNADO_RADIUS.get();
        long until = now + BalanceConfig.PHI_WEATHER_TORNADO_DURATION.get();
        addEvent(level, new ActiveEvent(PhiWeatherKind.ESSENCE_TORNADO, until, origin, radius, 1f));
        announceNear(level, origin, radius, "message.effecoria.weather.tornado_start");
    }

    private static void strikeLightning(ServerLevel level, BlockPos strike) {
        level.playSound(null, strike, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 8f, 0.85f);
        level.sendParticles(
                ModParticleTypes.PHI_SPARK.get(),
                strike.getX() + 0.5,
                strike.getY() + 1.0,
                strike.getZ() + 0.5,
                40,
                0.4,
                1.5,
                0.4,
                0.08);

        BlockPos ground = strike.below();
        BlockState groundState = level.getBlockState(ground);
        if (!groundState.isAir() && level.random.nextFloat() < BalanceConfig.PHI_WEATHER_LIGHTNING_VITRIFY_CHANCE.get()) {
            if (groundState.is(BlockTags.SAND) || groundState.is(ModBlocks.VITRIFIED_SAND.get())) {
                level.setBlock(ground, ModBlocks.PHI_GLASS.get().defaultBlockState(), 3);
            } else if (groundState.is(Blocks.STONE)
                    || groundState.is(ModBlocks.PHI_STONE.get())
                    || groundState.is(ModBlocks.VITRIFIED_STONE.get())) {
                if (level.random.nextFloat() < BalanceConfig.PHI_WEATHER_LIGHTNING_ORE_CHANCE.get()) {
                    level.setBlock(ground, ModBlocks.ESSENITE_ORE.get().defaultBlockState(), 3);
                }
            }
        }

        AABB box = new AABB(strike).inflate(2.5);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box)) {
            float remain = 1f;
            if (living instanceof Player player) {
                remain = PhiRadiationService.evaluate(player).remaining();
            }
            if (remain <= 0.05f) {
                continue;
            }
            living.hurt(level.damageSources().magic(), BalanceConfig.PHI_WEATHER_LIGHTNING_DAMAGE.get().floatValue() * remain);
            BreathDebuffs.apply(living, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2, false, false, true));
            BreathDebuffs.apply(living, new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, false, false, true));
        }
    }

    private static void applyPlayerEffects(ServerPlayer player, Snapshot snap, boolean openSky) {
        PhiWeatherKind kind = snap.kind();
        PlayerPsiData data = PsiHelper.get(player);
        float remain = PhiRadiationService.evaluate(player).remaining();

        if (kind == PhiWeatherKind.ESSENCE_RAIN && openSky && player.tickCount % 20 == 0) {
            if (data.initiated()) {
                float regen = BalanceConfig.PHI_WEATHER_RAIN_PSI_REGEN.get().floatValue();
                data.setCurrentPsi(Math.min(data.maxPsi(), data.currentPsi() + regen));
                PsiHelper.set(player, data);
            } else {
                int exposure = RAIN_EXPOSURE.merge(player.getUUID(), 1, Integer::sum);
                if (exposure >= BalanceConfig.PHI_WEATHER_RAIN_INTOX_TICKS.get() / 20) {
                    BreathDebuffs.apply(player, new MobEffectInstance(MobEffects.CONFUSION, 80, 0, false, false, true));
                    BreathDebuffs.apply(player, new MobEffectInstance(MobEffects.HUNGER, 40, 0, false, false, true));
                }
            }
        } else if (kind != PhiWeatherKind.ESSENCE_RAIN) {
            RAIN_EXPOSURE.remove(player.getUUID());
        }

        if (kind == PhiWeatherKind.ESSENCE_STORM && player.tickCount % 20 == 0 && remain > 0.001f) {
            float soft = EssencePlateauService.isBiome(player.level(), player.blockPosition()) ? 0.55f : 1f;
            float dmg = BalanceConfig.VITRIFIED_STORM_DAMAGE.get().floatValue() * remain * soft;
            if (dmg > 0f) {
                player.hurt(player.damageSources().magic(), dmg);
            }
            if (remain > 0.35f) {
                BreathDebuffs.apply(player, new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false, true));
            }
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        ModParticleTypes.PHI_SPARK.get(),
                        player.getX(),
                        player.getY() + 1.0,
                        player.getZ(),
                        6,
                        0.8,
                        0.6,
                        0.8,
                        0.02);
            }
        }

        if (kind == PhiWeatherKind.OMEGA_FOG && player.tickCount % 40 == 0) {
            data.setEntropyB(data.entropyB() + BalanceConfig.PHI_WEATHER_OMEGA_ENTROPY.get().floatValue());
            PsiHelper.set(player, data);
            BreathDebuffs.apply(player, new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false, true));
            BreathDebuffs.apply(player, new MobEffectInstance(MobEffects.CONFUSION, 80, 0, false, false, true));
            maybeOmegaWhisper(player);
        }

        if (kind == PhiWeatherKind.OMEGA_RAIN && openSky && player.tickCount % 20 == 0 && remain > 0.001f) {
            player.hurt(
                    player.damageSources().magic(),
                    BalanceConfig.PHI_WEATHER_OMEGA_RAIN_DAMAGE.get().floatValue() * remain);
            data.setEntropyB(data.entropyB() + BalanceConfig.PHI_WEATHER_OMEGA_ENTROPY.get().floatValue() * 1.5f);
            ExhaustionService.addExhaustion(data, 0.8f * remain);
            PsiHelper.set(player, data);
        }

        if (kind == PhiWeatherKind.BLOOD_RAIN && player.tickCount % 20 == 0) {
            if (data.initiated() && data.school() == MagicSchool.NECROMANCY) {
                data.setCurrentPsi(Math.min(data.maxPsi(), data.currentPsi() + 1.5f));
                PsiHelper.set(player, data);
            }
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        ModParticleTypes.CORRUPTION_BLOOD.get(),
                        player.getX(),
                        player.getY() + 1.2,
                        player.getZ(),
                        4,
                        0.5,
                        0.4,
                        0.5,
                        0.01);
            }
        }

        if (kind == PhiWeatherKind.ESSENCE_TORNADO && remain > 0.2f && player.tickCount % 60 == 0) {
            // Occasional relocate within biome if unprotected.
            if (player.getRandom().nextFloat() < 0.35f) {
                int tx = player.getBlockX() + player.getRandom().nextInt(25) - 12;
                int tz = player.getBlockZ() + player.getRandom().nextInt(25) - 12;
                int ty = player.level().getHeight(Heightmap.Types.MOTION_BLOCKING, tx, tz);
                player.teleportTo(tx + 0.5, ty, tz + 0.5);
                player.level().playSound(
                        null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.WEATHER, 0.8f, 1.2f);
            }
        }
    }

    private static void tickTornadoPull(ServerPlayer player, Snapshot snap) {
        // Pull toward event origin if present.
        List<ActiveEvent> list = EVENTS.get(player.level().dimension());
        if (list == null) {
            return;
        }
        BlockPos origin = null;
        synchronized (list) {
            for (ActiveEvent event : list) {
                if (event.kind() == PhiWeatherKind.ESSENCE_TORNADO
                        && event.covers(player.blockPosition(), player.level().getGameTime())) {
                    origin = event.origin();
                    break;
                }
            }
        }
        if (origin == null) {
            return;
        }
        Vec3 center = Vec3.atCenterOf(origin);
        Vec3 delta = center.subtract(player.position());
        double dist = delta.horizontalDistance();
        if (dist < 0.4 || dist > 48) {
            return;
        }
        Vec3 pull = delta.normalize().scale(BalanceConfig.PHI_WEATHER_TORNADO_PULL.get());
        player.setDeltaMovement(player.getDeltaMovement().add(pull.x, 0.05, pull.z));
        player.hasImpulse = true;
    }

    private static void maybeOmegaWhisper(ServerPlayer player) {
        int cd = OMEGA_WHISPER_CD.getOrDefault(player.getUUID(), 0);
        if (cd > 0) {
            OMEGA_WHISPER_CD.put(player.getUUID(), cd - 1);
            return;
        }
        if (player.getRandom().nextFloat() > 0.2f) {
            return;
        }
        OMEGA_WHISPER_CD.put(player.getUUID(), 8);
        int i = 1 + player.getRandom().nextInt(5);
        player.displayClientMessage(Component.translatable("message.effecoria.weather.omega_whisper." + i), true);
    }

    private static void announceNear(
            ServerLevel level, @Nullable BlockPos origin, double radius, String messageKey) {
        for (ServerPlayer player : level.players()) {
            if (origin == null || radius <= 0) {
                if (EssencePlateauService.isBiome(level, player.blockPosition())
                        || VitrifiedWastesService.isBiome(level, player.blockPosition())
                        || WhisperingSpireService.zoneAt(level, player.blockPosition())
                                != WhisperingSpireService.Zone.NONE) {
                    player.displayClientMessage(Component.translatable(messageKey), true);
                }
                continue;
            }
            if (player.distanceToSqr(Vec3.atCenterOf(origin)) <= radius * radius * 4) {
                player.displayClientMessage(Component.translatable(messageKey), true);
            }
        }
    }

    private static void addEvent(ServerLevel level, ActiveEvent event) {
        List<ActiveEvent> list = EVENTS.computeIfAbsent(level.dimension(), k -> new ArrayList<>());
        synchronized (list) {
            list.add(event);
        }
    }

    private static boolean hasEvent(ServerLevel level, PhiWeatherKind kind) {
        long now = level.getGameTime();
        List<ActiveEvent> list = EVENTS.get(level.dimension());
        if (list == null) {
            return false;
        }
        synchronized (list) {
            for (ActiveEvent event : list) {
                if (event.kind() == kind && now < event.untilTick()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void prune(ServerLevel level, long now) {
        List<ActiveEvent> list = EVENTS.get(level.dimension());
        if (list == null) {
            return;
        }
        boolean endedStorm = false;
        synchronized (list) {
            Iterator<ActiveEvent> it = list.iterator();
            while (it.hasNext()) {
                ActiveEvent event = it.next();
                if (now >= event.untilTick()) {
                    if (event.kind() == PhiWeatherKind.ESSENCE_STORM) {
                        endedStorm = true;
                    }
                    it.remove();
                }
            }
        }
        if (endedStorm && !isStormActive(level)) {
            long post = now + BalanceConfig.PHI_WEATHER_POST_STORM_TICKS.get();
            POST_STORM_PHI_UNTIL.put(level.dimension(), post);
            announceNear(level, null, 0, "message.effecoria.weather.post_storm");
        }
    }

    /** Client cache for HUD / particles. */
    private static volatile Snapshot CLIENT_SNAPSHOT = Snapshot.CLEAR;

    public static void setClientSnapshot(Snapshot snapshot) {
        CLIENT_SNAPSHOT = snapshot == null ? Snapshot.CLEAR : snapshot;
    }

    public static Snapshot clientSnapshot() {
        return CLIENT_SNAPSHOT;
    }
}
