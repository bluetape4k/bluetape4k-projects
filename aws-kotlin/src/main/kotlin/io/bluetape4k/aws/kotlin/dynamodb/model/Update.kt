package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.Update
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requireNotEmpty

/**
 * DSL 블록으로 DynamoDB [Update]를 빌드합니다 ([AttributeValue] 키 오버로드).
 *
 * ## 동작/계약
 * - [tableName]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [key]가 비어 있으면 `IllegalArgumentException`을 던진다.
 * - [updateExpression]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val update = updateOf(
 *     tableName = "users",
 *     key = mapOf("id" to AttributeValue.S("u1")),
 *     updateExpression = "SET #n = :name",
 *     expressionAttributeValues = mapOf(":name" to AttributeValue.S("Alice")),
 *     expressionAttributeNames = mapOf("#n" to "name")
 * )
 * // update.tableName == "users"
 * // update.updateExpression == "SET #n = :name"
 * ```
 *
 * @param tableName 업데이트할 DynamoDB 테이블 이름 (blank이면 예외)
 * @param key 업데이트할 항목의 기본 키 맵 (비어 있으면 예외)
 * @param updateExpression 업데이트 표현식 (blank이면 예외)
 * @param expressionAttributeValues 업데이트 표현식의 값 치환 맵
 * @param expressionAttributeNames 업데이트 표현식의 이름 치환 맵
 * @param conditionExpression 업데이트 조건 표현식
 */
@JvmName("updateOfAttributeValue")
inline fun updateOf(
    tableName: String,
    key: Map<String, AttributeValue>,
    updateExpression: String,
    expressionAttributeValues: Map<String, AttributeValue>,
    expressionAttributeNames: Map<String, String>? = null,
    conditionExpression: String? = null,
    crossinline builder: Update.Builder.() -> Unit = {},
): Update {
    tableName.requireNotBlank("tableName")
    key.requireNotEmpty("key")
    updateExpression.requireNotBlank("updateExpression")

    return Update {
        this.tableName = tableName
        this.key = key
        this.updateExpression = updateExpression
        this.expressionAttributeValues = expressionAttributeValues

        this.expressionAttributeNames = expressionAttributeNames
        this.conditionExpression = conditionExpression

        builder()
    }
}

/**
 * DSL 블록으로 DynamoDB [Update]를 빌드합니다 (Any? 키 오버로드).
 *
 * ## 동작/계약
 * - [key]의 각 값은 [toAttributeValueMap]을 통해 [AttributeValue]로 자동 변환된다.
 * - [tableName]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [updateExpression]이 blank이면 `IllegalArgumentException`을 던진다.
 *
 * ```kotlin
 * val update = updateOf(
 *     tableName = "users",
 *     key = mapOf("id" to "u1"),
 *     updateExpression = "SET #n = :name",
 *     expressionAttributeValues = mapOf(":name" to AttributeValue.S("Alice"))
 * )
 * // update.key?.get("id") == AttributeValue.S("u1")
 * ```
 *
 * @param tableName 업데이트할 DynamoDB 테이블 이름 (blank이면 예외)
 * @param key 업데이트할 항목의 기본 키 맵 (자동으로 [AttributeValue]로 변환)
 * @param updateExpression 업데이트 표현식 (blank이면 예외)
 * @param expressionAttributeValues 업데이트 표현식의 값 치환 맵
 * @param expressionAttributeNames 업데이트 표현식의 이름 치환 맵
 * @param conditionExpression 업데이트 조건 표현식
 */
@JvmName("updateOfAny")
inline fun updateOf(
    tableName: String,
    key: Map<String, Any?>,
    updateExpression: String,
    expressionAttributeValues: Map<String, AttributeValue>,
    expressionAttributeNames: Map<String, String>? = null,
    conditionExpression: String? = null,
    crossinline builder: Update.Builder.() -> Unit = {},
): Update {
    return updateOf(
        tableName,
        key.toAttributeValueMap(),
        updateExpression,
        expressionAttributeValues,
        expressionAttributeNames,
        conditionExpression,
        builder
    )
}
