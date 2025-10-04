package io.d2a.ara.paper.survival.devnull

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.inventory.meta.BundleMeta

class ItemPickupDevNullListener : Listener {

    @EventHandler
    fun onPickup(event: EntityPickupItemEvent) {
        val player = event.entity as? Player ?: return
        val stack = event.item.itemStack

        // search for dev null item in HOTBAR
        for (item in player.inventory.take(9)) {
            if (!DevNullItem.isDevNull(item)) {
                continue
            }
            val bundle = item.itemMeta as? BundleMeta ?: continue

            for (bundleItem in bundle.items) {
                if (bundleItem.isSimilar(stack)) {
                    event.item.remove()
                    event.isCancelled = true
                    return
                }
            }

        }
    }

}