package io.bluetape4k.aws.dynamodb.query

import io.bluetape4k.aws.dynamodb.examples.food.model.FoodDocument
import io.bluetape4k.aws.dynamodb.examples.food.model.FoodState
import io.bluetape4k.aws.dynamodb.examples.food.model.Schema.IDX_PK_UPDATED_AT
import io.bluetape4k.aws.dynamodb.model.describe
import io.bluetape4k.aws.dynamodb.schema.getTableSchema
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.info
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest
import java.time.Instant

class DynamoDbEnhancedDslTest {

    companion object: KLoggingChannel()

    @Test
    fun `build enhanced query`() {

        val request: QueryEnhancedRequest = queryEnhancedRequest<FoodDocument> {
            primaryKey { eq(2) }
            sortKey("updatedAt") { between(2 to 3) }

            filtering {
                attribute("state") { eq(FoodState.COOKING) } and
                        attribute("updatedAt") { lt(Instant.now()) }
            }
        }

        log.info { "request=${request.describe()}" }

        val expr =
            request.queryConditional().expression(getTableSchema<FoodDocument>(), IDX_PK_UPDATED_AT)

        log.info { "Expr=${expr.expression()}" }
        log.info { "Expr names=${expr.expressionNames()}" }
        log.info { "Expr values=${expr.expressionValues()}" }

        request.filterExpression().expression().shouldNotBeEmpty()
        request.filterExpression().expressionNames().size shouldBeEqualTo 2
        request.filterExpression().expressionValues().size shouldBeEqualTo 2
        request.scanIndexForward().shouldBeTrue()
    }
}
