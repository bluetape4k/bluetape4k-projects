package io.bluetape4k.aws.sns.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest

/**
 * DSL 블록으로 [PublishRequest]를 빌드합니다.
 *
 * ## 동작/계약
 * - [builder] 블록에서 `topicArn`, `message` 등을 직접 설정한다.
 *
 * ```kotlin
 * val req = publishRequest {
 *     topicArn("arn:aws:sns:ap-northeast-2:123456:my-topic")
 *     message("Hello SNS")
 * }
 * ```
 */
inline fun publishRequest(
    @BuilderInference builder: PublishRequest.Builder.() -> Unit,
): PublishRequest =
    PublishRequest.builder().apply(builder).build()

/**
 * 토픽 ARN과 메시지로 [PublishRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [topicArn]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [message]가 blank이면 `IllegalArgumentException`을 던진다.
 * - [snsAttributes]가 null이 아니면 메시지 속성으로 설정된다.
 *
 * ```kotlin
 * val req = publishRequestOf(
 *     topicArn = "arn:aws:sns:ap-northeast-2:123456:my-topic",
 *     message = "Hello SNS"
 * )
 * // req.topicArn().isNotBlank() == true
 * ```
 */
inline fun publishRequestOf(
    topicArn: String,
    message: String,
    snsAttributes: Map<String, MessageAttributeValue>? = null,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    @BuilderInference builder: PublishRequest.Builder.() -> Unit = {},
): PublishRequest {
    topicArn.requireNotBlank("topicArn")
    message.requireNotBlank("message")

    return publishRequest {
        topicArn(topicArn)
        message(message)
        snsAttributes?.let { messageAttributes(it) }
        overrideConfiguration?.let { overrideConfiguration(it) }

        builder()
    }
}
