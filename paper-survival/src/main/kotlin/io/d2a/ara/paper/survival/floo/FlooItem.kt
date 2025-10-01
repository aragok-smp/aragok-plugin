package io.d2a.ara.paper.survival.floo

import io.d2a.ara.paper.base.custom.CustomItems
import io.d2a.ara.paper.base.custom.CustomItems.Companion.NAMESPACE
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemRarity
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.plugin.Plugin

class FlooItem {

    companion object {
        val GLIMMERS_ITEM_MODEL = NamespacedKey(NAMESPACE, "floo_glimmers")
        val UNUSED_POWDER_ITEM_MODEL = NamespacedKey(NAMESPACE, "unused_floo_powder")
        val POWDER_ITEM_MODEL = NamespacedKey(NAMESPACE, "floo_powder")

        fun toGlimmersItem() =
            CustomItems.createCustomItem(GLIMMERS_ITEM_MODEL, "Floo Glimmers", rarity = ItemRarity.UNCOMMON)

        fun toUnusedPowderItem() =
            CustomItems.createCustomItem(UNUSED_POWDER_ITEM_MODEL, "Unused Floo Powder", rarity = ItemRarity.RARE)

        fun toPowderItem() =
            CustomItems.createCustomItem(POWDER_ITEM_MODEL, "Floo Powder", rarity = ItemRarity.EPIC)

        fun registerRecipe(plugin: Plugin) {
            val glimmersItem = toGlimmersItem()
            val unusedPowderItem = toUnusedPowderItem()

            plugin.server.apply {
                addRecipe(
                    ShapedRecipe(
                        NamespacedKey(plugin, "unused_floo_powder"),
                        unusedPowderItem
                    )
                        .shape(" G ", "GPG", " G ")
                        .setIngredient('G', RecipeChoice.ExactChoice(glimmersItem))
                        .setIngredient('P', Material.ENDER_PEARL)
                )
            }
        }
    }

}