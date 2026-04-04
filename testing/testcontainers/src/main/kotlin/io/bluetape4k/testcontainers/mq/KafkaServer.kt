package io.bluetape4k.testcontainers.mq

import io.bluetape4k.LibraryName
import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.PropertyExportingServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.testcontainers.withCompatKeys
import io.bluetape4k.utils.ShutdownQueue
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.testcontainers.kafka.ConfluentKafkaContainer
import org.testcontainers.utility.DockerImageName
import java.util.*


/**
 * Kafka 테스트 서버 컨테이너를 실행하고 producer/consumer 헬퍼를 제공합니다.
 *
 * ## 동작/계약
 * - `useTransaction=true`이면 트랜잭션 로그 복제 관련 환경 변수를 테스트 친화적으로 설정합니다.
 * - `useDefaultPort=true`이면 `9093` 포트 고정 바인딩을 시도하고, 아니면 동적 포트를 사용합니다.
 * - `start()` 호출 시 `bootstrapServers` 등 연결 정보를 시스템 프로퍼티로 기록합니다.
 *
 * ```kotlin
 * val server = KafkaServer()
 * server.start()
 * // server.bootstrapServers.isNotBlank() == true
 * ```
 *
 * 참고: [Kafka official images](https://hub.docker.com/_/kafka?tab=description&page=1&ordering=last_updated)
 * 비교: [kafka-junit](https://github.com/charithe/kafka-junit)
 */
