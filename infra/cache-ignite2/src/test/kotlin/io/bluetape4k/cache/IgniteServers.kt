package io.bluetape4k.cache

import io.bluetape4k.testcontainers.storage.Ignite2Server
import io.bluetape4k.utils.ShutdownQueue
import org.apache.ignite.Ignition
import org.apache.ignite.client.IgniteClient
import org.apache.ignite.configuration.ClientConfiguration

object IgniteServers {

    val ignite2Server by lazy { Ignite2Server.Launcher.ignite2 }

    val igniteClient: IgniteClient by lazy {
        Ignition.startClient(
            ClientConfiguration().apply {
                setAddresses(ignite2Server.url)
                setTimeout(60_000)  // 60초 소켓 타임아웃 (arm64 느린 초기화 대비)
            }
        ).also { ShutdownQueue.register { it.close() } }
    }
}
