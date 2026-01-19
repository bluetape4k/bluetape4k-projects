package io.nats.examples

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.nats.AbstractNatsTest
import io.bluetape4k.nats.client.publish
import io.bluetape4k.nats.client.request
import io.bluetape4k.support.toUtf8String
import io.nats.client.Connection
import io.nats.client.Message
import io.nats.client.impl.Headers
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

class CoreReplyRequestPatterns: AbstractNatsTest() {

    companion object: KLogging()

    @Nested
    inner class MultipleReplies {
        private val workerCount = 5

        private fun extractRequestIdFromSubject(msg: Message): String {
            val at = msg.subject.lastIndexOf(".")
            return msg.subject.substring(at + 1)
        }

        /**
         * Worker responds to requests of some work type, in this example TaskTypeA
         * There will be multiple instances of this worker and they all will respond to the same message.
         */
        inner class Worker(val nc: Connection, val id: Int): Runnable {
            override fun run() {
                val sub = nc.subscribe("Request.TaskTypeA.*")
                try {
                    val msg = sub.nextMessage(5000)  // a long wait here is simulating listening forever

                    // Do some work with that message...
                    val requestId = extractRequestIdFromSubject(msg)
                    log.debug { "${System.currentTimeMillis()}: Worker $id responding to request $requestId" }

                    // ... then publish to the replyTo, just like regular reply-request
                    nc.publish(msg.replyTo, "worker-$id worked on $requestId")
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }

        /**
         * Originator will
         *
         * - publish something that multiple workers will respond to
         * - handle the responses from the workers
         */
        inner class Originator(val nc: Connection): Runnable {

            val requesterId = Random.nextLong().toHexString()
            val latch = CountDownLatch(workerCount)

            init {
                val dispatcher = nc.createDispatcher { msg ->
                    log.debug {
                        "${System.currentTimeMillis()}: Originator received `${msg.data.toUtf8String()}` " +
                                "in response to ${extractRequestIdFromSubject(msg)}"
                    }
                    latch.countDown()
                }
                dispatcher.subscribe("Response.$requesterId.>")  // listens to all for the Responses for the requester id
            }

            override fun run() {
                // Typically some loop waiting to receive data. In the example, we publish 1 message then are finished
                var keepGoing = true
                while (keepGoing) {

                    // 1. Do whatever work you want to do
                    val taskType = "TaskTypeA"
                    val requestId = "rqst14273"

                    // 2. Publish to the task workers.
                    val subject = "Request.$taskType.$requestId"    // Request.TaskTypeA.requestId111
                    val replyTo =
                        "Response.$requesterId.$taskType.$requestId"  // Response.<requesterId>.TaskTypeA.requestId111
                    nc.publish(subject, replyTo, "this is the task data")

                    // For this example, we stop the loop by waiting once for the latch to count down or 2 seconds
                    try {
                        latch.await(2, TimeUnit.SECONDS)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                    keepGoing = false
                }
            }
        }

        @Test
        fun `multiple replies for one request`() {
            getConnection().use { nc ->
                // Start the workers first. They have to be subscribed before messages get published
                for (workerId in 1..workerCount) {
                    thread(start = true) { Worker(nc, workerId).run() }
                }

                // Start the originator and let it run
                val thread = thread(start = true) { Originator(nc).run() }

                thread.join(2000)
            }
        }
    }

    @Nested
    inner class ScalableWorkersOneReplyPerRequest {
        private val workerCount: Int = 5

        private fun extractRequestIdFromSubject(msg: Message): String {
            val at = msg.subject.lastIndexOf(".")
            return msg.subject.substring(at + 1)
        }

        /**
         * Worker responds to requests of some work type, in this example TaskTypeA
         * There will be multiple instances of this worker and they all will respond to the same message.
         */
        inner class Worker(val nc: Connection, val id: Int): Runnable {
            override fun run() {
                // queue 를 이용하여, 여러 개의 worker 들 중 하나의 worker 만 message 를 받도록 합니다.
                val sub = nc.subscribe("Request.TaskTypeA.*", "QTaskTypeA")
                try {
                    val msg = runCatching {
                        sub.nextMessage(2000) // a long wait here is simulating listening forever
                    }.getOrNull()

                    if (msg != null) {
                        // Do some work with that message...
                        val requestId = extractRequestIdFromSubject(msg)
                        log.debug { "${System.currentTimeMillis()}: Worker $id responding to request $requestId" }

                        // ... then publish to the replyTo, just like regular reply-request
                        nc.publish(msg.replyTo, "worker-$id worked on $requestId")
                    }
                } catch (e: InterruptedException) {
                    // log.warn(e) { "Thread is interrupted." }
                }
            }
        }

        /**
         * Originator will
         * 1. publish something that multiple workers will respond to
         * 2. handle the responses from the workers
         */
        inner class Originator(val nc: Connection): Runnable {
            override fun run() {
                // Typically some loop waiting to receive data. In the example, we publish 1 message then are finished
                var keepGoing = true
                while (keepGoing) {

                    // 1. Do whatever work you want to do
                    val taskType = "TaskTypeA"
                    val requestId = "rqst14273"

                    // 2. Publish to the task workers.
                    val subject = "Request.$taskType.$requestId"    // Request.TaskTypeA.requestId111


                    // For this example, we stop the loop by waiting once for the latch to count down or 2 seconds
                    try {
                        val msg = nc.request(subject, "this is the task data", timeout = 2.seconds)!!
                        log.debug { "${System.currentTimeMillis()}: Originator received `${msg.data.toUtf8String()}` in response" }
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                    keepGoing = false
                }
            }
        }

        @Test
        fun `scalable workers one reply per request`() {
            getConnection().use { nc ->
                // Start the workers first. They have to be subscribed before messages get published
                for (workerId in 1..workerCount) {
                    thread(start = true) { Worker(nc, workerId).run() }
                }

                // Start the originator and let it run
                val thread = thread(start = true) { Originator(nc).run() }

                thread.join(2000)
            }
        }
    }

    @Nested
    inner class Combo {
        private val workerACount = 5
        private val workerBCount = 3

        private fun extractTaskTypeFromSubject(msg: Message): String {
            val at = msg.subject.lastIndexOf(".")
            val temp = msg.subject.substring(0, at)
            val at2 = temp.lastIndexOf(".")
            return temp.substring(at2 + 1)
        }

        private fun extractRequestIdFromSubject(msg: Message): String {
            val at = msg.subject.lastIndexOf(".")
            return msg.subject.substring(at + 1)
        }

        /**
         * Worker responds to requests of some work type, in this example TaskTypeA
         * There will be multiple instances of this worker and they all will respond to the same message.
         */
        inner class Worker(val nc: Connection, val id: String, val taskType: String, val queue: Boolean): Runnable {
            override fun run() {
                // queue 를 이용하여, 여러 개의 worker 들 중 하나의 worker 만 message 를 받도록 합니다.
                val sub =
                    if (queue) nc.subscribe("Request.$taskType.*", "QTaskTypeA")
                    else nc.subscribe("Request.$taskType.*")

                try {
                    val msg = runCatching {
                        sub.nextMessage(2000) // a long wait here is simulating listening forever
                    }.getOrNull()

                    if (msg != null) {
                        // Do some work with that message...
                        val taskType = extractTaskTypeFromSubject(msg)
                        val requestId = extractRequestIdFromSubject(msg)
                        log.debug { "${System.currentTimeMillis()}: Worker $id responding to request $requestId for $taskType" }

                        // ... then publish to the replyTo, just like regular reply-request
                        nc.publish(msg.replyTo, "worker-$id worked on $requestId for $taskType")
                    }
                } catch (e: InterruptedException) {
                    // log.warn(e) { "Thread is interrupted." }
                }
            }
        }

        /**
         * Originator will
         *
         * 1. publish something that multiple workers will respond to
         * 2. handle the responses from the workers
         */
        inner class OriginatorA(val nc: Connection, workers: Int): Runnable {

            val requesterId = Random.nextLong().toHexString()
            val latch = CountDownLatch(workers)

            init {
                val dispatcher = nc.createDispatcher { msg ->
                    log.debug {
                        "${System.currentTimeMillis()}: Originator received `${msg.data.toUtf8String()}` " +
                                "in response to ${extractRequestIdFromSubject(msg)}"
                    }
                    latch.countDown()
                }
                dispatcher.subscribe("Response.$requesterId.>")  // listens to all for the Responses for the requester id
            }

            override fun run() {
                // Typically some loop waiting to receive data. In the example, we publish 1 message then are finished
                var keepGoing = true
                while (keepGoing) {

                    // 1. Do whatever work you want to do
                    val taskType = "TaskTypeA"
                    val requestId = "A14273"

                    // 2. Publish to the task workers.
                    val subject = "Request.$taskType.$requestId"    // Request.TaskTypeA.requestId111
                    val replyTo =
                        "Response.$requesterId.$taskType.$requestId"  // Response.<requesterId>.TaskTypeA.requestId111
                    nc.publish(subject, replyTo, "this is the task data")

                    // For this example, we stop the loop by waiting once for the latch to count down or 2 seconds
                    try {
                        latch.await(2, TimeUnit.SECONDS)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                    keepGoing = false
                }
            }
        }

        /**
         * Originator will
         *
         * 1. publish something that multiple queue workers are available to respond to
         * 2. handle the response from the workers
         */
        inner class OriginatorB(val nc: Connection): Runnable {
            override fun run() {
                // Typically some loop waiting to receive data. In the example, we publish 1 message then are finished
                var keepGoing = true
                while (keepGoing) {

                    // 1. Do whatever work you want to do
                    val taskType = "TaskTypeB"
                    val requestId = "B98765"

                    // 2. Publish to the task workers.
                    val subject = "Request.$taskType.$requestId"    // Request.TaskTypeA.requestId111


                    // For this example, we stop the loop by waiting once for the latch to count down or 2 seconds
                    try {
                        val msg = nc.request(subject, "this is the task data", timeout = 2.seconds)!!
                        log.debug { "${System.currentTimeMillis()}: Originator received `${msg.data.toUtf8String()}` in response" }
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                    keepGoing = false
                }
            }
        }

        @Test
        fun `scalable workers one reply per request`() {
            getConnection().use { nc ->
                // Start the workers first. They have to be subscribed before messages get published
                for (workerId in 1..workerACount) {
                    thread(start = true) { Worker(nc, "A$workerId", "TaskTypeA", false).run() }
                }

                for (workerId in 1..workerBCount) {
                    thread(start = true) { Worker(nc, "B$workerId", "TaskTypeB", true).run() }
                }

                // Start the originator and let it run
                val threadA = thread(start = true) { OriginatorA(nc, workerACount).run() }

                val threadB = thread(start = true) { OriginatorB(nc).run() }

                threadA.join(2000)
                threadB.join(2000)
            }
        }
    }

    /**
     * Scatter Gather. A simple version of multiple replies to one request.
     */
    @Nested
    inner class ScatterAndGather {
        @Test
        fun `scatter and gather`() {
            getConnection().use { requesterConn ->
                getConnection().use { responderConn1 ->
                    getConnection().use { responderConn2 ->
                        val d1 = responderConn1.createDispatcher { msg ->
                            log.debug { "Responder A replying to request ${msg.data.toUtf8String()} via subject `${msg.replyTo}`" }
                            val headers = Headers().put("responderId", "A")
                            responderConn1.publish(msg.replyTo, headers, msg.data)
                        }
                        d1.subscribe("scatter")

                        val d2 = responderConn2.createDispatcher { msg ->
                            log.debug { "Responder B replying to request ${msg.data.toUtf8String()} via subject `${msg.replyTo}`" }
                            val headers = Headers().put("responderId", "B")
                            responderConn2.publish(msg.replyTo, headers, msg.data)
                        }
                        d2.subscribe("scatter")

                        val latch = CountDownLatch(10)
                        val d = requesterConn.createDispatcher { msg ->
                            val mId = msg.data.toUtf8String()
                            val responderId = msg.headers.getFirst("responderId")
                            log.debug { "Response gathered for message $mId received from responderId $responderId." }
                            latch.countDown()
                        }
                        d.subscribe("gather")

                        repeat(10) {
                            log.debug { "Publish scatter request #$it" }
                            requesterConn.publish("scatter", "gather", it.toString())
                            Thread.sleep(10)
                        }
                        latch.await()
                    }
                }
            }
        }
    }
}
