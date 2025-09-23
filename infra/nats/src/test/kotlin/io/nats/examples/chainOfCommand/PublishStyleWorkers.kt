package io.nats.examples.chainOfCommand

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.nats.client.natsMessage
import io.nats.client.Message

object PublishStyleWorkers {

    class WorkerA(id: Int): Endpoint(id, "A") {

        companion object: KLogging()

        override fun command(cmd: String, msg: Message, transactionId: String?, aId: String?, bId: String?) {
            when (cmd) {
                "step1" -> {
                    log.debug { "Worker $endpointId step 1 processing transaction $transactionId. Publishing step 1 to B$bId" }
                    val message = natsMessage {
                        subject("B$bId.step1")
                        headers(msg.headers)
                    }
                    nc.publish(message)
                }

                "step2" -> {
                    log.debug { "Worker $endpointId step 2 processing transaction $transactionId. PUblishing step 2 to B$bId" }
                    val message = natsMessage {
                        subject("B$bId.step2")
                        headers(msg.headers)
                    }
                    nc.publish(message)
                }
            }
        }
    }

    class WorkerB(id: Int): Endpoint(id, "B") {

        companion object: KLogging()

        override fun command(cmd: String, msg: Message, transactionId: String?, aId: String?, bId: String?) {
            when (cmd) {
                "step1" -> {
                    log.debug { "Worker $endpointId step 1 processing transaction $transactionId. Publishing step 1 to A$aId" }
                    val message = natsMessage {
                        subject("A$aId.step2")
                        headers(msg.headers)
                    }
                    nc.publish(message)
                }

                "step2" -> {
                    log.debug { "Worker $endpointId step 2 transaction completed." }
                }
            }
        }
    }
}
