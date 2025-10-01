package io.d2a.ara.paper.base.extension

import net.kyori.adventure.sound.Sound
import net.kyori.adventure.sound.Sound.Emitter
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player

private fun Player.sendMessage(message: String, color: NamedTextColor, sound: Sound? = null) {
    this.sendMessage(Component.text(message, color))
    if (sound != null) {
        this.playSound(sound, Emitter.self())
    }
}

fun Player.fail(message: String, sound: Sound? = null) =
    this.sendMessage(message, NamedTextColor.RED, sound)

fun Player.success(message: String, sound: Sound? = null) =
    this.sendMessage(message, NamedTextColor.GREEN, sound)


private fun Player.sendActionBar(message: String, color: NamedTextColor, sound: Sound?) {
    this.sendActionBar(Component.text(message, color))
    if (sound != null) {
        this.playSound(sound, Emitter.self())
    }
}

fun Player.failActionBar(message: String, sound: Sound? = null) =
    this.sendActionBar(message, NamedTextColor.RED, sound)

fun Player.successActionBar(message: String, sound: Sound? = null) =
    this.sendActionBar(message, NamedTextColor.GREEN, sound)
