package io.d2a.ara.paper.survival.devnull

import io.d2a.ara.paper.base.custom.CustomItems.Companion.NAMESPACE
import io.d2a.ara.paper.base.extension.italic
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemRarity
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin

class DevNullItem {

    companion object {
        val PDC_DEV_NULL_ENABLEMENT = NamespacedKey(NAMESPACE, "dev_null")
        val ITEM_MODEL = NamespacedKey(NAMESPACE, "dev_null")

        fun isDevNull(item: ItemStack?): Boolean {
            // these checks are only to improve performance
            if (item?.type != Material.BLACK_BUNDLE || !item.hasItemMeta()) {
                return false
            }
            return item.itemMeta?.persistentDataContainer?.has(PDC_DEV_NULL_ENABLEMENT) == true
        }

        fun toItem(): ItemStack = ItemStack.of(Material.BLACK_BUNDLE).apply {
            itemMeta = itemMeta?.apply {
                displayName(Component.text("/dev/null").italic(false))
                setRarity(ItemRarity.RARE)
                itemModel = ITEM_MODEL
                persistentDataContainer.set(
                    PDC_DEV_NULL_ENABLEMENT,
                    PersistentDataType.BYTE,
                    1
                )
            }
        }

        fun registerRecipe(plugin: Plugin) {
            val devNullItem = toItem()

            plugin.server.apply {
                addRecipe(
                    ShapedRecipe(
                        NamespacedKey(plugin, "dev_null"),
                        devNullItem
                    )
                        .shape("OEO", "HRH", "OEO")
                        .setIngredient('O', Material.OBSIDIAN)
                        .setIngredient('E', Material.ENDER_PEARL)
                        .setIngredient('H', Material.HOPPER)
                        .setIngredient('R', Material.REDSTONE)
                )
            }
        }
    }

}