package io.bluetape4k.aws.kotlin.dynamodb

import aws.smithy.kotlin.runtime.net.url.Url
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class DynamoDbClientSupportTest {

    @Test
    fun `dynamoDbClientOf는 빈 endpoint를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            dynamoDbClientOf(endpointUrl = Url.parse(" \t  "), region = "us-east-1")
        }
    }

    @Test
    fun `dynamoDbClientOf는 빈 region를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            dynamoDbClientOf(endpointUrl = Url.parse("http://localhost:8000"), region = " \t ")
        }
    }

}
