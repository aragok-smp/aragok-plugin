package io.d2a.ara.paper.survival.hopper

import io.d2a.ara.paper.survival.hopper.HopperFilterItem.Companion.isHopperFilter
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.block.Hopper
import org.bukkit.block.ShulkerBox
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.event.inventory.InventoryPickupItemEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta
import org.bukkit.inventory.meta.EnchantmentStorageMeta
import org.bukkit.inventory.meta.ItemMeta

class HopperFilterLifecycleListener : Listener {

    companion object {
        val AIR = ItemStack.of(Material.AIR)
    }

    enum class Result {
        VANILLA, // it's not a smart hopper!
        ACCEPT,
        DENY,
        DELETE
    }

    private data class Filter(
        val type: HopperFilterItem.HopperFilterType,
        val shulker: ShulkerBox
    )

    @EventHandler
    fun onHopperItemAdd(event: InventoryMoveItemEvent) {
        // prevent filters from being moved by hoppers
        if (event.source.type == InventoryType.HOPPER && isHopperFilter(event.item)) {
            event.isCancelled = true
            return
        }

        if (event.destination.type != InventoryType.HOPPER) return
        val hopper = event.destination.holder as? Hopper ?: return

        val result = runHopper(hopper, event.item)
        if (result == Result.VANILLA) {
            return // do nothing, behave like a normal hopper
        }

        playParticles(hopper, result)

        when (result) {
            Result.DENY -> {
                event.isCancelled = true
            }

            Result.DELETE -> {
                event.isCancelled = false
                event.item = AIR
            }

            else -> {} // ACCEPT: do nothing, let the item be transferred
        }
    }

    @EventHandler
    fun onHopperPickup(event: InventoryPickupItemEvent) {
        if (event.inventory.type != InventoryType.HOPPER) return
        val hopper = event.inventory.holder as? Hopper ?: return
        val stack = event.item.itemStack

        val result = runHopper(hopper, stack)
        if (result == Result.VANILLA) return // do nothing, behave like a normal hopper
        playParticles(hopper, result)

        when (result) {
            Result.DENY -> {
                event.isCancelled = true
            }

            Result.DELETE -> {
                event.isCancelled = true
                event.item.remove()
            }

            else -> {} // ACCEPT: do nothing, let the item be transferred
        }
    }

