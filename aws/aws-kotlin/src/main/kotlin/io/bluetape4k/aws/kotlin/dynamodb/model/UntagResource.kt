package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.UntagResourceRequest
import io.bluetape4k.support.requireNotBlank

/**
 * DSL 블록으로 DynamoDB [UntagResourceRequest]를 빌드합니다.
 *
 * ## 동작/계약
 * - [resourceArn]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [tagKeys]는 제거할 태그 키 목록으로, 비어 있으면 어떤 태그도 제거되지 않는다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val req = untagResourceRequestOf(
 *     resourceArn = "arn:aws:dynamodb:us-east-1:123456789012:table/users",
 *     tagKeys = listOf("env", "team")
 * )
 * // req.tagKeys == listOf("env", "team")
 * ```
 *
 * @param resourceArn 태그를 제거할 리소스의 ARN (blank이면 예외)
 * @param tagKeys 제거할 태그 키 목록
 */
inline fun untagResourceRequestOf(
    resourceArn: String,
    tagKeys: List<String>,
    crossinline builder: UntagResourceRequest.Builder.() -> Unit = {},
): UntagResourceRequest {
    resourceArn.requireNotBlank("resourceArn")

    return UntagResourceRequest {
        this.resourceArn = resourceArn
        this.tagKeys = tagKeys
        builder()
    }
}
