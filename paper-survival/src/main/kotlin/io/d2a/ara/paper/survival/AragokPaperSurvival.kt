package io.d2a.ara.paper.survival

import io.d2a.ara.common.Common
import org.bukkit.plugin.java.JavaPlugin

class AragokPaperSurvival : JavaPlugin() {

    override fun onEnable() {
        val c = Common()
        c.hello()
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

}
