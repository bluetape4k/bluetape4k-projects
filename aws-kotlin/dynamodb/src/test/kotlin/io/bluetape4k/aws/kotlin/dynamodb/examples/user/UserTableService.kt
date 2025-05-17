package io.bluetape4k.aws.kotlin.dynamodb.examples.user

import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.createTable
import aws.sdk.kotlin.services.dynamodb.describeTable
import aws.sdk.kotlin.services.dynamodb.model.CreateTableResponse
import aws.sdk.kotlin.services.dynamodb.model.ScalarAttributeType
import aws.sdk.kotlin.services.dynamodb.model.TableStatus
import io.bluetape4k.aws.kotlin.dynamodb.deleteTableIfExists
import io.bluetape4k.aws.kotlin.dynamodb.model.attributeDefinitionOf
import io.bluetape4k.aws.kotlin.dynamodb.model.partitionKeyOf
import io.bluetape4k.aws.kotlin.dynamodb.model.provisionedThroughputOf
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug

class UserTableService(private val client: DynamoDbClient) {

    companion object: KLoggingChannel()

    suspend fun createTable(): CreateTableResponse {
        deleteTableIfExists()

        return client.createTable {
            tableName = USER_TABLE_NAME
            keySchema = listOf(partitionKeyOf("userId"))
            attributeDefinitions = listOf(
                attributeDefinitionOf("userId", ScalarAttributeType.S)
            )
            provisionedThroughput = provisionedThroughputOf(10, 10)
        }
    }

    suspend fun deleteTableIfExists() {
        client.deleteTableIfExists(USER_TABLE_NAME)
    }

    suspend fun checkTableStatus(): TableStatus? {
        val response = client.describeTable { tableName = USER_TABLE_NAME }
        log.debug { "Table status: ${response.table?.tableStatus}" }
        return response.table?.tableStatus
    }
}
