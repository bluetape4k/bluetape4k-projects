package io.bluetape4k.aws.kotlin.sqs

import aws.sdk.kotlin.services.sqs.SqsClient
import io.bluetape4k.aws.kotlin.tests.endpointUrl
import io.bluetape4k.aws.kotlin.tests.getCredentialsProvider
import io.bluetape4k.aws.kotlin.tests.getLocalStackServer
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.testcontainers.aws.LocalStackServer

abstract class AbstractKotlinSqsTest {

    companion object: KLoggingChannel() {
        @JvmStatic
        val sqsServer: LocalStackServer by lazy {
            getLocalStackServer("sqs")
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
        )
    }
}
