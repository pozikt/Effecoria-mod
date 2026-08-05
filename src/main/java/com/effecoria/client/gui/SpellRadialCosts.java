package com.effecoria.client.gui;

import com.effecoria.core.formula.FormulaEngine;
import com.effecoria.core.formula.PhiSample;
import com.effecoria.core.formula.PsiContext;
import com.effecoria.core.magic.SpellDefinition;
import com.effecoria.core.phi.CreativeGodMode;
import com.effecoria.core.phi.PhiFieldService;
import com.effecoria.core.psi.PsiHelper;
import com.effecoria.magic.SpellRegistry;
import com.effecoria.world.EssencePlateauService;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

/** Client-side spell cost preview for the radial menu. */
public final class SpellRadialCosts {
    private SpellRadialCosts() {}

    public static float previewCost(Player player, ResourceLocation spellId) {
        if (CreativeGodMode.isActive(player)) {
            return 0f;
        }
        Optional<SpellDefinition> def = SpellRegistry.get(spellId);
        if (def.isEmpty()) {
            return 0f;
        }
        PsiContext ctx = PsiHelper.toContext(player, PsiHelper.get(player));
        PhiSample phi = PhiFieldService.sample(player.level(), player.position(), player);
        return FormulaEngine.spellCost(ctx, phi, def.get())
                * EssencePlateauService.spellCostMultiplier(player.level(), player.position());
    }

    public static boolean canAfford(Player player, float cost) {
        if (CreativeGodMode.isActive(player)) {
            return true;
        }
        return PsiHelper.get(player).currentPsi() >= cost;
    }
}
