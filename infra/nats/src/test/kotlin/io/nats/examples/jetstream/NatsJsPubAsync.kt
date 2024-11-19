package io.nats.examples.jetstream

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.nats.AbstractNatsTest
import io.bluetape4k.nats.client.createStreamOrUpdateSubjects
import io.bluetape4k.nats.client.natsMessageOf
import io.nats.client.api.PublishAck
import kotlinx.coroutines.async
import kotlinx.coroutines.future.await
import org.junit.jupiter.api.RepeatedTest

class NatsJsPubAsync: AbstractNatsTest() {

    companion object: KLogging() {
        private const val DEFAULT_STREAM = "example-stream"
        private const val DEFAULT_SUBJECT = "example-subject"
        private const val DEFAULT_MESSAGE = "hello"
        private const val DEFAULT_MSG_COUNT = 50

        private const val REPEAT_SIZE = 3
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `jetstream publish asynchronously`() {
        getConnection().use { nc ->
            // Create a JetStream context.  This hangs off the original connection
            // allowing us to produce data to streams and consume data from
            // JetStream consumers.
            val js = nc.jetStream()

            nc.createStreamOrUpdateSubjects(DEFAULT_STREAM, subjects = arrayOf(DEFAULT_SUBJECT))

            val futures = List(DEFAULT_MSG_COUNT) { index ->
                val data = "$DEFAULT_MESSAGE-$index"
                val msg = natsMessageOf(DEFAULT_SUBJECT, data)
                log.debug { "Publishing message $data on subject $DEFAULT_SUBJECT" }

                // Publish a message
                js.publishAsync(msg)
            }

            futures.forEach {
                try {
                    // responder 를 설정하지 않았기 때문에 예외가 발생합니다.
                    // https://stackoverflow.com/questions/67502707/nats-io-natsnorespondersexception
                    val pa: PublishAck = it.get()
                    log.debug { "Publish succeeded on subject[$DEFAULT_SUBJECT], stream[${pa.stream}], seqno[${pa.seqno}" }
                } catch (e: Throwable) {
                    log.error(e) { "Publish failed. pulish ack=${it.get()}" }
                }
            }
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `jetstream publish with coroutines`() = runSuspendIO {
        getConnection().use { nc ->
            // Create a JetStream context.  This hangs off the original connection
            // allowing us to produce data to streams and consume data from
            // JetStream consumers.
            val js = nc.jetStream()

            nc.createStreamOrUpdateSubjects(DEFAULT_STREAM, subjects = arrayOf(DEFAULT_SUBJECT))

            val defers = List(DEFAULT_MSG_COUNT) { index ->
                async {
                    val data = "$DEFAULT_MESSAGE-$index"
                    val msg = natsMessageOf(DEFAULT_SUBJECT, data)
                    log.debug { "Publishing message $data on subject $DEFAULT_SUBJECT" }

                    // Publish a message
                    js.publishAsync(msg).await()
                }
            }

            defers.forEach {
                try {
                    // responder 를 설정하지 않았기 때문에 예외가 발생합니다.
                    // https://stackoverflow.com/questions/67502707/nats-io-natsnorespondersexception
                    val pa: PublishAck = it.await()
                    log.debug { "Publish succeeded on subject[$DEFAULT_SUBJECT], stream[${pa.stream}], seqno[${pa.seqno}]" }
                } catch (e: Throwable) {
                    log.error(e) { "Publish failed" }
                }
            }
        }
    }
}
