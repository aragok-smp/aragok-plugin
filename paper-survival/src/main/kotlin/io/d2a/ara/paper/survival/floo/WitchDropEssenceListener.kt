package io.d2a.ara.paper.survival.floo

import org.bukkit.NamespacedKey
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.inventory.ItemStack
import java.util.concurrent.ThreadLocalRandom

class WitchDropEssenceListener(
    val unusedFlooPowderRecipeKey: NamespacedKey
) : Listener {

    val essenceItem: ItemStack by lazy { FlooItem.toEssenceItem() }

    companion object {
        const val DROP_CHANCE = 0.5f
    }

    @EventHandler
    fun onWitchDeath(event: EntityDeathEvent) {
        if (event.entity.type != EntityType.WITCH) return

        val random = ThreadLocalRandom.current()
        if (random.nextFloat() < DROP_CHANCE) {
            event.drops.add(essenceItem.clone())
            // even more drops???
            if (random.nextFloat() < 0.5) {
                event.drops.add(essenceItem.clone())
            }

            // if the player is a killer, also unlock the recipe
            event.entity.killer?.discoverRecipe(unusedFlooPowderRecipeKey)
        }
    }

}