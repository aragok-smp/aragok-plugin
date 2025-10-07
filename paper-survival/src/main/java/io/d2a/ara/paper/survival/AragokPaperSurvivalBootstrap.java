package io.d2a.ara.paper.survival;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys;
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys;
import io.papermc.paper.tag.PostFlattenTagRegistrar;
import net.kyori.adventure.text.Component;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public class AragokPaperSurvivalBootstrap implements PluginBootstrap {

    @Override
    public void bootstrap(@NotNull final BootstrapContext context) {
        context.getLogger().info("Bootstrapping Aragok");

        final var GREEN_THUMB = TypedKey.create(RegistryKey.ENCHANTMENT, Constants.GREEN_THUMB);

        context.getLogger().info("Registering Green Thumb Enchantment with key: {}", GREEN_THUMB);
        context.getLifecycleManager().registerEventHandler(RegistryEvents.ENCHANTMENT
                .compose()
                .newHandler(event -> event
                        .registry()
                        .register(
                                GREEN_THUMB,
                                builder -> builder
                                        .description(Component.translatable(Constants.GREEN_THUMB_ENCHANTMENT_KEY, "Green Thumb"))
                                        .supportedItems(event.getOrCreateTag(ItemTypeTagKeys.ENCHANTABLE_MINING))
                                        .weight(5)
                                        .maxLevel(1)
                                        .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(1, 0))
                                        .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(50, 0))
                                        .anvilCost(2)
                                        .activeSlots(EquipmentSlotGroup.HAND)
                        )));

        context.getLogger().info("Registering Green Thumb Enchantment to relevant tags");
        context.getLifecycleManager().registerEventHandler(LifecycleEvents.TAGS
                        .postFlatten(RegistryKey.ENCHANTMENT),
                event -> {
                    final PostFlattenTagRegistrar<Enchantment> registrar = event.registrar();
                    registrar.addToTag(EnchantmentTagKeys.TRADEABLE, Set.of(GREEN_THUMB));
                    registrar.addToTag(EnchantmentTagKeys.NON_TREASURE, Set.of(GREEN_THUMB));
                    registrar.addToTag(EnchantmentTagKeys.IN_ENCHANTING_TABLE, Set.of(GREEN_THUMB));
                });
    }

}