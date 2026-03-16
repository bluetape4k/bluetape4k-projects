package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.ListGlobalTablesRequest

/**
 * DSL 블록으로 DynamoDB [ListGlobalTablesRequest]를 빌드합니다.
 *
 * ## 동작/계약
 * - 모든 파라미터가 null이면 첫 페이지의 전체 글로벌 테이블 목록을 요청한다.
 * - [exclusiveStartGlobalTableName]을 지정하면 해당 테이블 이름 이후부터 페이지네이션이 시작된다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val req = listGlobalTableRequestOf(limit = 20, regionName = "ap-northeast-2")
 * // req.limit == 20
 * // req.regionName == "ap-northeast-2"
 * ```
 *
 * @param exclusiveStartGlobalTableName 페이지네이션 시작 기준 글로벌 테이블 이름
 * @param regionName 조회할 AWS 리전 이름
 * @param limit 반환할 최대 글로벌 테이블 수
 */
inline fun listGlobalTableRequestOf(
    exclusiveStartGlobalTableName: String? = null,
    regionName: String? = null,
    limit: Int? = null,
    @BuilderInference crossinline builder: ListGlobalTablesRequest.Builder.() -> Unit = {},
): ListGlobalTablesRequest = ListGlobalTablesRequest {
    this.exclusiveStartGlobalTableName = exclusiveStartGlobalTableName
    this.regionName = regionName
    this.limit = limit

    builder()
}
