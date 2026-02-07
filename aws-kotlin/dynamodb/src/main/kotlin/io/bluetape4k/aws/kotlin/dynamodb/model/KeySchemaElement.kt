package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.KeySchemaElement
import aws.sdk.kotlin.services.dynamodb.model.KeyType

/**
 * [KeySchemaElement]를 생성합니다.
 *
 * @param attributeName 키 스키마의 속성 이름
 * @param keyType 키 스키마의 타입 (Partition Key=[KeyType.Hash], Sort Key=[KeyType.Range])
 */
fun keySchemaElementOf(
    attributeName: String? = null,
    keyType: KeyType = KeyType.Hash,
): KeySchemaElement {

    return KeySchemaElement {
        this.attributeName = attributeName
        this.keyType = keyType
    }
}

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
