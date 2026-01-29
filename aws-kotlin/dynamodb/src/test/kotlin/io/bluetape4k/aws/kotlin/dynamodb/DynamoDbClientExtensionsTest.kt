package io.bluetape4k.aws.kotlin.dynamodb

import aws.sdk.kotlin.services.dynamodb.model.TableClass
import io.bluetape4k.aws.kotlin.dynamodb.model.partitionKeyOf
import io.bluetape4k.aws.kotlin.dynamodb.model.sortKeyOf
import io.bluetape4k.aws.kotlin.dynamodb.model.stringAttrDefinitionOf
import io.bluetape4k.coroutines.flow.extensions.toFastList
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.mapNotNull
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class DynamoDbClientExtensionsTest: AbstractKotlinDynamoDbTest() {

    companion object: KLoggingChannel() {
        private const val TEST_TABLE_NAME = "test-table-for-client"
    }

    @Test
    @Order(0)
    fun `create table`() = runSuspendIO {
        client.deleteTableIfExists(TEST_TABLE_NAME)

        val response = client.createTable(TEST_TABLE_NAME) {
            keySchema = listOf(
                partitionKeyOf("Artist"),
                sortKeyOf("SongTitle")
            )
            attributeDefinitions = listOf(
                stringAttrDefinitionOf("Artist"),
                stringAttrDefinitionOf("SongTitle")
            )
            provisionedThroughput {
                readCapacityUnits = 5
                writeCapacityUnits = 5
            }
            tableClass = TableClass.Standard
        }
        log.debug { "Create table: ${response.tableDescription?.tableArn}" }

        client.waitForTableReady(TEST_TABLE_NAME)
    }

    @Test
    @Order(1)
    fun `scan paginated respects exclusive start key`() = runSuspendIO {
        client.putItem(TEST_TABLE_NAME, mapOf("Artist" to "Foo", "SongTitle" to "Bar"))
        client.putItem(TEST_TABLE_NAME, mapOf("Artist" to "Foo", "SongTitle" to "Baz"))
        client.putItem(TEST_TABLE_NAME, mapOf("Artist" to "Foo", "SongTitle" to "Qux"))

        val results = client
            .scanPaginated(
                TEST_TABLE_NAME,
                mapOf("Artist" to "Foo", "SongTitle" to "Bar"),
                1
            )
            .buffer()
            .mapNotNull { scan ->
                if (scan.items?.isNotEmpty() == true) {
                    scan.items!!.single()
                } else {
                    null
                }
            }
            .toFastList()


        results.forEach {
            log.debug { "item=$it" }
        }
        results shouldHaveSize 2
        results[0]["SongTitle"]?.asS() shouldBeEqualTo "Baz"
        results[1]["SongTitle"]?.asS() shouldBeEqualTo "Qux"
    }
}
