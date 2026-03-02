package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.PutItemRequest
import aws.sdk.kotlin.services.dynamodb.model.ReturnValue
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requireNotEmpty

/**
 * DSL 블록으로 DynamoDB [PutItemRequest]를 빌드합니다 ([AttributeValue] 항목 오버로드).
 *
 * ## 동작/계약
 * - [tableName]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [item]이 비어 있으면 `IllegalArgumentException`을 던진다.
 * - [builder] 블록으로 조건 표현식 등 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val req = putItemRequestOf(
 *     "users",
 *     mapOf("id" to AttributeValue.S("u1"), "name" to AttributeValue.S("Alice"))
 * )
 * // req.tableName == "users"
 * // req.item?.size == 2
 * ```
 *
 * @param tableName 저장할 DynamoDB 테이블 이름 (blank이면 예외)
 * @param item 저장할 항목의 속성 맵 (비어 있으면 예외)
 * @param returnValues 응답에 포함할 이전/현재 값 설정
 */
@JvmName("putItemRequestOfAttributeValue")
inline fun putItemRequestOf(
    tableName: String,
    item: Map<String, AttributeValue>,
    returnValues: ReturnValue? = null,
    @BuilderInference crossinline builder: PutItemRequest.Builder.() -> Unit = {},
): PutItemRequest {
    tableName.requireNotBlank("tableName")
    item.requireNotEmpty("item")

    return PutItemRequest {
        this.tableName = tableName
        this.item = item
        this.returnValues = returnValues

        builder()
    }
}

/**
 * DSL 블록으로 DynamoDB [PutItemRequest]를 빌드합니다 (Any? 항목 오버로드).
 *
 * ## 동작/계약
 * - [tableName]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [item]의 각 값은 [toAttributeValueMap]을 통해 [AttributeValue]로 자동 변환된다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val req = putItemRequestOf("users", mapOf("id" to "u1", "name" to "Alice"))
 * // req.item?.get("id") == AttributeValue.S("u1")
 * ```
 *
 * @param tableName 저장할 DynamoDB 테이블 이름 (blank이면 예외)
 * @param item 저장할 항목의 속성 맵 (자동으로 [AttributeValue]로 변환)
 * @param returnValues 응답에 포함할 이전/현재 값 설정
 */
@JvmName("putItemRequestOfAny")
inline fun putItemRequestOf(
    tableName: String,
    item: Map<String, Any?>,
    returnValues: ReturnValue? = null,
    @BuilderInference crossinline builder: PutItemRequest.Builder.() -> Unit = {},
): PutItemRequest =
    putItemRequestOf(
        tableName,
        item.toAttributeValueMap(),
        returnValues,
        builder,
    )
