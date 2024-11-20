package io.bluetape4k.aws.dynamodb.query

import io.bluetape4k.aws.dynamodb.model.describe
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class DynamoDbQueryDslTest {

    companion object: KLogging()

    @Test
    fun `build nested filter queries`() {
        val request = queryRequest {
            tableName = "local-table"

            primaryKey("myPrimaryKey") {
                eq(2)
            }
            sortKey("mySortKey") {
                between(2 to 3)
            }

            filtering {
                attribute("a") {
                    lt(1)
                } and attribute("b") {
                    gt(2)
                } or {
                    attribute("c") {
                        eq(3)
                    } and attributeExists("d") or {
                        attribute("e") {
                            ne(4)
                        }
                    }
                } or attributeExists("f")
            }
        }

        log.debug { "queryRequest=${request.describe()}" }

        request.keyConditions()["myPrimaryKey"].shouldNotBeNull()
        request.keyConditions()["mySortKey"].shouldNotBeNull()
        request.filterExpression().shouldNotBeEmpty()
        request.expressionAttributeNames().size shouldBeEqualTo 6
        request.expressionAttributeValues().size shouldBeEqualTo 4
    }
}
