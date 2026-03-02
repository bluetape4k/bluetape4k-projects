package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.DeleteTableRequest
import io.bluetape4k.support.requireNotBlank

/**
 * DSL 블록으로 DynamoDB [DeleteTableRequest]를 빌드합니다.
 *
 * ## 동작/계약
 * - [tableName]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val req = deleteTableRequestOf("users")
 * // req.tableName == "users"
 * ```
 *
 * @param tableName 삭제할 DynamoDB 테이블 이름 (blank이면 예외)
 */
inline fun deleteTableRequestOf(
    tableName: String,
    @BuilderInference crossinline builder: DeleteTableRequest.Builder.() -> Unit = {},
): DeleteTableRequest {
    tableName.requireNotBlank("tableName")

    return DeleteTableRequest {
        this.tableName = tableName

        builder()
    }
}
