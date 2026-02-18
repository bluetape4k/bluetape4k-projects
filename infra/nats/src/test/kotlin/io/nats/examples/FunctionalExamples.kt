package io.nats.examples

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.nats.AbstractNatsTest
import io.bluetape4k.nats.client.natsOptions
import io.bluetape4k.nats.client.publish
import io.bluetape4k.support.toUtf8String
import io.nats.client.Connection
import io.nats.client.NUID
import io.nats.client.Nats
import kotlinx.coroutines.future.await
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.CountDownLatch

class FunctionalExamples: AbstractNatsTest() {

    companion object: KLogging()

    @Test
    fun `auto unsubscribe`() {
        getConnection().use { nc ->
            val dispatcher = nc.createDispatcher { msg ->
                log.debug { "data=${msg.data.toUtf8String()}" }
            }

            val subscription = nc.subscribe("updates")
            subscription.unsubscribe(1) // 1 message

            dispatcher.subscribe("updates").unsubscribe("updates", 1)
        }
    }

    @Test
    fun `connect control 2k`() {
        val options = natsOptions {
            server(nats.url)
            maxControlLine(2 * 1024)
        }
        Nats.connect(options).use { nc ->
            nc.shouldNotBeNull()
            log.debug { "status=${nc.status}" }
            nc.status shouldBeEqualTo Connection.Status.CONNECTED
        }
    }

    @Test
    fun `connect max ping five`() {
        val options = natsOptions {
            server(nats.url)
            maxPingsOut(5) // Set max pings in flight
        }
        Nats.connect(options).use { nc ->
            nc.shouldNotBeNull()
            log.debug { "status=${nc.status}" }
            nc.status shouldBeEqualTo Connection.Status.CONNECTED
        }
    }

    @Test
    fun `connect with connectionName`() {
        val options = natsOptions {
            server(nats.url)
            connectionName("my-connection")
        }
        Nats.connect(options).use { nc ->
            nc.shouldNotBeNull()
            log.debug { "status=${nc.status}" }
            nc.status shouldBeEqualTo Connection.Status.CONNECTED
        }
    }

    @Test
    fun `connect with pedantic mode`() {
        val options = natsOptions {
            server(nats.url)
            pedantic()
        }
        Nats.connect(options).use { nc ->
            nc.shouldNotBeNull()
            log.debug { "status=${nc.status}" }
            nc.status shouldBeEqualTo Connection.Status.CONNECTED
        }
    }

    @Test
    fun `connect with token`() {
        val options = natsOptions {
            server(nats.url)
            token("mytoken".toCharArray())
        }
        Nats.connect(options).use { nc ->
            nc.shouldNotBeNull()
            log.debug { "status=${nc.status}" }
            nc.status shouldBeEqualTo Connection.Status.CONNECTED
        }
    }

    @Test
    fun `connect to multiple servers`() {
        val options = natsOptions {
            server(nats.url)
            server(natsDefault.url)
        }
        Nats.connect(options).use { nc ->
            nc.shouldNotBeNull()
            log.debug { "status=${nc.status}" }
            nc.status shouldBeEqualTo Connection.Status.CONNECTED
        }
    }

    @Test
    fun `connect with user password`() {
        val options = natsOptions {
            server(nats.url)
            userInfo("myname", "password")
        }
        Nats.connect(options).use { nc ->
            nc.shouldNotBeNull()
            log.debug { "status=${nc.status}" }
            nc.status shouldBeEqualTo Connection.Status.CONNECTED
        }
    }

    @Test
    fun `connect with verbose`() {
        val options = natsOptions {
            server(nats.url)
            verbose()
            connectionTimeout(Duration.ofSeconds(10))
        }
        Nats.connect(options).use { nc ->
            nc.shouldNotBeNull()
            log.debug { "status=${nc.status}" }
            nc.status shouldBeEqualTo Connection.Status.CONNECTED
        }
    }

    @Test
    fun `drain connection`() = runTest {
        val nc = getConnection()
        val latch = CountDownLatch(1)

        val dispatcher = nc.createDispatcher { msg ->
            log.debug { "msg.data=${msg.data.toUtf8String()}" }
            latch.countDown()
        }
        dispatcher.subscribe("updates")
        nc.publish("updates", "body")
        latch.await()

        // Drain the connection, which will close it
        val drained = nc.drain(Duration.ofSeconds(10)).await()
        drained.shouldBeTrue()
    }

