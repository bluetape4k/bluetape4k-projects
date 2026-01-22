package io.bluetape4k.logback.kafka.keyprovider

import ch.qos.logback.core.Context
import ch.qos.logback.core.CoreConstants
import io.bluetape4k.logback.kafka.utils.hashBytes

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
