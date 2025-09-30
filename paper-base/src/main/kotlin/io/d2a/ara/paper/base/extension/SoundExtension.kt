package io.d2a.ara.paper.base.extension

import org.bukkit.Sound

fun Sound.toAdventure(): net.kyori.adventure.sound.Sound.Builder =
    net.kyori.adventure.sound.Sound.sound()
        .type(this)
