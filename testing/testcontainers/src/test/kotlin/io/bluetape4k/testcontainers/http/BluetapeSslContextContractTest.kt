package io.bluetape4k.testcontainers.http

import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

class BluetapeSslContextContractTest {

    @Test
    fun `CA 인증서 입력 스트림은 trust manager 생성 후 닫힌다`() {
        val bytes = requireNotNull(BluetapeSslContextContractTest::class.java.getResourceAsStream("/certs/rootCA.pem"))
            .use { it.readBytes() }
        val input = TrackingInputStream(bytes)

        BluetapeSslContext.createTrustManagerFrom(input)

        input.closeCount shouldBeEqualTo 1
    }

    private class TrackingInputStream(bytes: ByteArray): InputStream() {
        private val delegate = ByteArrayInputStream(bytes)
        var closeCount = 0
            private set

        override fun read(): Int = delegate.read()

        override fun close() {
            closeCount++
            delegate.close()
        }
    }
}
