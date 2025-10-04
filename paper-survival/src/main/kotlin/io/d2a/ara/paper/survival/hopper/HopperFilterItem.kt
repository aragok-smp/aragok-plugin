package io.d2a.ara.paper.survival.hopper

import io.d2a.ara.paper.base.custom.CustomItems.Companion.NAMESPACE
import io.d2a.ara.paper.base.extension.*
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Hopper
import org.bukkit.inventory.ItemRarity
import org.bukkit.inventory.ItemStack

// TODO(future): when syncing inventories to other servers, we should make sure the player cannot
//  retrieve the hopper filter item on the other server
// TODO: prevent item duplication
class HopperFilterItem {

    enum class HopperFilterType {
        ALLOW,
        DENY,
        DELETE
    }

    companion object {
        val PDC_FILTER_TYPE = NamespacedKey(NAMESPACE, "hopper_filter_type")
        val PDC_IS_SMARTY_HOPPER = NamespacedKey(NAMESPACE, "is_smart_hopper")

        val SMART_HOPPER_MODEL = NamespacedKey(NAMESPACE, "smart_hopper")
        val ALLOW_FILTER_MODEL = NamespacedKey(NAMESPACE, "filter_accept")
        val DENY_FILTER_MODEL = NamespacedKey(NAMESPACE, "filter_deny")
        val DELETE_FILTER_MODEL = NamespacedKey(NAMESPACE, "filter_delete")

        /**
         * Creates a hopper filter item stack.
         * @param filterItemModel The item model key to use for the filter item.
         * @param filterDisplayName The display name to use for the filter item.
         * @return The created hopper filter item stack.
         */
        private fun createFilterItem(
            filterItemModel: NamespacedKey,
            filterDisplayName: Component,
            filterType: HopperFilterType,
            materialType: Material = Material.YELLOW_SHULKER_BOX
        ) =
            ItemStack.of(materialType).apply {
                itemMeta = itemMeta?.apply {
                    itemModel = filterItemModel

                    customName(filterDisplayName.italic(false))
                    setRarity(ItemRarity.UNCOMMON)

                    // for later (fast) lookup that this is a hopper filter
                    persistentDataContainer.setEnum(PDC_FILTER_TYPE, filterType)
                }
            }

        fun toItem(type: HopperFilterType) = when (type) {
            HopperFilterType.ALLOW -> createFilterItem(
                filterType = HopperFilterType.ALLOW,
                filterItemModel = ALLOW_FILTER_MODEL,
                filterDisplayName = Component.text("Accept Item Filter")
            )

            HopperFilterType.DENY -> createFilterItem(
                filterType = HopperFilterType.DENY,
                filterItemModel = DENY_FILTER_MODEL,
                filterDisplayName = Component.text("Deny Item Filter")
            )

            HopperFilterType.DELETE -> createFilterItem(
                filterType = HopperFilterType.DELETE,
                filterItemModel = DELETE_FILTER_MODEL,
                filterDisplayName = Component.text("Delete Item Filter")
            )
        }

        fun toSmartHopperItem() = ItemStack.of(Material.HOPPER).apply {
            itemMeta = itemMeta?.apply {
                itemModel = SMART_HOPPER_MODEL

                customName("Smart Hopper".text().italic(false))
                setRarity(ItemRarity.UNCOMMON)

                // for later (fast) lookup that this is a hopper filter
                persistentDataContainer.setTrue(PDC_IS_SMARTY_HOPPER)
            }
        }

        // fast lookup if the item stack is a hopper filter
        fun isHopperFilter(item: ItemStack?, type: Material = Material.YELLOW_SHULKER_BOX): Boolean {
            if (item?.type != type) return false
            return item.persistentDataContainer.has(PDC_FILTER_TYPE)
        }

        // nearly the same as isHopperFilter, but returns the type of filter, which is a bit more expensive
        fun getHopperFilterType(item: ItemStack?, type: Material = Material.YELLOW_SHULKER_BOX): HopperFilterType? {
            if (item?.type != type) return null
            return item.persistentDataContainer.getEnum<HopperFilterType>(PDC_FILTER_TYPE)
        }

        fun isSmartHopperItem(item: ItemStack?): Boolean {
            if (item?.type != Material.HOPPER) return false
            return item.persistentDataContainer.isTrue(PDC_IS_SMARTY_HOPPER)
        }

        fun isSmartHopperBlock(state: Hopper): Boolean {
            if (state.block.type != Material.HOPPER) return false
            return state.persistentDataContainer.isTrue(PDC_IS_SMARTY_HOPPER)
        }

        fun setSmartHopper(hopper: Hopper) {
            hopper.persist {
                setTrue(PDC_IS_SMARTY_HOPPER)
            }
        }
    }

}