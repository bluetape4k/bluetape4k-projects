package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.ConditionCheck
import aws.sdk.kotlin.services.dynamodb.model.Delete
import aws.sdk.kotlin.services.dynamodb.model.Put
import aws.sdk.kotlin.services.dynamodb.model.TransactWriteItem
import aws.sdk.kotlin.services.dynamodb.model.Update

/**
 * [Put] 객체로 DynamoDB [TransactWriteItem]을 생성합니다.
 *
 * ## 동작/계약
 * - [put]을 그대로 [TransactWriteItem]의 `put` 필드에 설정한다.
 *
 * ```kotlin
 * val item = transactWriteItemOf(putOf("users", mapOf("id" to AttributeValue.S("u1"))))
 * // item.put?.tableName == "users"
 * ```
 *
 * @param put 저장 작업을 정의하는 [Put] 객체
 */
fun transactWriteItemOf(put: Put): TransactWriteItem =
    TransactWriteItem {
        this.put = put
    }

/**
 * 테이블 이름과 항목으로 DynamoDB [TransactWriteItem]을 생성합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [putOf]를 호출하여 [Put]을 생성한 후 [TransactWriteItem]으로 래핑한다.
 * - [name]이 blank이면 `IllegalArgumentException`을 던진다.
 *
 * ```kotlin
 * val item = transactWriteItemOf("users", mapOf("id" to AttributeValue.S("u1"))) {}
 * // item.put?.tableName == "users"
 * ```
 *
 * @param name 저장할 DynamoDB 테이블 이름 (blank이면 예외)
 * @param item 저장할 항목의 [AttributeValue] 속성 맵
 */
inline fun transactWriteItemOf(
    name: String,
    item: Map<String, AttributeValue> = emptyMap(),
    crossinline putBuilder: Put.Builder.() -> Unit,
): TransactWriteItem =
    transactWriteItemOf(putOf(name, item, putBuilder))


/**
 * DSL 블록으로 DynamoDB [TransactWriteItem]을 빌드합니다.
 *
 * ## 동작/계약
 * - [conditionCheck], [delete], [put], [update] 중 하나를 지정하여 트랜잭션 쓰기 작업을 설정한다.
 * - 모두 null이면 빈 [TransactWriteItem]이 생성된다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val item = transactWriteItemOf(
 *     put = putOf("users", mapOf("id" to AttributeValue.S("u1")))
 * )
 * // item.put?.tableName == "users"
 * ```
 *
 * @param conditionCheck 조건 검사 작업
 * @param delete 삭제 작업
 * @param put 저장 작업
 * @param update 업데이트 작업
 */
fun transactWriteItemOf(
    conditionCheck: ConditionCheck? = null,
    delete: Delete? = null,
    put: Put? = null,
    update: Update? = null,
    @BuilderInference builder: TransactWriteItem.Builder.() -> Unit = {},
): TransactWriteItem = TransactWriteItem {
    this.conditionCheck = conditionCheck
    this.put = put
    this.update = update
    this.delete = delete

    builder()
}
