package io.d2a.ara.paper.survival.floo

import io.d2a.ara.paper.base.custom.CustomItems
import io.d2a.ara.paper.base.custom.CustomItems.Companion.NAMESPACE
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemRarity

class FlooItem {

    companion object {
        val ESSENCE_ITEM_MODEL = NamespacedKey(NAMESPACE, "floo_essence")
        val UNUSED_POWDER_ITEM_MODEL = NamespacedKey(NAMESPACE, "unused_floo_powder")
        val POWDER_ITEM_MODEL = NamespacedKey(NAMESPACE, "floo_powder")

        fun toEssenceItem() =
            CustomItems.createCustomItem(ESSENCE_ITEM_MODEL, "Floo Essence", rarity = ItemRarity.UNCOMMON)

        fun toUnusedPowderItem() =
            CustomItems.createCustomItem(UNUSED_POWDER_ITEM_MODEL, "Unused Floo Powder", rarity = ItemRarity.RARE)

        fun toPowderItem() =
            CustomItems.createCustomItem(POWDER_ITEM_MODEL, "Floo Powder", rarity = ItemRarity.EPIC)
    }

}