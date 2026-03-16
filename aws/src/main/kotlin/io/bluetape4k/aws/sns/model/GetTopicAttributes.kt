package io.bluetape4k.aws.sns.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.GetTopicAttributesRequest

/**
 * DSL 블록으로 [GetTopicAttributesRequest]를 빌드합니다.
 *
 * ## 동작/계약
 * - [builder] 블록에서 `topicArn` 등을 직접 설정한다.
 *
 * ```kotlin
 * val req = getTopicAttributesRequest {
 *     topicArn("arn:aws:sns:ap-northeast-2:123456:my-topic")
 * }
 * ```
 */
inline fun getTopicAttributesRequest(
    builder: GetTopicAttributesRequest.Builder.() -> Unit,
): GetTopicAttributesRequest =
    GetTopicAttributesRequest.builder().apply(builder).build()

/**
 * 토픽 ARN으로 [GetTopicAttributesRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [topicArn]이 non-null이면서 blank이면 `IllegalArgumentException`을 던진다.
 *
 * ```kotlin
 * val req = getTopicAttributesRequestOf("arn:aws:sns:ap-northeast-2:123456:my-topic")
 * // req.topicArn().isNotBlank() == true
 * ```
 */
inline fun getTopicAttributesRequestOf(
    topicArn: String? = null,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    builder: GetTopicAttributesRequest.Builder.() -> Unit = {},
): GetTopicAttributesRequest =
    getTopicAttributesRequest {
        topicArn?.let {
            topicArn.requireNotBlank("topicArn")
            topicArn(it)
        }
        overrideConfiguration?.let { overrideConfiguration(it) }

        builder()
    }
