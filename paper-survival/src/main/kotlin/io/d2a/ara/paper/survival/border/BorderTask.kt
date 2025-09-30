package io.d2a.ara.paper.survival.border

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.World
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import java.io.Closeable
import java.io.File
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.util.logging.Level

class BorderTask(
    private val plugin: Plugin,
    private val clock: Clock = Clock.systemDefaultZone()
) : Runnable, Closeable {

    private val stateFile: File = plugin.dataFolder
        .also { it.mkdirs() } // create the plugin folder if it doesn't exist
        .resolve("state.yaml")
        .apply {
            if (!exists()) {
                runCatching { createNewFile() }
                    .onFailure { plugin.logger.log(Level.SEVERE, "Failed to create state file", it) }
            }
        }

    private val state: YamlConfiguration = YamlConfiguration.loadConfiguration(stateFile)

    private val task: BukkitTask

    companion object {
        private const val LAST_ADVANCE_KEY = "borders.lastAdvancedTime"
        private val RUN_TIME: LocalTime = LocalTime.of(19, 0) // 7 PM server time
        private const val ADVANCE_AMOUNT = 500.0 // Amount to advance the border by each day
        private const val ADVANCE_TIME = 60L // Time in seconds for the border to advance
        private const val TICKS_PER_CHECK = 20L * 30L // Check every 30 seconds
        private const val MAX_BORDER_SIZE = 50_000.0 // Maximum border size
    }

    init {
        plugin.logger.info("World border will advance $ADVANCE_AMOUNT every day at $RUN_TIME server time.")

        // calculate the ticks to next 30s interval
        val seconds = LocalTime.now(clock).second
        val secondsToNextInterval = (30 - (seconds % 30)) % 30
        val initialDelayTicks = secondsToNextInterval * 20L

        task = plugin.server.scheduler.runTaskTimer(
            plugin,
            this,
            initialDelayTicks,
            TICKS_PER_CHECK
        )
    }

    override fun run() {
        // check if it's TIME to run
        if (LocalTime.now(clock).isBefore(RUN_TIME)) {
            return
        }

        val today = LocalDate.now(clock)
        val lastRun = state.getString(LAST_ADVANCE_KEY)?.let(LocalDate::parse)

        // if we ran today already, skip
        if (lastRun?.isEqual(today) == true) {
            return
        }

        advance(today)
    }

    override fun close() {
        task.cancel()
    }

    // public advance for manual triggering (e.g. command)
    fun advance() = advance(LocalDate.now(clock))

    private fun advance(today: LocalDate) {
        plugin.server.worlds.forEach(this::advanceBorder)

        state.set(LAST_ADVANCE_KEY, today.toString())
        runCatching { state.save(stateFile) }
            .onFailure {
                plugin.logger.log(Level.SEVERE, "Failed to save border state to file: ${stateFile.path}", it)
                this@BorderTask.close() // disable the task to prevent repeated advance attempts
            }
    }

    private fun advanceBorder(world: World) {
        val border = world.worldBorder
        val currentBorderSize = border.size
        if (currentBorderSize >= MAX_BORDER_SIZE) {
            // if the border is not enabled in this world, skip it
            plugin.logger.info("World border too large in world ${world.name}, skipping.")
            return
        }

        plugin.logger.info("Advancing border in world ${world.name} by $ADVANCE_AMOUNT over $ADVANCE_TIME seconds.")
        val newSize = currentBorderSize + ADVANCE_AMOUNT

        world.sendMessage(
            Component.text("World border advancing from $currentBorderSize to $newSize")
                .color(NamedTextColor.YELLOW)
        )
        border.setSize(newSize, ADVANCE_TIME)
    }

}