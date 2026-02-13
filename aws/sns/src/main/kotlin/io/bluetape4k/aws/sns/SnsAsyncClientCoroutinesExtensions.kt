package io.bluetape4k.aws.sns

import kotlinx.coroutines.future.await
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointResponse
import software.amazon.awssdk.services.sns.model.CreateTopicResponse

/**
 * [CreatePlatformEndpointResponse]를 코루틴 방식으로 반환합니다.
 */
suspend fun SnsAsyncClient.createPlatformEndpoint(
    token: String,
    platformApplicationArn: String,
): CreatePlatformEndpointResponse =
    createPlatformEndpointAsync(token, platformApplicationArn).await()

/**
 * [CreateTopicResponse]를 코루틴 방식으로 반환합니다.
 */
suspend fun SnsAsyncClient.createTopic(
    topicName: String,
    attributes: Map<String, String> = emptyMap(),
): CreateTopicResponse =
    createTopicAsync(topicName, attributes).await()

/**
 * FIFO [CreateTopicResponse]를 코루틴 방식으로 반환합니다.
 */
suspend fun SnsAsyncClient.createFIFOTopic(
    topicName: String,
    attributes: Map<String, String> = mapOf("FifoTopic" to "true", "ContentBasedDeduplication" to "true"),
): CreateTopicResponse =
    createFIFOTopicAsync(topicName, attributes).await()
