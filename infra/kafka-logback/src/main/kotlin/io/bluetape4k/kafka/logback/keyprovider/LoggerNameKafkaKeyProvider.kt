package io.bluetape4k.logback.kafka.keyprovider

import ch.qos.logback.classic.spi.ILoggingEvent
import io.bluetape4k.logback.kafka.utils.hashBytes
import java.util.concurrent.ConcurrentHashMap

/**
 * 로거 이름 기반 Kafka 키 제공자입니다.
 *
 * ## 동작/계약
 * - `loggerName -> hashBytes` 결과를 캐시에 저장해 재계산을 줄입니다.
 * - loggerName이 null이면 null 키를 반환합니다.
 */
class LoggerNameKafkaKeyProvider: AbstractKafkaKeyProvider<ILoggingEvent>() {

    // 계산을 계속 하지 않기 위해
    private val keyCaches = ConcurrentHashMap<String, ByteArray?>()

    override fun get(e: ILoggingEvent): ByteArray? {
        if (e.loggerName == null) return null
        return keyCaches.computeIfAbsent(e.loggerName) { it.hashBytes() }
    }
}
