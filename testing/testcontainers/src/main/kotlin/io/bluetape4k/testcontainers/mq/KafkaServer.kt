package io.bluetape4k.testcontainers.mq

import io.bluetape4k.LibraryName
import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.testcontainers.GenericServer
import io.bluetape4k.testcontainers.exposeCustomPorts
import io.bluetape4k.testcontainers.writeToSystemProperties
import io.bluetape4k.utils.ShutdownQueue
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.KafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import org.springframework.kafka.listener.ContainerProperties
import org.testcontainers.kafka.ConfluentKafkaContainer
import org.testcontainers.utility.DockerImageName
import java.util.*


/**
 * Docker를 이용하여 [kafka](http://kafka.apache.org)를 구동해주는 container 입니다.
 *
 * 참고: [Kafka official images](https://hub.docker.com/_/kafka?tab=description&page=1&ordering=last_updated)
 *
 * 비교: [kafka-junit](https://github.com/charithe/kafka-junit) 를 사용하면 Docker 없이도 가능합니다.
 *
 * ```
 * // start kafka server by docker
 * val kafka = KafkaServer().apply { start() }
 * ```
 *
 * @param imageName      Docker image name ([DockerImageName])
 * @param useDefaultPort Default port 를 사용할지 여부
 * @param reuse          재사용 여부
 */
