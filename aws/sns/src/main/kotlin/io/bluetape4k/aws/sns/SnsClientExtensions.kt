package io.bluetape4k.aws.sns

import io.bluetape4k.aws.sns.model.createPlatformEndpointRequest
import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointResponse
import software.amazon.awssdk.services.sns.model.CreateTopicResponse


fun SnsClient.createPlatformEndpoint(token: String, platformApplicationArn: String): CreatePlatformEndpointResponse {
    token.requireNotBlank("token")
    platformApplicationArn.requireNotBlank("platformApplicationArn")

    val request = createPlatformEndpointRequest {
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
