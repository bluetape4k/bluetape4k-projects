package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.TableClass
import aws.sdk.kotlin.services.dynamodb.model.UpdateTableRequest
import io.bluetape4k.support.requireNotBlank

/**
 * DSL 블록으로 DynamoDB [UpdateTableRequest]를 빌드합니다.
 *
 * ## 동작/계약
 * - [tableName]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [tableClass]가 null이면 테이블 클래스 변경 없이 요청이 생성된다.
 * - [builder] 블록으로 프로비저닝 처리량, GSI 업데이트 등 추가 필드를 설정할 수 있다.
 *
 * ```kotlin
 * val req = updateTableRequestOf("users", TableClass.Standard) {
 *     provisionedThroughput = provisionedThroughputOf(readCapacityUnits = 10L, writeCapacityUnits = 5L)
 * }
 * // req.tableName == "users"
 * // req.tableClass == TableClass.Standard
 * ```
 *
 * @param tableName 업데이트할 DynamoDB 테이블 이름 (blank이면 예외)
 * @param tableClass 변경할 테이블 클래스
 */
inline fun updateTableRequestOf(
    tableName: String,
    tableClass: TableClass? = null,
    crossinline builder: UpdateTableRequest.Builder.() -> Unit = {},
): UpdateTableRequest {
    tableName.requireNotBlank("tableName")

    return UpdateTableRequest {
        this.tableName = tableName
        this.tableClass = tableClass

        builder()
    }
}
