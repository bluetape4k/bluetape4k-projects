package io.bluetape4k.aws.kotlin.dynamodb

import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import io.bluetape4k.aws.kotlin.http.crtHttpEngineOf
import io.bluetape4k.aws.kotlin.tests.endpointUrl
import io.bluetape4k.aws.kotlin.tests.getCredentialsProvider
import io.bluetape4k.aws.kotlin.tests.getLocalStackServer
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.testcontainers.containers.localstack.LocalStackContainer

abstract class AbstractKotlinDynamoDbTest {

    companion object: KLoggingChannel() {
        @JvmStatic
        val dynamoDb: LocalStackContainer by lazy {
            getLocalStackServer(LocalStackContainer.Service.DYNAMODB)
        }

        @JvmStatic
        protected val client: DynamoDbClient by lazy {
            dynamoDbClientOf(
                endpointUrl = dynamoDb.endpointUrl,
                region = dynamoDb.region,
                credentialsProvider = dynamoDb.getCredentialsProvider(),
                httpClient = crtHttpEngineOf(),
            )
        }

        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(min: Int = 16, max: Int = 2048): String =
            Fakers.randomString(min, max)
    }
}
