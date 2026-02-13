package io.bluetape4k.aws.kotlin.sns

import aws.sdk.kotlin.services.sns.SnsClient
import io.bluetape4k.aws.kotlin.http.crtHttpEngineOf
import io.bluetape4k.aws.kotlin.tests.endpointUrl
import io.bluetape4k.aws.kotlin.tests.getCredentialsProvider
import io.bluetape4k.aws.kotlin.tests.getLocalStackServer
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.testcontainers.containers.localstack.LocalStackContainer

abstract class AbstractKotlinSnsTest {

    companion object: KLoggingChannel() {
        @JvmStatic
        val snsServer: LocalStackContainer by lazy {
            getLocalStackServer(LocalStackContainer.Service.SNS)
        }

        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(min: Int = 256, max: Int = 2048): String {
            return Fakers.randomString(min, max)
        }
    }

    protected val snsClient: SnsClient by lazy {
        snsClientOf(
            endpointUrl = snsServer.endpointUrl,
            region = snsServer.region,
            credentialsProvider = snsServer.getCredentialsProvider(),
            httpClient = crtHttpEngineOf()
        )
    }
}
