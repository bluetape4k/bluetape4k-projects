package io.bluetape4k.testcontainers.mq

import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
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

/**
 * Spring Kafka 통합 테스트에서 사용할 Bean 생성 헬퍼 함수 모음입니다.
 *
 * ## 동작/계약
 * - 모든 함수는 [KafkaServer.Launcher.kafka] 기본 서버를 참조하거나, 명시적인 [KafkaServer] 인스턴스를 받습니다.
 * - Producer/Consumer 팩토리, [KafkaTemplate], Listener 컨테이너 팩토리를 생성합니다.
 * - 각 함수는 매 호출마다 새 인스턴스를 반환합니다.
 *
 * ```kotlin
 * val producerFactory = KafkaServer.Launcher.Spring.getStringProducerFactory()
 * val template = KafkaServer.Launcher.Spring.getStringKafkaTemplate()
 * ```
 */
object KafkaServerSpringSupport {

    /**
     * 문자열 key/value 전용 [ProducerFactory]를 생성합니다.
     */
    fun getStringProducerFactory(
        kafkaServer: KafkaServer = KafkaServer.Launcher.kafka,
    ): ProducerFactory<String, String> {
        return getStringProducerFactory(KafkaServer.Launcher.getProducerProperties(kafkaServer))
    }

    /**
     * 주어진 properties로 문자열 key/value 전용 [ProducerFactory]를 생성합니다.
     */
    fun getStringProducerFactory(
        properties: MutableMap<String, Any?>,
    ): ProducerFactory<String, String> {
        return getProducerFactory(properties.apply {
            this[org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] =
                StringSerializer::class.java
            this[org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] =
                StringSerializer::class.java
        })
    }

    /**
     * 문자열 key/value 전용 [ConsumerFactory]를 생성합니다.
     */
    fun getStringConsumerFactory(
        kafkaServer: KafkaServer = KafkaServer.Launcher.kafka,
    ): ConsumerFactory<String, String> {
        return getStringConsumerFactory(KafkaServer.Launcher.getConsumerProperties(kafkaServer))
    }

    /**
     * 주어진 properties로 문자열 key/value 전용 [ConsumerFactory]를 생성합니다.
     */
    fun getStringConsumerFactory(
        properties: MutableMap<String, Any?>,
    ): ConsumerFactory<String, String> {
        return getConsumerFactory(properties.apply {
            this[org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] =
                StringDeserializer::class.java
            this[org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] =
                StringDeserializer::class.java
        })
    }

    /**
     * 문자열 key/value 전용 [KafkaTemplate]을 생성합니다.
     *
     * @param kafkaServer 연결할 [KafkaServer] 인스턴스
     * @param defaultTopic 기본 토픽 이름
     */
    fun getStringKafkaTemplate(
        kafkaServer: KafkaServer = KafkaServer.Launcher.kafka,
        defaultTopic: String = KafkaServer.Launcher.DEFAULT_TOPIC,
    ): KafkaTemplate<String, String> {
        return getStringKafkaTemplate(
            getStringProducerFactory(kafkaServer),
            true,
            getStringConsumerFactory(kafkaServer)
        ).apply {
            this.defaultTopic = defaultTopic
        }
    }

    /**
     * 주어진 팩토리로 문자열 key/value 전용 [KafkaTemplate]을 생성합니다.
     *
     * @param producerFactory 사용할 [ProducerFactory]
     * @param autoFlush 자동 플러시 여부
     * @param consumerFactory 선택적 [ConsumerFactory]
     */
    fun getStringKafkaTemplate(
        producerFactory: ProducerFactory<String, String>,
        autoFlush: Boolean = true,
        consumerFactory: ConsumerFactory<String, String>? = null,
    ): KafkaTemplate<String, String> {
        return KafkaTemplate(producerFactory, autoFlush).apply {
            consumerFactory?.let { setConsumerFactory(it) }
        }
    }

    /**
     * 직렬화기를 properties에서 자동 감지하는 [ProducerFactory]를 생성합니다.
     *
     * @param properties Producer 설정 맵 (직렬화기 클래스 포함)
     */
    fun <K, V> getProducerFactory(
        properties: MutableMap<String, Any?> = KafkaServer.Launcher.getProducerProperties(KafkaServer.Launcher.kafka),
    ): ProducerFactory<K, V> {
        return DefaultKafkaProducerFactory(properties)
    }

