package io.bluetape4k.mockserver.config

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

class HttpsConfigurationContractTest {

    @Test
    fun `키 저장소 입력 스트림은 복사 후 닫힌다`() {
        val input = TrackingInputStream()
        val output = ByteArrayOutputStream()

        copyHttpsKeyStore(input, output)

        input.closeCount shouldBeEqualTo 1
        output.toByteArray() shouldBeEqualTo PAYLOAD
    }

    @Test
    fun `키 저장소 복사에 실패해도 입력 스트림은 닫힌다`() {
        val input = TrackingInputStream(failOnRead = true)

        assertFailsWith<IOException> {
            copyHttpsKeyStore(input, ByteArrayOutputStream())
        }

        input.closeCount shouldBeEqualTo 1
    }

    private class TrackingInputStream(failOnRead: Boolean = false): InputStream() {
        private val delegate = ByteArrayInputStream(PAYLOAD)
        private val failOnRead = failOnRead
        var closeCount = 0
            private set

        override fun read(): Int {
            if (failOnRead) throw IOException("read failed")
            return delegate.read()
        }

        override fun close() {
            closeCount++
            delegate.close()
        }
    }

    private companion object {
        val PAYLOAD = "p12-payload".toByteArray()
    }
}
