package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.ListTablesRequest

/**
 * DSL 블록으로 DynamoDB [ListTablesRequest]를 빌드합니다.
 *
 * ## 동작/계약
 * - 모든 파라미터가 null이면 첫 페이지의 전체 테이블 목록을 요청한다.
 * - [exclusiveStartTableName]을 지정하면 해당 테이블 이름 이후부터 페이지네이션이 시작된다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val req = listTablesRequestOf(limit = 50)
 * // req.limit == 50
 * // req.exclusiveStartTableName == null
 * ```
 *
 * @param exclusiveStartTableName 페이지네이션 시작 기준 테이블 이름
 * @param limit 반환할 최대 테이블 수
 */
inline fun listTablesRequestOf(
    exclusiveStartTableName: String? = null,
    limit: Int? = null,
    @BuilderInference crossinline builder: ListTablesRequest.Builder.() -> Unit = {},
): ListTablesRequest = ListTablesRequest {
    this.exclusiveStartTableName = exclusiveStartTableName
    this.limit = limit

    builder()
}
