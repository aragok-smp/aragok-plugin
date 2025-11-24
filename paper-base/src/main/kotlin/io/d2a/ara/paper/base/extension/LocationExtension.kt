package io.d2a.ara.paper.base.extension

import org.bukkit.Location

fun Location.toBlockString(): String = "${this.blockX}, ${this.blockY}, ${this.blockZ}"