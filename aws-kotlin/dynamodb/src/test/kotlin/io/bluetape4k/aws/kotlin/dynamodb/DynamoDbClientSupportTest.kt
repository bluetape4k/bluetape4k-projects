package io.bluetape4k.aws.kotlin.dynamodb

import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class DynamoDbClientSupportTest {

    @Test
    fun `dynamoDbClientOf는 endpoint 없이도 생성된다`() {
        val client = dynamoDbClientOf(region = "us-east-1")
        client.close()
    }

    @Test
    fun `dynamoDbClientOf는 빈 endpoint를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            dynamoDbClientOf(endpointUrl = "   ", region = "us-east-1")
        }
    }
}
