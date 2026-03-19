package io.bluetape4k.aws.sns

import io.bluetape4k.aws.sns.model.createPlatformEndpointRequest
import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointResponse
import software.amazon.awssdk.services.sns.model.CreateTopicResponse


/**
 * 디바이스 [token]과 플랫폼 애플리케이션 ARN으로 SNS 플랫폼 엔드포인트를 생성합니다.
 *
 * ## 동작/계약
 * - [token]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [platformApplicationArn]이 blank이면 `IllegalArgumentException`을 던진다.
 *
 * ```kotlin
 * val response = snsClient.createPlatformEndpoint(
 *     token = "device-token-xyz",
 *     platformApplicationArn = "arn:aws:sns:ap-northeast-2:123456:app/GCM/my-app"
 * )
 * // response.endpointArn().isNotBlank() == true
 * ```
 */
fun SnsClient.createPlatformEndpoint(token: String, platformApplicationArn: String): CreatePlatformEndpointResponse {
    token.requireNotBlank("token")
    platformApplicationArn.requireNotBlank("platformApplicationArn")

    val request = createPlatformEndpointRequest {
        token(token)
        platformApplicationArn(platformApplicationArn)
    }
    return createPlatformEndpoint(request)
}

/**
 * [topicName]으로 SNS 토픽을 생성합니다.
 *
 * ## 동작/계약
 * - [topicName]이 blank이면 `IllegalArgumentException`을 던진다.
 *
 * ```kotlin
 * val response = snsClient.createTopic("my-topic")
 * // response.topicArn().isNotBlank() == true
 * ```
 */
fun SnsClient.createTopic(
    topicName: String,
    attributes: Map<String, String> = emptyMap(),
): CreateTopicResponse {
    topicName.requireNotBlank("topicName")
    return createTopic { it.name(topicName).attributes(attributes) }
}

/**
 * [topicName]으로 SNS FIFO 토픽을 생성합니다.
 *
 * ## 동작/계약
 * - [topicName]이 blank이면 `IllegalArgumentException`을 던진다.
 * - 기본 속성에 `FifoTopic=true`, `ContentBasedDeduplication=true`가 포함된다.
 *
 * ```kotlin
 * val response = snsClient.createFIFOTopic("my-topic.fifo")
 * // response.topicArn().contains(".fifo") == true
 * ```
 */
fun SnsClient.createFIFOTopic(
    topicName: String,
    attributes: Map<String, String> = mapOf("FifoTopic" to "true", "ContentBasedDeduplication" to "true"),
): CreateTopicResponse {
    topicName.requireNotBlank("topicName")
    return createTopic { it.name(topicName).attributes(attributes) }
}
