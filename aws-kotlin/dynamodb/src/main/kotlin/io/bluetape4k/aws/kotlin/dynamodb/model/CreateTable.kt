package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.CreateTableRequest
import aws.sdk.kotlin.services.dynamodb.model.StreamSpecification
import aws.sdk.kotlin.services.dynamodb.model.TableClass
import aws.sdk.kotlin.services.dynamodb.model.Tag
import io.bluetape4k.support.requireNotBlank

/**
 * DSL 블록으로 DynamoDB [CreateTableRequest]를 빌드합니다.
 *
 * ## 동작/계약
 * - [tableName]이 blank이면 `IllegalArgumentException`을 던진다.
 * - 키 스키마·속성 정의 등 추가 설정은 [builder] 블록으로 확장한다.
 *
 * ```kotlin
 * val req = createTableRequestOf("orders") {
 *     keySchema = listOf(keySchemaElementOf("id", KeyType.Hash))
 *     attributeDefinitions = listOf(attributeDefinitionOf("id", ScalarAttributeType.S))
 * }
 * ```
 *
 * @param tableName 생성할 테이블 이름
 * @throws IllegalArgumentException [tableName]이 blank인 경우
 */
inline fun createTableRequestOf(
    tableName: String,
    tableClass: TableClass = TableClass.Standard,
    tags: List<Tag>? = null,
    streamSpecification: StreamSpecification? = null,
    @BuilderInference crossinline builder: CreateTableRequest.Builder.() -> Unit = {},
): CreateTableRequest {
    tableName.requireNotBlank("tableName")

    return CreateTableRequest {
        this.tableName = tableName
        this.tableClass = tableClass
        this.tags = tags
        this.streamSpecification = streamSpecification

        builder()
    }
}
