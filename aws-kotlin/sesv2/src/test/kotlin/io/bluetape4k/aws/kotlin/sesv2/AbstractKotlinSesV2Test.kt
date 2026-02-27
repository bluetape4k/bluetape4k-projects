package io.bluetape4k.aws.kotlin.sesv2

import aws.sdk.kotlin.services.sesv2.SesV2Client
import io.bluetape4k.aws.kotlin.tests.endpointUrl
import io.bluetape4k.aws.kotlin.tests.getCredentialsProvider
import io.bluetape4k.aws.kotlin.tests.getLocalStackServer
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.testcontainers.containers.localstack.LocalStackContainer

abstract class AbstractKotlinSesV2Test {

    companion object: KLoggingChannel() {
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
        const val receiverEmail = "to-use@example.com"
    }

    protected val sesV2Client: SesV2Client by lazy {
        sesV2ClientOf(
            endpointUrl = snsServer.endpointUrl,
            region = snsServer.region,
            credentialsProvider = snsServer.getCredentialsProvider(),
        )
    }
}
