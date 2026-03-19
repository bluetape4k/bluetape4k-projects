package io.nats.examples.jetstream

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.nats.AbstractNatsTest
import io.bluetape4k.nats.client.createOrReplaceStream
import io.bluetape4k.nats.client.natsMessageOf
import io.bluetape4k.support.toUtf8String
import io.nats.client.JetStream
import io.nats.client.Message
import io.nats.client.api.PublishAck
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class NatsJsPubAsync2: AbstractNatsTest() {

    companion object: KLoggingChannel() {
        private const val STREAM = "example-stream"
        private const val SUBJECT = "example-subject"
        private const val DEFAULT_MESSAGE = "hello"
        private const val DEFAULT_MSG_COUNT = 10

        private const val REPEAT_SIZE = 3
    }

    @Test
    fun `publish asynchronously`() {
        getConnection().use { nc ->
            nc.createOrReplaceStream(STREAM, SUBJECT)
            val js = nc.jetStream()

            val stop = DEFAULT_MSG_COUNT
            val ackLatch = CountDownLatch(stop - 1)
            val queue = LinkedBlockingQueue<PublishRecord>()
            val redo = LinkedBlockingQueue<PublishRecord>()

            ReceiveThread(ackLatch, queue, redo).apply { start() }
            PublishThread(js, stop, ackLatch, queue, redo).apply { start() }

            ackLatch.await()
        }
    }

    data class PublishRecord(
        val msg: Message,
        val future: CompletableFuture<PublishAck>,
    )

    class ReceiveThread(
        private val ackLatch: CountDownLatch,
        private val queue: LinkedBlockingQueue<PublishRecord>,
        private val redo: LinkedBlockingQueue<PublishRecord>,
    ): Thread() {
        override fun run() {
            while (ackLatch.count > 0) {
                val record = queue.take()

                try {
                    if (record.future.isDone) {
                        val pa = record.future.get()
                        log.debug { "Pub ack received $pa" }
                        ackLatch.countDown()
                    } else {
                        // not done yet, put it back in the queue
                        // don't count it b/c we are not done with it.
                        queue.add(record)
                    }
                } catch (e: InterruptedException) {
                    redo.add(record)
                    ackLatch.countDown()
                } catch (e: ExecutionException) {
                    redo.add(record)
                    ackLatch.countDown()
                }
            }
        }
    }

    class PublishThread(
        private val js: JetStream,
        private val stop: Int,
        private val ackLatch: CountDownLatch,
        private val queue: LinkedBlockingQueue<PublishRecord>,
        private val redo: LinkedBlockingQueue<PublishRecord>,
    ): Thread() {
        override fun run() {
            // stop-1 갯수만큼 publish 한다
            for (x in 1 until stop) {
                Thread.sleep(10)
                // make unique message data if you want more than 1 message
                val data = "$DEFAULT_MESSAGE-$x"
                val msg = natsMessageOf(SUBJECT, data)
                log.debug { "Publishing message $data on subject $SUBJECT" }

                val future = js.publishAsync(msg)

                val record = PublishRecord(msg, future)
                queue.add(record)
            }

            // consumer 에서 받기에 실패한 record 는 다시 보낸다.
            while (ackLatch.count > 0) {
                runCatching {
                    redo.poll(10, TimeUnit.MILLISECONDS)?.let { record ->
                        log.debug { "RE publishing message ${record.msg.data.toUtf8String()}" }
                        val future = js.publishAsync(record.msg)
                        queue.add(record.copy(future = future))
                    }
                }
            }
        }
    }
}
