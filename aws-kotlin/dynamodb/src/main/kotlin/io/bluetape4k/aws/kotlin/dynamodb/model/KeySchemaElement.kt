package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.KeySchemaElement
import aws.sdk.kotlin.services.dynamodb.model.KeyType
import io.bluetape4k.support.requireNotBlank

/**
 * [KeySchemaElement]를 생성합니다.
 *
 * @param attributeName 키 스키마의 속성 이름
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
 * Partition Key를 생성합니다.
 *
 * @receiver Partition Key의 이름
 */
fun String.partitionKey(): KeySchemaElement =
    keySchemaElementOf(this, KeyType.Hash)

/**
 * Sort Key를 생성합니다.
 */
fun String.sortKey(): KeySchemaElement =
    keySchemaElementOf(this, KeyType.Range)

/**
 * Partition Key를 생성합니다.
 *
 * @param name Partition Key의 이름
 */
fun partitionKeyOf(name: String): KeySchemaElement =
    keySchemaElementOf(name, KeyType.Hash)

/**
 * Sort Key를 생성합니다.
 */
fun sortKeyOf(name: String): KeySchemaElement =
    keySchemaElementOf(name, KeyType.Range)
