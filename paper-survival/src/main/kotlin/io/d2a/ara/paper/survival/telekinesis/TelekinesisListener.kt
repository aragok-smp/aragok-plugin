package io.d2a.ara.paper.survival.telekinesis

import io.d2a.ara.paper.base.block.BlockKey
import org.bukkit.Bukkit
import org.bukkit.block.Container
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.block.BlockExpEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.inventory.EquipmentSlot
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom

class TelekinesisListener : Listener {

    companion object {
        const val MAX_ENTRIES = 5_000
        const val PURGE_PERCENTAGE = 0.2
    }

    private data class Entry(val owner: UUID, val expiresAt: Long, val level: Int)

    private val owners = ConcurrentHashMap<BlockKey, Entry>(512, 0.75f, 1)


    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val tool = player.inventory.getItem(EquipmentSlot.HAND)

        val level = TelekinesisUtil.getTelekinesisLevel(player, tool) ?: return

        // balancing: don't telekinetically pick up from containers
        // this may be a bit too overpowered otherwise
        if (event.block.state is Container) return

        putOwner(BlockKey.from(event.block.location), player.uniqueId, level)
    }

    @EventHandler
    fun onBlockDropItem(event: BlockDropItemEvent) {
        val entry = getEntryIfFresh(BlockKey.from(event.block.location)) ?: return
        val player = Bukkit.getPlayer(entry.owner) ?: return

        val iterator = event.items.iterator()
        while (iterator.hasNext()) {
            val itemEntity = iterator.next()

            val remainder = TelekinesisUtil.pickupFromItemEntity(player, itemEntity)
            if (remainder == null) {
                iterator.remove()
            } else {
                itemEntity.itemStack = remainder
            }
        }
    }

    @EventHandler
    fun onBlockExperience(event: BlockExpEvent) {
        val entry = getEntryIfFresh(BlockKey.from(event.block.location)) ?: return
        val player = Bukkit.getPlayer(entry.owner) ?: return

        // receiving experience requires telekinesis level 2
        if (entry.level >= 2) {
            val experience = event.expToDrop
            if (experience > 0) {
                player.giveExp(experience, true) // true = applyMending
                event.expToDrop = 0
            }
        }
    }

    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        val killer = event.entity.killer ?: return

        // I don't think you can kill an entity with your off-hand
        val tool = killer.inventory.getItem(EquipmentSlot.HAND)

        val telekinesisLevel = TelekinesisUtil.getTelekinesisLevel(killer, tool) ?: return

        // give items
        if (event.drops.isNotEmpty()) {
            val iterator = event.drops.iterator()
            while (iterator.hasNext()) {
                val drop = iterator.next()

                val remainder = TelekinesisUtil.pickupStack(killer, drop, event.entity.location)
                if (remainder == null) {
                    iterator.remove()
                } else {
                    drop.amount = remainder.amount
                }
            }
        }

        // give xp if level 2
        if (telekinesisLevel >= 2) {
            val experience = event.droppedExp
            if (experience > 0) {
                killer.giveExp(experience, true) // true = applyMending
                event.droppedExp = 0
            }
        }
    }

    private fun purgeSomeEntries() {
        // purge 20% of entries
        val rng = ThreadLocalRandom.current()
        var scanned = 0
        val limit = (owners.size * PURGE_PERCENTAGE).coerceAtLeast(64.0).toInt()
        val now = System.currentTimeMillis()
        val it = owners.entries.iterator()
        while (it.hasNext() && scanned < limit) {
            val (_, v) = it.next()
            scanned++

            if (rng.nextBoolean() || v.expiresAt < now) {
                it.remove()
            }
        }
    }

    private fun putOwner(key: BlockKey, owner: UUID, telekinesisLevel: Int) {
        val time = System.currentTimeMillis()
        owners[key] = Entry(owner, time + 4_000L, telekinesisLevel)

        if (owners.size > MAX_ENTRIES) {
            purgeSomeEntries()
        }
    }

    private fun getEntryIfFresh(key: BlockKey): Entry? {
        val entry = owners[key] ?: return null
        return if (entry.expiresAt >= System.currentTimeMillis()) entry else null
    }

}