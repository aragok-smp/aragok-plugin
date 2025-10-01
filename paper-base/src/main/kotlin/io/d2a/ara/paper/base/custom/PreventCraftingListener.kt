package io.d2a.ara.paper.base.custom

import io.d2a.ara.paper.base.custom.CustomItems.Companion.NAMESPACE
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.inventory.ItemStack
import java.util.logging.Logger

class PreventCraftingListener(
    val logger: Logger
) : Listener {

    /**
     * Checks if the given item stack is a custom item from our namespace.
     */
    fun isCustomItem(stack: ItemStack?): Boolean {
        if (stack?.hasItemMeta() != true) return false
        return stack.itemMeta?.itemModel?.namespace == NAMESPACE
    }

    @EventHandler
    fun onCraft(event: PrepareItemCraftEvent) {
        val result = event.inventory.result ?: return

        // if the result is a custom item as well, for now, we allow it.
        if (isCustomItem(result)) return

        // otherwise, if any of the ingredients is a custom item, we cancel the crafting
        for (stack in event.inventory.contents) {
            if (isCustomItem(stack)) {
                logger.info("Preventing crafting with custom item: $stack")
                event.inventory.result = null
                break
            }
        }
    }

}