package io.bluetape4k.aws.sns

import io.bluetape4k.aws.http.SdkHttpClientProvider
import io.bluetape4k.aws.sns.model.CreatePlatformEndpointRequest
import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.http.SdkHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.SnsClientBuilder
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointResponse
import software.amazon.awssdk.services.sns.model.CreateTopicResponse
import java.net.URI

inline fun SnsClient(builder: SnsClientBuilder.() -> Unit): SnsClient =
    SnsClient.builder().apply(builder).build()

inline fun snsClientOf(
    endpoint: URI,
    region: Region,
    credentialsProvider: AwsCredentialsProvider,
    httpClient: SdkHttpClient = SdkHttpClientProvider.Apache.apacheHttpClient,
    builder: SnsClientBuilder.() -> Unit = {},
): SnsClient = SnsClient {
    endpointOverride(endpoint)
    region(region)
    credentialsProvider(credentialsProvider)
    httpClient(httpClient)

    builder()
}

fun SnsClient.createEndpoint(token: String, platformApplicationArn: String): CreatePlatformEndpointResponse {
    token.requireNotBlank("token")
    platformApplicationArn.requireNotBlank("platformApplicationArn")

    val request = CreatePlatformEndpointRequest {
        token(token)
        platformApplicationArn(platformApplicationArn)
    }
    return createPlatformEndpoint(request)
}

fun SnsClient.createTopic(
    topicName: String,
    attributes: Map<String, String> = emptyMap(),
): CreateTopicResponse {
    topicName.requireNotBlank("topicName")
    return createTopic { it.name(topicName).attributes(attributes) }
}

fun SnsClient.createFIFOTopic(
    topicName: String,
    attributes: Map<String, String> = mapOf("FifoTopic" to "true", "ContentBasedDeduplication" to "true"),
): CreateTopicResponse {
    topicName.requireNotBlank("topicName")
    return createTopic { it.name(topicName).attributes(attributes) }
}
