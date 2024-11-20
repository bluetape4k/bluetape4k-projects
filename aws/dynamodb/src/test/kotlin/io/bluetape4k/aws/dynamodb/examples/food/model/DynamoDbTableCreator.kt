package io.bluetape4k.aws.dynamodb.examples.food.model

import io.bluetape4k.aws.dynamodb.enhanced.table
import io.bluetape4k.aws.dynamodb.examples.food.model.Schema.GLOBAL_INDICES
import io.bluetape4k.aws.dynamodb.examples.food.model.Schema.LOCAL_INDICES
import io.bluetape4k.aws.dynamodb.examples.food.model.Schema.TABLE_NAME
import io.bluetape4k.aws.dynamodb.model.CreateTableEnhancedRequest
import io.bluetape4k.aws.dynamodb.model.EnhancedGlobalSecondaryIndex
import io.bluetape4k.aws.dynamodb.model.EnhancedLocalSecondaryIndex
import io.bluetape4k.aws.dynamodb.model.projectionOf
import io.bluetape4k.aws.dynamodb.model.provisionedThroughputOf
import io.bluetape4k.aws.dynamodb.schema.DynamoDbAsyncTableCreator
import io.bluetape4k.logging.KLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient
import software.amazon.awssdk.services.dynamodb.model.ProjectionType

@Component
class DynamoDbTableCreator(
    private val tableCreator: DynamoDbAsyncTableCreator,
    private val asyncClient: DynamoDbEnhancedAsyncClient,
    @Value("\${aws.dynamodb.tablePrefix:local-}") val tablePrefix: String,
) {
    companion object: KLogging() {
        const val READ_CAPACITY_UNITS = 100L
        const val WRITE_CAPACITY_UNITS = 100L
    }

    val table = asyncClient.table<FoodDocument>("$tablePrefix$TABLE_NAME")

    suspend fun createTable(
        readCapacityUnits: Long = READ_CAPACITY_UNITS,
        writeCapacityUnits: Long = WRITE_CAPACITY_UNITS,
    ) {
        val localSecondaryIndices = LOCAL_INDICES.map { indexName ->
            EnhancedLocalSecondaryIndex {
                indexName(indexName)
                projection(projectionOf(ProjectionType.ALL))
            }
        }
        val globalSecondaryIndices = GLOBAL_INDICES.map { indexName ->
            EnhancedGlobalSecondaryIndex {
                indexName(indexName)
                projection(projectionOf(ProjectionType.ALL))
                provisionedThroughput(provisionedThroughputOf(readCapacityUnits, writeCapacityUnits))
            }
        }

        val provisionedThroughput = provisionedThroughputOf(readCapacityUnits, writeCapacityUnits)

        val createTableRequest = CreateTableEnhancedRequest {
            localSecondaryIndices(localSecondaryIndices)
            globalSecondaryIndices(globalSecondaryIndices)
            provisionedThroughput(provisionedThroughput)
        }
        tableCreator.tryCreateAsyncTable(table, createTableRequest)
    }
}
