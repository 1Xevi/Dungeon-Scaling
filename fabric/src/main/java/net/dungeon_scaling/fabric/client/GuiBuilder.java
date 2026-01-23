package net.dungeon_scaling.fabric.client;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import dev.isxander.yacl3.api.OptionGroup;
import me.shedaniel.autoconfig.AutoConfig;
import net.dungeon_scaling.config.ConfigServer;
import net.dungeon_scaling.config.ConfigServer.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;

import java.util.List;
import java.util.ArrayList;

import static net.dungeon_scaling.fabric.client.GuiUtils.*;


public class GuiBuilder {
    private static final ConfigServer DEFAULTS = new ConfigServer();

    // --- OVERLOADED EDITORS
    // ATTRIBUTES (leaf Node)
    private static void injectEditor(BuilderContext<OptionGroup.Builder> ctx, AttributeModifier item) {
        var builder = ctx.builder();

        builder.option(Option.<String>createBuilder()
                .name(Text.literal("Attribute ID"))
                .description(OptionDescription.of(Text.literal("The registry ID of the attribute (e.g. 'minecraft:generic.attack_damage').")))
                .binding(item.attribute, () -> item.attribute, v -> item.attribute = v)
                .controller(opt -> DropdownStringControllerBuilder.create(opt).values(getRegistryIds(Registries.ATTRIBUTE)))
                .addListener(onUpdate(val -> item.attribute = val, ctx.refreshSelf()))
                .build());

        builder.option(Option.<Float>createBuilder()
                .name(Text.literal("Value"))
                .description(OptionDescription.of(Text.literal("The amount to modify the attribute by. Effect depends on the Operation.")))
                .binding(item.value, () -> item.value, v -> item.value = v)
                .controller(FloatFieldControllerBuilder::create)
                .addListener(onUpdate(val -> item.value = val, ctx.refreshSelf()))
                .build());

        builder.option(Option.<Operation>createBuilder()
                .name(Text.literal("Operation"))
                .description(OptionDescription.of(Text.literal("ADDITION: Adds a flat amount.\nMULTIPLY_BASE: Multiplies the base value (e.g. 0.5 = +50%).")))
                .binding(item.operation, () -> item.operation, v -> item.operation = v)
                .controller(opt -> EnumControllerBuilder.create(opt).enumClass(Operation.class))
                //.addListener(onUpdate(val -> item.operation = val, ctx.refreshSelf()))
                .build());
    }

    // ENTITIES (recursive)
    private static void injectEditor(BuilderContext<OptionGroup.Builder> ctx, ConfigServer.EntityModifier item) {
        if (item.entity_matches == null) item.entity_matches = new ConfigServer.EntityModifier.Filters();
        var builder = ctx.builder();

        builder.option(Option.<ConfigServer.EntityModifier.Filters.Attitude>createBuilder()
                .name(Text.literal("Attitude"))
                .description(OptionDescription.of(Text.literal("Filter which mobs this rule applies to based on their hostility.")))
                .binding(item.entity_matches.attitude, () -> item.entity_matches.attitude, v -> item.entity_matches.attitude = v)
                .controller(opt -> EnumControllerBuilder.create(opt).enumClass(ConfigServer.EntityModifier.Filters.Attitude.class))
                //.addListener(onUpdate(val -> item.entity_matches.attitude = val, ctx.refreshSelf()))
                .build());

        builder.option(Option.<String>createBuilder()
                .name(Text.literal("Entity Type"))
                .description(OptionDescription.of(Text.literal("The specific entity type to target. If empty, matches all mobs allowed by the Attitude filter.")))
                .binding(item.entity_matches.type != null ? item.entity_matches.type : "",
                        () -> item.entity_matches.type, v -> item.entity_matches.type = v)
                .controller(opt -> DropdownStringControllerBuilder.create(opt).values(getRegistryIds(Registries.ENTITY_TYPE)))
                //.addListener(onUpdate(val -> item.entity_matches.type = val, ctx.refreshSelf()))
                .build());

        builder.option(Option.<Float>createBuilder()
                .name(Text.literal("Experience Multiplier"))
                .description(OptionDescription.of(Text.literal("Bonus XP dropped. (e.g., 0.5 = +50% XP).")))
                .binding(item.experience_multiplier, () -> item.experience_multiplier, v -> item.experience_multiplier = v)
                .controller(opt -> FloatFieldControllerBuilder.create(opt).range(0f, 2f))
                .build());

        // sublist for attributes
        addSubListButton(ctx,
                "Attributes", "Attribute",
                item.attributes,
                () -> new ConfigServer.AttributeModifier("minecraft:generic.movement_speed", 0.1f),
                AttributeModifier::getSummary,
                AttributeModifier::getDetails,
                GuiBuilder::injectEditor);
    }

