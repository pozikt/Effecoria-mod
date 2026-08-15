package com.effecoria.content;

import com.effecoria.EffecoriaMod;
import com.effecoria.alchemy.menu.AlembicMenu;
import com.effecoria.alchemy.menu.AssemblerMenu;
import com.effecoria.alchemy.menu.BurnerMenu;
import com.effecoria.alchemy.menu.FormSelectMenu;
import com.effecoria.alchemy.menu.ImprinterMenu;
import com.effecoria.alchemy.menu.PhiIncubatorMenu;
import com.effecoria.alchemy.menu.OmegaDamperMenu;
import com.effecoria.alchemy.menu.PhiFabricatorMenu;
import com.effecoria.alchemy.menu.MortarMenu;
import com.effecoria.alchemy.menu.SealInscriberMenu;
import com.effecoria.alchemy.menu.SparkReactorMenu;
import com.effecoria.alchemy.menu.HeartReactorMenu;
import com.effecoria.alchemy.menu.ForgeReactorMenu;
import com.effecoria.alchemy.menu.GeoWellMenu;
import com.effecoria.alchemy.menu.StarReactorMenu;
import com.effecoria.alchemy.menu.PhiArtilleryMenu;
import com.effecoria.alchemy.menu.ClimateArrayMenu;
import com.effecoria.alchemy.menu.PhiCartographyMenu;
import com.effecoria.alchemy.menu.TowerConsoleMenu;
import com.effecoria.alchemy.menu.PortalModulatorMenu;
import com.effecoria.alchemy.menu.PhiBeaconMenu;
import com.effecoria.alchemy.menu.PhiTurretMenu;
import com.effecoria.alchemy.menu.PhiCrusherMenu;
import com.effecoria.alchemy.menu.PhiWaterPurifierMenu;

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

    public static final DeferredHolder<MenuType<?>, MenuType<ForgeReactorMenu>> FORGE_REACTOR =
            MENUS.register("forge_reactor", () -> IMenuTypeExtension.create(ForgeReactorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<GeoWellMenu>> GEO_WELL =
            MENUS.register("geo_well", () -> IMenuTypeExtension.create(GeoWellMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<StarReactorMenu>> STAR_REACTOR =
            MENUS.register("star_reactor", () -> IMenuTypeExtension.create(StarReactorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<PhiArtilleryMenu>> PHI_ARTILLERY =
            MENUS.register("phi_artillery", () -> IMenuTypeExtension.create(PhiArtilleryMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ClimateArrayMenu>> CLIMATE_ARRAY =
            MENUS.register("climate_array", () -> IMenuTypeExtension.create(ClimateArrayMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<TowerConsoleMenu>> TOWER_CONSOLE =
            MENUS.register("tower_console", () -> IMenuTypeExtension.create(TowerConsoleMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<PhiCartographyMenu>> PHI_CARTOGRAPHY_TABLE =
            MENUS.register("phi_cartography_table", () -> IMenuTypeExtension.create(PhiCartographyMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<PortalModulatorMenu>> PORTAL_MODULATOR =
            MENUS.register("portal_modulator", () -> IMenuTypeExtension.create(PortalModulatorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<PhiBeaconMenu>> PHI_BEACON =
            MENUS.register("phi_beacon", () -> IMenuTypeExtension.create(PhiBeaconMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<PhiTurretMenu>> PHI_TURRET =
            MENUS.register("phi_turret", () -> IMenuTypeExtension.create(PhiTurretMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<PhiCrusherMenu>> PHI_CRUSHER =
            MENUS.register("phi_crusher", () -> IMenuTypeExtension.create(PhiCrusherMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<PhiWaterPurifierMenu>> PHI_WATER_PURIFIER =
            MENUS.register("phi_water_purifier", () -> IMenuTypeExtension.create(PhiWaterPurifierMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<AlembicMenu>> ALEMBIC =
            MENUS.register("alembic", () -> IMenuTypeExtension.create(AlembicMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ImprinterMenu>> IMPRINTER =
            MENUS.register("imprinter", () -> IMenuTypeExtension.create(ImprinterMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<PhiIncubatorMenu>> PHI_INCUBATOR =
            MENUS.register("phi_incubator", () -> IMenuTypeExtension.create(PhiIncubatorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<OmegaDamperMenu>> OMEGA_DAMPER =
            MENUS.register("omega_damper", () -> IMenuTypeExtension.create(OmegaDamperMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<PhiFabricatorMenu>> PHI_FABRICATOR =
            MENUS.register("phi_fabricator", () -> IMenuTypeExtension.create(PhiFabricatorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<FormSelectMenu>> SHAFT_LATHE =
            MENUS.register("shaft_lathe", () -> IMenuTypeExtension.create(FormSelectMenu::lathe));

    public static final DeferredHolder<MenuType<?>, MenuType<FormSelectMenu>> FACET_CUTTER =
            MENUS.register("facet_cutter", () -> IMenuTypeExtension.create(FormSelectMenu::cutter));

    public static final DeferredHolder<MenuType<?>, MenuType<AssemblerMenu>> ARTIFACT_ASSEMBLER =
            MENUS.register("artifact_assembler", () -> IMenuTypeExtension.create(AssemblerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<SealInscriberMenu>> SEAL_INSCRIBER =
            MENUS.register("seal_inscriber", () -> IMenuTypeExtension.create(SealInscriberMenu::new));
}
