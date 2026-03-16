package io.bluetape4k.aws.dynamodb.examples.food.model

import io.bluetape4k.aws.dynamodb.examples.food.model.Schema.IDX_PK_UPDATED_AT
import io.bluetape4k.aws.dynamodb.examples.food.model.Schema.IDX_SK_UPDATED_AT
import io.bluetape4k.aws.dynamodb.examples.food.model.Schema.IDX_SORT_KEY_PARTITION_KEY
import io.bluetape4k.aws.dynamodb.model.AbstractDynamoDbEntity
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey
import java.time.Instant

abstract class AbstractDynamoDocument: AbstractDynamoDbEntity() {

    @get:DynamoDbPartitionKey
    @get:DynamoDbSecondaryPartitionKey(indexNames = [IDX_PK_UPDATED_AT])
    @get:DynamoDbSecondarySortKey(indexNames = [IDX_SORT_KEY_PARTITION_KEY])
    override var partitionKey: String = ""

    @get:DynamoDbSortKey
    @get:DynamoDbSecondaryPartitionKey(indexNames = [IDX_SK_UPDATED_AT, IDX_SORT_KEY_PARTITION_KEY])
    override var sortKey: String = ""


    @get:DynamoDbSecondarySortKey(indexNames = [IDX_PK_UPDATED_AT, IDX_SK_UPDATED_AT])
    var updatedAt: Instant? = Instant.now()

}
