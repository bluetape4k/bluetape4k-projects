package io.bluetape4k.aws.cloudwatch

import io.bluetape4k.aws.http.SdkAsyncHttpClientProvider
import io.bluetape4k.aws.http.SdkHttpClientProvider
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.http.SdkHttpClient
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClientBuilder
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient
import software.amazon.awssdk.services.cloudwatch.CloudWatchClientBuilder
import java.net.URI

/**
 * [CloudWatchClient], [CloudWatchAsyncClient] 생성을 위한 Factory 입니다.
 */
object CloudWatchClientFactory {

    /**
     * 동기 [CloudWatchClient] 생성을 지원합니다.
     */
    object Sync {

        /**
         * DSL 빌더로 [CloudWatchClient]를 생성합니다.
         *
         * ```kotlin
         * val client = CloudWatchClientFactory.Sync.create { region(Region.AP_NORTHEAST_2) }
         * // client != null
         * ```
         */
        inline fun create(
            builder: CloudWatchClientBuilder.() -> Unit,
        ): CloudWatchClient =
            cloudWatchClient(builder)

        /**
         * endpoint, region, credentials 기반으로 [CloudWatchClient]를 생성합니다.
         *
         * ```kotlin
         * val client = CloudWatchClientFactory.Sync.create(region = Region.AP_NORTHEAST_2)
         * // client != null
         * ```
         */
        fun create(
            endpointOverride: URI? = null,
            region: Region? = null,
            credentialsProvider: AwsCredentialsProvider? = null,
            httpClient: SdkHttpClient = SdkHttpClientProvider.defaultHttpClient,
            builder: CloudWatchClientBuilder.() -> Unit = {},
        ): CloudWatchClient =
            cloudWatchClientOf(endpointOverride, region, credentialsProvider, httpClient, builder)
    }

    /**
     * 비동기 [CloudWatchAsyncClient] 생성을 지원합니다.
     */
    object Async {

        /**
         * DSL 빌더로 [CloudWatchAsyncClient]를 생성합니다.
         *
         * ```kotlin
         * val client = CloudWatchClientFactory.Async.create { region(Region.AP_NORTHEAST_2) }
         * // client != null
         * ```
         */
        inline fun create(
            builder: CloudWatchAsyncClientBuilder.() -> Unit,
        ): CloudWatchAsyncClient =
            cloudWatchAsyncClient(builder)

        /**
         * endpoint, region, credentials 기반으로 [CloudWatchAsyncClient]를 생성합니다.
         *
         * ```kotlin
         * val client = CloudWatchClientFactory.Async.create(region = Region.AP_NORTHEAST_2)
         * // client != null
         * ```
         */
        inline fun create(
            endpointOverride: URI? = null,
            region: Region? = null,
            credentialsProvider: AwsCredentialsProvider? = null,
            httpClient: SdkAsyncHttpClient = SdkAsyncHttpClientProvider.defaultHttpClient,
            builder: CloudWatchAsyncClientBuilder.() -> Unit = {},
        ): CloudWatchAsyncClient =
            cloudWatchAsyncClientOf(endpointOverride, region, credentialsProvider, httpClient, builder)
    }
}
