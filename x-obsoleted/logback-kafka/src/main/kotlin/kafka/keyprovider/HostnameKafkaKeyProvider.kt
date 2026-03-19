package io.bluetape4k.logback.kafka.keyprovider

import ch.qos.logback.core.Context
import ch.qos.logback.core.CoreConstants
import io.bluetape4k.logback.kafka.utils.hashBytes

/**
 * Logback 컨텍스트의 hostname 속성으로 Kafka 키를 생성하는 제공자입니다.
 *
 * ## 동작/계약
 * - 컨텍스트의 `HOSTNAME_KEY` 속성을 해시한 4바이트 키를 사용합니다.
 * - hostname이 없으면 최초 1회 오류 로그를 남기고 null 키를 반환할 수 있습니다.
 */
class HostnameKafkaKeyProvider: AbstractKafkaKeyProvider<Any>() {

    private var hostnameHash: ByteArray? = null

    override fun setContext(context: Context) {
        super.setContext(context)

        val hostname = context.getProperty(CoreConstants.HOSTNAME_KEY)
        if (hostname.isNullOrBlank()) {
            if (!errorWasShown) {
                addError("Hostname 을 찾을 수 없습니다. logback context에 [${CoreConstants.HOSTNAME_KEY}] 속성을 설정해주세요")
                errorWasShown = true
            }
        } else {
            hostnameHash = hostname.hashBytes()
            addInfo("Hostname[${hostname}]의 Kafka Key는 ${hostnameHash.contentToString()} 입니다")
        }
    }

    override fun get(e: Any): ByteArray? = hostnameHash

}
