package io.d2a.ara.paper.base.custom

import io.d2a.ara.paper.base.extension.italic
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemRarity
import org.bukkit.inventory.ItemStack

class CustomItems {

    companion object {

        /**
         * Namespace for all custom items in this plugin.
         */
        const val NAMESPACE = "aragokt"

        /**
         * Performant check if the given item stack is a custom item by checking its item model key.
         */
        fun isCustomItem(
            stack: ItemStack?,
            itemModel: NamespacedKey,
            type: Material? = Material.STICK
        ): Boolean {
            if (stack == null) return false
            // first we check the type which is much faster than accessing item meta
            if (stack.type != type) return false
            // note: we check for hasItemMeta before accessing itemMeta to avoid unnecessary object creation
            if (!stack.hasItemMeta()) return false
            return stack.itemMeta?.itemModel == itemModel
        }

        fun createCustomItem(
            itemModel: NamespacedKey,
            name: String,
            type: Material = Material.STICK,
            amount: Int = 1,
            rarity: ItemRarity? = null,
        ): ItemStack = ItemStack.of(type).apply {
            this.amount = amount

            val meta = this.itemMeta
            meta.itemModel = itemModel
            meta.displayName(Component.text(name).italic(false))
            if (rarity != null) {
                meta.setRarity(rarity)
            }

            this.itemMeta = meta
        }

    }

}