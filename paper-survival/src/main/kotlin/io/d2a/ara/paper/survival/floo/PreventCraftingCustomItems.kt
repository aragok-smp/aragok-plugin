package io.d2a.ara.paper.survival.floo

import io.d2a.ara.paper.survival.coal.NAMESPACE
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.inventory.ItemStack
import java.util.logging.Logger

class PreventCraftingCustomItems(
    val logger: Logger
) : Listener {

    fun isCustomItem(stack: ItemStack?): Boolean {
        if (stack?.hasItemMeta() != true) return false
        return stack.itemMeta?.itemModel?.namespace == NAMESPACE
    }

    @EventHandler
    fun onCraft(event: PrepareItemCraftEvent) {
        val result = event.inventory.result ?: return
        if (isCustomItem(result)) return

        for (stack in event.inventory.contents) {
            if (isCustomItem(stack)) {
                logger.info("Preventing crafting with custom item: $stack")
                event.inventory.result = null
                break
            }
        }
    }

}