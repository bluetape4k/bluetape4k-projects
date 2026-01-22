package io.bluetape4k.logback.kafka

import ch.qos.logback.core.UnsynchronizedAppenderBase
import ch.qos.logback.core.encoder.Encoder
import ch.qos.logback.core.spi.AppenderAttachable
import io.bluetape4k.logback.kafka.exporter.DefaultKafkaExporter
import io.bluetape4k.logback.kafka.exporter.KafkaExporter
import io.bluetape4k.logback.kafka.keyprovider.HostnameKafkaKeyProvider
import io.bluetape4k.logback.kafka.keyprovider.KafkaKeyProvider

abstract class AbstractKafkaAppender<E: Any>: UnsynchronizedAppenderBase<E>(), AppenderAttachable<E> {

    companion object {
        const val DEFAULT_BOOTSTRAP_SERVERS = "localhost:9093"
        const val DEFAULT_ACKS = "1"
    }

    /**
     * 로그를 발송할 Kafka Cluster의 Bootstrap Servers를 지정한다.
     */
    var bootstrapServers: String? = DEFAULT_BOOTSTRAP_SERVERS

    var acks: String? = DEFAULT_ACKS

    /**
     * 로그를 발송할 Topic을 지정한다.
     */
    var topic: String? = null

    /**
     * 특정 파티션에 메시지를 보내고 싶을 때 사용한다.
     */
    var partition: Int? = null
        set(value) {
            field = value?.let { maxOf(0, value) }
        }

    var producerConfig = HashMap<String, Any?>()

    var appendTimestamp: Boolean = true

    var keyProvider: KafkaKeyProvider<in E>? = HostnameKafkaKeyProvider()
    var exporter: KafkaExporter? = DefaultKafkaExporter()

    var encoder: Encoder<E>? = null

    open fun addProducerConfigValue(key: String, value: Any?) {
        producerConfig[key] = value
        addInfo("Add producer config: key=$key, value=$value")
    }

    open fun addProducerConfigValue(keyValue: String) {
        runCatching {
            val (key, value) = keyValue.split("=", limit = 2)
            addProducerConfigValue(key, value).apply {
                addInfo("Add producer config: key=$key, value=$value")
            }
        }.onFailure {
            addError("Fail to add producer config value: $keyValue", it)
        }
    }

    fun getProducerConfig(): Map<String, Any?> {
        return producerConfig
    }

    protected fun checkOptions(): Boolean {
        var validOptions = true
        val checkErrors = mutableListOf<String>()

        if (bootstrapServers.isNullOrBlank()) {
            checkErrors.add("bootstrap.servers is not set")
            validOptions = false
        }

        if (topic.isNullOrBlank()) {
            checkErrors.add("topic is not set")
            validOptions = false
        }

        if (keyProvider == null) {
            checkErrors.add("keyProvider is not set")
            validOptions = false
        }

        if (exporter == null) {
            checkErrors.add("exporter is not set")
            validOptions = false
        }

        if (encoder == null) {
            checkErrors.add("encoder is not set")
            validOptions = false
        }

        if (checkErrors.isNotEmpty()) {
            checkErrors.forEach {
                addError(it)
            }
        }

        return validOptions
    }
}
