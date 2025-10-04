package io.d2a.ara.paper.survival.hopper

import io.d2a.ara.paper.base.extension.VILLAGER_NO_SOUND
import io.d2a.ara.paper.base.extension.failActionBar
import io.d2a.ara.paper.base.extension.successActionBar
import io.d2a.ara.paper.base.extension.text
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.ShulkerBox
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta
import java.util.*

class HopperFilterEditor : Listener {

    private data class Session(
        val playerId: UUID,
        val type: HopperFilterItem.HopperFilterType,
        val handSlot: Int,
        val inv: Inventory,
        var returned: Boolean = false
    )

    private val sessions = mutableMapOf<UUID, Session>()

    companion object {
        private val INPUT_SLOTS = setOf(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25
        )
        private const val CONFIRM_SLOT = 4
        private const val CANCEL_SLOT = 0


        private val paneStack = ItemStack.of(Material.GRAY_STAINED_GLASS_PANE).apply {
            itemMeta = itemMeta.apply {
                customName(" ".text())
            }
        }

        private val confirmStack = ItemStack.of(Material.LIME_STAINED_GLASS_PANE).apply {
            itemMeta = itemMeta.apply {
                customName("Apply".text(NamedTextColor.GREEN))
            }
        }

        private val cancel = ItemStack.of(Material.RED_STAINED_GLASS_PANE).apply {
            itemMeta = itemMeta.apply {
                customName("Cancel".text(NamedTextColor.RED))
            }
        }
    }

    @EventHandler(
        ignoreCancelled = true,
        priority = EventPriority.HIGHEST
    )
    fun onOpenEditor(event: PlayerInteractEvent) {
        val item = event.item ?: return
        if (!event.action.isRightClick) return
        if (!HopperFilterItem.isHopperFilter(item)) return

        // really don't do ANYTHING right-clicky with that item!!
        event.isCancelled = true

        val filterType = HopperFilterItem.getHopperFilterType(item) ?: return
        val player = event.player

        val title = "Configure ${filterType.name} Filter".text()
        val inventory = Bukkit.createInventory(null, 27, title)

        // background

        for (i in 0 until inventory.size) {
            inventory.setItem(i, paneStack)
        }
        inventory.setItem(CONFIRM_SLOT, confirmStack)
        inventory.setItem(CANCEL_SLOT, cancel)

        // clear input slots
        INPUT_SLOTS.forEach { inventory.setItem(it, null) }

        val session = Session(player.uniqueId, filterType, player.inventory.heldItemSlot, inventory)
        sessions[player.uniqueId] = session

        player.openInventory(inventory)
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Make the item useless
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    @EventHandler(
        ignoreCancelled = true,
        priority = EventPriority.HIGHEST
    )
    fun onClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val session = sessions[player.uniqueId] ?: return

        val top = event.view.topInventory
        if (top !== session.inv) return

        val clickedTop = event.rawSlot < top.size

        // Block shift-clicks; they can bypass slot rules
        if (event.click.isShiftClick) {
            event.isCancelled = true
            return
        }

        // Disallow hotbar number swaps into non-input areas of the top inv
        if (clickedTop && event.click == ClickType.NUMBER_KEY && event.rawSlot !in INPUT_SLOTS) {
            event.isCancelled = true
            return
        }

        // Handle buttons
        if (clickedTop) {
            when (event.rawSlot) {
                CANCEL_SLOT -> {
                    event.isCancelled = true
                    if (!session.returned) {
                        returnItemsToPlayer(player, top)
                        session.returned = true
                    }
                    closeAndCleanup(player)
                }

                CONFIRM_SLOT -> {
                    event.isCancelled = true
                    applyToFilter(player, session, top)
                }

                else -> {
                    // Only allow interaction on input slots; block background & buttons
                    if (event.rawSlot !in INPUT_SLOTS) {
                        event.isCancelled = true
                    }
                }
            }
        }
    }

