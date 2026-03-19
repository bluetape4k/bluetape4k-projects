package io.bluetape4k.logback.kafka.keyprovider

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Context
import ch.qos.logback.core.CoreConstants
import io.bluetape4k.logback.kafka.utils.hashBytes

/**
 * Logback 컨텍스트 이름으로 Kafka 키를 생성하는 제공자입니다.
 *
 * ## 동작/계약
 * - 컨텍스트의 `CONTEXT_NAME_KEY` 속성을 해시한 키를 사용합니다.
 * - context name 미설정 시 최초 1회 오류 로그 후 null 키를 반환합니다.
 */
class ContextNameKafkaKeyProvider: AbstractKafkaKeyProvider<ILoggingEvent>() {

    private var contextNameHash: ByteArray? = null

    override fun setContext(context: Context) {
        super.setContext(context)

        val contextName = context.getProperty(CoreConstants.CONTEXT_NAME_KEY)

        if (contextName.isNullOrBlank()) {
            if (!errorWasShown) {
                addError("Context name을 찾을 수 없습니다. logback context에 [${CoreConstants.CONTEXT_NAME_KEY}] 속성을 설정해주세요")
                errorWasShown = true
            }
        } else {
            contextNameHash = contextName.hashBytes()
            addInfo("Context Name[${contextName}]의 Kafka Key는 ${contextNameHash.contentToString()} 입니다")
        }
    }

    override fun get(e: ILoggingEvent): ByteArray? = contextNameHash
}
