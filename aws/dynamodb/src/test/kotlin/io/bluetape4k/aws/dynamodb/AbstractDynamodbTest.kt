package io.bluetape4k.aws.dynamodb

import io.bluetape4k.aws.auth.staticCredentialsProviderOf
import io.bluetape4k.aws.dynamodb.enhanced.dynamoDbEnhancedAsyncClientOf
import io.bluetape4k.aws.http.SdkAsyncHttpClientProvider
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.testcontainers.aws.LocalStackServer
import org.testcontainers.containers.localstack.LocalStackContainer
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import java.net.URI

abstract class AbstractDynamodbTest {

    companion object: KLoggingChannel() {

        @JvmStatic
        protected val DynamoDb: LocalStackServer by lazy {
            LocalStackServer.Launcher.localStack.withServices(LocalStackContainer.Service.DYNAMODB)
        }

        @JvmStatic
        protected val endpoint: URI by lazy {
            DynamoDb.getEndpointOverride(LocalStackContainer.Service.DYNAMODB)
        }

        @JvmStatic
        protected val credentialsProvider: StaticCredentialsProvider by lazy {
            staticCredentialsProviderOf(DynamoDb.accessKey, DynamoDb.secretKey)
        }

        @JvmStatic
        protected val region: Region
            get() = Region.of(DynamoDb.region)

        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(): String {
            return Fakers.randomString(256, 2048)
        }

        @JvmStatic
        val client: DynamoDbClient by lazy {
            dynamoDbClient {
                credentialsProvider(credentialsProvider)
                endpointOverride(endpoint)
                region(region)
            }
        }

        @JvmStatic
        val asyncClient: DynamoDbAsyncClient by lazy {
            dynamoDbAsyncClient {
                credentialsProvider(credentialsProvider)
                endpointOverride(endpoint)
                region(region)
                httpClient(SdkAsyncHttpClientProvider.Netty.nettyNioAsyncHttpClient)
            }
        }

        @JvmStatic
        val enhancedAsyncClient: DynamoDbEnhancedAsyncClient by lazy {
            dynamoDbEnhancedAsyncClientOf(asyncClient)
        }
    }
}
