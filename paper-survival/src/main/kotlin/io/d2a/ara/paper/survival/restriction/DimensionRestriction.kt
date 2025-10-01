package io.d2a.ara.paper.survival.restriction

import io.d2a.ara.paper.base.configuration.Configuration
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.World.Environment
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerPortalEvent
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

class DimensionRestriction(
    plugin: Plugin
) : Listener {

    val state = Configuration(plugin, "dimension_restriction_state.yaml", autoSave = true)
    val netherEnabled = state.bind("nether.enabled", default = false)
    val endEnabled = state.bind("end.enabled", default = false)

    @EventHandler(
        ignoreCancelled = true,
        priority = EventPriority.LOW
    )
    fun onNetherPortalEnter(event: PlayerPortalEvent) {
        if (event.to.world.environment == Environment.NETHER) {
            if (netherEnabled.get() != true) {
                event.isCancelled = true
                event.player.sendActionBar(Component.text("The Nether is currently disabled.", NamedTextColor.RED))
            }
        } else if (event.to.world.environment == Environment.THE_END) {
            if (endEnabled.get() != true) {
                event.isCancelled = true

                event.player.apply {
                    sendActionBar(Component.text("The End is currently disabled.", NamedTextColor.RED))

                    // push player up
                    velocity = velocity.setY(1.0)

                    // also add fire resistance temporarily in case they land in the lava
                    if (!hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) {
                        addPotionEffect(
                            PotionEffect(
                                PotionEffectType.FIRE_RESISTANCE,
                                20 * 10, // 10 seconds
                                1,
                                false,
                                false
                            )
                        )
                    }
                }
            }
        }
    }

}