class KafkaServer private constructor(
    imageName: DockerImageName,
    useTransaction: Boolean,
    useDefaultPort: Boolean,
    reuse: Boolean,
): ConfluentKafkaContainer(imageName), GenericServer {

    companion object {
        private val log = KotlinLogging.logger { }

        const val IMAGE = "confluentinc/cp-kafka"
        const val NAME = "kafka"
        const val TAG = "7.5.2"
        const val PORT = 9093

        @JvmStatic
        operator fun invoke(
            image: String = IMAGE,
            tag: String = TAG,
            useTransaction: Boolean = false,
            useDefaultPort: Boolean = false,
            reuse: Boolean = true,
        ): KafkaServer {
            val imageName = DockerImageName.parse(image).withTag(tag)
            return KafkaServer(imageName, useTransaction, useDefaultPort, reuse)
        }

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

        val extraProps = mapOf(
            "bootstrapServers" to bootstrapServers,
            "boundPortNumbers" to boundPortNumbers.joinToString()
        )
        writeToSystemProperties(NAME, extraProps)
    }

    object Launcher {

        const val DEFAULT_TOPIC = "$LibraryName.test-topic.1"

        val kafka: KafkaServer by lazy {
            KafkaServer().apply {
                start()
                ShutdownQueue.register(this)
            }
        }

        private val stringSerializer by lazy { StringSerializer() }
        private val stringDeserializer by lazy { StringDeserializer() }

        /**
         * [KafkaProducer] 를 생성하기 위한 properties 를 반환합니다.
         */
        fun getProducerProperties(kafkaServer: KafkaServer = kafka): MutableMap<String, Any?> {
            return mutableMapOf(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaServer.bootstrapServers,
                ProducerConfig.CLIENT_ID_CONFIG to UUID.randomUUID().encodeBase62(),
                ProducerConfig.COMPRESSION_TYPE_CONFIG to "lz4",
                ProducerConfig.LINGER_MS_CONFIG to "0",
                ProducerConfig.BATCH_SIZE_CONFIG to "1"
            )
        }

        /**
         * [KafkaConsumer] 를 생성하기 위한 properties 를 반환합니다.
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

        fun createStringProducer(kafkaServer: KafkaServer = kafka): KafkaProducer<String, String> {
            val props = getProducerProperties(kafkaServer)
            return KafkaProducer(props, stringSerializer, stringSerializer)
        }

        fun createStringConsumer(kafkaServer: KafkaServer = kafka): KafkaConsumer<String, String> {
            val props = getConsumerProperties(kafkaServer)
            return KafkaConsumer(props, stringDeserializer, stringDeserializer)
        }


        fun createBinaryProducer(kafkaServer: KafkaServer = kafka): KafkaProducer<ByteArray?, ByteArray?> {
            val props = getProducerProperties(kafkaServer)
            return KafkaProducer(props, ByteArraySerializer(), ByteArraySerializer())
        }

        fun createBinaryConsumer(kafkaServer: KafkaServer = kafka): KafkaConsumer<ByteArray?, ByteArray?> {
            val props = getConsumerProperties(kafkaServer)
            return KafkaConsumer(props, ByteArrayDeserializer(), ByteArrayDeserializer())
        }

        /**
         * Spring Kafka 사용을 위한 Bean 생성을 도와주는 object 입니다.
         */
        object Spring {

            fun getStringProducerFactory(kafkaServer: KafkaServer = kafka): ProducerFactory<String, String> {
                return getStringProducerFactory(getProducerProperties(kafkaServer))
            }

            fun getStringProducerFactory(properties: MutableMap<String, Any?>): ProducerFactory<String, String> {
                return getProducerFactory(properties.apply {
                    this[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
                    this[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
                })
            }

            fun getStringConsumerFactory(kafkaServer: KafkaServer = kafka): ConsumerFactory<String, String> {
                return getStringConsumerFactory(getConsumerProperties(kafkaServer))
            }

            fun getStringConsumerFactory(properties: MutableMap<String, Any?>): ConsumerFactory<String, String> {
                return getConsumerFactory(properties.apply {
                    this[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
                    this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
                })
            }

            fun getStringKafkaTemplate(
                kafkaServer: KafkaServer = kafka,
                defaultTopic: String = DEFAULT_TOPIC,
            ): KafkaTemplate<String, String> {
                return getStringKafkaTemplate(
                    getStringProducerFactory(kafkaServer),
                    true,
                    getStringConsumerFactory(kafkaServer)
                ).apply {
                    this.defaultTopic = defaultTopic
                }
            }

            fun getStringKafkaTemplate(
                producerFactory: ProducerFactory<String, String>,
                autoFlush: Boolean = true,
                consumerFactory: ConsumerFactory<String, String>? = null,
            ): KafkaTemplate<String, String> {
                return KafkaTemplate(producerFactory, autoFlush).apply {
                    consumerFactory?.let { setConsumerFactory(it) }
                }
            }

            fun <K, V> getProducerFactory(
                properties: MutableMap<String, Any?> = getProducerProperties(kafka),
            ): ProducerFactory<K, V> {
                return DefaultKafkaProducerFactory(properties)
            }

            fun <K, V> getProducerFactory(
                keySerializer: Serializer<K>,
                valueSerializer: Serializer<V>,
                properties: MutableMap<String, Any?> = getProducerProperties(kafka),
            ): ProducerFactory<K, V> {
                return DefaultKafkaProducerFactory(properties, keySerializer, valueSerializer)
            }

            fun <K, V> getConsumerFactory(
                properties: MutableMap<String, Any?> = getConsumerProperties(kafka),
            ): ConsumerFactory<K, V> {
                return DefaultKafkaConsumerFactory(properties)
            }

            fun <K, V> getConsumerFactory(
                keyDeserializer: Deserializer<K>,
                valueDeserializer: Deserializer<V>,
                properties: MutableMap<String, Any?> = getConsumerProperties(kafka),
            ): ConsumerFactory<K, V> {
                return DefaultKafkaConsumerFactory(properties, keyDeserializer, valueDeserializer)
            }

            fun <K, V> getKafkaTemplate(
                keySerializer: Serializer<K>,
                valueSerializer: Serializer<V>,
                keyDeserializer: Deserializer<K>,
                valueDeserializer: Deserializer<V>,
                kafkaServer: KafkaServer = kafka,
                defaultTopic: String = DEFAULT_TOPIC,
            ): KafkaTemplate<K, V> {
                val producerFactory = getProducerFactory(
                    keySerializer,
                    valueSerializer,
                    getProducerProperties(kafkaServer)
                )
                val consumerFactory = getConsumerFactory(
                    keyDeserializer,
                    valueDeserializer,
                    getConsumerProperties(kafkaServer)
                )
                return KafkaTemplate(producerFactory, true).apply {
                    setConsumerFactory(consumerFactory)
                    this.defaultTopic = defaultTopic
                }
            }

            fun <K, V> getConcurrentKafkaListenerContainerFactory(
                consumerFactory: ConsumerFactory<K, V>,
            ): ConcurrentKafkaListenerContainerFactory<K, V> {
                return ConcurrentKafkaListenerContainerFactory<K, V>().apply {
                    this.consumerFactory = consumerFactory
                }
            }

            fun <K, V> getKafkaManualAckListenerContainerFactory(
                consumerFactory: ConsumerFactory<K, V>,
            ): KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<K, V>> {
                return ConcurrentKafkaListenerContainerFactory<K, V>().apply {
                    this.consumerFactory = consumerFactory

                    this.containerProperties.apply {
                        this.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
                        this.idleEventInterval = 100L
                        this.pollTimeout = 50L
                    }

                    this.setAckDiscarded(true)
                }
            }
        }
    }
}
