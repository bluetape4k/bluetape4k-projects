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

/**
 * DynamoDB 클라이언트 생성 진입점을 제공합니다.
 *
 * 동기/비동기/Enhanced Async 클라이언트 생성을 한 곳에서 호출할 수 있습니다.
 */
object DynamoDbClientFactory {

    /**
     * 동기 [DynamoDbClient] 생성 유틸리티입니다.
     */
    object Sync {

        /**
         * [DynamoDbClientBuilder] DSL로 [DynamoDbClient]를 생성합니다.
         *
         * ```kotlin
         * val client = DynamoDbClientFactory.Sync.create {
         *     region(Region.AP_NORTHEAST_2)
         * }
         *
         * check(client.serviceName() == "DynamoDb")
         * ```
         */
        inline fun create(
            builder: DynamoDbClientBuilder.() -> Unit,
        ): DynamoDbClient =
            dynamoDbClient(builder)

        /**
         * 기본 파라미터와 커스텀 builder를 조합해 [DynamoDbClient]를 생성합니다.
         *
         * ```kotlin
         * val client = DynamoDbClientFactory.Sync.create(
         *     region = Region.AP_NORTHEAST_2,
         * )
         *
         * check(client != null)
         * ```
         */
        inline fun create(
            endpointOverride: URI? = null,
            region: Region? = null,
            credentialsProvider: AwsCredentialsProvider? = null,
            httpClient: SdkHttpClient = SdkHttpClientProvider.defaultHttpClient,
            builder: DynamoDbClientBuilder.() -> Unit = {},
        ): DynamoDbClient =
            dynamoDbClientOf(endpointOverride, region, credentialsProvider, httpClient, builder)
    }

    /**
     * 비동기 [DynamoDbAsyncClient] 생성 유틸리티입니다.
     */
    object Async {

        /**
         * [DynamoDbAsyncClientBuilder] DSL로 [DynamoDbAsyncClient]를 생성합니다.
         *
         * ```kotlin
         * val client = DynamoDbClientFactory.Async.create {
         *     region(Region.AP_NORTHEAST_2)
         * }
         *
         * check(client.serviceName() == "DynamoDb")
         * ```
         */
        inline fun create(
            builder: DynamoDbAsyncClientBuilder.() -> Unit,
        ): DynamoDbAsyncClient =
            dynamoDbAsyncClient(builder)

        /**
         * 기본 파라미터와 커스텀 builder를 조합해 [DynamoDbAsyncClient]를 생성합니다.
         *
         * ```kotlin
         * val client = DynamoDbClientFactory.Async.create(
         *     region = Region.AP_NORTHEAST_2,
         * )
         *
         * check(client != null)
         * ```
         */
        inline fun create(
            endpointOverride: URI? = null,
            region: Region? = null,
            credentialsProvider: AwsCredentialsProvider? = null,
            httpClient: SdkAsyncHttpClient = SdkAsyncHttpClientProvider.defaultHttpClient,
            builder: DynamoDbAsyncClientBuilder.() -> Unit = {},
        ): DynamoDbAsyncClient =
            dynamoDbAsyncClientOf(endpointOverride, region, credentialsProvider, httpClient, builder)
    }

    /**
     * [DynamoDbEnhancedAsyncClient] 생성 유틸리티입니다.
     */
    object EnhancedAsync {

        /**
         * [DynamoDbEnhancedAsyncClient.Builder] DSL로 Enhanced Async 클라이언트를 생성합니다.
         *
         * ```kotlin
         * val enhanced = DynamoDbClientFactory.EnhancedAsync.create {
         *     dynamoDbClient(DynamoDbAsyncClient.create())
         * }
         *
         * check(enhanced != null)
         * ```
         */
        inline fun create(
            builder: DynamoDbEnhancedAsyncClient.Builder.() -> Unit,
        ): DynamoDbEnhancedAsyncClient =
            dynamoDbEnhancedAsyncClient(builder)

        /**
         * 기존 [DynamoDbAsyncClient]를 감싸 [DynamoDbEnhancedAsyncClient]를 생성합니다.
         *
         * ```kotlin
         * val asyncClient = DynamoDbClientFactory.Async.create { region(Region.AP_NORTHEAST_2) }
         * val enhanced = DynamoDbClientFactory.EnhancedAsync.create(asyncClient)
         *
         * check(enhanced != null)
         * ```
         */
        inline fun create(
            asyncClient: DynamoDbAsyncClient,
            builder: DynamoDbEnhancedAsyncClient.Builder.() -> Unit = {},
        ): DynamoDbEnhancedAsyncClient {
            return dynamoDbEnhancedAsyncClientOf(asyncClient, builder)
        }

        /**
         * 연결 파라미터로 [DynamoDbAsyncClient]를 만들고, 이를 기반으로 Enhanced Async 클라이언트를 생성합니다.
         *
         * ```kotlin
         * val enhanced = DynamoDbClientFactory.EnhancedAsync.create(
         *     region = Region.AP_NORTHEAST_2,
         * )
         *
         * check(enhanced != null)
         * ```
         */
        inline fun create(
            endpointOverride: URI? = null,
            region: Region? = null,
            credentialsProvider: AwsCredentialsProvider? = null,
            httpClient: SdkAsyncHttpClient = SdkAsyncHttpClientProvider.defaultHttpClient,
            builder: DynamoDbEnhancedAsyncClient.Builder.() -> Unit = {},
        ): DynamoDbEnhancedAsyncClient {
            val asyncClient = Async.create(endpointOverride, region, credentialsProvider, httpClient)
            return dynamoDbEnhancedAsyncClientOf(asyncClient, builder)
        }
    }
}
