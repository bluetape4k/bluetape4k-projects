@file:Suppress("NOTHING_TO_INLINE")

package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.Put
import aws.sdk.kotlin.services.dynamodb.model.PutRequest
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requireNotEmpty

/**
 * DSL 블록으로 DynamoDB [Put]을 빌드합니다 ([AttributeValue] 항목 오버로드).
 *
 * ## 동작/계약
 * - [tableName]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [item]이 비어 있으면 `IllegalArgumentException`을 던진다.
 * - [builder] 블록으로 조건 표현식 등 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val put = putOf("users", mapOf("id" to AttributeValue.S("u1"), "name" to AttributeValue.S("Alice")))
 * // put.tableName == "users"
 * // put.item?.size == 2
 * ```
 *
 * @param tableName 저장할 DynamoDB 테이블 이름 (blank이면 예외)
 * @param item 저장할 항목의 속성 맵 (비어 있으면 예외)
 */
@JvmName("putOfAttributeValue")
inline fun putOf(
    tableName: String,
    item: Map<String, AttributeValue>? = null,
    crossinline builder: Put.Builder.() -> Unit = {},
): Put {
    tableName.requireNotBlank("tableName")
    item.requireNotEmpty("item")

    return Put {
        this.tableName = tableName
        this.item = item

        builder()
    }
}

/**
 * DSL 블록으로 DynamoDB [Put]을 빌드합니다 (Any? 항목 오버로드).
 *
 * ## 동작/계약
 * - [tableName]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [item]의 각 값은 [toAttributeValueMap]을 통해 [AttributeValue]로 자동 변환된다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val put = putOf("users", mapOf("id" to "u1", "name" to "Alice"))
 * // put.item?.get("id") == AttributeValue.S("u1")
 * ```
 *
 * @param tableName 저장할 DynamoDB 테이블 이름 (blank이면 예외)
 * @param item 저장할 항목의 속성 맵 (자동으로 [AttributeValue]로 변환)
 */
@JvmName("putOfAny")
inline fun putOf(
    tableName: String,
    item: Map<String, Any?>? = null,
    crossinline builder: Put.Builder.() -> Unit = {},
): Put =
    putOf(tableName, item?.toAttributeValueMap(), builder)

/**
 * DynamoDB [PutRequest]를 빌드합니다 ([AttributeValue] 항목 오버로드).
 *
 * ## 동작/계약
 * - [item]은 저장할 항목의 속성 맵으로, 변환 없이 직접 설정된다.
 *
 * ```kotlin
 * val req = putRequestOf(mapOf("id" to AttributeValue.S("u1")))
 * // req.item?.get("id") == AttributeValue.S("u1")
 * ```
 *
 * @param item 저장할 항목의 [AttributeValue] 속성 맵
 */
@JvmName("putRequestOfAttributeValue")
inline fun putRequestOf(
    item: Map<String, AttributeValue>,
): PutRequest = PutRequest {
    this.item = item
}

/**
 * DynamoDB [PutRequest]를 빌드합니다 (Any? 항목 오버로드).
 *
 * ## 동작/계약
 * - [item]의 각 값은 [toAttributeValueMap]을 통해 [AttributeValue]로 자동 변환된다.
 *
 * ```kotlin
 * val req = putRequestOf(mapOf("id" to "u1"))
 * // req.item?.get("id") == AttributeValue.S("u1")
 * ```
 *
 * @param item 저장할 항목의 속성 맵 (자동으로 [AttributeValue]로 변환)
 */
@JvmName("putRequestOfAny")
inline fun putRequestOf(
    item: Map<String, Any?>,
    crossinline builder: PutRequest.Builder.() -> Unit = {},
): PutRequest =
    putRequestOf(item.toAttributeValueMap())
