package io.bluetape4k.aws.dynamodb

import io.bluetape4k.aws.AbstractAwsTest
import io.bluetape4k.aws.http.SdkAsyncHttpClientProvider
import io.bluetape4k.aws.http.SdkHttpClientProvider
import io.bluetape4k.logging.coroutines.KLoggingChannel
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

abstract class AbstractDynamodbTest: AbstractAwsTest() {

    companion object: KLoggingChannel()

    val client: DynamoDbClient by lazy {
        DynamoDbClientFactory.Sync.create(
            endpointOverride = localStackServer.endpoint,
            region = localStackServer.region(),
            credentialsProvider = localStackServer.credentialsProvider,
            httpClient = SdkHttpClientProvider.defaultHttpClient
        )
    }

    val asyncClient: DynamoDbAsyncClient by lazy {
        DynamoDbClientFactory.Async.create(
            endpointOverride = localStackServer.endpoint,
            region = localStackServer.region(),
            credentialsProvider = localStackServer.credentialsProvider,
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
