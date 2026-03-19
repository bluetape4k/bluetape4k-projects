package io.nats.examples

import io.bluetape4k.logging.KLogging
import io.bluetape4k.nats.AbstractNatsTest
import io.bluetape4k.nats.client.api.consumerConfiguration
import io.bluetape4k.nats.client.api.streamConfiguration
import io.bluetape4k.nats.client.forcedDeleteStream
import io.bluetape4k.nats.client.publish
import io.bluetape4k.support.toUtf8String
import io.nats.client.PushSubscribeOptions
import io.nats.client.api.DeliverPolicy
import io.nats.client.api.StorageType
import io.nats.client.support.JsonUtils
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.time.Duration

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class RecreateConsumerExample: AbstractNatsTest() {

    companion object: KLogging() {
        private const val TEST_STREAM_NAME = "re-stream"
        private const val MAX_MESSAGE = 20
    }

    @Test
    @Order(1)
    fun `publish message`() {
        getConnection().use { nc ->
            val jsm = nc.jetStreamManagement()
            jsm.forcedDeleteStream(TEST_STREAM_NAME)

            val streamConfig = streamConfiguration(TEST_STREAM_NAME) {
                subjects("re-sub")
                storageType(StorageType.Memory)
            }
            jsm.addStream(streamConfig)

            for (x in 1..MAX_MESSAGE) {
                nc.publish("re-sub", "re-msg-$x")
            }

            JsonUtils.printFormatted(jsm.getStreamInfo(TEST_STREAM_NAME).streamState)
        }
    }

    @Test
    @Order(2)
    fun `make consumer`() {
        getConnection().use { nc ->
            val jsm = nc.jetStreamManagement()

            val cc = consumerConfiguration {
                durable("original-durable-name")
                deliverSubject("original-deliver-subject")
                filterSubject("re-sub")
                maxAckPending(5)   // prevents server from sending all of the messages
                // at once since this example stops before finishing reading
                ackWait(Duration.ofSeconds(3))
            }
            jsm.addOrUpdateConsumer(TEST_STREAM_NAME, cc)

            val js = nc.jetStream()
            val pso = PushSubscribeOptions.bind(TEST_STREAM_NAME, "original-durable-name")
            val sub = js.subscribe("re-sub", pso)

            // only read 10 of the 20
            for (x in 1..10) {
                val m = sub.nextMessage(1000)
                m.ack()
                println(
                    "Read Message, Stream Sequence: ${m.metaData().streamSequence()}, Data: ${m.data.toUtf8String()}"
                )
            }
            sub.unsubscribe()

            Thread.sleep(1000) // just give the server time to make sure the durable reflects the actual ack state.

            val ci = jsm.getConsumerInfo(TEST_STREAM_NAME, "original-durable-name")
            val lastSeq = ci.ackFloor.streamSequence
            println("The last acknowledged message's stream sequence is: $lastSeq")
        }
    }

    @Test
    @Order(3)
    fun `re-create consumer`() {
        getConnection().use { nc ->
            val jsm = nc.jetStreamManagement()

            val ci = jsm.getConsumerInfo(TEST_STREAM_NAME, "original-durable-name")
            val lastSeq = ci.ackFloor.streamSequence
            println("The last acknowledged message's stream sequence is: $lastSeq")

            // We are done with the original consumer so we can get rid of it.
            jsm.deleteConsumer(TEST_STREAM_NAME, "original-durable-name")

            val cc = consumerConfiguration(ci.consumerConfiguration) {
                name("updated-durable-name")                        // name 과 durable 은 같아야 합니다.
                durable("updated-durable-name")                     // but change the durable name
                deliverSubject("update-deliver-subject")  // change the deliver subject just to be safe
                deliverPolicy(DeliverPolicy.ByStartSequence)  // setup the consumer to start at a specific stream sequence
                startSequence(lastSeq + 1)    // tell the consumer which stream sequence
                // ... change anything else you want
            }
            jsm.addOrUpdateConsumer(TEST_STREAM_NAME, cc)

            val js = nc.jetStream()
            val pso = PushSubscribeOptions.bind(TEST_STREAM_NAME, "updated-durable-name")
            val sub = js.subscribe("re-sub", pso)

            var m = sub.nextMessage(1000)
            while (m != null) {
                m.ack()
                println(
                    "Read Message, Stream Sequence: ${
                        m.metaData().streamSequence()
                    }, Data: ${m.data.toUtf8String()}"
                )
                m = sub.nextMessage(1000)
            }
        }
    }
}
