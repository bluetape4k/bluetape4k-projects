package io.bluetape4k.aws.kotlin.dynamodb

import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import io.bluetape4k.aws.kotlin.tests.endpointUrl
import io.bluetape4k.aws.kotlin.tests.getCredentialsProvider
import io.bluetape4k.aws.kotlin.tests.getLocalStackServer
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.utils.ShutdownQueue
import org.testcontainers.containers.localstack.LocalStackContainer

abstract class AbstractKotlinDynamoDbTest {

    companion object: KLogging() {
        @JvmStatic
        val dynamoDb: LocalStackContainer by lazy {
            getLocalStackServer(LocalStackContainer.Service.DYNAMODB)
        }

        @JvmStatic
        protected val client: DynamoDbClient by lazy {
            DynamoDbClient {
                endpointUrl = dynamoDb.endpointUrl
                region = dynamoDb.region
                credentialsProvider = dynamoDb.getCredentialsProvider()
            }.apply {
                log.debug { "DynamoDbClient created with endpoint: ${dynamoDb.endpoint}" }
                ShutdownQueue.register(this)
            }
        }

        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(min: Int = 16, max: Int = 2048): String =
            Fakers.randomString(min, max)
    }
}
