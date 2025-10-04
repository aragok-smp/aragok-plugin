package io.d2a.ara.paper.survival.hopper

import io.d2a.ara.paper.base.custom.CustomItems.Companion.NAMESPACE
import io.d2a.ara.paper.base.extension.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Hopper
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.inventory.ItemRarity
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BundleMeta
import java.util.logging.Logger

// TODO(future): when syncing inventories to other servers, we should make sure the player cannot
//  retrieve the hopper filter item on the other server
class HopperFilterItem(
    val logger: Logger
) : Listener {

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
            materialType: Material = Material.YELLOW_BUNDLE
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

                customName("Smart Hoper".text().italic(false))
                setRarity(ItemRarity.UNCOMMON)

                // for later (fast) lookup that this is a hopper filter
                persistentDataContainer.setTrue(PDC_IS_SMARTY_HOPPER)
            }
        }

        // fast lookup if the item stack is a hopper filter
        fun isHopperFilter(item: ItemStack?, type: Material = Material.YELLOW_BUNDLE): Boolean {
            if (item?.type != type) return false
            return item.persistentDataContainer.has(PDC_FILTER_TYPE)
        }

        // nearly the same as isHopperFilter, but returns the type of filter, which is a bit more expensive
        fun getHopperFilterType(item: ItemStack?, type: Material = Material.YELLOW_BUNDLE): HopperFilterType? {
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

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Smart Hopper Placement
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @EventHandler
    fun onHopperPlace(event: BlockPlaceEvent) {
        if (event.itemInHand.type != Material.HOPPER) return
        if (!isSmartHopperItem(event.itemInHand)) return

        val state = event.block.state as? Hopper ?: return
        setSmartHopper(state)

        event.block.world.spawnParticle(
            Particle.HEART,
            event.block.location.toCenterLocation(),
            10, 0.5, 0.5, 0.5, 0.1
        )

        // say hello to the smart hop(p)er!
        event.block.world.playSound(event.block.location, Sound.ENTITY_VILLAGER_CELEBRATE, 0.4f, 1.0f)
        logger.info("${event.player.name} placed a smart hopper at ${event.block.location}")
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    fun onHopperBreak(event: BlockBreakEvent) {
        if (event.block.type != Material.HOPPER) return

        val state = event.block.state as? Hopper ?: return
        if (!isSmartHopperBlock(state)) return

        event.block.world.spawnParticle(
            Particle.SMOKE,
            event.block.location.toCenterLocation(),
            20, 0.5, 0.5, 0.5, 0.1
        )

        // the smart hopper drops as normal hopper
        // but at least you get some XP /shrug
        event.expToDrop = 15

        (event.block.state as? Hopper)?.customName(
            "Former Smart Hopper".text()
                .color(NamedTextColor.LIGHT_PURPLE)
                .italic(false)
        )

        logger.info("${event.player.name} broke a smart hopper at ${event.block.location}")
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Craft Hopper Filter Item
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @EventHandler
    fun onHopperCraft(event: PrepareItemCraftEvent) {
        var hopperFilterType: HopperFilterType? = null

        for (item in event.inventory.matrix) {
            val filter = getHopperFilterType(item)
            if (filter != null) {
                if (hopperFilterType != null) {
                    // more than one filter item in the recipe, invalid
                    hopperFilterType = null
                    break
                }
                hopperFilterType = filter
            }
        }

        // no, or multiple filter items found
        // we don't want that!
        if (hopperFilterType == null) return

        // output a fresh hopper filter item of the same type
        val stack = toItem(hopperFilterType).apply {
            val bundle = itemMeta as? BundleMeta ?: return // this should never happen though

            for (item in event.inventory.matrix) {
                if (item == null || isHopperFilter(item)) continue
                val stack = item.clone()
                stack.amount = 1

                bundle.addItem(stack)
            }

            itemMeta = bundle
        }

        event.inventory.result = stack
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Prevent Using Hopper Filter Items
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @EventHandler(
        priority = EventPriority.LOW,
        ignoreCancelled = true
    )
    fun onInventoryClick(event: InventoryClickEvent) {
        if (
            event.action != InventoryAction.NOTHING && // looks like NOTHING is used when crafting by clicking with bundle
            event.action != InventoryAction.PICKUP_FROM_BUNDLE &&
            event.action != InventoryAction.PICKUP_ALL_INTO_BUNDLE &&
            event.action != InventoryAction.PICKUP_SOME_INTO_BUNDLE &&
            event.action != InventoryAction.PLACE_FROM_BUNDLE &&
            event.action != InventoryAction.PLACE_ALL_INTO_BUNDLE &&
            event.action != InventoryAction.PLACE_SOME_INTO_BUNDLE
        ) {
            return
        }
        if (isHopperFilter(event.cursor) || isHopperFilter(event.currentItem)) {
            // prevent moving hopper filter items in/out of bundles
            event.isCancelled = true
        }
    }

}