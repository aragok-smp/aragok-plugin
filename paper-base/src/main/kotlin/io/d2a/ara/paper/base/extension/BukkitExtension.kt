package io.d2a.ara.paper.base.extension

import net.kyori.adventure.sound.Sound
import org.bukkit.Server

// Convert Bukkit Sound to Adventure Sound
fun org.bukkit.Sound.toAdventure(): Sound.Builder =
    Sound.sound().type(this)

// Broadcast using MiniMessage formatting
fun Server.broadcastRichMessage(message: String) =
    this.broadcast(message.toComponent(TextFormat.MINI_MESSAGE))