    private fun playParticles(hopper: Hopper, result: Result) {
        val world = hopper.world
        val loc = hopper.block.location.clone()
            .add(0.5, 0.7, 0.5)

        when (result) {
            Result.ACCEPT -> world.spawnParticle(Particle.HAPPY_VILLAGER, loc, 1)
            Result.DENY -> world.spawnParticle(Particle.ANGRY_VILLAGER, loc, 1)
            Result.DELETE -> world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 2)
            else -> return
        }
    }

    private fun runHopper(hopper: Hopper, stack: ItemStack): Result {
        if (!HopperFilterItem.isSmartHopperBlock(hopper)) return Result.VANILLA

        val filters = hopper.inventory.contents
            .withIndex()
            .asSequence()
            .filter { (index, it) -> index != 0 && it != null && it.type != Material.AIR }
            .mapNotNull { (_, it) ->
                val type = HopperFilterItem.getHopperFilterType(it) ?: return@mapNotNull null
                val blockStateMeta = it!!.itemMeta as? BlockStateMeta ?: return@mapNotNull null
                val shulkerBox = blockStateMeta.blockState as? ShulkerBox ?: return@mapNotNull null
                Filter(type, shulkerBox)
            }
            .toList()

        if (filters.isEmpty()) return Result.VANILLA // no filters, behave like a normal hopper

        // special case: first filter has some implicit defaults
        // if the first filter is an ACCEPT filter: default to deny all other
        // if the first filter is an DENY filter: default to accept all other
        // if the first filter is a DELETE filter: default to accept all other
        //      this is useful if you want to delete some items, but allow the rest to go through.
        var decision: Result = when (filters.first().type) {
            HopperFilterItem.HopperFilterType.ALLOW -> Result.DENY
            HopperFilterItem.HopperFilterType.DENY -> Result.ACCEPT
            HopperFilterItem.HopperFilterType.DELETE -> Result.ACCEPT // all items after delete are considered ACCEPTED
        }

        // now our filters can override any implicit defaults
        for ((index, filter) in filters.withIndex()) {
            // check if the current item (candidate) matches any item in the shulker (=> filter)
            val match = shulkerBoxMatchesFast(filter.shulker, stack)
            if (!match) continue

            when (filter.type) {
                // allow overrides any previous decision
                HopperFilterItem.HopperFilterType.ALLOW -> decision = Result.ACCEPT

                // deny overrides any previous decision
                HopperFilterItem.HopperFilterType.DENY -> decision = Result.DENY


                // delete only applies when the previous decision was DENY.
                // note it has consequences if DELETE is the first filter:
                //      if we don't match the item in DELETE, the remaining items will still be considered DENY
                HopperFilterItem.HopperFilterType.DELETE -> {
                    // the deny filter expects the decision to be DENY.

                    // if the deny filter is the first filter, it is also considered DENY thus deletes items.
                    val isFirstFilter = index == 0
                    if (isFirstFilter || decision == Result.DENY) {
                        return Result.DELETE // deletion is considered final.
                    }
                }
            }
        }

        return decision
    }

    /**
     * Rules:
     * - Type must match.
     * - If the pattern item has any enchants (regular or stored), candidate must have those (>= level).
     * - If the pattern item has CustomModelData, candidate must have the **same** CMD.
     * - If the pattern has neither enchants nor CMD, type-only match is enough.
     * - Empty shulker == wildcard (*).
     */
    private fun shulkerBoxMatchesFast(shulkerBox: ShulkerBox, candidate: ItemStack): Boolean {
        val items = shulkerBox.inventory.contents

        // empty shulker = wildcard
        if (items.all { it == null || it.type == Material.AIR }) {
            return true
        }

        val candidateType = candidate.type
        val candidateMeta =
            candidate.itemMeta // TODO: make this more performant; if (candidate.hasItemMeta()) candidate.itemMeta else null

        for (pat in items) {
            if (pat?.type != candidateType) continue
            val patMeta = pat.itemMeta // TODO: make this more performant if (pat.hasItemMeta()) pat.itemMeta else null

            val hasPatEnchants =
                patMeta?.enchants?.isNotEmpty() == true ||
                        (patMeta as? EnchantmentStorageMeta)?.storedEnchants?.isNotEmpty() == true

            val hasPatItemModel = patMeta?.hasItemModel() == true

            if (!hasPatEnchants && !hasPatItemModel) {
                // type-only match is enough
                return true
            }

            if (hasPatEnchants && !hasRequiredEnchants(candidateMeta, patMeta)) continue
            if (hasPatItemModel && !hasSameItemModel(candidateMeta, patMeta)) continue

            return true
        }

        return false
    }

    private fun meetsRequirements(
        required: Map<Enchantment, Int>,
        actual: Map<Enchantment, Int>
    ): Boolean {
        if (required.isEmpty()) return true
        if (actual.isEmpty()) return false
        for ((enchantment, lvl) in required) {
            val have = actual[enchantment] ?: 0
            if (have < lvl) return false
        }
        return true
    }

    private fun hasRequiredEnchants(candidate: ItemMeta, pattern: ItemMeta): Boolean {
        // direct enchants (on tools, armor, etc.)
        if (!meetsRequirements(pattern.enchants, candidate.enchants)) return false

        // stored enchants (on enchanted books, etc.)
        val patternStored = (pattern as? EnchantmentStorageMeta)?.storedEnchants
        if (patternStored != null && patternStored.isNotEmpty()) {
            val candidateStored = (candidate as? EnchantmentStorageMeta)?.storedEnchants
                ?: return false
            if (!meetsRequirements(patternStored, candidateStored)) return false
        }

        return true
    }

    private fun hasSameItemModel(candidate: ItemMeta, pat: ItemMeta): Boolean {
        if (!pat.hasItemModel()) return true
        if (!candidate.hasItemModel()) return false
        return pat.itemModel == candidate.itemModel
    }


}