package io.bluetape4k.cache

import com.hazelcast.client.HazelcastClient
import com.hazelcast.client.config.ClientConfig
import com.hazelcast.core.HazelcastInstance
import io.bluetape4k.testcontainers.storage.HazelcastServer
import io.bluetape4k.utils.ShutdownQueue

object HazelcastServers {

    val hazelcastServer: HazelcastServer by lazy { HazelcastServer.Launcher.hazelcast }

    val hazelcastClient: HazelcastInstance by lazy {
        val config = ClientConfig().apply {
            networkConfig.addAddress(hazelcastServer.url)
        }
        HazelcastClient.newHazelcastClient(config)
            .also {
                ShutdownQueue.register { it.shutdown() }
            }
    }
}
