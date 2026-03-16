package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.Get
import io.bluetape4k.support.requireNotBlank

/**
 * DSL 블록으로 DynamoDB [Get]을 빌드합니다 ([AttributeValue] 키 오버로드).
 *
 * ## 동작/계약
 * - [tableName]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [key]는 조회할 항목의 기본 키 맵으로, null이면 설정되지 않는다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val get = getOf("users", mapOf("id" to AttributeValue.S("u1")))
 * // get.tableName == "users"
 * // get.key?.get("id") == AttributeValue.S("u1")
 * ```
 *
 * @param tableName 조회할 DynamoDB 테이블 이름 (blank이면 예외)
 * @param key 조회할 항목의 기본 키 맵
 * @param expressionAttributeNames 프로젝션 표현식 속성 이름 치환 맵
 * @param projectionExpression 반환할 속성을 지정하는 프로젝션 표현식
 */
@JvmName("getOfAttributeValue")
inline fun getOf(
    tableName: String,
    key: Map<String, AttributeValue>? = null,
    expressionAttributeNames: Map<String, String>? = null,
    projectionExpression: String? = null,
    @BuilderInference crossinline builder: Get.Builder.() -> Unit = {},
): Get {
    tableName.requireNotBlank("tableName")

    return Get {
        this.tableName = tableName
        this.key = key
        this.expressionAttributeNames = expressionAttributeNames
        this.projectionExpression = projectionExpression

        builder()
    }
}

/**
 * DSL 블록으로 DynamoDB [Get]을 빌드합니다 (Any? 키 오버로드).
 *
 * ## 동작/계약
 * - [tableName]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [key]의 각 값은 [toAttributeValueMap]을 통해 [AttributeValue]로 자동 변환된다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val get = getOf("users", mapOf("id" to "u1"))
 * // get.key?.get("id") == AttributeValue.S("u1")
 * ```
 *
 * @param tableName 조회할 DynamoDB 테이블 이름 (blank이면 예외)
 * @param key 조회할 항목의 기본 키 맵 (자동으로 [AttributeValue]로 변환)
 * @param expressionAttributeNames 프로젝션 표현식 속성 이름 치환 맵
 * @param projectionExpression 반환할 속성을 지정하는 프로젝션 표현식
 */
@JvmName("getOfAny")
inline fun getOf(
    tableName: String,
    key: Map<String, Any?>? = null,
    expressionAttributeNames: Map<String, String>? = null,
    projectionExpression: String? = null,
    @BuilderInference crossinline builder: Get.Builder.() -> Unit = {},
): Get = getOf(
    tableName,
    key?.toAttributeValueMap(),
    expressionAttributeNames,
    projectionExpression,
    builder,
)