class KafkaServer private constructor(
    imageName: DockerImageName,
    useTransaction: Boolean,
    useDefaultPort: Boolean,
    reuse: Boolean,
): ConfluentKafkaContainer(imageName), GenericServer, PropertyExportingServer {

    companion object: KLogging() {
        const val IMAGE = "confluentinc/cp-kafka"
        const val NAME = "kafka"
        const val TAG = "7.5.2"
        const val PORT = 9093

        /**
         * 이미지 이름/태그로 [KafkaServer] 인스턴스를 생성합니다.
         *
         * ## 동작/계약
         * - `image`, `tag`가 blank이면 [IllegalArgumentException]이 발생합니다.
         * - 문자열 인자를 [DockerImageName]으로 변환해 새 인스턴스를 반환합니다.
         * - 컨테이너 시작은 호출자가 `start()`로 수행해야 합니다.
         *
         * ```kotlin
         * val server = KafkaServer(image = KafkaServer.IMAGE, tag = KafkaServer.TAG)
         * // server.isRunning == false
         * ```
         */
        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useTransaction: Boolean = false,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): KafkaServer {
            image.requireNotBlank("image")
            tag.requireNotBlank("tag")
            val imageName = DockerImageName.parse(image).withTag(tag)
            return KafkaServer(imageName, useTransaction, useDefaultPort, reuse)
        }

        /**
         * [DockerImageName]으로 [KafkaServer] 인스턴스를 생성합니다.
         *
         * ## 동작/계약
         * - 전달한 `imageName`을 그대로 사용해 새 인스턴스를 반환합니다.
         * - 이 함수는 컨테이너를 시작하지 않습니다.
         */
        @JvmStatic
        operator fun invoke(
            imageName: DockerImageName,
            useTransaction: Boolean = false,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): KafkaServer {
            return KafkaServer(imageName, useTransaction, useDefaultPort, reuse)
        }
    }

    override val port: Int get() = getMappedPort(PORT)

    override val propertyNamespace: String = NAME

    override fun propertyKeys(): Set<String> = setOf(
        "host", "port", "url",
        "bootstrap-servers", "bound-port-numbers",
        "bootstrapServers", "boundPortNumbers",   // compat keys
    )

    override fun properties(): Map<String, String> = buildMap {
        put("host", host)
        put("port", port.toString())
        put("url", url)
        put("bootstrap-servers", bootstrapServers)
        put("bound-port-numbers", boundPortNumbers.joinToString(","))
    }.withCompatKeys(mapOf(
        "bootstrap-servers" to "bootstrapServers",
        "bound-port-numbers" to "boundPortNumbers",
    ))

    init {
        addExposedPorts(PORT)
        withReuse(reuse)

        // HINT: Transaction 관련 테스트를 위해서는 다음과 같은 값을 넣어줘야 합니다.
        // HINT: 테스트 시에는 transaction log replica 를 1로 설정해야 합니다. (기본은 3)
        // see : https://github.com/testcontainers/testcontainers-java/issues/1816
        if (useTransaction) {
            addEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
            addEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1")
        }

        if (useDefaultPort) {
            exposeCustomPorts(PORT)
        }
    }

    override fun start() {
        super.start()
        writeToSystemProperties()
    }

    /**
     * 테스트 전역에서 재사용할 Kafka 서버와 클라이언트/스프링 팩토리 헬퍼를 제공합니다.
     *
     * ## 동작/계약
     * - `kafka`는 첫 접근 시 서버를 시작하고 [ShutdownQueue]에 종료 훅을 등록합니다.
     * - Producer/Consumer 생성 함수는 매 호출마다 새 인스턴스를 반환합니다.
     * - 프로퍼티 팩토리 함수는 입력 맵을 기반으로 새 설정 객체를 생성합니다.
     */
    object Launcher {

        const val DEFAULT_TOPIC = "$LibraryName.test-topic.1"

        val kafka: KafkaServer by lazy {
            KafkaServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }

        /**
         * [KafkaProducer] 를 생성하기 위한 properties 를 반환합니다.
         *
         * ## 동작/계약
         * - 매 호출마다 새로운 [MutableMap] 인스턴스를 생성합니다.
         * - `CLIENT_ID_CONFIG`는 호출 단위로 고유한 값이 생성됩니다.
         *
         * ```kotlin
         * val props = KafkaServer.Launcher.getProducerProperties()
         * // props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] != null
         * ```
         */
        fun getProducerProperties(kafkaServer: KafkaServer = kafka): MutableMap<String, Any?> {
            return mutableMapOf(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaServer.bootstrapServers,
                ProducerConfig.CLIENT_ID_CONFIG to UUID.randomUUID().encodeBase62(),
                ProducerConfig.COMPRESSION_TYPE_CONFIG to "lz4",
                ProducerConfig.LINGER_MS_CONFIG to 0,
                ProducerConfig.BATCH_SIZE_CONFIG to 1
            )
        }

        /**
         * [KafkaConsumer] 를 생성하기 위한 properties 를 반환합니다.
         *
         * ## 동작/계약
         * - 매 호출마다 새로운 [MutableMap] 인스턴스를 생성합니다.
         * - `GROUP_ID_CONFIG`, `CLIENT_ID_CONFIG`는 호출 단위로 고유한 값이 생성됩니다.
         *
         * ```kotlin
         * val props = KafkaServer.Launcher.getConsumerProperties()
         * // props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] == "earliest"
         * ```
         */
        fun getConsumerProperties(kafkaServer: KafkaServer = kafka): MutableMap<String, Any?> {
            return mutableMapOf(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaServer.bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG to UUID.randomUUID().encodeBase62(),
                ConsumerConfig.CLIENT_ID_CONFIG to UUID.randomUUID().encodeBase62(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
                // ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to "true",
                // ConsumerConfig.RETRY_BACKOFF_MS_CONFIG to 100
            )
        }

        /**
         * 문자열 key/value 전용 [KafkaProducer] 인스턴스를 생성합니다.
         *
         * ## 동작/계약
         * - 호출마다 새로운 producer/serializer 인스턴스를 생성합니다.
         * - 직렬화기 인스턴스를 공유하지 않아 `close()` 이후 재사용 문제를 방지합니다.
         *
         * ```kotlin
         * val producer = KafkaServer.Launcher.createStringProducer()
         * // producer != null
         * ```
         */
        fun createStringProducer(kafkaServer: KafkaServer = kafka): KafkaProducer<String, String> {
            val props = getProducerProperties(kafkaServer)
            return KafkaProducer(props, StringSerializer(), StringSerializer())
        }

        /**
         * 문자열 key/value 전용 [KafkaConsumer] 인스턴스를 생성합니다.
         *
         * ## 동작/계약
         * - 호출마다 새로운 consumer/deserializer 인스턴스를 생성합니다.
         * - 역직렬화기 인스턴스를 공유하지 않아 `close()` 이후 재사용 문제를 방지합니다.
         *
         * ```kotlin
         * val consumer = KafkaServer.Launcher.createStringConsumer()
         * // consumer != null
         * ```
         */
        fun createStringConsumer(kafkaServer: KafkaServer = kafka): KafkaConsumer<String, String> {
            val props = getConsumerProperties(kafkaServer)
            return KafkaConsumer(props, StringDeserializer(), StringDeserializer())
        }

        /**
         * 바이너리 key/value 전용 [KafkaProducer] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val producer = KafkaServer.Launcher.createBinaryProducer()
         * // producer != null
         * ```
         */
        fun createBinaryProducer(kafkaServer: KafkaServer = kafka): KafkaProducer<ByteArray?, ByteArray?> {
            val props = getProducerProperties(kafkaServer)
            return KafkaProducer(props, ByteArraySerializer(), ByteArraySerializer())
        }

        /**
         * 바이너리 key/value 전용 [KafkaConsumer] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val consumer = KafkaServer.Launcher.createBinaryConsumer()
         * // consumer != null
         * ```
         */
        fun createBinaryConsumer(kafkaServer: KafkaServer = kafka): KafkaConsumer<ByteArray?, ByteArray?> {
            val props = getConsumerProperties(kafkaServer)
            return KafkaConsumer(props, ByteArrayDeserializer(), ByteArrayDeserializer())
        }

    }
}
