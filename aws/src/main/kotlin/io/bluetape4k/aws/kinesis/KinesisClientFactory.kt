package io.bluetape4k.aws.kinesis

import io.bluetape4k.aws.http.SdkAsyncHttpClientProvider
import io.bluetape4k.aws.http.SdkHttpClientProvider
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.http.SdkHttpClient
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.awssdk.services.kinesis.KinesisAsyncClientBuilder
import software.amazon.awssdk.services.kinesis.KinesisClient
import software.amazon.awssdk.services.kinesis.KinesisClientBuilder
import java.net.URI

/**
 * [KinesisClient], [KinesisAsyncClient] 생성을 위한 Factory 입니다.
 */
object KinesisClientFactory {

    /**
     * 동기 [KinesisClient] 생성을 지원합니다.
     */
    object Sync {

        /**
         * DSL 빌더 블록으로 [KinesisClient]를 생성합니다.
         */
        inline fun create(
            builder: KinesisClientBuilder.() -> Unit,
        ): KinesisClient =
            kinesisClient(builder)

        /**
         * endpoint, region, credentials 기반으로 [KinesisClient]를 생성합니다.
         */
        fun create(
            endpointOverride: URI? = null,
            region: Region? = null,
            credentialsProvider: AwsCredentialsProvider? = null,
            httpClient: SdkHttpClient = SdkHttpClientProvider.defaultHttpClient,
            builder: KinesisClientBuilder.() -> Unit = {},
        ): KinesisClient =
            kinesisClientOf(endpointOverride, region, credentialsProvider, httpClient, builder)
    }

    /**
     * 비동기 [KinesisAsyncClient] 생성을 지원합니다.
     */
    object Async {

        /**
         * DSL 빌더 블록으로 [KinesisAsyncClient]를 생성합니다.
         */
        inline fun create(
            builder: KinesisAsyncClientBuilder.() -> Unit,
        ): KinesisAsyncClient =
            kinesisAsyncClient(builder)

        /**
         * endpoint, region, credentials 기반으로 [KinesisAsyncClient]를 생성합니다.
         */
        inline fun create(
            endpointOverride: URI? = null,
            region: Region? = null,
            credentialsProvider: AwsCredentialsProvider? = null,
            httpClient: SdkAsyncHttpClient = SdkAsyncHttpClientProvider.defaultHttpClient,
            builder: KinesisAsyncClientBuilder.() -> Unit = {},
        ): KinesisAsyncClient =
            kinesisAsyncClientOf(endpointOverride, region, credentialsProvider, httpClient, builder)
    }
}
