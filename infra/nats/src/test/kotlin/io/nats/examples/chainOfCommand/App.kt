package io.nats.examples.chainOfCommand

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.nats.AbstractNatsTest
import io.bluetape4k.nats.client.natsMessage
import io.nats.client.impl.Headers
import org.junit.jupiter.api.Test
import java.time.Duration

class App: AbstractNatsTest() {

    companion object: KLogging()

    @Test
    fun `publish style workers 1 2`() {
        val endpoints = listOf(
            PublishStyleWorkers.WorkerA(1),
            PublishStyleWorkers.WorkerA(2),
            PublishStyleWorkers.WorkerB(1),
            PublishStyleWorkers.WorkerB(2),
            PublishStyleWorkers.WorkerB(3),
        )

        endpoints.forEach { e ->
            log.debug { "Worker ${e.endpointId} of type ${e.javaClass.simpleName} started." }
        }

        var transactionId = 10000
        val input = Input(1, 2)
        getConnection().use { nc ->
            val headers = Headers().apply {
                put("transactionId", (++transactionId).toString())
                put("aId", input.aId.toString())
                put("bId", input.bId.toString())
            }
            val message = natsMessage {
                subject("A${input.aId}.step1")
                headers(headers)
            }
            log.debug { "Publish Style starting for $input, transaction $transactionId. Publishing step 1 on A${input.aId}" }
            nc.publish(message)

            Thread.sleep(2000)
            nc.flush(Duration.ofSeconds(1))
        }
    }

    @Test
    fun `request style workers 1 2`() {
        val endpoints = listOf(
            RequestStyleWorkers.WorkerA(1),
            RequestStyleWorkers.WorkerA(2),
            RequestStyleWorkers.WorkerB(1),
            RequestStyleWorkers.WorkerB(2),
            RequestStyleWorkers.WorkerB(3),
        )

        endpoints.forEach { e ->
            log.debug { "Worker ${e.endpointId} of type ${e.javaClass.simpleName} started." }
        }

        var transactionId = 30000
        val input = Input(1, 2)

        getConnection().use { nc ->
            val headers = Headers().apply {
                put("transactionId", (++transactionId).toString())
                put("aId", input.aId.toString())
                put("bId", input.bId.toString())
            }
            val message = natsMessage {
                subject("A${input.aId}.step1")
                headers(headers)
            }
            log.debug { "Request Style starting for $input, transaction $transactionId. Publishing step 1 on A${input.aId}" }
            val response = nc.request(message, Duration.ofSeconds(2))
            log.debug { "Request Style starter received a response: $response" }

            nc.flush(Duration.ofSeconds(1))
        }
    }
}