    /**
     * 명시적 직렬화기로 [ProducerFactory]를 생성합니다.
     *
     * @param keySerializer key 직렬화기
     * @param valueSerializer value 직렬화기
     * @param properties Producer 설정 맵
     */
    fun <K, V> getProducerFactory(
        keySerializer: Serializer<K>,
        valueSerializer: Serializer<V>,
        properties: MutableMap<String, Any?> = KafkaServer.Launcher.getProducerProperties(KafkaServer.Launcher.kafka),
    ): ProducerFactory<K, V> {
        return DefaultKafkaProducerFactory(properties, keySerializer, valueSerializer)
    }

    /**
     * 역직렬화기를 properties에서 자동 감지하는 [ConsumerFactory]를 생성합니다.
     *
     * @param properties Consumer 설정 맵 (역직렬화기 클래스 포함)
     */
    fun <K, V> getConsumerFactory(
        properties: MutableMap<String, Any?> = KafkaServer.Launcher.getConsumerProperties(KafkaServer.Launcher.kafka),
    ): ConsumerFactory<K, V> {
        return DefaultKafkaConsumerFactory(properties)
    }

    /**
     * 명시적 역직렬화기로 [ConsumerFactory]를 생성합니다.
     *
     * @param keyDeserializer key 역직렬화기
     * @param valueDeserializer value 역직렬화기
     * @param properties Consumer 설정 맵
     */
    fun <K, V> getConsumerFactory(
        keyDeserializer: Deserializer<K>,
        valueDeserializer: Deserializer<V>,
        properties: MutableMap<String, Any?> = KafkaServer.Launcher.getConsumerProperties(KafkaServer.Launcher.kafka),
    ): ConsumerFactory<K, V> {
        return DefaultKafkaConsumerFactory(properties, keyDeserializer, valueDeserializer)
    }

    /**
     * 명시적 직렬화기/역직렬화기로 [KafkaTemplate]을 생성합니다.
     *
     * @param keySerializer key 직렬화기
     * @param valueSerializer value 직렬화기
     * @param keyDeserializer key 역직렬화기
     * @param valueDeserializer value 역직렬화기
     * @param kafkaServer 연결할 [KafkaServer] 인스턴스
     * @param defaultTopic 기본 토픽 이름
     */
    fun <K, V> getKafkaTemplate(
        keySerializer: Serializer<K>,
        valueSerializer: Serializer<V>,
        keyDeserializer: Deserializer<K>,
        valueDeserializer: Deserializer<V>,
        kafkaServer: KafkaServer = KafkaServer.Launcher.kafka,
        defaultTopic: String = KafkaServer.Launcher.DEFAULT_TOPIC,
    ): KafkaTemplate<K, V> {
        val producerFactory = getProducerFactory(
            keySerializer,
            valueSerializer,
            KafkaServer.Launcher.getProducerProperties(kafkaServer)
        )
        val consumerFactory = getConsumerFactory(
            keyDeserializer,
            valueDeserializer,
            KafkaServer.Launcher.getConsumerProperties(kafkaServer)
        )
        return KafkaTemplate(producerFactory, true).apply {
            setConsumerFactory(consumerFactory)
            this.defaultTopic = defaultTopic
        }
    }

    /**
     * 기본 설정의 [ConcurrentKafkaListenerContainerFactory]를 생성합니다.
     *
     * @param consumerFactory 사용할 [ConsumerFactory]
     */
    fun <K, V> getConcurrentKafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<K, V>,
    ): ConcurrentKafkaListenerContainerFactory<K, V> {
        return ConcurrentKafkaListenerContainerFactory<K, V>().apply {
            this.consumerFactory = consumerFactory
        }
    }

    /**
     * 수동 ACK 모드로 구성된 [KafkaListenerContainerFactory]를 생성합니다.
     *
     * ## 동작/계약
     * - `MANUAL_IMMEDIATE` ACK 모드를 사용합니다.
     * - `idleEventInterval = 100ms`, `pollTimeout = 50ms`로 설정됩니다.
     * - `ackDiscarded = true`로 설정됩니다.
     *
     * @param consumerFactory 사용할 [ConsumerFactory]
     */
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

/**
 * [KafkaServer.Launcher]에서 Spring Kafka 헬퍼에 접근하기 위한 확장 프로퍼티입니다.
 *
 * ```kotlin
 * val template = KafkaServer.Launcher.Spring.getStringKafkaTemplate()
 * ```
 */
val KafkaServer.Launcher.Spring: KafkaServerSpringSupport get() = KafkaServerSpringSupport
