package io.bluetape4k.aws.sns.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.Tag
import software.amazon.awssdk.services.sns.model.TagResourceRequest

/**
 * DSL 블록으로 [TagResourceRequest]를 빌드합니다.
 *
 * ## 동작/계약
 * - [builder] 블록에서 `resourceArn`, `tags` 등을 직접 설정한다.
 *
 * ```kotlin
 * val req = tagResourceRequest {
 *     resourceArn("arn:aws:sns:ap-northeast-2:123456:my-topic")
 *     tags(listOf(Tag.builder().key("env").value("prod").build()))
 * }
 * ```
 */
inline fun tagResourceRequest(
    @BuilderInference builder: TagResourceRequest.Builder.() -> Unit,
): TagResourceRequest =
    TagResourceRequest.builder().apply(builder).build()

/**
 * 리소스 ARN과 태그 목록으로 [TagResourceRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [resourceArn]이 blank이면 `IllegalArgumentException`을 던진다.
 *
 * ```kotlin
 * val tag = Tag.builder().key("env").value("prod").build()
 * val req = tagResourceRequestOf(
 *     resourceArn = "arn:aws:sns:ap-northeast-2:123456:my-topic",
 *     tags = listOf(tag)
 * )
 * // req.resourceArn().isNotBlank() == true
 * ```
 */
inline fun tagResourceRequestOf(
    resourceArn: String,
    tags: Collection<Tag>,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    @BuilderInference builder: TagResourceRequest.Builder.() -> Unit = {},
): TagResourceRequest {
    resourceArn.requireNotBlank("resourceArn")

    return tagResourceRequest {
        resourceArn(resourceArn)
        tags(tags)
        overrideConfiguration?.let { overrideConfiguration(it) }

        builder()
    }
}
