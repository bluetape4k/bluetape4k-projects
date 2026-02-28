package io.bluetape4k.aws.dynamodb

import io.bluetape4k.aws.auth.staticCredentialsProviderOf
import io.bluetape4k.aws.http.SdkAsyncHttpClientProvider
import io.bluetape4k.aws.http.SdkHttpClientProvider
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
        protected val endpointOverride: URI by lazy {
            DynamoDb.getEndpointOverride(LocalStackContainer.Service.DYNAMODB)
        }

        @JvmStatic
        protected val region: Region
            get() = Region.of(DynamoDb.region)

        @JvmStatic
        protected val credentialsProvider: StaticCredentialsProvider by lazy {
            staticCredentialsProviderOf(DynamoDb.accessKey, DynamoDb.secretKey)
        }

        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(): String {
            return Fakers.randomString(256, 2048)
        }
    }

    val client: DynamoDbClient by lazy {
        DynamoDbClientFactory.Sync.create(
            endpointOverride = endpointOverride,
            region = region,
            credentialsProvider = credentialsProvider,
            httpClient = SdkHttpClientProvider.defaultHttpClient
        )
    }

    val asyncClient: DynamoDbAsyncClient by lazy {
        DynamoDbClientFactory.Async.create(
            endpointOverride = endpointOverride,
            region = region,
            credentialsProvider = credentialsProvider,
            httpClient = SdkAsyncHttpClientProvider.defaultHttpClient
        )
    }

    val enhancedAsyncClient: DynamoDbEnhancedAsyncClient by lazy {
        DynamoDbClientFactory.EnhancedAsync.create(asyncClient) {
            this.dynamoDbClient(asyncClient)
            // this.extensions(AtomicCounterExtension.builder().build())
        }
    }

}
