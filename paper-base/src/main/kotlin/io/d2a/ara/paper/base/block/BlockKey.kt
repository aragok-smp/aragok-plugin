package io.d2a.ara.paper.base.block

import org.bukkit.Location
import java.util.*

data class BlockKey(val worldId: UUID, val x: Int, val y: Int, val z: Int) {
    companion object {
        fun from(loc: Location): BlockKey {
            return BlockKey(loc.world.uid, loc.blockX, loc.blockY, loc.blockZ)
        }
    }
}
