package io.nats.examples.chainOfCommand

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.nats.AbstractNatsTest
import io.nats.client.Connection
import io.nats.client.Dispatcher
import io.nats.client.Message
import io.nats.client.Nats
import java.time.Duration

abstract class Endpoint(
    id: Int,
    type: String,
): AutoCloseable {

    companion object: KLogging()

    val endpointId = type + id
    val nc: Connection = Nats.connect(AbstractNatsTest.nats.url)
    val dispatcher: Dispatcher

    init {
        dispatcher = nc.createDispatcher(::handle)
        dispatcher.subscribe("$endpointId.>")
        nc.flush(Duration.ofSeconds(1))
    }

    private fun handle(msg: Message) {
        log.debug { "Handle message. subject=${msg.subject}" }
        val cmd = msg.subject.split(".")[1]
        val transactionId = msg.headers.getFirst("transactionId")
        val aId = msg.headers.getFirst("aId")
        val bId = msg.headers.getFirst("bId")

        log.debug { "Worker $endpointId received a message on subject: ${msg.subject} command is: $cmd" }
        command(cmd, msg, transactionId, aId, bId)
    }

    protected abstract fun command(cmd: String, msg: Message, transactionId: String?, aId: String?, bId: String?)

    override fun close() {
        nc.close()
    }
}
