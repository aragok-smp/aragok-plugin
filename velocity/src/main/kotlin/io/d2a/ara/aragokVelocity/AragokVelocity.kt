package io.d2a.ara.aragokVelocity;

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import io.d2a.ara.common.Common
import org.slf4j.Logger

@Plugin(
    id = "velocity",
    name = "aragok-velocity",
    version = "1.0-SNAPSHOT",
)
class AragokVelocity @Inject constructor(val logger: Logger) {

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        val h = Common()
        h.hello()
    }

}