    // ITEMS (recursive)
    private static void injectEditor(BuilderContext<OptionGroup.Builder> ctx, ItemModifier item) {
        if (item.item_matches == null) item.item_matches = new ConfigServer.ItemModifier.Filters();
        var builder = ctx.builder();

        // item ID
        builder.option(Option.<String>createBuilder()
                .name(Text.literal("Target Item ID"))
                .description(OptionDescription.of(Text.literal("Leave empty to match by Regex instead."))) // Explanation!
                .binding(item.item_matches.id,
                        () -> item.item_matches.id,
                        v -> item.item_matches.id = v)
                .controller(StringControllerBuilder::create)
                //.addListener(onUpdate(val -> item.item_matches.id = val, ctx.refreshSelf()))
                .build());

        // loot table Regex
        builder.option(Option.<String>createBuilder()
                .name(Text.literal("Loot Table Regex"))
                .description(OptionDescription.of(Text.literal("Matches the loot table name (e.g. '~.*chests/.*'). Leave empty to ignore.")))
                .binding(item.item_matches.loot_table_regex,
                        () -> item.item_matches.loot_table_regex,
                        v -> item.item_matches.loot_table_regex = v)
                .controller(StringControllerBuilder::create)
                //.addListener(onUpdate(val -> item.item_matches.loot_table_regex = val, ctx.refreshSelf()))
                .build());

        // rarity regex
        builder.option(Option.<String>createBuilder()
                .name(Text.literal("Rarity Regex"))
                .description(OptionDescription.of(Text.literal("Matches item rarity (common, uncommon, etc).")))
                .binding(item.item_matches.rarity_regex,
                        () -> item.item_matches.rarity_regex,
                        v -> item.item_matches.rarity_regex = v)
                .controller(StringControllerBuilder::create)
                .addListener(onUpdate(val -> item.item_matches.rarity_regex = val, ctx.refreshSelf()))
                .build());

        // attributes button (Recursive)
        addSubListButton(ctx,
                "Attributes", "Attribute",
                item.attributes,
                () -> new ConfigServer.AttributeModifier("minecraft:generic.armor", 0f),
                AttributeModifier::getSummary,
                AttributeModifier::getDetails,
                GuiBuilder::injectEditor);
    }

    private static void injectEditor(BuilderContext<OptionGroup.Builder> ctx, ConfigServer.ScalingRule item) {
        var builder = ctx.builder();
        var parent = ctx.parent();

        List<String> validDifficulties = ConfigServer.fetch().difficulty_types.stream()
                .map(type -> type.name)
                .sorted()
                .toList();

        // difficulty assignment
        builder.option(Option.<String>createBuilder()
                .name(Text.literal("Difficulty Name"))
                .description(OptionDescription.of(Text.literal("The ID of the Difficulty Preset to apply here (e.g. 'adventure').")))
                .binding(item.difficulty.name, () -> item.difficulty.name, v -> item.difficulty.name = v)
                .controller(opt -> DropdownStringControllerBuilder.create(opt)
                        .values(validDifficulties)
                )
                .addListener(onUpdate(val -> item.difficulty.name = val, ctx.refreshSelf()))
                .build()
        );

        builder.option(Option.<Integer>createBuilder()
                .name(Text.literal("Difficulty Level"))
                .description(OptionDescription.of(Text.literal("The scaling multiplier level. Higher levels = stronger mobs/loot based on the Difficulty.")))
                .binding(item.difficulty.level, () -> item.difficulty.level, v -> item.difficulty.level = v)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                        .range(0, 10)
                        .step(1))
                //.addListener(onUpdate(val -> item.difficulty.level = val, ctx.refreshSelf()))
                .build()
        );

