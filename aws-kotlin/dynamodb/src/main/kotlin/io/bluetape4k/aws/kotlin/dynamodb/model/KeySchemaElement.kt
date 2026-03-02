package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.KeySchemaElement
import aws.sdk.kotlin.services.dynamodb.model.KeyType
import io.bluetape4k.support.requireNotBlank

/**
 * DynamoDB [KeySchemaElement]를 생성합니다.
 *
 * ## 동작/계약
 * - [attributeName]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [keyType] 기본값은 [KeyType.Hash] (파티션 키)이다.
 *
 * ```kotlin
 * val elem = keySchemaElementOf("userId", KeyType.Hash)
 * // elem.attributeName == "userId"
 * // elem.keyType == KeyType.Hash
 * ```
 *
 * @param attributeName 키 스키마의 속성 이름 (blank이면 예외)
 * @param keyType 키 스키마의 타입 (Partition Key=[KeyType.Hash], Sort Key=[KeyType.Range])
 */
fun keySchemaElementOf(
    attributeName: String?,
    keyType: KeyType? = KeyType.Hash,
): KeySchemaElement {
    attributeName.requireNotBlank("attributeName")

    return KeySchemaElement {
        this.attributeName = attributeName
        this.keyType = keyType
    }
}

/**
 * 이 문자열을 속성 이름으로 하는 DynamoDB 파티션 키 [KeySchemaElement]를 생성합니다.
 *
 * ## 동작/계약
 * - 수신자 문자열이 blank이면 `IllegalArgumentException`을 던진다.
 * - [KeyType.Hash]로 고정된 [KeySchemaElement]를 반환한다.
 *
 * ```kotlin
 * val pk = "userId".partitionKey()
 * // pk.attributeName == "userId"
 * // pk.keyType == KeyType.Hash
 * ```
 */
fun String.partitionKey(): KeySchemaElement =
    keySchemaElementOf(this, KeyType.Hash)

/**
 * 이 문자열을 속성 이름으로 하는 DynamoDB 정렬 키 [KeySchemaElement]를 생성합니다.
 *
 * ## 동작/계약
 * - 수신자 문자열이 blank이면 `IllegalArgumentException`을 던진다.
 * - [KeyType.Range]로 고정된 [KeySchemaElement]를 반환한다.
 *
 * ```kotlin
 * val sk = "createdAt".sortKey()
 * // sk.attributeName == "createdAt"
 * // sk.keyType == KeyType.Range
 * ```
 */
fun String.sortKey(): KeySchemaElement =
    keySchemaElementOf(this, KeyType.Range)

/**
 * 지정한 이름으로 DynamoDB 파티션 키 [KeySchemaElement]를 생성합니다.
 *
 * ## 동작/계약
 * - [name]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [KeyType.Hash]로 고정된 [KeySchemaElement]를 반환한다.
 *
 * ```kotlin
 * val pk = partitionKeyOf("userId")
 * // pk.attributeName == "userId"
 * // pk.keyType == KeyType.Hash
 * ```
 *
 * @param name 파티션 키의 속성 이름 (blank이면 예외)
 */
fun partitionKeyOf(name: String): KeySchemaElement =
    keySchemaElementOf(name, KeyType.Hash)

/**
 * 지정한 이름으로 DynamoDB 정렬 키 [KeySchemaElement]를 생성합니다.
 *
 * ## 동작/계약
 * - [name]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [KeyType.Range]로 고정된 [KeySchemaElement]를 반환한다.
 *
 * ```kotlin
 * val sk = sortKeyOf("createdAt")
 * // sk.attributeName == "createdAt"
 * // sk.keyType == KeyType.Range
 * ```
 *
 * @param name 정렬 키의 속성 이름 (blank이면 예외)
 */
fun sortKeyOf(name: String): KeySchemaElement =
    keySchemaElementOf(name, KeyType.Range)
