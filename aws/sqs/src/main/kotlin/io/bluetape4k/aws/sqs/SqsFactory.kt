package io.bluetape4k.aws.sqs

import io.bluetape4k.aws.auth.LocalAwsCredentialsProvider
import io.bluetape4k.aws.http.SdkAsyncHttpClientProvider
import io.bluetape4k.aws.http.SdkHttpClientProvider
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.http.SdkHttpClient
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.SqsAsyncClientBuilder
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.SqsClientBuilder
import java.net.URI

/**
 * [SqsClient], [SqsAsyncClient] 생성을 위한 Factory 입니다.
 */
object SqsFactory {

    /**
     * 동기 [SqsClient] 생성을 지원합니다.
     */
    object Sync {

        inline fun create(
            @BuilderInference builder: SqsClientBuilder.() -> Unit,
        ): SqsClient = sqsClient(builder)

        inline fun create(
            endpointOverride: URI,
            region: Region = Region.AP_NORTHEAST_2,
            credentialsProvider: AwsCredentialsProvider = LocalAwsCredentialsProvider,
            httpClient: SdkHttpClient = SdkHttpClientProvider.defaultHttpClient,
            @BuilderInference builder: SqsClientBuilder.() -> Unit = {},
        ): SqsClient = sqsClientOf(
            endpointOverride,
            region,
            credentialsProvider,
            httpClient,
            builder
        )
    }

    /**
     * 비동기 [SqsAsyncClient] 생성을 지원합니다.
     */
    object Async {

        inline fun create(
            @BuilderInference builder: SqsAsyncClientBuilder.() -> Unit,
        ): SqsAsyncClient = sqsAsyncClient(builder)

        inline fun create(
            endpointOverride: URI,
            region: Region = Region.AP_NORTHEAST_2,
            credentialsProvider: AwsCredentialsProvider = LocalAwsCredentialsProvider,
            httpClient: SdkAsyncHttpClient = SdkAsyncHttpClientProvider.defaultHttpClient,
            @BuilderInference builder: SqsAsyncClientBuilder.() -> Unit = {},
        ): SqsAsyncClient = sqsAsyncClientOf(
            endpointOverride,
            region,
            credentialsProvider,
            httpClient,
            builder,
        )
    }
}
