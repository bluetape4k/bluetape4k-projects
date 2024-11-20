package io.bluetape4k.aws.dynamodb.schema

import io.bluetape4k.aws.dynamodb.model.DynamoDbEntity
import software.amazon.awssdk.enhanced.dynamodb.TableSchema

/**
 * [DynamoDbEntity]를 상속받은 Entity에 대한 Table Schema를 가져옵니다.
 *
 * ```
 * val schema = getTableSchema<FoodDocument>()
 * ```
 *
 * @return [TableSchema] 인스턴스
 */
inline fun <reified T: DynamoDbEntity> getTableSchema(): TableSchema<T> =
    TableSchema.fromClass(T::class.java)
