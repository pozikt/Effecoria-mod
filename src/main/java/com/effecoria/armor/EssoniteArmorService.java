package com.effecoria.armor;

import com.effecoria.config.BalanceConfig;
import com.effecoria.content.ModEntities;
import com.effecoria.content.ModItems;
import com.effecoria.content.ModParticleTypes;
import com.effecoria.core.phi.PhiFieldService;
import com.effecoria.effect.common.CommonWardService;
import com.effecoria.effect.elemental.SteamFlightService;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;

/**
 * Tick + combat + active ability logic for essonite Φ-contour armor.
 */
public final class EssoniteArmorService {
    private EssoniteArmorService() {}

    public static void tickPlayer(ServerPlayer player) {
        if (!hasAny(player)) {
            clearTransient(player);
            return;
        }

        long gameTime = player.level().getGameTime();
        tickWings(player, gameTime);
        tickCrystalSkin(player, gameTime);

        if (player.tickCount % 20 == 0) {
            tickChargeRegen(player);
            tickSelfRepair(player);
            tickCold(player);
            tickServare(player);
            tickCamo(player);
            tickPhiVision(player);
            tickAbnegatioRecharge(player);
        }
    }

    public static boolean hasAny(Player player) {
        return EssoniteArmorData.bestWornTier(player).isPresent();
    }

    private static void tickChargeRegen(ServerPlayer player) {
        var phi = PhiFieldService.sample(player.level(), player.position(), player);
        if (phi.zeroFlux() || phi.effectiveValue() <= 0.001f) {
            return;
        }
        float env = Mth.clamp(phi.effectiveValue(), 0f, 3f);
        for (EquipmentSlot slot : armorSlots()) {
            ItemStack stack = player.getItemBySlot(slot);
            var tier = EssoniteArmorData.tierOf(stack);
            if (tier.isEmpty()) {
                continue;
            }
            float rate = BalanceConfig.ESSONITE_ARMOR_REGEN_SCALE.get().floatValue()
                    * tier.get().regenPerSecond()
                    * (0.35f + 0.65f * Math.min(1f, env / 1.5f))
                    * EssoniteArmorData.slotWeight(slot);
            EssoniteArmorData.addCharge(stack, rate);
        }
    }