        builder.option(ButtonOption.createBuilder()
                .name(Text.literal("§b[>] Edit Match Criteria"))
                .description(OptionDescription.of(Text.literal("Configure Dimension, Biome, Structure, or Entity targets.")))
                .action((s, b) -> MinecraftClient.getInstance().setScreen(
                        GuiUtils.createGeneric(
                                s,
                                Text.literal("Match Criteria"),
                                item.match,
                                (catBuilder) -> injectEditor(catBuilder, item.match),
                                null
                        )
                )).build()
        );

        // sublist for overrides (recursive)
        addSubListButton(
                ctx,
                "Sub-Overrides", "Override",
                item.overrides,
                ConfigServer.ScalingRule::new,
                ScalingRule::getSummary,
                ScalingRule::getDetails,
                GuiBuilder::injectEditor);
    }

    private static void injectEditor(BuilderContext<ConfigCategory.Builder> ctx, ConfigServer.Rewards config) {
        addGenericList(
                ctx,
                "§6§lWeapons List", "Weapon Rule",
                config.weapons,
                ConfigServer.ItemModifier::new,
                ConfigServer.ItemModifier::getSummary,

                (item) -> "§7" + item.attributes.size() + " attributes defined.",
                GuiBuilder::injectEditor
        );

        // ARMOR LIST
        addGenericList(ctx,
                "§6§lArmor List",
                "Armor Rule", config.armor,
                ConfigServer.ItemModifier::new,
                ConfigServer.ItemModifier::getSummary,

                (item) -> "§7" + item.attributes.size() + " attributes defined.",
                GuiBuilder::injectEditor
        );
    }

    private static void injectEditor(BuilderContext<OptionGroup.Builder> ctx, ConfigServer.DifficultyType set) {
        var builder = ctx.builder();
        var config = ConfigServer.fetch();

        builder.option(Option.<String>createBuilder()
                .name(Text.literal("Name"))
                .binding(set.name,
                        () -> set.name,
                        v -> set.name = v
                )
                .controller(StringControllerBuilder::create).build());

        List<String> parents = new ArrayList<>(config.difficulty_types.stream()
                .map(t -> t.name)
                .filter(n -> !n.equals(set.name))
                .toList());

        parents.add(0, "");

        builder.option(Option.<String>createBuilder()
                .name(Text.literal("Parent"))
                .binding(
                        set.parent,
                        () -> set.parent,
                        v -> set.parent = v
                )
                .controller(
                        o -> DropdownStringControllerBuilder.create(o)
                                .values(parents)
                )
                .build());

        addSubListButton(ctx, "Entity Rules", "Rule", set.entities, EntityModifier::new,
                EntityModifier::getSummary,
                m -> "§7" + m.attributes.size() + " Modifiers",
                GuiBuilder::injectEditor);
    }


    private static void injectEditor(BuilderContext<ConfigCategory.Builder> ctx, ConfigServer.ScalingRule.Context match) {
        var builder = ctx.builder();

        builder.option(Option.<String>createBuilder()
                .name(Text.literal("Registry Lookup"))
                .description(OptionDescription.of(Text.literal("Search for IDs here. Does NOT save to config.")))
                .binding("", () -> "", v -> {})
                .controller(opt -> DropdownStringControllerBuilder.create(opt)
                        .values(GuiUtils.getRegistryIds(RegistryKeys.STRUCTURE)))
                .build());

        builder.option(Option.<String>createBuilder()
                .name(Text.literal("Dimension ID / Regex"))
                .binding(match.dimension, () -> match.dimension, v -> match.dimension = v)
                .controller(StringControllerBuilder::create)
                .build());

        builder.option(Option.<String>createBuilder()
                .name(Text.literal("Biome ID / Regex"))
                .binding(match.biome, () -> match.biome, v -> match.biome = v)
                .controller(StringControllerBuilder::create)
                .build());

        builder.option(Option.<String>createBuilder()
                .name(Text.literal("Structure ID / Regex"))
                .binding(match.structure, () -> match.structure, v -> match.structure = v)
                .controller(StringControllerBuilder::create)
                .build());

        builder.option(Option.<String>createBuilder()
                .name(Text.literal("Entity ID / Regex"))
                .binding(match.entity, () -> match.entity, v -> match.entity = v)
                .controller(StringControllerBuilder::create)
                .build());
    }


    private static void injectPresetEditor(BuilderContext<ConfigCategory.Builder> ctx, List<ConfigServer.DifficultyType> config) {
        addGenericList(
                ctx,
                "§6§lDifficulty Presets", "Preset", config,
                () -> new DifficultyType("new_preset"),
                p -> p.name,
                p -> "§7Parent: " + (!p.parent.isEmpty() ? p.parent : "None (root)"),
                GuiBuilder::injectEditor
        );
    }

    private static void injectRuleEditor(BuilderContext<ConfigCategory.Builder> ctx, List<ConfigServer.ScalingRule> config) {
        addGenericList(ctx, "§6§lHierarchical Rules", "Rule", config,
                ScalingRule::new,
                ScalingRule::getSummary,
                ScalingRule::getDetails,
                GuiBuilder::injectEditor
        );
    }

    // -- CATEGORY BUILDERS
    private static OptionGroup buildCategory(ConfigServer.Meta config, Screen screen) {
        var builder = OptionGroup.createBuilder().name(Text.literal("§6§lGlobal"));

        builder.option(buildBool(
                "Sanitize Config",
                "Automatically validates and repairs the config file on startup to prevent crashes.",
                DEFAULTS.meta.sanitize_config,
                () -> config.sanitize_config,
                v -> config.sanitize_config = v
        ));

        builder.option(Option.<ConfigServer.RoundingMode>createBuilder()
                .name(Text.literal("Rounding Precision"))
                .description(OptionDescription.of(Text.literal("Snaps attribute values to the nearest grid. Use 'Very High' for speed, 'Whole Numbers' for damage.")))
                .binding(DEFAULTS.meta.rounding_mode,
                        () -> config.rounding_mode,
                        v -> config.rounding_mode = v)
                .controller(opt -> EnumControllerBuilder.create(opt)
                        .enumClass(ConfigServer.RoundingMode.class)
                        .formatValue(v -> Text.literal(v.toString())))
                .build());

        builder.option(buildBool(
                "Merge Item Modifiers",
                "If enabled, difficulty bonuses are added to existing attributes. If disabled, they replace them.",
                DEFAULTS.meta.merge_item_modifiers,
                () -> config.merge_item_modifiers,
                v -> config.merge_item_modifiers = v
        ));

        builder.option(buildBool(
                "Global Loot Scaling",
                "Master switch to enable or disable all loot modification features.",
                DEFAULTS.meta.global_loot_scaling,
                () -> config.global_loot_scaling,
                v -> config.global_loot_scaling = v
        ));

        builder.option(buildBool(
                "Override Enchant Rarity",
                "Allows generating rare enchantments more frequently on high-level items.",
                DEFAULTS.meta.enable_scaled_items_rarity,
                () -> config.enable_overriding_enchantment_rarity,
                v -> config.enable_overriding_enchantment_rarity = v
        ));

        builder.option(buildBool(
                "Enable Scaled Items Rarity",
                "Changes the item name color (Common, Rare, Epic) based on its power level.",
                DEFAULTS.meta.enable_scaled_items_rarity,
                () -> config.enable_scaled_items_rarity,
                v -> config.enable_scaled_items_rarity = v
        ));

        return builder.build();
    }

    private static  OptionGroup buildCategory(ConfigServer.Announcement config, Screen screen) {
        var builder = OptionGroup.createBuilder().name(Text.literal("§6§lAnnouncements"));
        var ctx = new BuilderContext<>(builder, screen, () -> MinecraftClient.getInstance().setScreen(create(screen)));

        builder.option(buildBool(
                "Enabled",
                "Show titles on area change.",
                DEFAULTS.announcement.enabled,
                () -> config.enabled, v -> config.enabled = v));


        builder.option(Option.<Integer>createBuilder()
                .name(Text.literal("Cooldown (Seconds)"))
                .binding(
                        DEFAULTS.announcement.reannounce_cooldown_seconds,
                        () -> config.reannounce_cooldown_seconds, v -> config.reannounce_cooldown_seconds = v)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                        .range(0, 300)
                        .step(1))
                .build());

        builder.option(Option.<Integer>createBuilder()
                .name(Text.literal("Interval Check (Seconds)"))
                .binding(DEFAULTS.announcement.check_interval_seconds,
                        () -> config.check_interval_seconds, v -> config.check_interval_seconds = v)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                        .range(1, 10)
                        .step(1))
                .build());

        return builder.build();
    }

    private static OptionGroup buildCategory(ConfigServer config, Screen currentScreen) {
        var builder = OptionGroup.createBuilder()
                .name(Text.literal("§6§lDifficulty Configuration"))
                .description(OptionDescription.of(Text.literal("Configure detailed scaling rules and loot tables.")))
                .collapsed(false);

        builder.option(ButtonOption.createBuilder()
                .name(Text.literal("§e[>] Configure §lDifficulty Scaling"))
                .description(OptionDescription.of(Text.literal("Edit Difficulty Presets and Hierarchical Rules (Biomes/Dimensions).")))
                .action((screen, button) -> MinecraftClient.getInstance().setScreen(
                        createGeneric(
                                screen,
                                Text.literal("Difficulty Scaling Settings"),
                                config,
                                (catBuilder) -> {
                                    injectPresetEditor(catBuilder, config.difficulty_types);
                                    injectRuleEditor(catBuilder, config.scaling_rules);
                                },
                                null
                        )
                ))
                .build());

        builder.option(ButtonOption.createBuilder()
                .name(Text.literal("§e[>] Configure §lLoot Scaling"))
                .description(OptionDescription.of(Text.literal("Edit Loot Rules and Quality Bonuses.")))
                .action((screen, button) -> MinecraftClient.getInstance().setScreen(
                        createGeneric(
                                screen,
                                Text.literal("Loot Scaling Settings"),
                                config.loot_scaling,
                                (catBuilder) -> {
                                    injectEditor(catBuilder, config.loot_scaling);
                                },
                                null
                        )
                ))
                .build());


        return builder.build();
    }

    // -- MAIN ENTRY POINT
    public static Screen create(Screen thisScreen) {
        var config = ConfigServer.fetch();

        return YetAnotherConfigLib.createBuilder()
                .title(Text.literal("Dungeon Scaling Settings"))
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Dungeon Scaling Settings"))
                        .group(buildCategory(config.meta, thisScreen))
                        .group(buildCategory(config.announcement, thisScreen))
                        .group(buildCategory(config, thisScreen))
                        .build()
                )
                .save(() -> {
                    AutoConfig.getConfigHolder(ConfigServer.class).save();

                    MinecraftClient.getInstance().execute(
                            () -> MinecraftClient.getInstance().setScreen(create(thisScreen))
                    );
                })
                .build()
                .generateScreen(thisScreen);
    }
}