package io.bluetape4k.aws.cloudwatch

import io.bluetape4k.aws.cloudwatch.model.cloudwatchlogs.inputLogEventOf
import io.bluetape4k.codec.Base58
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.SAME_THREAD)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class CloudWatchLogsClientTest: AbstractCloudWatchTest() {

    companion object: KLogging() {
        private val LOG_GROUP_NAME = "/bluetape4k/test-${Base58.randomString(6).lowercase()}"
        private val LOG_STREAM_NAME = "test-stream-${Base58.randomString(4).lowercase()}"
    }

    @Test
    @Order(1)
    fun `create log group`() {
        val response = logsClient.createLogGroup(LOG_GROUP_NAME)

        response.sdkHttpResponse().statusCode() shouldBeEqualTo 200
        log.debug { "createLogGroup response: ${response.sdkHttpResponse().statusCode()}" }
    }

    @Test
    @Order(2)
    fun `create log stream`() {
        val response = logsClient.createLogStream(LOG_GROUP_NAME, LOG_STREAM_NAME)

        response.sdkHttpResponse().statusCode() shouldBeEqualTo 200
        log.debug { "createLogStream response: ${response.sdkHttpResponse().statusCode()}" }
    }

    @Test
    @Order(3)
    fun `put log events`() {
        val events = listOf(
            inputLogEventOf(System.currentTimeMillis(), "First log message"),
            inputLogEventOf(System.currentTimeMillis() + 1, "Second log message"),
            inputLogEventOf(System.currentTimeMillis() + 2, "Third log message"),
        )

        val response = logsClient.putLogEvents(LOG_GROUP_NAME, LOG_STREAM_NAME, events)

        response.sdkHttpResponse().statusCode() shouldBeEqualTo 200
        log.debug { "putLogEvents response: ${response.sdkHttpResponse().statusCode()}" }
    }

    @Test
    @Order(4)
    fun `describe log groups`() {
        val response = logsClient.describeLogGroups(logGroupNamePrefix = "/bluetape4k")

        response.logGroups().shouldNotBeNull().shouldNotBeEmpty()
        response.logGroups().forEach { group ->
            log.debug { "logGroup: ${group.logGroupName()}" }
        }
    }

    @Test
    @Order(5)
    fun `describe log streams`() {
        val response = logsClient.describeLogStreams(LOG_GROUP_NAME)

        response.logStreams().shouldNotBeNull().shouldNotBeEmpty()
        response.logStreams().forEach { stream ->
            log.debug { "logStream: ${stream.logStreamName()}" }
        }
    }
}
