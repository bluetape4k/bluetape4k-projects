package io.nats.examples.chainOfCommand

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.nats.client.natsMessage
import io.nats.client.Message
import java.time.Duration

object RequestStyleWorkers {

    class WorkerA(id: Int): Endpoint(id, "A") {

        companion object: KLogging()

        override fun command(cmd: String, msg: Message, transactionId: String, aId: String, bId: String) {
            when (cmd) {
                "step1" -> {
                    log.debug { "Worker $endpointId step 1 processing transaction $transactionId" }

                    log.debug { "Worker $endpointId replying to step 1 request" }
                    nc.publish(msg.replyTo, msg.data)
                    Thread.sleep(250)

                    log.debug { "Worker $endpointId requesting step 1 to B$bId" }
                    val message = natsMessage {
                        subject("B$bId.step1")
                        headers(msg.headers)
                    }
                    val response = nc.request(message, Duration.ofSeconds(2))
                    log.debug { "Worker $endpointId, step 1 received a response: $response" }
                }

                "step2" -> {
                    log.debug { "Worker $endpointId step 2 processing transaction $transactionId" }

                    log.debug { "Worker $endpointId replying to step 2 request" }
                    nc.publish(msg.replyTo, msg.data)
                    Thread.sleep(250)

                    log.debug { "Worker $endpointId requesting step 2 to B$bId" }
                    val message = natsMessage {
                        subject("B$bId.step2")
                        headers(msg.headers)
                    }
                    val response = nc.request(message, Duration.ofSeconds(2))
                    log.debug { "Worker $endpointId, step 2 received a response: $response" }
                }
            }
        }
    }

    class WorkerB(id: Int): Endpoint(id, "B") {

        companion object: KLogging()

        override fun command(cmd: String, msg: Message, transactionId: String, aId: String, bId: String) {
            when (cmd) {
                "step1" -> {
                    log.debug { "Worker $endpointId step 1 processing transaction $transactionId" }

                    log.debug { "Worker $endpointId replying to step 1 request" }
                    nc.publish(msg.replyTo, msg.data)
                    Thread.sleep(250)

                    log.debug { "Worker $endpointId requesting step 2 to A$aId" }
                    val message = natsMessage {
                        subject("A$aId.step2")
                        headers(msg.headers)
                    }
                    val response = nc.request(message, Duration.ofSeconds(2))
                    log.debug { "Worker $endpointId, step 1 received a response: $response" }
                }

                "step2" -> {
                    log.debug { "Worker $endpointId step 2 processing transaction $transactionId" }

                    log.debug { "Worker $endpointId replying to step 2 request" }
                    nc.publish(msg.replyTo, msg.data)
                    Thread.sleep(250)

                    log.debug { "Worker $endpointId step 2 transaction completed." }
                }
            }
        }
    }
}
