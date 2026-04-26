package io.bluetape4k.pulsar

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.mq.PulsarServer
import org.apache.pulsar.client.api.PulsarClient
import java.util.UUID

abstract class AbstractPulsarTest {

    companion object: KLogging() {
        @JvmStatic
        val pulsar: PulsarServer by lazy { PulsarServer.Launcher.pulsar }

        @JvmStatic
        fun newClient(): PulsarClient = PulsarServer.Launcher.PulsarClient(pulsar.url)

        @JvmStatic
        fun newTopic(): String = "test-topic-${UUID.randomUUID()}"

        @JvmStatic
        fun newSubscription(): String = "test-sub-${UUID.randomUUID()}"
    }
}
