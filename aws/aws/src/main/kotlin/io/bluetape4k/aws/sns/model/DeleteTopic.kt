package io.bluetape4k.aws.sns.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.DeleteTopicRequest

/**
 * DSL 블록으로 [DeleteTopicRequest]를 빌드합니다.
 *
 * ## 동작/계약
 * - [builder] 블록에서 `topicArn` 등을 직접 설정한다.
 *
 * ```kotlin
 * val req = deleteTopicRequest {
 *     topicArn("arn:aws:sns:ap-northeast-2:123456:my-topic")
 * }
 * ```
 */
inline fun deleteTopicRequest(
    builder: DeleteTopicRequest.Builder.() -> Unit,
): DeleteTopicRequest =
    DeleteTopicRequest.builder().apply(builder).build()

/**
 * 토픽 ARN으로 [DeleteTopicRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [topicArn]이 blank이면 `IllegalArgumentException`을 던진다.
 *
 * ```kotlin
 * val req = deleteTopicRequestOf("arn:aws:sns:ap-northeast-2:123456:my-topic")
 * // req.topicArn().isNotBlank() == true
 * ```
 */
inline fun deleteTopicRequestOf(
    topicArn: String,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    builder: DeleteTopicRequest.Builder.() -> Unit = {},
): DeleteTopicRequest {
    topicArn.requireNotBlank("topicArn")

    return deleteTopicRequest {
        topicArn(topicArn)
        overrideConfiguration?.let { overrideConfiguration(it) }
        builder()
    }
}