    @EventHandler(
        ignoreCancelled = true,
        priority = EventPriority.HIGHEST
    )
    fun onDrag(event: InventoryDragEvent) {
        val player = event.whoClicked as? Player ?: return
        val session = sessions[player.uniqueId] ?: return

        val top = event.view.topInventory
        if (top !== session.inv) return

        // Deny any drag that touches top slots outside our input grid
        val touchesTop = event.rawSlots.any { it < top.size }
        if (!touchesTop) return
        if (event.rawSlots.any { it < top.size && it !in INPUT_SLOTS }) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onClose(e: InventoryCloseEvent) {
        val player = e.player as? Player ?: return
        val session = sessions[player.uniqueId] ?: return

        val top = e.view.topInventory
        if (top !== session.inv) return

        // If the player closes without Apply, return items
        if (!session.returned) {
            returnItemsToPlayer(player, top)
            session.returned = true
        }
        cleanup(player)
    }

    private fun applyToFilter(player: Player, session: Session, top: Inventory) {
        val samples = mutableListOf<ItemStack>()
        for (slot in INPUT_SLOTS) {
            val stack = top.getItem(slot) ?: continue
            val one = stack.clone().apply { amount = 1 }
            samples.add(one)
        }

        val hand = player.inventory.getItem(session.handSlot)
        val filter = if (hand != null && HopperFilterItem.isHopperFilter(hand))
            hand
        else
            player.inventory.contents.firstOrNull { it != null && HopperFilterItem.isHopperFilter(it) }

        if (filter == null) {
            player.failActionBar("Filter item not found anymore.", sound = VILLAGER_NO_SOUND)
            return
        }

        val type = HopperFilterItem.getHopperFilterType(filter) ?: session.type
        val programmed = HopperFilterItem.toItem(type)

        val blockStateMeta = programmed.itemMeta as? BlockStateMeta
        if (blockStateMeta == null) {
            player.failActionBar("Unexpected: filter is not block state-able.")
            return
        }

        val shulkerBox = blockStateMeta.blockState as? ShulkerBox
        if (shulkerBox == null) {
            player.failActionBar("Unexpected: filter is not a shulker box.")
            return
        }

        // update the inventory of the shulker
        shulkerBox.inventory.clear()
        samples.forEach { shulkerBox.inventory.addItem(it) }
        blockStateMeta.blockState = shulkerBox
        programmed.itemMeta = blockStateMeta

        // Replace the item in hand if possible; otherwise add to inv/drop
        if (hand != null && HopperFilterItem.isHopperFilter(hand)) {
            player.inventory.setItem(session.handSlot, programmed)
        } else {
            val leftover = player.inventory.addItem(programmed)
            if (leftover.isNotEmpty()) {
                leftover.values.forEach { player.world.dropItemNaturally(player.location, it) }
            }
        }

        // Consume exactly 1 from each input slot
        for (slot in INPUT_SLOTS) {
            val st = top.getItem(slot) ?: continue
            if (st.amount <= 1) top.setItem(slot, null) else st.amount -= 1
        }
        if (!session.returned) {
            returnItemsToPlayer(player, top)
            session.returned = true
        }

        player.playSound(player.location, Sound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 0.8f, 1.1f)
        player.successActionBar("Filter updated.")

        closeAndCleanup(player)
    }

    private fun closeAndCleanup(player: Player) {
        // Avoid recursion into onClose returning items; clean first, then close
        val session = sessions[player.uniqueId]
        cleanup(player)

        if (session != null && player.openInventory.topInventory === session.inv) {
            player.closeInventory()
        }
    }

    private fun cleanup(player: Player) {
        sessions.remove(player.uniqueId)
    }

    private fun returnItemsToPlayer(p: Player, top: Inventory) {
        for (slot in INPUT_SLOTS) {
            val st = top.getItem(slot) ?: continue
            top.setItem(slot, null)
            val leftover = p.inventory.addItem(st)
            if (leftover.isNotEmpty()) {
                leftover.values.forEach { p.world.dropItemNaturally(p.location, it) }
            }
        }
    }

}