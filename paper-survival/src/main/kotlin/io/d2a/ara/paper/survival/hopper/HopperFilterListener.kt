package io.d2a.ara.paper.survival.hopper

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material
import org.bukkit.block.Container
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack

class HopperFilterListener : Listener {

    companion object {
        val PLAIN = PlainTextComponentSerializer.plainText()
        var AIR = ItemStack.of(Material.AIR)
    }

    enum class TransferResult {
        ACCEPT,
        DENY,
        NEUTRAL,
        DELETE
    }

    fun matches(filterName: String, itemName: String): TransferResult {
        var checkingFilterName = filterName

        // prefixing '%' means to delete the item instead of transferring it
        var acceptResult = TransferResult.ACCEPT
        if (checkingFilterName.startsWith("@")) {
            checkingFilterName = filterName.drop(1)
            acceptResult = TransferResult.DELETE
        }

        // prefixing '!' means negation
        var truthy = true
        if (checkingFilterName.startsWith('!')) {
            checkingFilterName = filterName.drop(1)
            truthy = false
        }

        // TODO(future): support prefixing AND suffixing with '*'
        if (checkingFilterName.startsWith('*')) {
            checkingFilterName = filterName.substring(1)
            return if (itemName.startsWith(checkingFilterName) == truthy) {
                acceptResult
            } else {
                TransferResult.DENY
            }
        }

        if (filterName.endsWith('*')) {
            checkingFilterName = checkingFilterName.dropLast(1)
            return if (itemName.endsWith(checkingFilterName) == truthy) {
                acceptResult
            } else {
                TransferResult.DENY
            }
        }

        return if ((itemName == checkingFilterName) == truthy) {
            acceptResult
        } else {
            TransferResult.DENY
        }
    }

    fun getResult(container: Container, stack: ItemStack): TransferResult {
        val plainName = container.customName()?.let {
            PLAIN.serialize(it).lowercase()
        } ?: return TransferResult.NEUTRAL
        if (plainName.isBlank()) return TransferResult.NEUTRAL

        val itemName = stack.type.name.lowercase()
        for (part in plainName.split(',')) {
            val result = matches(part, itemName)
            if (result != TransferResult.DENY) {
                return result
            }
        }
        return TransferResult.DENY
    }

    @EventHandler
    fun onHopperTransfer(event: InventoryMoveItemEvent) {
        if (event.destination.type != InventoryType.HOPPER) return
        val container = event.destination.holder as? Container ?: return

        // filter based on container name
        val result = getResult(container, event.item)
        when(result) {
            TransferResult.DENY -> {
                event.isCancelled = true
            }
            TransferResult.DELETE -> {
                // this might be illegal, but whatever
                event.isCancelled = false
                event.item = AIR
            }
            else -> {
                // do nothing
            }
        }
    }

}