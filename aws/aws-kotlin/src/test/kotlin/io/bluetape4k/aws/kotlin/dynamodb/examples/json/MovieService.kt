package io.bluetape4k.aws.kotlin.dynamodb.examples.json

import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.CreateTableRequest
import aws.sdk.kotlin.services.dynamodb.model.QueryResponse
import aws.sdk.kotlin.services.dynamodb.putItem
import aws.sdk.kotlin.services.dynamodb.query
import io.bluetape4k.aws.kotlin.dynamodb.existsTable
import io.bluetape4k.aws.kotlin.dynamodb.model.numberAttributeDefinition
import io.bluetape4k.aws.kotlin.dynamodb.model.partitionKey
import io.bluetape4k.aws.kotlin.dynamodb.model.provisionedThroughputOf
import io.bluetape4k.aws.kotlin.dynamodb.model.sortKey
import io.bluetape4k.aws.kotlin.dynamodb.model.stringAttributeDefinition
import io.bluetape4k.aws.kotlin.dynamodb.model.toAttributeValue
import io.bluetape4k.jackson.Jackson
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.utils.Resourcex

class MovieService(private val client: DynamoDbClient) {

    companion object: KLoggingChannel() {
        private val objectMapper = Jackson.defaultJsonMapper
    }

    suspend fun createMovieTable(tableName: String) {
        if (client.existsTable(tableName)) return

        val request = CreateTableRequest {
            this.tableName = tableName
            keySchema = listOf(
                "year".partitionKey(),
                "title".sortKey(),
            )
            attributeDefinitions = listOf(
                "year".numberAttributeDefinition(),
                "title".stringAttributeDefinition()
            )
            provisionedThroughput = provisionedThroughputOf(10, 10)
        }

        val response = client.createTable(request)
        log.info { "Create table: ${response.tableDescription?.tableArn}, ${response.tableDescription?.tableStatus}" }
    }

    /**
     * JSON 파일로부터 영화 정보를 읽어 DynamoDB [tableName] 테이블에 저장합니다.
     */
    suspend fun loadMovies(tableName: String) {
        val data = Resourcex.getString("/movies/data.json")
        val elements = objectMapper.readTree(data).elements()

        elements.forEach {
            val attrValue = it.toAttributeValue() as? AttributeValue.M
                ?: error("Unexpected a top level object value")
            log.debug { "attrValue.value=${attrValue.value}" }

            client.putItem {
                this.tableName = tableName
                this.item = attrValue.value
            }
        }
    }

    suspend fun moviesInYear(tableName: String, year: Int): QueryResponse {
        return client.query {
            this.tableName = tableName
            keyConditionExpression = "#year = :year"
            expressionAttributeNames = mapOf("#year" to "year")
            expressionAttributeValues = mapOf(":year" to year.toAttributeValue())
        }
    }
}
