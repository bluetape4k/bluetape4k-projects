package io.bluetape4k.aws.sts

import io.bluetape4k.aws.http.SdkAsyncHttpClientProvider
import io.bluetape4k.aws.http.SdkHttpClientProvider
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.http.SdkHttpClient
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sts.StsAsyncClient
import software.amazon.awssdk.services.sts.StsAsyncClientBuilder
import software.amazon.awssdk.services.sts.StsClient
import software.amazon.awssdk.services.sts.StsClientBuilder
import java.net.URI

/**
 * [StsClient], [StsAsyncClient] 생성을 위한 Factory 입니다.
 */
object StsClientFactory {

    /**
     * 동기 [StsClient] 생성을 지원합니다.
     */
    object Sync {

        /**
         * DSL 블록으로 [StsClient]를 생성합니다.
         */
        inline fun create(
            @BuilderInference builder: StsClientBuilder.() -> Unit,
        ): StsClient =
            stsClient(builder)

        /**
         * endpoint, region, credentials 기반으로 [StsClient]를 생성합니다.
         */
        fun create(
            endpointOverride: URI? = null,
            region: Region? = null,
            credentialsProvider: AwsCredentialsProvider? = null,
            httpClient: SdkHttpClient = SdkHttpClientProvider.defaultHttpClient,
            @BuilderInference builder: StsClientBuilder.() -> Unit = {},
        ): StsClient =
            stsClientOf(endpointOverride, region, credentialsProvider, httpClient, builder)
    }

    /**
     * 비동기 [StsAsyncClient] 생성을 지원합니다.
     */
    object Async {

        /**
         * DSL 블록으로 [StsAsyncClient]를 생성합니다.
         */
        inline fun create(
            @BuilderInference builder: StsAsyncClientBuilder.() -> Unit,
        ): StsAsyncClient =
            stsAsyncClient(builder)

        /**
         * endpoint, region, credentials 기반으로 [StsAsyncClient]를 생성합니다.
         */
        inline fun create(
            endpointOverride: URI? = null,
            region: Region? = null,
            credentialsProvider: AwsCredentialsProvider? = null,
            httpClient: SdkAsyncHttpClient = SdkAsyncHttpClientProvider.defaultHttpClient,
            @BuilderInference builder: StsAsyncClientBuilder.() -> Unit = {},
        ): StsAsyncClient =
            stsAsyncClientOf(endpointOverride, region, credentialsProvider, httpClient, builder)
    }
}
