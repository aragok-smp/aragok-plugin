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
                val blockStateMeta = it?.itemMeta as? BlockStateMeta ?: return@mapNotNull null
                val shulkerBox = blockStateMeta.blockState as? ShulkerBox ?: return@mapNotNull null
                Filter(type, shulkerBox)
            }
            .toList()

        if (filters.isEmpty()) return Result.VANILLA // no filters, behave like a normal hopper

        val hasAnyDeny = filters.any { it.type == HopperFilterItem.HopperFilterType.DENY }

        var decision: Result? = null
        for ((type, shulkerBox) in filters) {
            val match = shulkerBoxMatchesFast(shulkerBox, stack)
            if (!match) continue

            when (type) {
                HopperFilterItem.HopperFilterType.ALLOW -> {
                    decision = Result.ACCEPT // later rules can override
                }

                HopperFilterItem.HopperFilterType.DENY -> {
                    decision = Result.DENY // later rules can override
                }

                HopperFilterItem.HopperFilterType.DELETE -> {
                    // if no choice yet, treat as if it's currently allowed
                    if (decision == null || decision == Result.ACCEPT) {
                        return Result.DELETE
                    }
                }
            }
        }

        if (decision != null) {
            return decision
        }

        return if (hasAnyDeny) Result.ACCEPT else Result.DENY
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