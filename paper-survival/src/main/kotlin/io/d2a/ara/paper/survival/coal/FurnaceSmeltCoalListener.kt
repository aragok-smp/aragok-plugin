package io.d2a.ara.paper.survival.coal

import io.d2a.ara.paper.base.custom.CustomItems.Companion.NAMESPACE
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.block.TileState
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.FurnaceBurnEvent
import org.bukkit.event.inventory.FurnaceSmeltEvent
import org.bukkit.event.inventory.FurnaceStartSmeltEvent
import org.bukkit.persistence.PersistentDataType
import java.util.concurrent.ThreadLocalRandom

class FurnaceSmeltCoalListener : Listener {

    companion object {
        val PDC_FURNACE_TYPE = NamespacedKey(NAMESPACE, "enriched_furnace_type")
    }

    // FurnaceBurnEvent: used to change burn time (the time the fuel lasts)
    @EventHandler
    fun onFurnaceBurn(event: FurnaceBurnEvent) {
        // we only care about tile-state blocks that can hold PDC
        val tileState = event.block.getState(false) as? TileState ?: return

        val specialCoalType = CoalType.fromItem(event.fuel)
        if (specialCoalType == null) {
            // no special coal type -> we can clean up the PDC
            tileState.persistentDataContainer.apply {
                remove(PDC_FURNACE_TYPE)
            }
            return
        }

        // otherwise we have some special coal type -> we can increase the burn time
        val newBurnTime = (event.burnTime * specialCoalType.burnTimeMultiplier).toInt()
        event.burnTime = newBurnTime

        // store the coal type and remaining burn time in the furnace PDC
        tileState.persistentDataContainer.apply {
            set(PDC_FURNACE_TYPE, PersistentDataType.BYTE, specialCoalType.ordinal.toByte())
        }
    }


    // FurnaceStartSmeltEvent: used to change the speed of smelting items => cook time
    @EventHandler
    fun onFurnaceStartSmelt(event: FurnaceStartSmeltEvent) {
        val coalType = getCoalType(event.block) ?: return

        event.totalCookTime = (event.totalCookTime * coalType.cookTimeMultiplier).toInt()
    }

    // FurnaceSmeltEvent: used to change the result of smelting => duplicate items
    @EventHandler
    fun onFurnaceSmelt(event: FurnaceSmeltEvent) {
        val coalType = getCoalType(event.block) ?: return

        if (ThreadLocalRandom.current().nextFloat() < coalType.duplicateChance) {
            event.result.amount += 1
        }
    }

    /**
     * Get the coal type used in the furnace from the furnace's PDC.
     * Returns null if no special coal type was used.
     */
    private fun getCoalType(block: Block): CoalType? {
        val tileState = block.getState(false) as? TileState
            ?: return null
        val coalTypeOrdinal = tileState.persistentDataContainer.run {
            get(PDC_FURNACE_TYPE, PersistentDataType.BYTE)
        } ?: return null
        return CoalType.entries.getOrNull(coalTypeOrdinal.toInt())
    }

}