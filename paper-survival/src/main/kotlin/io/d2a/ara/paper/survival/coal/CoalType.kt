package io.d2a.ara.paper.survival.coal

import io.d2a.ara.paper.base.custom.CustomItems.Companion.NAMESPACE
import io.d2a.ara.paper.base.extension.getEnum
import io.d2a.ara.paper.base.extension.italic
import io.d2a.ara.paper.base.extension.setEnum
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemRarity
import org.bukkit.inventory.ItemStack

/**
 * Specialized types of coal with different boosts to smelting and chances to duplicate
 * items when used in a furnace.
 */
enum class CoalType(
    val itemModel: NamespacedKey,
    val displayName: String,
    val rarity: ItemRarity,
    val burnTimeMultiplier: Float,
    val cookTimeMultiplier: Float,
    val duplicateChance: Float,
) {
    ENRICHED(
        NamespacedKey(NAMESPACE, "enriched_coal"),
        "Enriched Coal",
        ItemRarity.UNCOMMON,
        1.8f,
        0.9f, // omg so bad
        0.0f
    ),
    INFUSED(
        NamespacedKey(NAMESPACE, "infused_coal"),
        "Infused Coal",
        ItemRarity.RARE,
        3.0f, // has the longest burn time
        0.60f,
        0.8f // 100 items -> around 8 extra items
    ),
    SUPERCHARGED(
        NamespacedKey(NAMESPACE, "supercharged_coal"),
        "Supercharged Coal",
        ItemRarity.EPIC,
        3.2f, // just slightly better burn time
        0.10f,
        0.15f // 100 items -> around 15 extra items
    );


    /**
     * Converts this CoalItem to a Bukkit ItemStack with appropriate metadata.
     */
    fun toItem(): ItemStack = ItemStack.of(Material.COAL).apply {
        itemMeta = itemMeta?.apply {
            displayName(Component.text(this@CoalType.displayName).italic(false))
            setRarity(this@CoalType.rarity)
            itemModel = this@CoalType.itemModel
            persistentDataContainer.setEnum(PDC_KEY_COAL_TYPE, this@CoalType)
        }
    }

    companion object {
        val PDC_KEY_COAL_TYPE = NamespacedKey(NAMESPACE, "coal_type")

        /**
         * Retrieves the CoalType from the given ItemStack, if it is a special coal item.
         * Returns null if the item is not a special coal item.
         */
        fun fromItem(item: ItemStack?): CoalType? {
            // these checks are only to improve performance
            if (item?.type != Material.COAL || !item.hasItemMeta()) {
                return null
            }
            return item.itemMeta.persistentDataContainer.getEnum<CoalType>(PDC_KEY_COAL_TYPE)
        }
    }


}