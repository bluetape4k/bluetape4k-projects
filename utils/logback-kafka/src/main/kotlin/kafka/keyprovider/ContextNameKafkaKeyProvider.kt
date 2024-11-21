package io.bluetape4k.logback.kafka.keyprovider

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Context
import ch.qos.logback.core.CoreConstants
import io.bluetape4k.logback.kafka.utils.hashBytes

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
