package com.effecoria.effect.organic;

import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.formula.SpellCombat;
import com.effecoria.core.magic.SpellEffectEntry;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Staggered ground spikes of roots/thorns — combat layout of Evoker fangs without the jaw model.
 */
public final class OrganicSpikeWaveService {
    private static final List<PendingSpike> PENDING = new CopyOnWriteArrayList<>();

    private OrganicSpikeWaveService() {}

    private record PendingSpike(
            ServerLevel level, double x, double y, double z, long fireAt, UUID owner, float damage) {}

    public static void launch(ServerPlayer caster, SpellEffectEntry effect, float power) {
        ServerLevel level = caster.serverLevel();
        int count = effect.params().has("count") ? effect.params().get("count").getAsInt() : 8;
        double spacing = effect.params().has("spacing") ? effect.params().get("spacing").getAsDouble() : 0.9;
        int warmup = effect.params().has("warmup_ticks") ? effect.params().get("warmup_ticks").getAsInt() : 12;
        int stagger = effect.params().has("stagger_ticks") ? effect.params().get("stagger_ticks").getAsInt() : 2;
        count = Math.min(20, Math.max(3, Math.round(count * (0.85f + power / 120f))));
        float damage = effect.params().has("damage")
                ? effect.params().get("damage").getAsFloat() * (power / 50f)
                : 6f * (0.85f + power / 120f);

        Vec3 look = caster.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0, look.z);
        if (horizontal.lengthSqr() < 1.0E-4) {
            horizontal = new Vec3(1, 0, 0);
        }
        horizontal = horizontal.normalize();

        long now = level.getGameTime();
        for (int i = 0; i < count; i++) {
            double along = (i + 1) * spacing;
            double x = caster.getX() + horizontal.x * along;
            double z = caster.getZ() + horizontal.z * along;
            double y = findGroundY(level, x, caster.getY(), z);
            PENDING.add(new PendingSpike(level, x, y, z, now + warmup + (long) i * stagger, caster.getUUID(), damage));
        }

        level.playSound(null, caster.blockPosition(), SoundEvents.AZALEA_PLACE, SoundSource.PLAYERS, 0.9f, 0.75f);
        level.playSound(null, caster.blockPosition(), SoundEvents.MOSS_PLACE, SoundSource.PLAYERS, 0.7f, 0.9f);
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        Iterator<PendingSpike> it = PENDING.iterator();
        while (it.hasNext()) {
            PendingSpike spike = it.next();
            if (spike.level() != level) {
                continue;
            }
            if (now < spike.fireAt()) {
                continue;
            }
            PENDING.remove(spike);
            erupt(spike);
        }
    }

    private static void erupt(PendingSpike spike) {
        ServerLevel level = spike.level();
        double x = spike.x();
        double y = spike.y();
        double z = spike.z();

        level.sendParticles(ModParticleTypes.ORGANIC_ROOT.get(), x, y + 0.1, z, 8, 0.15, 0.35, 0.15, 0.02);
        level.sendParticles(ModParticleTypes.ORGANIC_THORN.get(), x, y + 0.35, z, 5, 0.12, 0.25, 0.12, 0.04);
        level.sendParticles(ModParticleTypes.ORGANIC_LEAF.get(), x, y + 0.5, z, 3, 0.2, 0.2, 0.2, 0.01);
        level.playSound(
                null,
                x,
                y,
                z,
                SoundEvents.ROOTED_DIRT_PLACE,
                SoundSource.PLAYERS,
                0.45f,
                0.85f + level.random.nextFloat() * 0.3f);

        AABB box = new AABB(x - 0.55, y, z - 0.55, x + 0.55, y + 1.4, z + 0.55);
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(spike.owner());
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (owner != null && entity.getUUID().equals(owner.getUUID())) {
                continue;
            }
            if (owner != null) {
                SpellCombat.hurtMagic(owner, entity, spike.damage());
            } else {
                entity.hurt(level.damageSources().magic(), spike.damage());
            }
            entity.hurtMarked = true;
        }
    }

    private static double findGroundY(ServerLevel level, double x, double referenceY, double z) {
        net.minecraft.core.BlockPos.MutableBlockPos pos = new net.minecraft.core.BlockPos.MutableBlockPos();
        int startY = (int) Math.floor(referenceY) + 2;
        for (int dy = 0; dy < 12; dy++) {
            pos.set((int) Math.floor(x), startY - dy, (int) Math.floor(z));
            if (!level.getBlockState(pos).isAir() && level.getBlockState(pos).isSolidRender(level, pos)) {
                return pos.getY() + 1;
            }
        }
        return referenceY;
    }
}
