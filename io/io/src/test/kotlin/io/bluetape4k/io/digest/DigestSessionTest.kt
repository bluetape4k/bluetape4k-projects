package io.bluetape4k.io.digest

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContentEqual
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger

class DigestSessionTest {

    private val digester = Digesters.SHA256

    @Test
    fun `incremental updates preserve byte order without changing caller arrays`() {
        val first = byteArrayOf(0x01, 0x02)
        val middle = byteArrayOf(0x2d)
        val last = byteArrayOf(0x03, 0x04)
        val expectedInput = first + middle + last
        val session = digester.newSession()

        session.update(first).update(middle[0]).update(last, 0, last.size)

        session.digest() shouldContentEqual digester.digest(expectedInput)

        val next = byteArrayOf(0x05, 0x06)
        val nextSession = digester.newSession()
        nextSession.update(next)
        next.fill(0)

        nextSession.digest() shouldContentEqual digester.digest(byteArrayOf(0x05, 0x06))
    }

    @Test
    fun `digest and digestHex reset the session for reuse`() {
        val first = "first".toByteArray(StandardCharsets.UTF_8)
        val second = "second".toByteArray(StandardCharsets.UTF_8)
        val session = digester.newSession()

        session.update(first).digest() shouldContentEqual digester.digest(first)
        session.digest() shouldContentEqual digester.digest(ByteArray(0))
        session.update(second).digest() shouldContentEqual digester.digest(second)

        session.update(first).digestHex() shouldBeEqualTo digester.digestHex(first)
        session.update(second).digestHex() shouldBeEqualTo digester.digestHex(second)
    }

    @Test
    fun `empty sessions digest the empty byte array`() {
        digester.newSession().digest() shouldContentEqual digester.digest(ByteArray(0))
        digester.newSession().digestHex() shouldBeEqualTo digester.digestHex(ByteArray(0))
    }

    @Test
    fun `invalid range propagates the JDK exception and leaves the session reusable`() {
        val input = byteArrayOf(1, 2, 3)
        val expectedFailure = assertFailsWith<RuntimeException> {
            MessageDigest.getInstance("SHA-256").update(input, -1, 1)
        }
        val session = digester.newSession()
        val actualFailure = assertFailsWith<RuntimeException> {
            session.update(input, -1, 1)
        }

        actualFailure.javaClass shouldBeEqualTo expectedFailure.javaClass
        session.digest() shouldContentEqual digester.digest(ByteArray(0))
    }

    @Test
    fun `independent sessions from the shared digester remain isolated under concurrency`() {
        val workers = 8
        val rounds = 8
        val jobCount = workers * rounds
        val sequence = AtomicInteger()

        MultithreadingTester()
            .workers(workers)
            .rounds(rounds)
            .add {
                val index = sequence.getAndIncrement()
                val input = "request-$index".toByteArray(StandardCharsets.UTF_8)
                val expected = MessageDigest.getInstance("SHA-256").digest(input)
                val boundary = input.size / 2
                val session = digester.newSession()
                    .update(input, 0, boundary)
                    .update(input, boundary, input.size - boundary)

                session.digest() shouldContentEqual expected
                digester.digest(input) shouldContentEqual expected
            }
            .run()

        sequence.get() shouldBeEqualTo jobCount
    }
}
