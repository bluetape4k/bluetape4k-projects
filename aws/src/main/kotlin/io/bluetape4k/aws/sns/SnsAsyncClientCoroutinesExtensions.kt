package io.bluetape4k.aws.sns

import kotlinx.coroutines.future.await
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointResponse
import software.amazon.awssdk.services.sns.model.CreateTopicResponse

/**
 * 디바이스 토큰과 플랫폼 ARN으로 SNS 플랫폼 엔드포인트를 코루틴으로 생성합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [createPlatformEndpointAsync]를 호출한 뒤 `await()`로 완료를 기다린다.
 *
 * ```kotlin
 * val response = snsAsyncClient.createPlatformEndpoint(
 *     token = "device-token-xyz",
 *     platformApplicationArn = "arn:aws:sns:ap-northeast-2:123456:app/GCM/my-app"
 * )
 * // response.endpointArn().isNotBlank() == true
 * ```
 */
suspend fun SnsAsyncClient.createPlatformEndpoint(
    token: String,
    platformApplicationArn: String,
): CreatePlatformEndpointResponse =
    createPlatformEndpointAsync(token, platformApplicationArn).await()

/**
 * 토픽 이름으로 SNS 토픽을 코루틴으로 생성합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [createTopicAsync]를 호출한 뒤 `await()`로 완료를 기다린다.
 *
 * ```kotlin
 * val response = snsAsyncClient.createTopic("my-topic")
 * // response.topicArn().isNotBlank() == true
 * ```
 */
suspend fun SnsAsyncClient.createTopic(
    topicName: String,
    attributes: Map<String, String> = emptyMap(),
): CreateTopicResponse =
    createTopicAsync(topicName, attributes).await()

/**
 * FIFO SNS 토픽을 코루틴으로 생성합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [createFIFOTopicAsync]를 호출한 뒤 `await()`로 완료를 기다린다.
 * - 기본 속성에 `FifoTopic=true`, `ContentBasedDeduplication=true`가 포함된다.
 *
 * ```kotlin
 * val response = snsAsyncClient.createFIFOTopic("my-topic.fifo")
 * // response.topicArn().contains(".fifo") == true
 * ```
 */
suspend fun SnsAsyncClient.createFIFOTopic(
    topicName: String,
    attributes: Map<String, String> = mapOf("FifoTopic" to "true", "ContentBasedDeduplication" to "true"),
): CreateTopicResponse =
    createFIFOTopicAsync(topicName, attributes).await()
