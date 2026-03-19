package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.Tag
import aws.sdk.kotlin.services.dynamodb.model.TagResourceRequest
import io.bluetape4k.support.requireNotBlank

/**
 * DSL 블록으로 DynamoDB [TagResourceRequest]를 빌드합니다 (List 오버로드).
 *
 * ## 동작/계약
 * - [resourceArn]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [tags]가 null이면 태그 없이 요청이 생성된다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val req = tagResourceRequestOf(
 *     resourceArn = "arn:aws:dynamodb:us-east-1:123456789012:table/users",
 *     tags = listOf(Tag { key = "env"; value = "prod" })
 * )
 * // req.tags?.size == 1
 * ```
 *
 * @param resourceArn 태그를 추가할 리소스의 ARN (blank이면 예외)
 * @param tags 추가할 [Tag] 목록
 */
@JvmName("tagResourceRequestOfTagList")
inline fun tagResourceRequestOf(
    resourceArn: String,
    tags: List<Tag>? = null,
    crossinline builder: TagResourceRequest.Builder.() -> Unit = {},
): TagResourceRequest {
    resourceArn.requireNotBlank("resourceArn")

    return TagResourceRequest {
        this.resourceArn = resourceArn
        this.tags = tags

        builder()
    }
}

/**
 * DSL 블록으로 DynamoDB [TagResourceRequest]를 빌드합니다 (vararg 오버로드).
 *
 * ## 동작/계약
 * - [resourceArn]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [tags] 가변 인자를 리스트로 변환하여 내부적으로 List 오버로드에 위임한다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val req = tagResourceRequestOf(
 *     resourceArn = "arn:aws:dynamodb:us-east-1:123456789012:table/users",
 *     Tag { key = "env"; value = "prod" },
 *     Tag { key = "team"; value = "backend" }
 * )
 * // req.tags?.size == 2
 * ```
 *
 * @param resourceArn 태그를 추가할 리소스의 ARN (blank이면 예외)
 * @param tags 추가할 [Tag] (가변 인자)
 */
@JvmName("tagResourceRequestOfTagArray")
inline fun tagResourceRequestOf(
    resourceArn: String,
    vararg tags: Tag,
    crossinline builder: TagResourceRequest.Builder.() -> Unit = {},
): TagResourceRequest {
    resourceArn.requireNotBlank("resourceArn")

    return tagResourceRequestOf(
        resourceArn,
        tags.toList(),
        builder
    )
}
