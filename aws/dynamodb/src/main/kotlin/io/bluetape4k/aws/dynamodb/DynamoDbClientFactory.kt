package io.bluetape4k.aws.dynamodb

import io.bluetape4k.aws.dynamodb.enhanced.dynamoDbEnhancedAsyncClient
import io.bluetape4k.aws.dynamodb.enhanced.dynamoDbEnhancedAsyncClientOf
import io.bluetape4k.aws.http.SdkAsyncHttpClientProvider
import io.bluetape4k.aws.http.SdkHttpClientProvider
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient
import software.amazon.awssdk.http.SdkHttpClient
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder
import java.net.URI

object DynamoDbClientFactory {

    object Sync {

        inline fun create(
            @BuilderInference builder: DynamoDbClientBuilder.() -> Unit,
        ): DynamoDbClient =
            dynamoDbClient(builder)

        inline fun create(
            endpointOverride: URI? = null,
            region: Region? = null,
            credentialsProvider: AwsCredentialsProvider? = null,
            httpClient: SdkHttpClient = SdkHttpClientProvider.defaultHttpClient,
            @BuilderInference builder: DynamoDbClientBuilder.() -> Unit = {},
        ): DynamoDbClient =
            dynamoDbClientOf(endpointOverride, region, credentialsProvider, httpClient, builder)
    }

    object Async {

        inline fun create(
            @BuilderInference builder: DynamoDbAsyncClientBuilder.() -> Unit,
        ): DynamoDbAsyncClient =
            dynamoDbAsyncClient(builder)

        inline fun create(
            endpointOverride: URI? = null,
            region: Region? = null,
            credentialsProvider: AwsCredentialsProvider? = null,
            httpClient: SdkAsyncHttpClient = SdkAsyncHttpClientProvider.defaultHttpClient,
            @BuilderInference builder: DynamoDbAsyncClientBuilder.() -> Unit = {},
        ): DynamoDbAsyncClient =
            dynamoDbAsyncClientOf(endpointOverride, region, credentialsProvider, httpClient, builder)
    }

    object EnhancedAsync {

        inline fun create(
            @BuilderInference builder: DynamoDbEnhancedAsyncClient.Builder.() -> Unit,
        ): DynamoDbEnhancedAsyncClient =
            dynamoDbEnhancedAsyncClient(builder)

        inline fun create(
            asyncClient: DynamoDbAsyncClient,
            @BuilderInference builder: DynamoDbEnhancedAsyncClient.Builder.() -> Unit = {},
        ): DynamoDbEnhancedAsyncClient {
            return dynamoDbEnhancedAsyncClientOf(asyncClient, builder)
        }

        inline fun create(
            endpointOverride: URI? = null,
            region: Region? = null,
            credentialsProvider: AwsCredentialsProvider? = null,
            httpClient: SdkAsyncHttpClient = SdkAsyncHttpClientProvider.defaultHttpClient,
            @BuilderInference builder: DynamoDbEnhancedAsyncClient.Builder.() -> Unit = {},
        ): DynamoDbEnhancedAsyncClient {
            val asyncClient = Async.create(endpointOverride, region, credentialsProvider, httpClient)
            return dynamoDbEnhancedAsyncClientOf(asyncClient, builder)
        }
    }
}
