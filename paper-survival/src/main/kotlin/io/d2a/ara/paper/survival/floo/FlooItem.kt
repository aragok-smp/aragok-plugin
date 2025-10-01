package io.d2a.ara.paper.survival.floo

import io.d2a.ara.paper.base.extension.noItalic
import io.d2a.ara.paper.survival.coal.NAMESPACE
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemRarity
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.plugin.Plugin

class FlooItem {

    companion object {
        val GLIMMERS_ITEM_MODEL = NamespacedKey(NAMESPACE, "floo_glimmers")
        val UNUSED_POWDER_ITEM_MODEL = NamespacedKey(NAMESPACE, "unused_floo_powder")
        val POWDER_ITEM_MODEL = NamespacedKey(NAMESPACE, "floo_powder")

        fun toGlimmersItem(): ItemStack = ItemStack.of(Material.STICK).apply {
            itemMeta = itemMeta?.apply {
                displayName(Component.text("Floo Glimmers").noItalic())
                setRarity(ItemRarity.UNCOMMON)
                itemModel = GLIMMERS_ITEM_MODEL
            }
        }

        fun toUnusedPowderItem(): ItemStack = ItemStack.of(Material.STICK).apply {
            itemMeta = itemMeta?.apply {
                displayName(Component.text("Unused Floo Powder").noItalic())
                setRarity(ItemRarity.RARE)
                itemModel = UNUSED_POWDER_ITEM_MODEL
            }
        }

        fun toPowderItem(): ItemStack = ItemStack.of(Material.STICK).apply {
            itemMeta = itemMeta?.apply {
                displayName(Component.text("Floo Powder").noItalic())
                setRarity(ItemRarity.RARE)
                itemModel = POWDER_ITEM_MODEL
            }
        }

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