    @Test
    fun `drain subscription`() = runTest {
        getConnection().use { nc ->
            val latch = CountDownLatch(1)

            val dispatcher = nc.createDispatcher { msg ->
                log.debug { "msg.data=${msg.data.toUtf8String()}" }
                latch.countDown()
            }
            dispatcher.subscribe("updates")
            nc.publish("updates", "body")
            latch.await()

            // Drain the connection, which will close it
            val drained = nc.drain(Duration.ofSeconds(10)).await()
            drained.shouldBeTrue()
        }
    }

    @Test
    fun `flush publish queue`() {
        getConnection().use { nc ->
            nc.publish("updates", "All is Well")
            nc.flush(Duration.ofSeconds(1))
        }
    }

    @Test
    fun `get max payload`() {
        getConnection().use { nc ->
            log.debug { "max payload=${nc.maxPayload}" }
        }
    }

    @Test
    fun `publish for wildcards`() {
        getConnection().use { nc ->

            val latch = CountDownLatch(4)

            val dispatcher = nc.createDispatcher { msg ->
                log.debug { "msg.data=${msg.data.toUtf8String()}" }
                latch.countDown()
            }
            dispatcher.subscribe("time.>")

            val zoneId = ZoneId.of("Asia/Seoul")
            val zonedDateTime = ZonedDateTime.ofInstant(Instant.now(), zoneId)
            val formatted = zonedDateTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)

            nc.publish("time.kr", formatted)
            nc.publish("time.kr.seoul", formatted)

            val zoneId2 = ZoneId.of("America/New_York")
            val zonedDateTime2 = ZonedDateTime.ofInstant(Instant.now(), zoneId2)
            val formatted2 = zonedDateTime2.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)

            nc.publish("time.us.east", formatted2)
            nc.publish("time.us.east.atlanta", formatted2)

            nc.flush(Duration.ZERO)

            latch.await()
        }
    }

    @Test
    fun `publish and reply`() = runTest {
        getConnection().use { nc ->
            val dispatcher = nc.createDispatcher { msg ->
                log.debug { "msg.data=${msg.data.toUtf8String()}, replyTo=${msg.replyTo}" }
                val response = "reply to ${msg.data.toUtf8String()}"
                nc.publish(msg.replyTo, response)
            }
            dispatcher.subscribe("time")

            val uniqueReplyTo = NUID.nextGlobal()
            val reply = nc.subscribe(uniqueReplyTo)
            reply.unsubscribe(1)

            nc.publish("time", uniqueReplyTo, "my message")

            val msg = reply.nextMessage(Duration.ofSeconds(1))

            log.debug { "msg=${msg?.data?.toUtf8String()}" }
            msg.shouldNotBeNull()
            msg.data.toUtf8String() shouldContain "reply to"
        }
    }

    @Test
    fun `reconnect 5 MB`() {
        val options = natsOptions {
            server(nats.url)
            reconnectBufferSize(5 * 1024 * 1024)
            reconnectWait(Duration.ofSeconds(10))   // Set Reconnect Wait
            maxReconnects(10)   // Set max reconnect attempts
        }
        Nats.connect(options).use { nc ->
            nc.shouldNotBeNull()
            log.debug { "status=${nc.status}" }
            nc.status shouldBeEqualTo Connection.Status.CONNECTED
        }
    }

    @Test
    fun `Subscribe with Queue`() = runTest {
        getConnection().use { nc ->
            val latch = CountDownLatch(10)

            val dispatcher = nc.createDispatcher { msg ->
                val str = msg.data.toUtf8String()
                log.debug { "Received: $str" }
                latch.countDown()
            }
            dispatcher.subscribe("updates", "workers")

            List(10) {
                nc.publish("updates", "message $it")
            }

            latch.await()
        }
    }

    @Test
    fun `Subscribe with star`() = runTest {
        getConnection().use { nc ->
            val latch = CountDownLatch(10)
            val received = mutableListOf<String>()
            val dispatcher = nc.createDispatcher { msg ->
                val str = msg.data.toUtf8String()
                received.add(str)
                log.debug { "Received: $str" }
                latch.countDown()
            }
            dispatcher.subscribe("time.*.east")

            repeat(10) {
                nc.publish("time.us", "us message $it")
            }

            repeat(10) {
                nc.publish("time.us.east", "us.east message $it")
            }

            latch.await()
            received.all { it.contains("us.east") }.shouldBeTrue()
        }
    }

    @Test
    fun `Subscribe Sync`() {
        getConnection().use { nc ->
            val subscription = nc.subscribe("updates")

            List(10) {
                nc.publish("updates", "message $it.")
            }
            repeat(10) {
                val msg = subscription.nextMessage(Duration.ZERO)
                log.debug { "Received: ${msg.data.toUtf8String()}" }
            }
        }
    }
}