    private static void tickSelfRepair(ServerPlayer player) {
        float pool = EssoniteArmorData.poolCharge(player);
        if (pool < BalanceConfig.ESSONITE_ARMOR_REPAIR_MIN_CHARGE.get().floatValue()) {
            return;
        }
        var phi = PhiFieldService.sample(player.level(), player.position(), player);
        if (phi.effectiveValue() <= 0.05f) {
            return;
        }
        int mend = BalanceConfig.ESSONITE_ARMOR_REPAIR_PER_SECOND.get();
        for (EquipmentSlot slot : armorSlots()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!EssoniteArmorData.isEssonite(stack) || !stack.isDamaged()) {
                continue;
            }
            float firmitas = EssoniteArmorData.hasPhoneme(stack, EssonitePhoneme.FIRMITAS) ? 2f : 1f;
            stack.setDamageValue(Math.max(0, stack.getDamageValue() - Math.max(1, Math.round(mend * firmitas))));
            EssoniteArmorData.addCharge(stack, -0.01f);
        }
    }

    private static void tickCold(ServerPlayer player) {
        float pool = EssoniteArmorData.poolCharge(player);
        if (pool < 0.05f) {
            return;
        }
        if (player.isOnFire() || player.getRemainingFireTicks() > 0) {
            player.setRemainingFireTicks(Math.max(0, player.getRemainingFireTicks() - 15));
            if (player.tickCount % 40 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 45, 0, true, false, false));
            }
        }
    }

    private static void tickServare(ServerPlayer player) {
        float pool = EssoniteArmorData.poolCharge(player);
        if (pool < 0.2f) {
            return;
        }
        boolean any = false;
        for (EquipmentSlot slot : armorSlots()) {
            if (EssoniteArmorData.hasPhoneme(player.getItemBySlot(slot), EssonitePhoneme.SERVARE)) {
                any = true;
                break;
            }
        }
        if (any && player.getHealth() < player.getMaxHealth()) {
            player.heal(BalanceConfig.ESSONITE_ARMOR_SERVARE_HEAL.get().floatValue());
            EssoniteArmorData.drainPool(player, 0.02f);
        }
    }

    private static void tickCamo(ServerPlayer player) {
        String family = camoFamily(player);
        for (EquipmentSlot slot : armorSlots()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (EssoniteArmorData.isEssonite(stack)) {
                EssoniteArmorData.setCamo(stack, family);
            }
        }
    }

    private static String camoFamily(ServerPlayer player) {
        Holder<Biome> biome = player.level().getBiome(player.blockPosition());
        ResourceLocation key = biome.unwrapKey().map(k -> k.location()).orElse(null);
        String path = key == null ? "" : key.getPath();
        if (path.contains("wasteland") || path.contains("scar") || path.contains("dead") || path.contains("vitrified")) {
            return "scar";
        }
        if (path.contains("desert") || path.contains("badlands") || path.contains("plateau") || path.contains("savanna")) {
            return "gold";
        }
        if (path.contains("forest") || path.contains("jungle") || path.contains("taiga") || path.contains("grove")) {
            return "emerald";
        }
        return "none";
    }

    private static void tickPhiVision(ServerPlayer player) {
        var helmet = EssoniteArmorData.helmetTier(player);
        if (helmet.isEmpty()) {
            return;
        }
        if (EssoniteArmorData.charge(player.getItemBySlot(EquipmentSlot.HEAD)) < 0.05f) {
            return;
        }
        double r = BalanceConfig.ESSONITE_ARMOR_VISION_RANGE.get();
        AABB box = player.getBoundingBox().inflate(r);
        for (Entity entity : player.serverLevel().getEntities(player, box, EssoniteArmorService::isPhiFauna)) {
            if (entity instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, true, false, false));
            }
        }
    }

    private static boolean isPhiFauna(Entity entity) {
        return entity.getType() == ModEntities.PHI_LARVA.get()
                || entity.getType() == ModEntities.CRYSTAL_CRAB.get()
                || entity.getType() == ModEntities.EIDOS.get()
                || entity.getType() == ModEntities.ESSENCE_WYVERN.get()
                || entity.getType() == ModEntities.VITRIFIED_GOLEM.get()
                || entity.getType() == ModEntities.DEATH_SHADOW.get()
                || entity.getType() == ModEntities.MIRAGE_HORROR.get();
    }

    private static void tickAbnegatioRecharge(ServerPlayer player) {
        for (EquipmentSlot slot : armorSlots()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (EssoniteArmorData.hasPhoneme(stack, EssonitePhoneme.ABNEGATIO)
                    && !EssoniteArmorData.abnegatioReady(stack)
                    && EssoniteArmorData.charge(stack) > 0.4f) {
                EssoniteArmorData.setAbnegatioReady(stack, true);
                EssoniteArmorData.addCharge(stack, -0.15f);
            }
        }
    }

    public static void onPiezoHit(ServerPlayer player, float damage) {
        if (!hasAny(player) || damage <= 0.05f) {
            return;
        }
        float gain = damage
                * BalanceConfig.ESSONITE_ARMOR_PIEZO_SCALE.get().floatValue()
                * EssoniteArmorData.bestWornTier(player).map(EssoniteArmorTier::capacityWeight).orElse(0.3f);
        EssoniteArmorData.distributeCharge(player, Math.min(0.25f, gain));
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(
                    ModParticleTypes.PHI_SPARK.get(),
                    player.getX(),
                    player.getY() + 1.0,
                    player.getZ(),
                    6,
                    0.35,
                    0.4,
                    0.35,
                    0.01);
        }
    }

    /** Fraction of spell Ψ cost paid by armor (0..1 of actualCost). Also drains pool. */
    public static float subsidizeCast(ServerPlayer player, float actualCost) {
        if (!hasAny(player) || actualCost <= 0f) {
            return 0f;
        }
        float fraction = EssoniteArmorData.bestWornTier(player)
                .map(EssoniteArmorTier::castSubsidyFraction)
                .orElse(0f);
        fraction *= BalanceConfig.ESSONITE_ARMOR_SUBSIDY_SCALE.get().floatValue();
        float pool = EssoniteArmorData.poolCharge(player);
        float want = actualCost * fraction;
        // Map Ψ points to pool fraction: full pool ≈ subsidy_pool_psi config
        float poolPsi = BalanceConfig.ESSONITE_ARMOR_POOL_PSI.get().floatValue();
        float availablePsi = pool * poolPsi;
        float pay = Math.min(want, availablePsi);
        if (pay <= 0.01f) {
            return 0f;
        }
        EssoniteArmorData.drainPool(player, pay / poolPsi);
        return pay;
    }

    public static float mentalResistBonus(LivingEntity target) {
        if (!(target instanceof Player player) || !hasAny(player)) {
            return 0f;
        }
        float tier = EssoniteArmorData.bestWornTier(player).map(t -> 0.06f + 0.05f * t.rank()).orElse(0f);
        return tier * (0.4f + 0.6f * EssoniteArmorData.poolCharge(player));
    }

    public static boolean blocksCorruption(LivingEntity target) {
        if (!(target instanceof Player player)) {
            return false;
        }
        if (isOmegaActive(player)) {
            return true;
        }
        float chance = mentalResistBonus(player) * 1.4f;
        return player.getRandom().nextFloat() < chance;
    }

    public static boolean isOmegaActive(Player player) {
        long until = player.getPersistentData().getLong(EssoniteArmorData.PLAYER_OMEGA_UNTIL);
        return until > player.level().getGameTime();
    }

    public static boolean isCrystalSkinActive(Player player) {
        long until = player.getPersistentData().getLong(EssoniteArmorData.PLAYER_SKIN_UNTIL);
        return until > player.level().getGameTime();
    }

    public static boolean tryAbnegatioAbsorb(Player player) {
        for (EquipmentSlot slot : armorSlots()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (EssoniteArmorData.hasPhoneme(stack, EssonitePhoneme.ABNEGATIO)
                    && EssoniteArmorData.abnegatioReady(stack)) {
                EssoniteArmorData.setAbnegatioReady(stack, false);
                EssoniteArmorData.addCharge(stack, -0.2f);
                player.displayClientMessage(Component.translatable("message.effecoria.armor_abnegatio"), true);
                return true;
            }
        }
        return false;
    }

    public static boolean canUnequip(Player player, ItemStack stack) {
        if (!EssoniteArmorData.hasPhoneme(stack, EssonitePhoneme.CLAUSURA)) {
            return true;
        }
        if (player.getAbilities().instabuild) {
            return true;
        }
        return hasItem(player, ModItems.PSI_KEY.get());
    }

    private static boolean hasItem(Player player, net.minecraft.world.item.Item item) {
        if (player.getOffhandItem().is(item)) {
            return true;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                return true;
            }
        }
        return false;
    }

    public static void activateSelected(ServerPlayer player) {
        if (player.isShiftKeyDown() && toggleUmbra(player)) {
            return;
        }
        EssoniteArmorAbility ability = EssoniteArmorData.selectedAbility(player);
        long now = player.level().getGameTime();
        long cdUntil = player.getPersistentData().getLong(EssoniteArmorData.PLAYER_ABILITY_CD);
        if (cdUntil > now) {
            player.displayClientMessage(Component.translatable("message.effecoria.armor_ability_cd"), true);
            return;
        }
        boolean ok = switch (ability) {
            case FLASH -> activateFlash(player);
            case CRYSTAL_SKIN -> activateCrystalSkin(player);
            case WINGS -> activateWings(player);
            case OMEGA_BLOCK -> activateOmega(player);
        };
        if (ok) {
            player.getPersistentData()
                    .putLong(
                            EssoniteArmorData.PLAYER_ABILITY_CD,
                            now + BalanceConfig.ESSONITE_ARMOR_ABILITY_COOLDOWN_TICKS.get());
        }
    }

    public static void cycleAbility(ServerPlayer player) {
        EssoniteArmorAbility next = EssoniteArmorAbility.cycle(EssoniteArmorData.selectedAbility(player));
        EssoniteArmorData.setSelectedAbility(player, next);
        player.displayClientMessage(
                Component.translatable("message.effecoria.armor_ability_select", Component.translatable(
                        "armor.effecoria.ability." + next.id())),
                true);
    }

    private static boolean toggleUmbra(ServerPlayer player) {
        boolean found = false;
        boolean turnOn = false;
        for (EquipmentSlot slot : armorSlots()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!EssoniteArmorData.hasPhoneme(stack, EssonitePhoneme.UMBRA)) {
                continue;
            }
            found = true;
            boolean next = !EssoniteArmorData.umbraActive(stack);
            EssoniteArmorData.setUmbraActive(stack, next);
            turnOn = next;
        }
        if (!found) {
            return false;
        }
        if (turnOn) {
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 20 * 20, 0, false, false, true));
            player.displayClientMessage(Component.translatable("message.effecoria.armor_umbra_on"), true);
        } else {
            player.removeEffect(MobEffects.INVISIBILITY);
            player.displayClientMessage(Component.translatable("message.effecoria.armor_umbra_off"), true);
        }
        return true;
    }

    private static boolean activateFlash(ServerPlayer player) {
        var tier = EssoniteArmorData.bestWornTier(player);
        if (tier.isEmpty() || !tier.get().allowsFlash()) {
            return false;
        }
        float pool = EssoniteArmorData.poolCharge(player);
        if (pool < 0.25f) {
            player.displayClientMessage(Component.translatable("message.effecoria.armor_need_charge"), true);
            return false;
        }
        EssoniteArmorData.drainPool(player, 1f);
        ServerLevel level = player.serverLevel();
        double radius = BalanceConfig.ESSONITE_ARMOR_FLASH_RADIUS.get();
        float knock = BalanceConfig.ESSONITE_ARMOR_FLASH_KNOCKBACK.get().floatValue()
                * (0.7f + 0.3f * tier.get().capacityWeight());
        AABB box = player.getBoundingBox().inflate(radius);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive())) {
            Vec3 push = living.position().subtract(player.position()).normalize().scale(knock).add(0, 0.35, 0);
            living.setDeltaMovement(living.getDeltaMovement().add(push));
            living.hurtMarked = true;
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
            if (living instanceof ServerPlayer other) {
                CommonWardService.clear(other);
            }
        }
        level.sendParticles(
                ModParticleTypes.PHI_SPARK.get(),
                player.getX(),
                player.getY() + 1.0,
                player.getZ(),
                40,
                radius * 0.4,
                0.6,
                radius * 0.4,
                0.02);
        level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.PLAYERS,
                1.2f,
                0.8f);
        player.displayClientMessage(Component.translatable("message.effecoria.armor_flash"), true);
        return true;
    }

    private static boolean activateCrystalSkin(ServerPlayer player) {
        var chest = EssoniteArmorData.chestTier(player);
        if (chest.isEmpty() || !chest.get().allowsCrystalSkin()) {
            player.displayClientMessage(Component.translatable("message.effecoria.armor_need_crystal"), true);
            return false;
        }
        if (EssoniteArmorData.poolCharge(player) < 0.45f) {
            player.displayClientMessage(Component.translatable("message.effecoria.armor_need_charge"), true);
            return false;
        }
        EssoniteArmorData.drainPool(player, 0.55f);
        int ticks = BalanceConfig.ESSONITE_ARMOR_SKIN_TICKS.get();
        player.getPersistentData().putLong(EssoniteArmorData.PLAYER_SKIN_UNTIL, player.level().getGameTime() + ticks);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 4, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 255, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, ticks, 255, false, false, false));
        player.displayClientMessage(Component.translatable("message.effecoria.armor_crystal_skin"), true);
        return true;
    }

    private static boolean activateWings(ServerPlayer player) {
        var chest = EssoniteArmorData.chestTier(player);
        if (chest.isEmpty() || !chest.get().allowsWings()) {
            player.displayClientMessage(Component.translatable("message.effecoria.armor_need_crystal"), true);
            return false;
        }
        if (EssoniteArmorData.poolCharge(player) < 0.55f) {
            player.displayClientMessage(Component.translatable("message.effecoria.armor_need_charge"), true);
            return false;
        }
        EssoniteArmorData.drainPool(player, 0.65f);
        int ticks = BalanceConfig.ESSONITE_ARMOR_WINGS_TICKS.get();
        player.getPersistentData().putLong(EssoniteArmorData.PLAYER_WINGS_UNTIL, player.level().getGameTime() + ticks);
        SteamFlightService.activate(
                player,
                BalanceConfig.ESSONITE_ARMOR_WINGS_DRAIN.get().floatValue(),
                BalanceConfig.ESSONITE_ARMOR_WINGS_BOOST.get().floatValue());
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, ticks, 0, false, false, true));
        player.displayClientMessage(Component.translatable("message.effecoria.armor_wings"), true);
        return true;
    }

    private static boolean activateOmega(ServerPlayer player) {
        var chest = EssoniteArmorData.chestTier(player);
        if (chest.isEmpty() || !chest.get().allowsOmegaBlock()) {
            player.displayClientMessage(Component.translatable("message.effecoria.armor_need_crystal"), true);
            return false;
        }
        int need = chest.get() == EssoniteArmorTier.STAR
                ? BalanceConfig.ESSONITE_ARMOR_OMEGA_INSERTS_STAR.get()
                : BalanceConfig.ESSONITE_ARMOR_OMEGA_INSERTS_CRYSTAL.get();
        if (!consumeInserts(player, need)) {
            player.displayClientMessage(
                    Component.translatable("message.effecoria.armor_need_inserts", need), true);
            return false;
        }
        if (EssoniteArmorData.poolCharge(player) < 0.35f) {
            player.displayClientMessage(Component.translatable("message.effecoria.armor_need_charge"), true);
            return false;
        }
        EssoniteArmorData.drainPool(player, 0.5f);
        int ticks = BalanceConfig.ESSONITE_ARMOR_OMEGA_TICKS.get();
        player.getPersistentData().putLong(EssoniteArmorData.PLAYER_OMEGA_UNTIL, player.level().getGameTime() + ticks);
        player.displayClientMessage(Component.translatable("message.effecoria.armor_omega"), true);
        return true;
    }

    private static boolean consumeInserts(ServerPlayer player, int need) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        int have = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.VOID_OBSIDIAN_INSERT.get())) {
                have += stack.getCount();
            }
        }
        if (player.getOffhandItem().is(ModItems.VOID_OBSIDIAN_INSERT.get())) {
            have += player.getOffhandItem().getCount();
        }
        if (have < need) {
            return false;
        }
        int left = need;
        left = shrink(player.getInventory().items, left);
        if (left > 0 && player.getOffhandItem().is(ModItems.VOID_OBSIDIAN_INSERT.get())) {
            ItemStack off = player.getOffhandItem();
            int take = Math.min(left, off.getCount());
            off.shrink(take);
            left -= take;
        }
        return left <= 0;
    }

    private static int shrink(Iterable<ItemStack> items, int left) {
        for (ItemStack stack : items) {
            if (!stack.is(ModItems.VOID_OBSIDIAN_INSERT.get())) {
                continue;
            }
            int take = Math.min(left, stack.getCount());
            stack.shrink(take);
            left -= take;
            if (left <= 0) {
                break;
            }
        }
        return left;
    }

    private static void tickWings(ServerPlayer player, long gameTime) {
        long until = player.getPersistentData().getLong(EssoniteArmorData.PLAYER_WINGS_UNTIL);
        if (until > 0 && until <= gameTime) {
            player.getPersistentData().remove(EssoniteArmorData.PLAYER_WINGS_UNTIL);
        }
    }

    private static void tickCrystalSkin(ServerPlayer player, long gameTime) {
        long until = player.getPersistentData().getLong(EssoniteArmorData.PLAYER_SKIN_UNTIL);
        if (until > 0 && until <= gameTime) {
            player.getPersistentData().remove(EssoniteArmorData.PLAYER_SKIN_UNTIL);
        }
    }

    private static void clearTransient(ServerPlayer player) {
        player.getPersistentData().remove(EssoniteArmorData.PLAYER_WINGS_UNTIL);
        player.getPersistentData().remove(EssoniteArmorData.PLAYER_SKIN_UNTIL);
    }

    private static EquipmentSlot[] armorSlots() {
        return new EquipmentSlot[] {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        };
    }
}
