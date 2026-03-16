package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.Get
import aws.sdk.kotlin.services.dynamodb.model.KeysAndAttributes
import aws.sdk.kotlin.services.dynamodb.model.ReturnConsumedCapacity
import aws.sdk.kotlin.services.dynamodb.model.TransactGetItem
import aws.sdk.kotlin.services.dynamodb.model.TransactGetItemsRequest
import io.bluetape4k.support.requireNotEmpty

/**
 * [Get] 객체로 DynamoDB [TransactGetItem]을 생성합니다.
 *
 * ## 동작/계약
 * - [get]을 그대로 [TransactGetItem]의 `get` 필드에 설정한다.
 *
 * ```kotlin
 * val item = transactGetItemOf(getOf("users", mapOf("id" to AttributeValue.S("u1"))))
 * // item.get?.tableName == "users"
 * ```
 *
 * @param get 조회 작업을 정의하는 [Get] 객체
 */
fun transactGetItemOf(get: Get): TransactGetItem =
    TransactGetItem {
        this.get = get
    }

/**
 * 테이블 이름과 키로 DynamoDB [TransactGetItem]을 생성합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [getOf]를 호출하여 [Get]을 생성한 후 [TransactGetItem]으로 래핑한다.
 * - [tableName]이 blank이면 `IllegalArgumentException`을 던진다.
 *
 * ```kotlin
 * val item = transactGetItemOf("users", emptyMap()) {
 *     projectionExpression = "id, name"
 * }
 * // item.get?.tableName == "users"
 * ```
 *
 * @param tableName 조회할 DynamoDB 테이블 이름 (blank이면 예외)
 * @param key 조회할 항목의 키와 속성 맵
 * @param expressionAttributeNames 프로젝션 표현식 속성 이름 치환 맵
 * @param projectionExpression 반환할 속성을 지정하는 프로젝션 표현식
 */
inline fun transactGetItemOf(
    tableName: String,
    key: Map<String, KeysAndAttributes> = emptyMap(),
    expressionAttributeNames: Map<String, String>? = null,
    projectionExpression: String? = null,
    crossinline builder: Get.Builder.() -> Unit,
): TransactGetItem =
    transactGetItemOf(getOf(tableName, key, expressionAttributeNames, projectionExpression, builder))

/**
 * DSL 블록으로 DynamoDB [TransactGetItemsRequest]를 빌드합니다.
 *
 * ## 동작/계약
 * - [transactItems]가 비어 있으면 `IllegalArgumentException`을 던진다.
 * - [returnConsumedCapacity]가 null이면 소비 용량 정보를 반환하지 않는다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val req = transactGetItemsRequestOf(
 *     transactItems = listOf(transactGetItemOf(getOf("users", mapOf("id" to AttributeValue.S("u1")))))
 * )
 * // req.transactItems?.size == 1
 * ```
 *
 * @param transactItems 트랜잭션으로 조회할 [TransactGetItem] 목록 (비어 있으면 예외)
 * @param returnConsumedCapacity 소비된 용량 반환 여부
 */
inline fun transactGetItemsRequestOf(
    transactItems: List<TransactGetItem>,
    returnConsumedCapacity: ReturnConsumedCapacity? = null,
    @BuilderInference crossinline builder: TransactGetItemsRequest.Builder.() -> Unit = {},
): TransactGetItemsRequest {
    transactItems.requireNotEmpty("transactItems")

    return TransactGetItemsRequest {
        this.transactItems = transactItems
        this.returnConsumedCapacity = returnConsumedCapacity

        builder()
    }
}
