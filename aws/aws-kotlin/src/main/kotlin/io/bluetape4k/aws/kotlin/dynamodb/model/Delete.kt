package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.Delete
import aws.sdk.kotlin.services.dynamodb.model.DeleteRequest
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requireNotEmpty

/**
 * DSL 블록으로 DynamoDB [Delete]를 빌드합니다 ([AttributeValue] 키 오버로드).
 *
 * ## 동작/계약
 * - [tableName]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [key]는 삭제할 항목의 기본 키 맵으로, null이면 설정되지 않는다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val del = deleteOf("users", mapOf("id" to AttributeValue.S("u1")))
 * // del.tableName == "users"
 * // del.key?.get("id") == AttributeValue.S("u1")
 * ```
 *
 * @param tableName 삭제 대상 DynamoDB 테이블 이름 (blank이면 예외)
 * @param key 삭제할 항목의 기본 키 맵
 */
@JvmName("deleteOfAttributeValue")
inline fun deleteOf(
    tableName: String,
    key: Map<String, AttributeValue>? = null,
    crossinline builder: Delete.Builder.() -> Unit = {},
): Delete {
    tableName.requireNotBlank("tableName")

    return Delete {
        this.tableName = tableName
        this.key = key

        builder()
    }
}

/**
 * DSL 블록으로 DynamoDB [Delete]를 빌드합니다 (Any? 키 오버로드).
 *
 * ## 동작/계약
 * - [tableName]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [key]의 각 값은 [toAttributeValueMap]을 통해 [AttributeValue]로 자동 변환된다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val del = deleteOf("users", mapOf("id" to "u1"))
 * // del.key?.get("id") == AttributeValue.S("u1")
 * ```
 *
 * @param tableName 삭제 대상 DynamoDB 테이블 이름 (blank이면 예외)
 * @param key 삭제할 항목의 기본 키 맵 (자동으로 [AttributeValue]로 변환)
 */
@JvmName("deleteOfAny")
inline fun deleteOf(
    tableName: String,
    key: Map<String, Any?>? = null,
    crossinline builder: Delete.Builder.() -> Unit = {},
): Delete {
    tableName.requireNotBlank("tableName")

    return Delete {
        this.tableName = tableName
        this.key = key?.toAttributeValueMap()

        builder()
    }
}

/**
 * DSL 블록으로 DynamoDB [DeleteRequest]를 빌드합니다 ([AttributeValue] 키 오버로드).
 *
 * ## 동작/계약
 * - [key]가 비어 있으면 `IllegalArgumentException`을 던진다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val req = deleteRequestOf(mapOf("id" to AttributeValue.S("u1")))
 * // req.key?.get("id") == AttributeValue.S("u1")
 * ```
 *
 * @param key 삭제할 항목의 기본 키 맵 (비어 있으면 예외)
 */
@JvmName("deleteRequestOfAttributeValue")
inline fun deleteRequestOf(
    key: Map<String, AttributeValue>,
    crossinline builder: DeleteRequest.Builder.() -> Unit = {},
): DeleteRequest {
    key.requireNotEmpty("key")

    return DeleteRequest {
        this.key = key

        builder()
    }
}

/**
 * DSL 블록으로 DynamoDB [DeleteRequest]를 빌드합니다 (Any? 키 오버로드).
 *
 * ## 동작/계약
 * - [key]가 비어 있으면 `IllegalArgumentException`을 던진다.
 * - [key]의 각 값은 [toAttributeValueMap]을 통해 [AttributeValue]로 자동 변환된다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val req = deleteRequestOf(mapOf("id" to "u1"))
 * // req.key?.get("id") == AttributeValue.S("u1")
 * ```
 *
 * @param key 삭제할 항목의 기본 키 맵 (비어 있으면 예외, 자동으로 [AttributeValue]로 변환)
 */
@JvmName("deleteRequestOfAny")
inline fun deleteRequestOf(
    key: Map<String, Any?>,
    crossinline builder: DeleteRequest.Builder.() -> Unit = {},
): DeleteRequest {
    key.requireNotEmpty("key")

    return DeleteRequest {
        this.key = key.toAttributeValueMap()

        builder()
    }
}
