package io.bluetape4k.aws.dynamodb.examples.food.repository

import io.bluetape4k.aws.dynamodb.enhanced.table
import io.bluetape4k.aws.dynamodb.examples.food.model.CustomerDocument
import io.bluetape4k.aws.dynamodb.examples.food.model.Schema
import io.bluetape4k.aws.dynamodb.model.QueryEnhancedRequest
import io.bluetape4k.aws.dynamodb.model.dynamoDbKeyOf
import io.bluetape4k.aws.dynamodb.repository.DynamoDbCoroutineRepository
import io.bluetape4k.aws.dynamodb.repository.findFirst
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.flow.toList
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional

@Repository
class CustomerRepository(
    @Autowired override val client: DynamoDbEnhancedAsyncClient,
    @Value("\${aws.dynamodb.tablePrefix:local-}") tablePrefix: String,
): DynamoDbCoroutineRepository<CustomerDocument> {

    companion object: KLogging()

    override val itemClass: Class<CustomerDocument> = CustomerDocument::class.java
    override val table: DynamoDbAsyncTable<CustomerDocument> by lazy {
        client.table("$tablePrefix${Schema.TABLE_NAME}")
    }

    suspend fun findByPartitionKey(partitionKey: String): List<CustomerDocument> {
        val queryRequest = QueryEnhancedRequest {
            queryConditional(QueryConditional.keyEqualTo(dynamoDbKeyOf(partitionKey)))
        }
        return table.query(queryRequest).findFirst().toList()
    }
}
