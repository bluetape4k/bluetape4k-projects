package io.bluetape4k.aws.kotlin.ses

import aws.sdk.kotlin.services.ses.SesClient
import io.bluetape4k.aws.kotlin.http.defaultCrtHttpEngineOf
import io.bluetape4k.aws.kotlin.tests.endpointUrl
import io.bluetape4k.aws.kotlin.tests.getCredentialsProvider
import io.bluetape4k.aws.kotlin.tests.getLocalStackServer
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.containers.localstack.LocalStackContainer

abstract class AbstractKotlinSesTest {

    companion object: KLogging() {
        @JvmStatic
        val snsServer: LocalStackContainer by lazy {
            getLocalStackServer(LocalStackContainer.Service.SES)
        }

        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(min: Int = 256, max: Int = 2048): String {
            return Fakers.randomString(min, max)
        }

        const val domain = "example.com"
        const val senderEmail = "from-user@example.com"
        const val receiverEamil = "to-use@example.com"
    }

    protected val sesClient: SesClient = SesClient {
        credentialsProvider = snsServer.getCredentialsProvider()
        endpointUrl = snsServer.endpointUrl
        region = snsServer.region
        httpClient = defaultCrtHttpEngineOf()
    }.apply {
        log.info { "SesClient created with endpoint: ${snsServer.endpoint}" }

        // JVM 종료 시 SnsClient를 닫습니다.
        ShutdownQueue.register(this)
    }
}
