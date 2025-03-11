package io.bluetape4k.quarkus.tests.resources

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.testcontainers.mq.KafkaServer
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager

/**
 * Quarkus 테스트 시 Kafka 서버를 사용할 수 있도록 해주는 [QuarkusTestResourceLifecycleManager] 구현체입니다.
 */
class KafkaTestResource: QuarkusTestResourceLifecycleManager {

    companion object: KLogging() {
        val kafka by lazy { KafkaServer.Launcher.kafka }
        val bootstrapServers: String get() = kafka.bootstrapServers
    }

    override fun start(): MutableMap<String, String> {
        log.info { "Starting Kafka Server for Testing ..." }
        kafka.start()
        log.info { "Success to start Kafka Server for Testing ..." }

        return mutableMapOf(
            "quarkus.kafka.bootstrap-servers" to kafka.bootstrapServers,
            "quarkus.kafka-streams.bootstrap-servers" to kafka.bootstrapServers
        )
    }

    override fun stop() {
        log.info { "Stopping Kafka Server for Testing ..." }
        runCatching { kafka.stop() }
        log.info { "Success to stop Kafka Server" }
    }
}
