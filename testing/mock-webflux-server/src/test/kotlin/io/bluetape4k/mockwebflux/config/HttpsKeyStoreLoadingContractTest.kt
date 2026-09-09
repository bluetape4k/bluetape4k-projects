package io.bluetape4k.mockwebflux.config

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

class HttpsKeyStoreLoadingContractTest {

    @Test
    fun `키 저장소 입력 스트림은 로드 후 닫힌다`() {
        val bytes = requireNotNull(HttpsKeyStoreLoadingContractTest::class.java.getResourceAsStream("/certs/localhost.p12"))
            .use { it.readBytes() }
        val input = TrackingInputStream(bytes)

        loadHttpsKeyStore(input, "changeit")

        input.closeCount shouldBeEqualTo 1
    }

    @Test
    fun `키 저장소 로드에 실패해도 입력 스트림은 닫힌다`() {
        val input = TrackingInputStream(failOnRead = true)

        assertFailsWith<IOException> {
            loadHttpsKeyStore(input, "changeit")
        }

        input.closeCount shouldBeEqualTo 1
    }

    private class TrackingInputStream(
        bytes: ByteArray = ByteArray(0),
        private val failOnRead: Boolean = false,
    ): InputStream() {
        private val delegate = ByteArrayInputStream(bytes)
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
}
