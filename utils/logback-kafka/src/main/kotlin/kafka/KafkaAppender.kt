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

class KafkaAppender<E>: AbstractKafkaAppender<E>() {

    companion object {
        /**
         * Kafka Client의 로그는 따로 처리하기 위해 (`org.apache.kafka.clients`)
         */
        internal val KAFKA_LOGGER_PREFIX = Producer::class.java.packageName.replaceFirst(".producer", "", true)
    }

    private val attacher = AppenderAttachableImpl<E>()

    // Kafka Client가 출력하는 로그는 따로 처리하기 위해 queue에 담아둔다.
    private val deferQueue = ConcurrentLinkedQueue<E>()

    private val producer: Producer<ByteArray?, ByteArray?>? by lazy { createProducer() }

    private val exportExceptionHandler = ExportExceptionHandler<E> { event, exception ->
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
     */
    private fun drainDeferQueue() {
        var event = deferQueue.poll()
        while (event != null) {
            append(event)
            event = deferQueue.poll()
        }
    }

    override fun append(event: E) {
        val value = encoder?.encode(event) ?: return
        val key = keyProvider?.get(event)
        val timestamp: Long? = if (appendTimestamp) getTimestamp(event) else null

        val record = ProducerRecord(topic, partition, timestamp, key, value)

        if (producer != null) {
            exporter!!.export(producer!!, record, event, exportExceptionHandler)
        } else {
            exportExceptionHandler.handle(event, null)
        }
    }

    private fun getTimestamp(event: E): Long = when (event) {
        is ILoggingEvent -> event.timeStamp
        else             -> System.currentTimeMillis()
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

    override fun iteratorForAppenders(): MutableIterator<Appender<E>> {
        return attacher.iteratorForAppenders()
    }

    override fun getAppender(name: String): Appender<E> {
        return attacher.getAppender(name)
    }

    override fun detachAndStopAllAppenders() {
        attacher.detachAndStopAllAppenders()
    }

    override fun detachAppender(name: String): Boolean {
        return attacher.detachAppender(name)
    }

    override fun detachAppender(appender: Appender<E>): Boolean {
        return attacher.detachAppender(appender)
    }

    override fun isAttached(appender: Appender<E>): Boolean {
        return attacher.isAttached(appender)
    }

    internal fun createProducer(): Producer<ByteArray?, ByteArray?>? {
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
