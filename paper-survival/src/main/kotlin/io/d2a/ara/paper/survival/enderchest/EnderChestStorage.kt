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
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Lazy, per-channel inventory storage
 * - Each channel saves into ./channels/<channel>.yml
 * - Inventories created/loaded on first access
 */
@OptIn(ExperimentalAtomicApi::class)
class EnderChestStorage(
    private val plugin: Plugin,
    private val folder: File = File(plugin.dataFolder, "channels"),
    private val inventorySize: Int = 27,
) {

    companion object {
        // The prefix of the ender chest inventory title
        const val INVENTORY_PREFIX = "Ender Storage: "
    }

    private val logger = plugin.logger

    private val inventories: MutableMap<String, Inventory> = ConcurrentHashMap()
    private val dirty: MutableMap<String, AtomicBoolean> = ConcurrentHashMap()
    private val saveScheduled: MutableMap<String, AtomicBoolean> = ConcurrentHashMap()
    private var autosaveTask: BukkitTask? = null


    /**
     * Get or load the (shared) inventory for the given channel.
     * If the inventory does not exist, a new empty one is created.
     */
    fun getOrLoad(channel: String): Inventory {
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

    /**
     * Mark this channel as dirty, which will cause it to be saved on the next autosave run.
     * It debounces multiple calls within 2 seconds into a single save.
     */
    fun markDirty(channel: String) {
        dirty.getOrPut(channel) { AtomicBoolean(false) }.store(true)

        val scheduledFlag = saveScheduled.getOrPut(channel) { AtomicBoolean(false) }
        if (!scheduledFlag.compareAndSet(false, newValue = true)) return // already scheduled

        // debounce
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            scheduledFlag.store(false)

            val snapshot = snapshotOnMain(channel) ?: return@Runnable
            plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
                writeSnapshot(channel, snapshot)
            })
        }, 40L)
    }

    /**
     * Start autosaving dirty inventories every [intervalSeconds] seconds (default: 300s = 5min).
     */
    fun startAutosave(intervalSeconds: Long = 300L) {
        autosaveTask = plugin.server.scheduler.runTaskTimerAsynchronously(
            plugin,
            Runnable { flushDirtyAsync() },
            20 * intervalSeconds,
            20 * intervalSeconds
        )
    }

    /**
     * Stop autosaving inventories.
     */
    fun stopAutosave() {
        autosaveTask?.cancel()
    }

    /**
     * Immediately save all inventories synchronously.
     * This is useful when the plugin is being disabled, and we want to save all the inventories.
     */
    fun flushAllSync() {
        for (key in inventories.keys) {
            val snapshot = snapshotOnMain(key) ?: continue
            writeSnapshot(key, snapshot)
        }
    }

    private fun flushDirtyAsync() {
        for ((key, flag) in dirty) {
            if (!flag.compareAndSet(expectedValue = true, newValue = false)) continue
            plugin.server.scheduler.runTask(plugin, Runnable {
                // we have to do the snapshot on the main thread
                val snapshot = snapshotOnMain(key) ?: return@Runnable

                // but then we can write it async :)
                plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
                    writeSnapshot(key, snapshot)
                })
            })
        }
    }

    // this must be on the main thread
    private fun snapshotOnMain(channel: String): List<ItemStack?>? {
        val inventory = inventories[channel] ?: return null
        return inventory.contents.toList() // copy
    }

    /**
     * Thread-safe file write of the given snapshot for the given channel.
     */
    private fun writeSnapshot(channel: String, snapshot: List<ItemStack?>) {
        val file = fileFor(channel)

        val tmp = File(file.parentFile, "${file.name}.tmp")
        if (tmp.exists()) {
            logger.warning("Temporary file ${tmp.path} already exists, overwriting")
            tmp.delete()
        }

        val yaml = YamlConfiguration()
        yaml.set("contents", snapshot)
        file.parentFile.mkdir()
        yaml.save(tmp)

        // atomic replace
        Files.move(tmp.toPath(), file.toPath(),
            StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)

        logger.info("Saved ender chest channel '$channel' to ${file.path}")
    }

    // returns the path to the file for the given channel
    private fun fileFor(channel: String): File {
        val safe = URLEncoder.encode(channel, StandardCharsets.UTF_8)
            .replace("+", "%20")
        return File(folder, "$safe.yml")
    }

}