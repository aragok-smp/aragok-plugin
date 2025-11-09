package io.d2a.ara.paper.survival.telekinesis

import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.World
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

object TelekinesisEffects {

    private const val MIN_TICKS_BETWEEN = 2L
    private val lastFxTick: MutableMap<UUID, Long> = ConcurrentHashMap()

    private val dust = Particle.DustOptions(Color.WHITE, 0.3f)

    fun attractRibbonToPlayer(source: Location, player: Player, steps: Int = 10) {
        val now = player.world.fullTime
        val last = lastFxTick.putIfAbsent(player.uniqueId, 0L) ?: 0L
        if (now - last < MIN_TICKS_BETWEEN) return
        lastFxTick[player.uniqueId] = now

        val world = source.world ?: return
        val target = player.location.clone().add(0.0, 1.0, 0.0)

        spawnRibbon(world, source, target, steps)
    }

    private fun spawnRibbon(world: World, from: Location, to: Location, steps: Int) {
        val s = max(4, steps)
        val dx = (to.x - from.x) / s
        val dy = (to.y - from.y) / s
        val dz = (to.z - from.z) / s

        var x = from.x
        var y = from.y
        var z = from.z

        for (i in 0..s) {
            world.spawnParticle(
                Particle.DUST,
                x, y, z,
                1, 0.0, 0.0, 0.0,
                0.0, dust, true
            )

            x += dx; y += dy; z += dz
        }
    }
}
