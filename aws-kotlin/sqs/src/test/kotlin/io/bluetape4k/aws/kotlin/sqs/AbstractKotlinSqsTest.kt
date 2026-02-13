package io.bluetape4k.aws.kotlin.sqs

import aws.sdk.kotlin.services.sqs.SqsClient
import io.bluetape4k.aws.kotlin.http.crtHttpEngineOf
import io.bluetape4k.aws.kotlin.tests.endpointUrl
import io.bluetape4k.aws.kotlin.tests.getCredentialsProvider
import io.bluetape4k.aws.kotlin.tests.getLocalStackServer
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.testcontainers.containers.localstack.LocalStackContainer

abstract class AbstractKotlinSqsTest {

    companion object: KLoggingChannel() {
        @JvmStatic
        val sqsServer: LocalStackContainer by lazy {
            getLocalStackServer(LocalStackContainer.Service.SQS)
        }

        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(min: Int = 256, max: Int = 2048): String {
            return Fakers.randomString(min, max)
        }
    }

    protected val sqsClient: SqsClient by lazy {
        sqsClientOf(
            endpointUrl = sqsServer.endpointUrl,
            region = sqsServer.region,
            credentialsProvider = sqsServer.getCredentialsProvider(),
            httpClient = crtHttpEngineOf()
        )
    }
}
