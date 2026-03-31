package io.bluetape4k.aws.kotlin.cloudwatch

import io.bluetape4k.aws.kotlin.cloudwatch.model.cloudwatchlogs.inputLogEventOf
import io.bluetape4k.codec.Base58
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class CloudWatchLogsClientExtensionsTest: AbstractKotlinCloudWatchTest() {

    companion object: KLoggingChannel() {
        private val LOG_GROUP_NAME = "/bluetape4k/kotlin-test-${Base58.randomString(6).lowercase()}"
        private val LOG_STREAM_NAME = "kotlin-stream-${Base58.randomString(4).lowercase()}"
    }

    @Test
    @Order(1)
    fun `create log group`() = runSuspendIO {
        withCloudWatchLogsClient(
            localStackServer.endpointUrl,
            localStackServer.region,
            localStackServer.credentialsProvider,
        ) { client ->
            client.createLogGroup(LOG_GROUP_NAME)
            log.debug { "createLogGroup completed: $LOG_GROUP_NAME" }
        }
    }

    @Test
    @Order(2)
    fun `create log stream`() = runSuspendIO {
        withCloudWatchLogsClient(
            localStackServer.endpointUrl,
            localStackServer.region,
            localStackServer.credentialsProvider,
        ) { client ->
            client.createLogStream(LOG_GROUP_NAME, LOG_STREAM_NAME)
            log.debug { "createLogStream completed: $LOG_GROUP_NAME/$LOG_STREAM_NAME" }
        }
    }

    @Test
    @Order(3)
    fun `put log events`() = runSuspendIO {
        val events = listOf(
            inputLogEventOf(System.currentTimeMillis(), "Kotlin log message 1"),
            inputLogEventOf(System.currentTimeMillis() + 1, "Kotlin log message 2"),
            inputLogEventOf(System.currentTimeMillis() + 2, "Kotlin log message 3"),
        )

        withCloudWatchLogsClient(
            localStackServer.endpointUrl,
            localStackServer.region,
            localStackServer.credentialsProvider,
        ) { client ->
            client.putLogEvents(LOG_GROUP_NAME, LOG_STREAM_NAME, events)
            log.debug { "putLogEvents completed: count=${events.size}" }
        }
    }

    @Test
    @Order(4)
    fun `describe log groups`() = runSuspendIO {
        withCloudWatchLogsClient(
            localStackServer.endpointUrl,
            localStackServer.region,
            localStackServer.credentialsProvider,
        ) { client ->
            val response = client.describeLogGroups(logGroupNamePrefix = "/bluetape4k")
            response.logGroups.shouldNotBeNull().shouldNotBeEmpty()
            response.logGroups!!.forEach { group ->
                log.debug { "logGroup: ${group.logGroupName}" }
            }
        }
    }

    @Test
    @Order(5)
    fun `describe log streams`() = runSuspendIO {
        withCloudWatchLogsClient(
            localStackServer.endpointUrl,
            localStackServer.region,
            localStackServer.credentialsProvider,
        ) { client ->
            val response = client.describeLogStreams(LOG_GROUP_NAME)
            response.logStreams.shouldNotBeNull().shouldNotBeEmpty()
            response.logStreams!!.forEach { stream ->
                log.debug { "logStream: ${stream.logStreamName}" }
            }
        }
    }
}
