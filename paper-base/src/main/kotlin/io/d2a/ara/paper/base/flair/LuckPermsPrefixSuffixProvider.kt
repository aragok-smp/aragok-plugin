package io.d2a.ara.paper.base.flair

import net.luckperms.api.LuckPerms
import net.luckperms.api.platform.PlayerAdapter
import org.bukkit.entity.Player

fun LuckPerms.playerAdapter(): PlayerAdapter<Player> =
    this.getPlayerAdapter(Player::class.java)

class LuckPermsPrefixSuffixProvider(
    val luckPerms: LuckPerms
) : PrefixSuffixProvider {

    private val adapter: PlayerAdapter<Player> by lazy { luckPerms.playerAdapter() }

    override fun prefix(player: Player): String =
        adapter.getMetaData(player).prefix?.takeIf { it.isNotBlank() }.orEmpty()

    override fun suffix(player: Player): String =
        adapter.getMetaData(player).suffix?.takeIf { it.isNotBlank() }.orEmpty()

}