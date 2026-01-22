package io.bluetape4k.logback.kafka.keyprovider

import ch.qos.logback.classic.spi.ILoggingEvent
import io.bluetape4k.logback.kafka.utils.hashBytes
import java.util.concurrent.ConcurrentHashMap

class LoggerNameKafkaKeyProvider: AbstractKafkaKeyProvider<ILoggingEvent>() {

    // 계산을 계속 하지 않기 위해
    private val keyCaches = ConcurrentHashMap<String, ByteArray?>()

    override fun get(e: ILoggingEvent): ByteArray? {
        if (e.loggerName == null) return null
        return keyCaches.computeIfAbsent(e.loggerName) { it.hashBytes() }
    }
}
