package com.effecoria.content;

import com.effecoria.EffecoriaMod;
import com.effecoria.alchemy.menu.AlembicMenu;
import com.effecoria.alchemy.menu.BurnerMenu;
import com.effecoria.alchemy.menu.ImprinterMenu;
import com.effecoria.alchemy.menu.MortarMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    private ModMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, EffecoriaMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<MortarMenu>> MORTAR =
            MENUS.register("mortar", () -> IMenuTypeExtension.create(MortarMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<BurnerMenu>> BURNER =
            MENUS.register("burner", () -> IMenuTypeExtension.create(BurnerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<AlembicMenu>> ALEMBIC =
            MENUS.register("alembic", () -> IMenuTypeExtension.create(AlembicMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ImprinterMenu>> IMPRINTER =
            MENUS.register("imprinter", () -> IMenuTypeExtension.create(ImprinterMenu::new));
}
