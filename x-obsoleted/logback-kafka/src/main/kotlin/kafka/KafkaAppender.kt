package io.bluetape4k.logback.kafka

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import ch.qos.logback.core.spi.AppenderAttachableImpl
import io.bluetape4k.logback.kafka.exporter.ExportExceptionHandler
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.ByteArraySerializer
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Logback 이벤트를 Kafka 토픽으로 전송하는 appender 구현체입니다.
 *
 * ## 동작/계약
 * - Kafka 클라이언트 내부 로그는 [deferQueue]에 지연 적재 후 순차 전송합니다.
 * - 전송 실패는 [exportExceptionHandler]를 통해 경고 및 fallback appender로 전달됩니다.
 * - 종료 시 producer flush/close를 시도합니다.
 */
class KafkaAppender<E : Any> : AbstractKafkaAppender<E>() {
    companion object {
        /**
         * Kafka Client의 로그는 따로 처리하기 위해 (`org.apache.kafka.clients`)
         */
        internal val KAFKA_LOGGER_PREFIX =
            Producer::class.java.packageName.replaceFirst(".producer", "", true)
    }

    private val attacher = AppenderAttachableImpl<E>()

    // Kafka Client가 출력하는 로그는 따로 처리하기 위해 queue에 담아둔다.
    private val deferQueue = ConcurrentLinkedQueue<E>()

    private val producer: Producer<ByteArray, ByteArray>? by lazy { createProducer() }

    private val exportExceptionHandler =
        ExportExceptionHandler<E> { event, exception ->
            if (exception != null) {
                addWarn("Fail to export log to Kafka: ${exception.message}", exception)
                // KafkaProducer 자체의 문제라면 새롭게 생성하게 한다. (Broker 장애로 Producer를 새롭게 생성해야 하는 경우가 있다)
            }
            // 다른 Appender에게도 로그를 전달한다.
            attacher.appendLoopOnAppenders(event)
        }

    override fun doAppend(event: E) {
        // Kafka 관련 로그를 모아 둔 deferQueue의 로그를 먼저 처리한다.
        drainDeferQueue()

        val isKafkaLog = event is ILoggingEvent && event.loggerName.startsWith(KAFKA_LOGGER_PREFIX)
        if (isKafkaLog) {
            deferQueue.offer(event)
        } else {
            super.doAppend(event)
        }
    }

    /**
     * Kafka 관련 로그를 모아 둔 deferQueue의 로그를 발송한다.
     *
     * ## 동작/계약
     * - 큐를 비울 때까지 poll하며 append를 재호출합니다.
     * - drain 후 producer flush를 수행합니다.
     */
    private fun drainDeferQueue() {
        do {
            val event = deferQueue.poll()
            event?.let { append(it) }
        } while (event != null)

        producer?.flush()
    }

    override fun append(event: E) {
        val value = encoder?.encode(event) ?: return
        val key = keyProvider?.get(event)
        val timestamp: Long? = if (appendTimestamp) getTimestamp(event) else null

        val record: ProducerRecord<ByteArray, ByteArray> = ProducerRecord(topic, partition, timestamp, key, value)

        val currentProducer = producer
        if (currentProducer != null) {
            checkNotNull(
                exporter
            ) { "exporter가 초기화되지 않았습니다." }.export(currentProducer, record, event, exportExceptionHandler)
        } else {
            exportExceptionHandler.handle(event, null)
        }
    }

    private fun getTimestamp(event: E): Long =
        when (event) {
            is ILoggingEvent -> event.timeStamp
            else -> System.currentTimeMillis()
        }

    override fun start() {
        if (!checkOptions()) {
            return
        }
        super.start()
    }

    override fun stop() {
        drainDeferQueue()
        deferQueue.clear()
        runCatching {
            producer?.let {
                it.flush()
                it.close()
                addInfo("Kafka Producer closed.")
            }
        }.onFailure {
            addWarn("Fail to close Kafka Producer: ${it.message}", it)
        }
        super.stop()
    }

    override fun addAppender(newAppender: Appender<E>) {
        attacher.addAppender(newAppender)
    }

    override fun iteratorForAppenders(): MutableIterator<Appender<E>> = attacher.iteratorForAppenders()

    override fun getAppender(name: String): Appender<E> = attacher.getAppender(name)

    override fun detachAndStopAllAppenders() {
        attacher.detachAndStopAllAppenders()
    }

    override fun detachAppender(name: String): Boolean = attacher.detachAppender(name)

    override fun detachAppender(appender: Appender<E>): Boolean = attacher.detachAppender(appender)

    override fun isAttached(appender: Appender<E>): Boolean = attacher.isAttached(appender)

    internal fun createProducer(): Producer<ByteArray, ByteArray>? {
        producerConfig[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers ?: DEFAULT_BOOTSTRAP_SERVERS
        producerConfig[ProducerConfig.ACKS_CONFIG] = acks ?: DEFAULT_ACKS

        return try {
            return KafkaProducer(producerConfig, ByteArraySerializer(), ByteArraySerializer()).apply {
                addInfo("Create Kafka Producer for Logging with config: $producerConfig")
            }
        } catch (e: Exception) {
            addError("Fail to create Kafka Producer for Logging with config: $producerConfig", e)
            null
        }
    }
}
