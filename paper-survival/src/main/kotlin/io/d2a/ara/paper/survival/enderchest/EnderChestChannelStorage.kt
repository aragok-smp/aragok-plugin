package io.d2a.ara.paper.survival.enderchest

import io.d2a.ara.paper.base.extension.text
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
class EnderChestChannelStorage(
    private val plugin: Plugin,
    private val folder: File = File(plugin.dataFolder, "channels"),
    private val inventorySize: Int = 27,
) {

    companion object {
        const val INVENTORY_PREFIX = "Ender Storage: "
    }

    private val inventories: MutableMap<String, Inventory> = ConcurrentHashMap()
    private val dirty: MutableMap<String, AtomicBoolean> = ConcurrentHashMap()

    private var bukkitTask: BukkitTask? = null

    // returns the path to the file for the given channel
    private fun fileFor(channel: String): File {
        val safe = URLEncoder.encode(channel, StandardCharsets.UTF_8)
            .replace("+", "%20")
        return File(folder, "$safe.yml")
    }

    // gets or creates an inventory for the given channel
    fun getOrCreateInventory(channel: String): Inventory {
        return inventories.getOrPut(channel) {
            folder.mkdirs()

            val file = fileFor(channel)
            val inventory = plugin.server.createInventory(null, inventorySize, "$INVENTORY_PREFIX$channel".text())

            if (file.exists()) {
                // load from file
                val yaml = YamlConfiguration.loadConfiguration(file)

                @Suppress("UNCHECKED_CAST") // ugh
                val list = yaml.getList("contents") as? List<ItemStack>
                inventory.contents = (list?.toTypedArray() ?: arrayOfNulls(inventorySize))
            }

            dirty.putIfAbsent(channel, AtomicBoolean(false))
            inventory
        }
    }

    fun markDirty(channel: String) {
        dirty.getOrPut(channel) { AtomicBoolean(false) }.store(true)
        requestSaveSoon(channel)
    }

    fun startAutosave(intervalSeconds: Long = 300L) {
        bukkitTask = plugin.server.scheduler.runTaskTimerAsynchronously(
            plugin,
            Runnable { flushDirtyAsync() },
            20 * intervalSeconds,
            20 * intervalSeconds
        )
    }

    fun stopAutosave() {
        bukkitTask?.cancel()
    }

    fun flushAllSync() {
        for (key in inventories.keys) {
            saveOneSync(key)
        }
    }

    private fun requestSaveSoon(channel: String, delayTicks: Long = 40L) {
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            val snapshot = snapshotOne(channel) ?: return@Runnable
            plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
                writeSnapshot(channel, snapshot)
            })
        }, delayTicks)
    }

    private fun flushDirtyAsync() {
        for ((key, flag) in dirty) {
            if (!flag.compareAndSet(expectedValue = true, newValue = false)) continue
            plugin.server.scheduler.runTask(plugin, Runnable {
                val snapshot = snapshotOne(key) ?: return@Runnable
                plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
                    writeSnapshot(key, snapshot)
                })
            })
        }
    }

    private fun saveOneSync(channel: String) {
        val snapshot = snapshotOne(channel) ?: return
        writeSnapshot(channel, snapshot)
    }

    // this must be on the main thread
    private fun snapshotOne(channel: String): List<ItemStack?>? {
        val inventory = inventories[channel] ?: return null
        return inventory.contents.toList()
    }

    private fun writeSnapshot(channel: String, snapshot: List<ItemStack?>) {
        val file = fileFor(channel)
        file.parentFile.mkdir()

        val yaml = YamlConfiguration()
        yaml.set("contents", snapshot)
        yaml.save(file)
    }

}