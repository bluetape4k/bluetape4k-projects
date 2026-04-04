package io.bluetape4k.aws.dynamodb.query

/**
 * DynamoDB Query DSL 스코프를 제한하는 Marker 어노테이션입니다.
 *
 * ```kotlin
 * @DynamoDslMarker
 * class QueryBuilder {
 *     var tableName: String = ""
 * }
 * // DSL 함수 내에서 중첩 스코프 혼용 방지
 * ```
 */
@DslMarker
annotation class DynamoDslMarker
