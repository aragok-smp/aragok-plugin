package io.d2a.ara.paper.survival;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys;
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys;
import io.papermc.paper.tag.PostFlattenTagRegistrar;
import io.papermc.paper.tag.PreFlattenTagRegistrar;
import io.papermc.paper.tag.TagEntry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public class AragokPaperSurvivalBootstrap implements PluginBootstrap {

    @Override
    @SuppressWarnings("NullableProblems")
    public void bootstrap(@NotNull final BootstrapContext context) {
        context.getLogger().info("Bootstrapping Aragok");

        final LifecycleEventManager<BootstrapContext> manager =
                context.getLifecycleManager();

        manager.registerEventHandler(LifecycleEvents.TAGS.preFlatten(RegistryKey.ITEM), event -> {
            final PreFlattenTagRegistrar<ItemType> registrar = event.registrar();
            registrar.setTag(Constants.ENCHANTABLE_WEAPON_MINING, Set.of(
                    TagEntry.tagEntry(ItemTypeTagKeys.ENCHANTABLE_MINING),
                    TagEntry.tagEntry(ItemTypeTagKeys.ENCHANTABLE_WEAPON)
            ));
        });

        manager.registerEventHandler(RegistryEvents.ENCHANTMENT
                .compose()
                .newHandler(event -> {
                    final var registry = event.registry();

                    context.getLogger().info("Registering Green Thumb Enchantment with key: {}", Constants.GREEN_THUMB_TYPED_KEY);
                    registry.register(
                            Constants.GREEN_THUMB_TYPED_KEY,
                            builder -> builder
                                    .description(Component.translatable(Constants.GREEN_THUMB_ENCHANTMENT_KEY, "Green Thumb"))
                                    .supportedItems(event.getOrCreateTag(ItemTypeTagKeys.HOES))
                                    .weight(5)
                                    .maxLevel(1)
                                    .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(1, 0))
                                    .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(50, 0))
                                    .anvilCost(2)
                                    .activeSlots(EquipmentSlotGroup.HAND)
                    );

                    context.getLogger().info("Registering Telekinesis Enchantment with key: {}", Constants.TELEKINESIS_TYPED_KEY);
                    registry.register(
                            Constants.TELEKINESIS_TYPED_KEY,
                            builder -> builder
                                    .description(Component.translatable(
                                            Constants.TELEKINESIS_ENCHANTMENT_KEY,
                                            "Telekinesis",
                                            Style.style(NamedTextColor.YELLOW)
                                    ))
                                    .supportedItems(event.getOrCreateTag(Constants.ENCHANTABLE_WEAPON_MINING))
                                    .weight(1)
                                    .maxLevel(2)
                                    .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(30, 0))
                                    .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(70, 0))
                                    .anvilCost(5)
                                    .activeSlots(EquipmentSlotGroup.MAINHAND)
                    );
                }));

        manager.registerEventHandler(
                LifecycleEvents.TAGS.postFlatten(RegistryKey.ENCHANTMENT),
                event -> {
                    context.getLogger().info("Registering Enchantments to relevant tags");

                    final PostFlattenTagRegistrar<Enchantment> registrar = event.registrar();
                    registrar.addToTag(EnchantmentTagKeys.TRADEABLE,
                            Set.of(Constants.GREEN_THUMB_TYPED_KEY, Constants.TELEKINESIS_TYPED_KEY));
                    registrar.addToTag(EnchantmentTagKeys.NON_TREASURE,
                            Set.of(Constants.GREEN_THUMB_TYPED_KEY, Constants.TELEKINESIS_TYPED_KEY));
                    registrar.addToTag(EnchantmentTagKeys.IN_ENCHANTING_TABLE,
                            Set.of(Constants.GREEN_THUMB_TYPED_KEY, Constants.TELEKINESIS_TYPED_KEY));
                });
    }

}
