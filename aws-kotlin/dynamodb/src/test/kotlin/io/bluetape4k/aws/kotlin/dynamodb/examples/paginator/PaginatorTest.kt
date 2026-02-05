package io.bluetape4k.aws.kotlin.dynamodb.examples.paginator

import aws.sdk.kotlin.services.dynamodb.createTable
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.ScalarAttributeType
import aws.sdk.kotlin.services.dynamodb.model.TableClass
import aws.sdk.kotlin.services.dynamodb.paginators.scanPaginated
import aws.sdk.kotlin.services.dynamodb.putItem
import io.bluetape4k.aws.kotlin.dynamodb.AbstractKotlinDynamoDbTest
import io.bluetape4k.aws.kotlin.dynamodb.deleteTableIfExists
import io.bluetape4k.aws.kotlin.dynamodb.existsTable
import io.bluetape4k.aws.kotlin.dynamodb.model.attributeDefinitionOf
import io.bluetape4k.aws.kotlin.dynamodb.model.partitionKeyOf
import io.bluetape4k.aws.kotlin.dynamodb.model.provisionedThroughputOf
import io.bluetape4k.aws.kotlin.dynamodb.model.sortKeyOf
import io.bluetape4k.aws.kotlin.dynamodb.model.toAttributeValue
import io.bluetape4k.codec.Base58
import io.bluetape4k.collections.eclipse.fastListOf
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.flow.buffer
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class PaginatorTest: AbstractKotlinDynamoDbTest() {

    companion object: KLoggingChannel()

    private val testTableName = "test-table-${Base58.randomString(8).lowercase()}"

    @BeforeAll
    fun beforeAll() = runSuspendIO {
        client.deleteTableIfExists(testTableName)

        log.debug { "Create $testTableName table ..." }

        val response = client.createTable {
            tableName = testTableName
            attributeDefinitions = listOf(
                attributeDefinitionOf("Artist", ScalarAttributeType.S),
                attributeDefinitionOf("SongTitle", ScalarAttributeType.S)
            )
            keySchema = listOf(
                partitionKeyOf("Artist"),
                sortKeyOf("SongTitle")
            )
            provisionedThroughput = provisionedThroughputOf(5, 5)
            tableClass = TableClass.Standard
        }
        log.debug { "Create Table. ${response.tableDescription}" }
    }

    @AfterAll
    fun afterAll() = runSuspendIO {
        client.deleteTableIfExists(testTableName)
    }

    @Test
    fun `check test table exists`() = runSuspendIO {
        client.existsTable(testTableName).shouldBeTrue()
    }

    @Test
    fun `첫번째 Key를 제외한 나머지 Key를 조회한다`() = runSuspendIO {
        client.putItem {
            tableName = testTableName
            item = mapOf(
                "Artist" to "Foo".toAttributeValue(),
                "SongTitle" to "Bar".toAttributeValue()
            )
        }
        client.putItem {
            tableName = testTableName
            item = mapOf(
                "Artist" to "Foo".toAttributeValue(),
                "SongTitle" to "Baz".toAttributeValue()
            )
        }
        client.putItem {
            tableName = testTableName
            item = mapOf(
                "Artist" to "Foo".toAttributeValue(),
                "SongTitle" to "Qux".toAttributeValue()
            )
        }

        val results = fastListOf<Map<String, AttributeValue>>()

        // StartKey에 해당하는 아이템을 제외하고 나머지 아이템을 조회한다.
        client
            .scanPaginated {
                tableName = testTableName
                exclusiveStartKey = mapOf(
                    "Artist" to "Foo".toAttributeValue(),
                    "SongTitle" to "Bar".toAttributeValue()
                )
                limit = 1
            }
            .buffer()
            .collect { scan ->
                if (scan.items?.isNotEmpty() == true) {
                    results.add(scan.items!!.single())
                }
            }

        results.forEach { entry ->
            log.debug { "Item=$entry" }
        }

        results shouldHaveSize 2
        results[0]["SongTitle"]?.asS() shouldBeEqualTo "Baz"
        results[1]["SongTitle"]?.asS() shouldBeEqualTo "Qux"
    }
}
