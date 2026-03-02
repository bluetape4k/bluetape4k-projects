package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.ListTagsOfResourceRequest
import io.bluetape4k.support.requireNotBlank

/**
 * DSL 블록으로 DynamoDB [ListTagsOfResourceRequest]를 빌드합니다.
 *
 * ## 동작/계약
 * - [resourceArn]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [nextToken]을 지정하면 해당 토큰 이후부터 페이지네이션이 시작된다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val req = listTagsOfResourceRequestOf("arn:aws:dynamodb:us-east-1:123456789012:table/users")
 * // req.resourceArn == "arn:aws:dynamodb:us-east-1:123456789012:table/users"
 * // req.nextToken == null
 * ```
 *
 * @param resourceArn 태그를 조회할 리소스의 ARN (blank이면 예외)
 * @param nextToken 페이지네이션 토큰
 */
inline fun listTagsOfResourceRequestOf(
    resourceArn: String,
    nextToken: String? = null,
    @BuilderInference crossinline builder: ListTagsOfResourceRequest.Builder.() -> Unit = {},
): ListTagsOfResourceRequest {
    resourceArn.requireNotBlank("resourceArn")

    return ListTagsOfResourceRequest {
        this.resourceArn = resourceArn
        this.nextToken = nextToken

        builder()
    }
}
