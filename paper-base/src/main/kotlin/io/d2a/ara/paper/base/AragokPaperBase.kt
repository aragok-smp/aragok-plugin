package io.d2a.ara.paper.base

import io.d2a.ara.common.Common
import org.bukkit.plugin.java.JavaPlugin

class AragokPaperBase : JavaPlugin() {

    override fun onEnable() {
        val c = Common()
        c.hello()
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}
