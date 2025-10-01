package io.d2a.ara.paper.base.extension

import net.kyori.adventure.sound.Sound

fun org.bukkit.Sound.toAdventure(): Sound.Builder = Sound.sound().type(this)
