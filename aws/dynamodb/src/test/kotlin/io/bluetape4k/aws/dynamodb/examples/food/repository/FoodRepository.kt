package io.bluetape4k.aws.dynamodb.examples.food.repository

import io.bluetape4k.aws.dynamodb.enhanced.table
import io.bluetape4k.aws.dynamodb.examples.food.model.FoodDocument
import io.bluetape4k.aws.dynamodb.examples.food.model.Schema
import io.bluetape4k.aws.dynamodb.model.QueryEnhancedRequest
import io.bluetape4k.aws.dynamodb.model.keyOf
import io.bluetape4k.aws.dynamodb.repository.DynamoDbCoroutineRepository
import io.bluetape4k.aws.dynamodb.repository.findFirst
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.info
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
import java.time.Instant

@Repository
class FoodRepository(
    @param:Autowired override val client: DynamoDbEnhancedAsyncClient,
    @Value($$"${aws.dynamodb.tablePrefix:local-}") tablePrefix: String,
): DynamoDbCoroutineRepository<FoodDocument> {

    companion object: KLoggingChannel()

    override val itemClass: Class<FoodDocument> = FoodDocument::class.java
    override val table: DynamoDbAsyncTable<FoodDocument> by lazy {
        client.table("$tablePrefix${Schema.TABLE_NAME}")
    }

    suspend fun findByPartitionKey(
        partitionKey: String,
        updatedAtFrom: Instant,
        updatedAtTo: Instant,
    ): List<FoodDocument> {
        val fromKey = keyOf(partitionKey, updatedAtFrom.toString())
        val toKey = keyOf(partitionKey, updatedAtTo.toString())

        val queryRequest = QueryEnhancedRequest {
            queryConditional(QueryConditional.sortBetween(fromKey, toKey))
        }
        log.info { "queryRequest=$queryRequest" }

        return table
            .index(Schema.IDX_PK_UPDATED_AT)
            .query(queryRequest)
            .findFirst()
    }
}
