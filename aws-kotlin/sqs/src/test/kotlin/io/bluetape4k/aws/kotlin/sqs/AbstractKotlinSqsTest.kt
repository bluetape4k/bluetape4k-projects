package io.bluetape4k.aws.kotlin.sqs

import aws.sdk.kotlin.services.sqs.SqsClient
import io.bluetape4k.aws.kotlin.http.defaultCrtHttpEngineOf
import io.bluetape4k.aws.kotlin.tests.endpointUrl
import io.bluetape4k.aws.kotlin.tests.getCredentialsProvider
import io.bluetape4k.aws.kotlin.tests.getLocalStackServer
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.containers.localstack.LocalStackContainer

abstract class AbstractKotlinSqsTest {

    companion object: KLogging() {
        @JvmStatic
        val sqsServer: LocalStackContainer by lazy {
            getLocalStackServer(LocalStackContainer.Service.SQS)
        }

        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(min: Int = 256, max: Int = 2048): String {
            return io.bluetape4k.junit5.faker.Fakers.randomString(min, max)
        }
    }

    protected val sqsClient: SqsClient = SqsClient {
        credentialsProvider = sqsServer.getCredentialsProvider()
        endpointUrl = sqsServer.endpointUrl
        region = sqsServer.region
        httpClient = defaultCrtHttpEngineOf()
    }.apply {
        log.info { "SqsClient created with endpoint: ${sqsServer.endpoint}" }

        // JVM 종료 시 SqsClient를 닫습니다.
        ShutdownQueue.register(this)
    }

}
