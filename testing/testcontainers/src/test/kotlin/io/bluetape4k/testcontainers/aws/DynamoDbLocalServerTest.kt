package io.bluetape4k.testcontainers.aws

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContainAll
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.assertions.shouldStartWith
import io.bluetape4k.testcontainers.AbstractContainerTest
import org.junit.jupiter.api.Test

class DynamoDbLocalServerTest: AbstractContainerTest() {

    @Test
    fun `blank image tag is rejected`() {
        assertFailsWith<IllegalArgumentException> { DynamoDbLocalServer(image = " ") }
        assertFailsWith<IllegalArgumentException> { DynamoDbLocalServer(tag = " ") }
    }

    @Test
    fun `property keys are available before start`() {
        val server = DynamoDbLocalServer(reuse = false)

        server.propertyKeys() shouldContainAll setOf(
            "host",
            "port",
            "url",
            "endpoint",
            "aws-endpoint",
            "aws-access-key",
            "aws-secret-key",
            "region",
        )
    }

    @Test
    fun `property namespace is dynamodb local`() {
        DynamoDbLocalServer(reuse = false).propertyNamespace shouldBeEqualTo DynamoDbLocalServer.NAME
    }

    @Test
    fun `DynamoDB Local starts and exports connection properties`() {
        DynamoDbLocalServer(reuse = false).use { server ->
            server.start()

            server.isRunning.shouldBeTrue()
            server.awsEndpoint.shouldNotBeNull()
            server.awsEndpoint.toString() shouldStartWith "http://"
            server.endpoint shouldBeEqualTo server.awsEndpoint
            server.awsAccessKey shouldBeEqualTo DynamoDbLocalServer.DEFAULT_ACCESS_KEY
            server.awsSecretKey shouldBeEqualTo DynamoDbLocalServer.DEFAULT_SECRET_KEY
            server.regionName shouldBeEqualTo DynamoDbLocalServer.DEFAULT_REGION
            System.getProperty("testcontainers.dynamodb-local.endpoint") shouldBeEqualTo server.endpoint.toString()
            System.getProperty("testcontainers.dynamodb-local.aws-endpoint") shouldBeEqualTo server.awsEndpoint.toString()
        }
    }
}
