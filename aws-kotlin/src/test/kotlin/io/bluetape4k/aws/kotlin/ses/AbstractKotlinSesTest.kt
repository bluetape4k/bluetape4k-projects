package io.bluetape4k.aws.kotlin.ses

import aws.sdk.kotlin.services.ses.SesClient
import io.bluetape4k.aws.kotlin.tests.endpointUrl
import io.bluetape4k.aws.kotlin.tests.getCredentialsProvider
import io.bluetape4k.aws.kotlin.tests.getLocalStackServer
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.testcontainers.aws.LocalStackServer

abstract class AbstractKotlinSesTest {

    companion object: KLoggingChannel() {
        @JvmStatic
        val snsServer: LocalStackServer by lazy {
            getLocalStackServer("ses")
        }

        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(min: Int = 256, max: Int = 2048): String {
            return Fakers.randomString(min, max)
        }

        const val domain = "example.com"
        const val senderEmail = "from-user@example.com"
        const val receiverEmail = "to-use@example.com"
    }

    protected val sesClient: SesClient by lazy {
        sesClientOf(
            endpointUrl = snsServer.endpointUrl,
            region = snsServer.region,
            credentialsProvider = snsServer.getCredentialsProvider(),
        )
    }
}
