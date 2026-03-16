package io.bluetape4k.aws.sns.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.SubscribeRequest

/**
 * DSL 블록으로 [SubscribeRequest]를 빌드합니다.
 *
 * ## 동작/계약
 * - [builder] 블록에서 `topicArn`, `protocol`, `endpoint` 등을 직접 설정한다.
 *
 * ```kotlin
 * val req = subscribeRequest {
 *     topicArn("arn:aws:sns:ap-northeast-2:123456:my-topic")
 *     protocol("email")
 *     endpoint("user@example.com")
 * }
 * ```
 */
inline fun subscribeRequest(
    builder: SubscribeRequest.Builder.() -> Unit,
): SubscribeRequest =
    SubscribeRequest.builder().apply(builder).build()

/**
 * 토픽 ARN, 프로토콜, 엔드포인트로 [SubscribeRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [topicArn]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [protocol]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [endpoint]가 blank이면 `IllegalArgumentException`을 던진다.
 *
 * ```kotlin
 * val req = subscribeRequestOf(
 *     topicArn = "arn:aws:sns:ap-northeast-2:123456:my-topic",
 *     protocol = "sqs",
 *     endpoint = "arn:aws:sqs:ap-northeast-2:123456:my-queue"
 * )
 * // req.topicArn().isNotBlank() == true
 * ```
 */
inline fun subscribeRequestOf(
    topicArn: String,
    protocol: String,
    endpoint: String,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    builder: SubscribeRequest.Builder.() -> Unit = {},
): SubscribeRequest {
    topicArn.requireNotBlank("topicArn")
    protocol.requireNotBlank("protocol")
    endpoint.requireNotBlank("endpoint")

    return subscribeRequest {
        topicArn(topicArn)
        protocol(protocol)
        endpoint(endpoint)
        overrideConfiguration?.let { overrideConfiguration(it) }

        builder()
    }
}
