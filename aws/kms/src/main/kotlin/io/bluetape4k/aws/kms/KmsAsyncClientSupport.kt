package io.bluetape4k.aws.kms

import io.bluetape4k.aws.http.SdkAsyncHttpClientProvider
import io.bluetape4k.utils.ShutdownQueue
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.kms.KmsAsyncClient
import software.amazon.awssdk.services.kms.KmsAsyncClientBuilder
import java.net.URI

inline fun kmsAsyncClient(
    @BuilderInference builder: KmsAsyncClientBuilder.() -> Unit,
): KmsAsyncClient =
    KmsAsyncClient.builder().apply(builder).build()
        .apply {
            ShutdownQueue.register(this)
        }

inline fun kmsAsyncClientOf(
    endpointOverride: URI,
    region: Region,
    credentialsProvider: AwsCredentialsProvider,
    httpClient: SdkAsyncHttpClient = SdkAsyncHttpClientProvider.Netty.nettyNioAsyncHttpClient,
    @BuilderInference builder: KmsAsyncClientBuilder.() -> Unit,
): KmsAsyncClient = kmsAsyncClient {
    endpointOverride(endpointOverride)
    region(region)
    credentialsProvider(credentialsProvider)
    httpClient(httpClient)

    builder()
}
