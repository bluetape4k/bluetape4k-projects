package io.bluetape4k.aws.sns

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointResponse
import software.amazon.awssdk.services.sns.model.CreateTopicResponse
import java.util.concurrent.CompletableFuture

/**
 * 디바이스 [token]과 플랫폼 애플리케이션 ARN으로 SNS 플랫폼 엔드포인트를 비동기로 생성합니다.
 *
 * ## 동작/계약
 * - [token]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [platformApplicationArn]이 blank이면 `IllegalArgumentException`을 던진다.
 *
 * ```kotlin
 * val response = snsAsyncClient.createPlatformEndpointAsync(
 *     token = "device-token-xyz",
 *     platformApplicationArn = "arn:aws:sns:ap-northeast-2:123456:app/GCM/my-app"
 * ).join()
 * // response.endpointArn().isNotBlank() == true
 * ```
 */
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

/**
 * [topicName]으로 SNS 토픽을 비동기로 생성합니다.
 *
 * ## 동작/계약
 * - [topicName]이 blank이면 `IllegalArgumentException`을 던진다.
 *
 * ```kotlin
 * val response = snsAsyncClient.createTopicAsync("my-topic").join()
 * // response.topicArn().isNotBlank() == true
 * ```
 */
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

/**
 * [topicName]으로 SNS FIFO 토픽을 비동기로 생성합니다.
 *
 * ## 동작/계약
 * - [topicName]이 blank이면 `IllegalArgumentException`을 던진다.
 * - 기본 속성에 `FifoTopic=true`, `ContentBasedDeduplication=true`가 포함된다.
 *
 * ```kotlin
 * val response = snsAsyncClient.createFIFOTopicAsync("my-topic.fifo").join()
 * // response.topicArn().contains(".fifo") == true
 * ```
 */
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
