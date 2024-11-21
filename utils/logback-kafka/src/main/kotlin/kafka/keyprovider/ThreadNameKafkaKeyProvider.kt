package io.bluetape4k.logback.kafka.keyprovider

import ch.qos.logback.classic.spi.ILoggingEvent
import io.bluetape4k.logback.kafka.utils.hashBytes

class ThreadNameKafkaKeyProvider: AbstractKafkaKeyProvider<ILoggingEvent>() {

    override fun get(e: ILoggingEvent): ByteArray? {
        return e.threadName.hashBytes()
    }
}
