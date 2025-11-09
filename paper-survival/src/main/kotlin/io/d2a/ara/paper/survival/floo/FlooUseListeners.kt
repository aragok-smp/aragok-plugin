package io.d2a.ara.paper.survival.floo

import io.d2a.ara.paper.base.custom.CustomItems
import io.d2a.ara.paper.base.custom.CustomItems.Companion.NAMESPACE
import io.d2a.ara.paper.base.extension.*
import net.kyori.adventure.sound.Sound.Emitter
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.*
import org.bukkit.block.Campfire
import org.bukkit.block.Dispenser
import org.bukkit.block.data.Directional
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockDispenseEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByBlockEvent
import org.bukkit.event.entity.ItemSpawnEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*
import java.util.logging.Logger

class FlooUseListeners(
    private val logger: Logger,
) : Listener {

    companion object {
        private val PLAIN = PlainTextComponentSerializer.plainText()

        // Powder destination tags
        val FLOO_POWDER_DESTINATION_NAME = NamespacedKey(NAMESPACE, "floo_destination_name")
        val FLOO_POWDER_DESTINATION_X = NamespacedKey(NAMESPACE, "floo_destination_x")
        val FLOO_POWDER_DESTINATION_Y = NamespacedKey(NAMESPACE, "floo_destination_y")
        val FLOO_POWDER_DESTINATION_Z = NamespacedKey(NAMESPACE, "floo_destination_z")
        val FLOO_POWDER_DESTINATION_WORLD = NamespacedKey(NAMESPACE, "floo_destination_world")

//        const val EXP_PER_REGISTRATION = 50
    }

    fun isPoweredFlooPowder(stack: ItemStack?) = CustomItems.isCustomItem(stack, FlooItem.POWDER_ITEM_MODEL)
    fun isUnusedFlooPowder(stack: ItemStack?) = CustomItems.isCustomItem(stack, FlooItem.UNUSED_POWDER_ITEM_MODEL)

    /**
     * When a soul campfire is placed with a custom name, we store that name inside the block's persistent data container.
     * This allows us to later identify the soul fire as a "Floo Network" connection
     */
    @EventHandler(ignoreCancelled = true)
    fun onSoulFirePlace(event: BlockPlaceEvent) {
        if (event.itemInHand.type != Material.SOUL_CAMPFIRE) return
        if (!event.itemInHand.hasItemMeta()) return
        val meta = event.itemInHand.itemMeta ?: return
        val nameComponent = meta.displayName() ?: return
        val plainName = PLAIN.serialize(nameComponent)
        if (plainName.isEmpty()) return

        val state = event.block.state as? Campfire ?: return
        state.persist {
            setString(FLOO_POWDER_DESTINATION_NAME, plainName)
        } ?: return logger.info("Failed to persist floo soul fire name on place, aborting")

        val location = event.block.location.toCenterLocation()
        event.block.world.spawnParticle(Particle.TOTEM_OF_UNDYING, location, 20, 0.5, 0.5, 0.5, 0.1)

        logger.info("${event.player.name} placed a Floo Network soul fire with name: $plainName at $location")
    }

    /**
     * Checks whether the given location is a valid floo soul fire location
     * (soul campfire with lodestone underneath)
     * @param location the location to check
     * @return true if the location is a valid floo soul fire location, false otherwise
     */
    fun isValidFlooSoulFireLocation(location: Location): Boolean {
        val block = location.block
        if (block.type != Material.SOUL_CAMPFIRE) return false

        val belowBlock = block.getRelative(0, -1, 0)
        return belowBlock.type == Material.LODESTONE
    }

    @EventHandler(ignoreCancelled = true)
    fun onSoulFireRightClick(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val hand = event.hand ?: return
        val clicked = event.clickedBlock ?: return
        if (clicked.type != Material.SOUL_CAMPFIRE) return

        // make sure we right-clicked with some unused floo powder
        val item = event.item ?: return
        if (!isUnusedFlooPowder(item)) return

        val campfire = clicked.state as? Campfire ?: return
        val flooNetworkName = campfire.persistentDataContainer.getString(FLOO_POWDER_DESTINATION_NAME)
            ?: return event.player.failActionBar("This soul fire is not connected to the Floo Network.")

        // TODO: discuss if we want to re-enable experience cost for floo powder
//        val totalExperience = event.player.totalExperience
//        val cost = (EXP_PER_REGISTRATION * item.amount).coerceAtLeast(1)
//        if (totalExperience < cost) {
//            val remaining = cost - totalExperience
//            return event.player.failActionBar(
//                "You need $cost exp to register this connection (need $remaining more).",
//                sound = VILLAGER_NO_SOUND
//            )
//        }
//        event.player.totalExperience = totalExperience - cost

        val powderItem = FlooItem.toPowderItem().apply {
            amount = item.amount
            itemMeta = itemMeta.apply {
                customName(Component.text(flooNetworkName).italic(false))
                persistentDataContainer.apply {
                    setString(FLOO_POWDER_DESTINATION_NAME, flooNetworkName)
                    setInt(FLOO_POWDER_DESTINATION_X, clicked.x)
                    setInt(FLOO_POWDER_DESTINATION_Y, clicked.y)
                    setInt(FLOO_POWDER_DESTINATION_Z, clicked.z)
                    setString(FLOO_POWDER_DESTINATION_WORLD, clicked.world.uid.toString())
                }
            }
        }

        event.player.inventory.setItem(hand, powderItem)
        event.player.playSound(
            Sound.BLOCK_AMETHYST_BLOCK_CHIME.toAdventure()
                .volume(1f)
                .pitch(1.5f)
                .build(), Emitter.self()
        )

        val location = clicked.location.toCenterLocation()
        clicked.world.spawnParticle(Particle.SOUL_FIRE_FLAME, location, 30, 0.5, 0.5, 0.5, 0.1)
    }

    private fun addNauseaEffect(player: Player) {
        player.addPotionEffect(PotionEffect(PotionEffectType.NAUSEA, 60, 1, false, false, false))
        player.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 40, 1, false, false, false))
    }

    private fun tryTeleportWithPowder(
        player: Player,
        powderPdc: PersistentDataContainer,
        onTeleportSuccess: () -> Unit,
        onTeleportFail: () -> Unit,
    ) {
        // get information about the connection
        val destinationName = powderPdc.getString(FLOO_POWDER_DESTINATION_NAME) ?: return
        val destinationX = powderPdc.getInt(FLOO_POWDER_DESTINATION_X) ?: return
        val destinationY = powderPdc.getInt(FLOO_POWDER_DESTINATION_Y) ?: return
        val destinationZ = powderPdc.getInt(FLOO_POWDER_DESTINATION_Z) ?: return
        val destinationWorldUID = powderPdc.getString(FLOO_POWDER_DESTINATION_WORLD) ?: return

        val world = Bukkit.getWorld(UUID.fromString(destinationWorldUID))
            ?: run {
                onTeleportFail.invoke()
                return player.failActionBar("The destination world was not found.")
            }

        // get target soul fire and make sure it is valid and has the same name
        val destinationBlock = world.getBlockAt(destinationX, destinationY, destinationZ)
        val destinationCampfire = destinationBlock.state as? Campfire
            ?: run {
                onTeleportFail.invoke()
                return player.failActionBar("The destination was not found.")
            }
        val actualName = destinationCampfire.persistentDataContainer.getString(FLOO_POWDER_DESTINATION_NAME)
            ?: run {
                onTeleportFail.invoke()
                return player.failActionBar("The destination is not connected to the Floo Network.")
            }
        if (destinationName != actualName) {
            onTeleportFail.invoke()
            return player.failActionBar(
                "The destination has changed and is no longer valid.",
                sound = VILLAGER_NO_SOUND
            )
        }

        // get delta y when teleporting to preserve player height relative to campfire
        val deltaY = player.location.y - player.location.blockY.toDouble()

        val destinationLocation = destinationBlock.location.clone().add(0.5, 0.0, 0.5).apply {
            yaw = player.location.yaw
            pitch = player.location.pitch
            y += deltaY
        }
        val oldPlayerLocation = player.location.clone()

        player.sendActionBar(
            Component.text("Preparing to teleport to ", NamedTextColor.GRAY)
                .append(Component.text(destinationName, NamedTextColor.YELLOW))
                .append(Component.text("...", NamedTextColor.GRAY))
        )

        player.teleportAsync(destinationLocation)
            .thenAccept { result ->
                if (result) {
                    onTeleportSuccess.invoke()

                    player.sendActionBar(
                        Component.text("Teleported to ", NamedTextColor.GRAY)
                            .append(Component.text(destinationName, NamedTextColor.YELLOW))
                    )

                    player.damage(1.0)
                    addNauseaEffect(player)

                    playTeleportEffects(oldPlayerLocation)
                    if (oldPlayerLocation.distanceSquared(destinationLocation) > 10) {
                        playTeleportEffects(destinationLocation)
                    }
                } else {
                    onTeleportFail.invoke()

                    player.sendActionBar(
                        Component.text("Teleportation to ", NamedTextColor.RED)
                            .append(Component.text(destinationName, NamedTextColor.YELLOW))
                            .append(Component.text(" failed.", NamedTextColor.RED))
                    )
                }
            }
    }

    @EventHandler(ignoreCancelled = true)
    fun onItemDrop(event: PlayerDropItemEvent) {
        val item = event.itemDrop.itemStack
        // only allow dropping a single item
        if (item.amount != 1) return
        if (!isPoweredFlooPowder(item)) return

        val pdc = item.itemMeta?.persistentDataContainer ?: return
        val player = event.player

        if (!isValidFlooSoulFireLocation(player.location)) return

        // let the item exist until we know if the teleport was successful
        // so the player can pick it back up if needed
        event.itemDrop.setCanPlayerPickup(false)
        event.itemDrop.setCanMobPickup(false)

        tryTeleportWithPowder(
            player, pdc,
            onTeleportSuccess = {
                // remove the dropped item on success
                event.itemDrop.remove()
            },
            onTeleportFail = {
                // allow the player to pick the item back up
                event.itemDrop.setCanPlayerPickup(true)
                event.itemDrop.setCanMobPickup(true)
            }
        )
    }

    private val items = ArrayList<Location>(16)

    // This is a horrible, horrible hack:
    // since we cannot really update the dispenser inventory when dispensing the floo powder,
    // we have to remove it right after it spawns in the world.
    // This is probably a _very_ unsafe way to do and probably allows for some duplication glitches,
    // but I want to get this shit done.
    @EventHandler
    fun onItemSpawn(event: ItemSpawnEvent) {
        if (!isPoweredFlooPowder(event.entity.itemStack)) return

        val itemLocation = event.entity.location

        val it = items.iterator()
        while (it.hasNext()) {
            val next = it.next()

            val distance = next.distanceSquared(itemLocation)
            if (distance < 4) {
                event.isCancelled = true
                it.remove()
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onDispenserFires(event: BlockDispenseEvent) {
        val source = event.block

        val dispenser = source.state as? Dispenser ?: return
        val directional = dispenser.blockData as? Directional ?: return

        val fireAtBlock = source.getRelative(directional.facing)
        if (fireAtBlock.type != Material.SOUL_CAMPFIRE) return

        // TODO: discuss if we want to re-enable this check
        //  by disabling it, we allow dispensers to be hidden e.g. under a campfire,
        //  which would look way nicer :)
//        if (!isValidFlooSoulFireLocation(fireAtBlock.location)) return

        val item = event.item
        if (item.amount != 1) return
        if (!isPoweredFlooPowder(item)) return

        val pdc = item.itemMeta?.persistentDataContainer ?: return

        val playersHere = fireAtBlock.world.getNearbyPlayers(
            fireAtBlock.location.toCenterLocation(),
            0.8, 1.2, 0.8
        ).filter {
            val feetBelow = it.location.clone().subtract(0.0, 0.01, 0.0).block
            feetBelow == fireAtBlock
        }

        val targetPlayer = playersHere.firstOrNull() ?: return

        // we don't cancel the event here, we use the hacky hack above to "remove" / destroy the item
        items.add(dispenser.location.clone())
        if (items.size >= 16) {
            items.removeAt(0)
        }

        tryTeleportWithPowder(
            targetPlayer, pdc,
            onTeleportSuccess = {
                // nothing additional to do
            },
            onTeleportFail = {
                // play explosion
                source.world.createExplosion(
                    source.location.toCenterLocation(),
                    0.0f,
                    false,
                    false,
                    null
                )
            }
        )
    }

    @EventHandler
    fun onSoulFireDamage(event: EntityDamageByBlockEvent) {
        val player = event.entity as? Player ?: return
        if (event.damager?.type != Material.SOUL_CAMPFIRE) return

        if (isPoweredFlooPowder(player.inventory.itemInMainHand) ||
            isPoweredFlooPowder(player.inventory.itemInOffHand)
        ) {
            event.isCancelled = true
        }
    }


    fun playTeleportEffects(location: Location) {
        location.world.strikeLightningEffect(location)
        location.world.spawnParticle(Particle.SOUL_FIRE_FLAME, location, 50, 0.5, 0.5, 0.5, 0.2)
    }

}