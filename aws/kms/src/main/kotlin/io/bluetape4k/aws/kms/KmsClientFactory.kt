package io.bluetape4k.aws.kms

import io.bluetape4k.aws.http.SdkAsyncHttpClientProvider
import io.bluetape4k.aws.http.SdkHttpClientProvider
import io.bluetape4k.logging.KLogging
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.http.SdkHttpClient
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.kms.KmsAsyncClient
import software.amazon.awssdk.services.kms.KmsAsyncClientBuilder
import software.amazon.awssdk.services.kms.KmsClient
import software.amazon.awssdk.services.kms.KmsClientBuilder
import java.net.URI

object KmsClientFactory: KLogging() {

    object Sync {

        inline fun create(
            @BuilderInference builder: KmsClientBuilder.() -> Unit,
        ): KmsClient = kmsClient(builder)

        inline fun create(
            endpointOverride: URI,
            region: Region,
            credentialsProvider: AwsCredentialsProvider? = null,
            httpClient: SdkHttpClient = SdkHttpClientProvider.defaultHttpClient,
            @BuilderInference builder: KmsClientBuilder.() -> Unit = {},
        ): KmsClient =
            kmsClientOf(endpointOverride, region, credentialsProvider, httpClient, builder)
    }

    object Async {
        inline fun create(
            @BuilderInference builder: KmsAsyncClientBuilder.() -> Unit,
        ): KmsAsyncClient = kmsAsyncClient(builder)

        inline fun create(
            endpointOverride: URI,
            region: Region,
            credentialsProvider: AwsCredentialsProvider? = null,
            httpClient: SdkAsyncHttpClient = SdkAsyncHttpClientProvider.defaultHttpClient,
            @BuilderInference builder: KmsAsyncClientBuilder.() -> Unit = {},
        ): KmsAsyncClient =
            kmsAsyncClientOf(endpointOverride, region, credentialsProvider, httpClient, builder)
    }
}
