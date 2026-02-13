package io.bluetape4k.aws.sns

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointResponse
import software.amazon.awssdk.services.sns.model.CreateTopicResponse
import java.util.concurrent.CompletableFuture

fun SnsAsyncClient.createPlatformEndpointAsync(
    token: String,
    platformApplicationArn: String,
): CompletableFuture<CreatePlatformEndpointResponse> {
    token.requireNotBlank("token")
    platformApplicationArn.requireNotBlank("platformApplicationArn")

    return createPlatformEndpoint {
        it.token(token)
        it.platformApplicationArn(platformApplicationArn)
    }
}

fun SnsAsyncClient.createTopicAsync(
    topicName: String,
    attributes: Map<String, String> = emptyMap(),
): CompletableFuture<CreateTopicResponse> {
    topicName.requireNotBlank("topicName")
    return createTopic {
        it.name(topicName)
            .attributes(attributes)
    }
}

fun SnsAsyncClient.createFIFOTopicAsync(
    topicName: String,
    attributes: Map<String, String> = mapOf("FifoTopic" to "true", "ContentBasedDeduplication" to "true"),
): CompletableFuture<CreateTopicResponse> {
    topicName.requireNotBlank("topicName")

    return createTopic {
        it.name(topicName)
            .attributes(attributes)
    }
}
