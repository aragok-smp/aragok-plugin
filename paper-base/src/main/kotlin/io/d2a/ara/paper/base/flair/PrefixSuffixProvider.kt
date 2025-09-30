package io.d2a.ara.paper.base.flair

import org.bukkit.entity.Player

interface PrefixSuffixProvider {

    fun prefix(player: Player): String
    fun suffix(player: Player): String

}