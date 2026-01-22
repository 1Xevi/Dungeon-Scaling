package net.dungeon_scaling.fabric.client;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import net.dungeon_scaling.config.ConfigServer; // Only needed for auto-save hook
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.registry.*;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class GuiUtils {
    private static final Map<Object, List<String>> REGISTRY_CACHE = new ConcurrentHashMap<>();

    public static void clearCache() {
        REGISTRY_CACHE.clear();
    }

    @FunctionalInterface
    public interface EditorInjector<T> {
        void inject(BuilderContext<OptionGroup.Builder> ctx, T item);
    }

    public record BuilderContext<B>(
            B builder,
            Screen parent,
            Runnable refreshSelf) {
    }

    public static <T> OptionEventListener<T> onUpdate(Consumer<T> setter, Runnable refresher) {
        return (option, event) -> {
            if (event == OptionEventListener.Event.STATE_CHANGE) {
                setter.accept(option.pendingValue());
                refresher.run();
            }
        };
    }

    // generic screen builder
    public static <T> Screen createGeneric(
            Screen parent,
            Text title,
            T configObject,
            Consumer<BuilderContext<ConfigCategory.Builder>> contentGenerator,
            Runnable onReturn
    ) {
        return new ScreenFactory(parent, title, contentGenerator, onReturn).build();
    }

    private static class ScreenFactory {
        private final Screen parent;
        private final Text title;
        private final Consumer<BuilderContext<ConfigCategory.Builder>> contentGenerator;
        private final Runnable onReturn;

        public ScreenFactory(Screen parent, Text title, Consumer<BuilderContext<ConfigCategory.Builder>> contentGenerator, Runnable onReturn) {
            this.parent = parent;
            this.title = title;
            this.contentGenerator = contentGenerator;
            this.onReturn = onReturn;
        }

        public Screen build() {
            var builder = YetAnotherConfigLib.createBuilder()
                    .title(title);

            var catBuilder = ConfigCategory.createBuilder()
                    .name(Text.literal("Settings"));

            Runnable refreshAction = () -> {
                AutoConfig.getConfigHolder(ConfigServer.class).save();
                MinecraftClient.getInstance().setScreen(this.build());
            };

            contentGenerator.accept(new BuilderContext<>(catBuilder, parent, refreshAction));

            return builder
                    .category(catBuilder.build())
                    .save(refreshAction)
                    .build()
                    .generateScreen(parent);
        }
    }

    // generic list builder
    public static <T> void addGenericList(
            BuilderContext<ConfigCategory.Builder> ctx,
            String listTitle, String itemLabel,
            List<T> list,
            Supplier<T> factory,
            Function<T, String> nameProvider,
            Function<T, String> descriptionProvider,
            BiConsumer<BuilderContext<OptionGroup.Builder>, T> editorInjector
    ) {
        var headerGroup = OptionGroup.createBuilder()
                .name(Text.literal(listTitle))
                .collapsed(false);
        headerGroup.option(ButtonOption.createBuilder()
                .name(Text.literal("§a[+] Add New " + itemLabel))
                .action((s, b) -> {
                    list.add(factory.get());
                    //AutoConfig.getConfigHolder(ConfigServer.class).save();
                    ctx.refreshSelf.run();
                })
                .build());

        ctx.builder.group(headerGroup.build());

        for (int i = 0; i < list.size(); i++) {
            int index = i;
            T item = list.get(i);

            var itemGroup = OptionGroup.createBuilder()
                    .name(Text.literal(nameProvider.apply(item)))
                    .description(OptionDescription.of(Text.literal(descriptionProvider.apply(item))))
                    .collapsed(true);

            editorInjector.accept(new BuilderContext<>(itemGroup, ctx.parent, ctx.refreshSelf), item);

            itemGroup.option(ButtonOption.createBuilder()
                    .name(Text.literal("§c[X] Delete this entry"))
                    .action((s, b) -> {
                        list.remove(index);
                        //AutoConfig.getConfigHolder(ConfigServer.class).save();
                        ctx.refreshSelf.run();
                    })
                    .build());

            ctx.builder.group(itemGroup.build());
        }
    }

    // sublist button
    public static <T> void addSubListButton(
            BuilderContext<OptionGroup.Builder> ctx,
            String title, String label,
            List<T> list,
            Supplier<T> factory,
            Function<T, String> nameProvider,
            Function<T, String> descriptionProvider,
            BiConsumer<BuilderContext<OptionGroup.Builder>, T> editorInjector
    ) {
        StringBuilder previewText = new StringBuilder("§7Contents:");
        if (list.isEmpty()) {
            previewText.append("\n§8(Empty)");
        } else {
            int limit = 5;
            for (int i = 0; i < Math.min(list.size(), limit); i++) {
                previewText.append("\n§7- ").append(nameProvider.apply(list.get(i)));
            }
            if (list.size() > limit) {
                previewText.append("\n§8... and ").append(list.size() - limit).append(" more.");
            }
        }

        var builder = ctx.builder();
        var parent = ctx.parent();

        builder.option(ButtonOption.createBuilder()
                .name(Text.literal("§e[>] Edit " + title + " §7(" + list.size() + ")"))
                .description(OptionDescription.of(Text.literal(previewText.toString())))
                .action((s, b) -> {
                    MinecraftClient.getInstance().setScreen(
                            createGeneric(
                                    s,
                                    Text.literal(title),
                                    list,
                                    (subCtx) -> addGenericList(
                                            subCtx,
                                            title,
                                            label,
                                            list,
                                            factory,
                                            nameProvider,
                                            descriptionProvider,
                                            editorInjector
                                    ),
                                    () -> MinecraftClient.getInstance().setScreen(s)
                            )
                    );
                })
                .build());
    }

    // helpers

    public static Option<Boolean> buildBool(String name, String desc, boolean def, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return Option.<Boolean>createBuilder().name(Text.literal(name))
                .description(OptionDescription.of(Text.literal(desc)))
                .binding(def, getter, setter)
                .controller(TickBoxControllerBuilder::create).build();
    }

    public static List<String> getRegistryIds(@Nullable Registry<?> registry) {
        if (registry == null) return List.of("");

        if (REGISTRY_CACHE.containsKey(registry)) {
            return REGISTRY_CACHE.get(registry);
        }

        List<String> ids = new ArrayList<>(registry.getIds().stream()
                .map(Identifier::toString).sorted().toList());
        ids.add(0, "");

        REGISTRY_CACHE.put(registry, ids);
        return ids;
    }

    public static List<String> getRegistryIds(RegistryKey<? extends Registry<?>> key) {
        if (REGISTRY_CACHE.containsKey(key)) {
            return REGISTRY_CACHE.get(key);
        }

        List<String> result;
        var client = MinecraftClient.getInstance();

        if (client.world != null) {
            try {
                var regOptional = client.world.getRegistryManager().getOptional(key);
                if (regOptional.isPresent()) {
                    result = getRegistryIds(regOptional.get());
                    REGISTRY_CACHE.put(key, result);
                    return result;
                }
            } catch (Exception ignored) {

            }
        }

        if (key.equals(RegistryKeys.ENTITY_TYPE)) {
            result = getRegistryIds(Registries.ENTITY_TYPE);
        } else if (key.equals(RegistryKeys.ITEM)) {
            result = getRegistryIds(Registries.ITEM);
        } else if (key.equals(RegistryKeys.BLOCK)) {
            result = getRegistryIds(Registries.BLOCK);
        } else if (key.equals(RegistryKeys.DIMENSION)) {
            List<String> defaults = new ArrayList<>();
            defaults.add("");
            defaults.add("minecraft:overworld");
            defaults.add("minecraft:the_nether");
            defaults.add("minecraft:the_end");
            result = defaults;
        } else {
            try {
                var wrapper = BuiltinRegistries.createWrapperLookup().getWrapperOrThrow(key);
                List<String> ids = new ArrayList<>(wrapper.streamKeys()
                        .map(k -> k.getValue().toString())
                        .sorted()
                        .toList());
                ids.add(0, "");
                result = ids;
            } catch (Exception e) {
                result = new ArrayList<>(List.of(""));
            }
        }

        REGISTRY_CACHE.put(key, result);
        return result;
    }

    public static boolean wouldLoop(ConfigServer.DifficultyType child, String candidateParentName, List<ConfigServer.DifficultyType> allTypes) {
        String current = candidateParentName;
        for(int i = 0; i < 100; i++) {
            if (current == null || current.isEmpty()) return false;
            if (current.equals(child.name)) return true;

            String finalCurrent = current;
            var next = allTypes.stream().filter(t -> t.name.equals(finalCurrent)).findFirst();
            if (next.isEmpty()) return false;

            current = next.get().parent;
        }
        return false;
    }
}