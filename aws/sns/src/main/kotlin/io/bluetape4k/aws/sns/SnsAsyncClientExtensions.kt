package io.bluetape4k.aws.sns

import io.bluetape4k.aws.http.SdkAsyncHttpClientProvider
import io.bluetape4k.aws.sns.model.CreatePlatformEndpointRequest
import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.endpoints.Endpoint
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.SnsAsyncClientBuilder
import software.amazon.awssdk.services.sns.endpoints.internal.RuleResult.endpoint
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointResponse
import software.amazon.awssdk.services.sns.model.CreateTopicResponse
import java.util.concurrent.CompletableFuture

inline fun SnsAsyncClient(
    @BuilderInference builder: SnsAsyncClientBuilder.() -> Unit,
): SnsAsyncClient =
    SnsAsyncClient.builder().apply(builder).build()

inline fun snsAsyncClientOf(
    endpoint: Endpoint,
    region: Region,
    credentialsProvider: AwsCredentialsProvider,
    httpClient: SdkAsyncHttpClient = SdkAsyncHttpClientProvider.Netty.nettyNioAsyncHttpClient,
    @BuilderInference builder: SnsAsyncClientBuilder.() -> Unit = {},
): SnsAsyncClient = SnsAsyncClient {
    endpoint(endpoint)
    region(region)
    credentialsProvider(credentialsProvider)
    httpClient(httpClient)

    builder()
}


fun SnsAsyncClient.createEndpoint(
    token: String,
    platformApplicationArn: String,
): CompletableFuture<CreatePlatformEndpointResponse> {
    token.requireNotBlank("token")
    platformApplicationArn.requireNotBlank("platformApplicationArn")

    val request = CreatePlatformEndpointRequest {
        token(token)
        platformApplicationArn(platformApplicationArn)
    }
    return createPlatformEndpoint(request)
}

fun SnsAsyncClient.createTopic(
    topicName: String,
    attributes: Map<String, String> = emptyMap(),
): CompletableFuture<CreateTopicResponse> {
    topicName.requireNotBlank("topicName")
    return createTopic { it.name(topicName).attributes(attributes) }
}

fun SnsAsyncClient.createFIFOTopic(
    topicName: String,
    attributes: Map<String, String> = mapOf("FifoTopic" to "true", "ContentBasedDeduplication" to "true"),
): CompletableFuture<CreateTopicResponse> {
    topicName.requireNotBlank("topicName")
    return createTopic { it.name(topicName).attributes(attributes) }
}
