package io.bluetape4k.aws.dynamodb.examples.food

import io.bluetape4k.aws.dynamodb.examples.food.repository.FoodRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.info
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

class FoodApplicationTest: AbstractFoodApplicationTest() {

    companion object: KLoggingChannel()

    @Autowired
    private lateinit var repository: FoodRepository

    @Value($$"${aws.dynamodb.endpoint}")
    private lateinit var dynamoDbEndpoint: String

    @Test
    fun `context loading`() {
        repository.shouldNotBeNull()

        log.info { "DynamoDb endpoint=$dynamoDbEndpoint" }
        dynamoDbEndpoint shouldBeEqualTo "http://localhost:${DynamoDb.port}"
    }
}
