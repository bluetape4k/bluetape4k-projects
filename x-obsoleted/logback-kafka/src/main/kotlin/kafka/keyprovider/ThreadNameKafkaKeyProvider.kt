package io.bluetape4k.logback.kafka.keyprovider

import ch.qos.logback.classic.spi.ILoggingEvent
import io.bluetape4k.logback.kafka.utils.hashBytes

/**
 * 스레드 이름 기반 Kafka 키 제공자입니다.
 *
 * ## 동작/계약
 * - 이벤트의 [ILoggingEvent.threadName]을 해시해 키를 생성합니다.
 * - 동일 스레드 이름은 동일 키를 생성합니다.
 */
class ThreadNameKafkaKeyProvider: AbstractKafkaKeyProvider<ILoggingEvent>() {

    override fun get(e: ILoggingEvent): ByteArray? {
        return e.threadName.hashBytes()
    }
}
