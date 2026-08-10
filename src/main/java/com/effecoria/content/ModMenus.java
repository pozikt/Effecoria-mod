package com.effecoria.content;

import com.effecoria.EffecoriaMod;
import com.effecoria.alchemy.menu.AlembicMenu;
import com.effecoria.alchemy.menu.AssemblerMenu;
import com.effecoria.alchemy.menu.BurnerMenu;
import com.effecoria.alchemy.menu.FormSelectMenu;
import com.effecoria.alchemy.menu.ImprinterMenu;
import com.effecoria.alchemy.menu.MortarMenu;
import com.effecoria.alchemy.menu.SealInscriberMenu;
import com.effecoria.alchemy.menu.SparkReactorMenu;
import com.effecoria.alchemy.menu.HeartReactorMenu;

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

    public static final DeferredHolder<MenuType<?>, MenuType<SparkReactorMenu>> SPARK_REACTOR =
            MENUS.register("spark_reactor", () -> IMenuTypeExtension.create(SparkReactorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<HeartReactorMenu>> HEART_REACTOR =
            MENUS.register("heart_reactor", () -> IMenuTypeExtension.create(HeartReactorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<AlembicMenu>> ALEMBIC =
            MENUS.register("alembic", () -> IMenuTypeExtension.create(AlembicMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ImprinterMenu>> IMPRINTER =
            MENUS.register("imprinter", () -> IMenuTypeExtension.create(ImprinterMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<FormSelectMenu>> SHAFT_LATHE =
            MENUS.register("shaft_lathe", () -> IMenuTypeExtension.create(FormSelectMenu::lathe));

    public static final DeferredHolder<MenuType<?>, MenuType<FormSelectMenu>> FACET_CUTTER =
            MENUS.register("facet_cutter", () -> IMenuTypeExtension.create(FormSelectMenu::cutter));

    public static final DeferredHolder<MenuType<?>, MenuType<AssemblerMenu>> ARTIFACT_ASSEMBLER =
            MENUS.register("artifact_assembler", () -> IMenuTypeExtension.create(AssemblerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<SealInscriberMenu>> SEAL_INSCRIBER =
            MENUS.register("seal_inscriber", () -> IMenuTypeExtension.create(SealInscriberMenu::new));
}
