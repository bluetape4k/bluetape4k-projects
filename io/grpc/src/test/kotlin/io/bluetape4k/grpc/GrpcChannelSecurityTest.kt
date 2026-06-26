package io.bluetape4k.grpc

import io.bluetape4k.assertions.assertFailsWith
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.grpc.ManagedChannelBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GrpcChannelSecurityTest {

    private val builder = mockk<ManagedChannelBuilder<*>>(relaxed = true)

    private class TestClient(
        host: String = AbstractGrpcClient.DEFAULT_HOST,
        port: Int = AbstractGrpcClient.DEFAULT_PORT,
        channelSecurity: GrpcChannelSecurity = GrpcChannelSecurity.TRANSPORT_SECURITY,
    ): AbstractGrpcClient(host, port, channelSecurity)

    @BeforeEach
    fun setUp() {
        clearMocks(builder)
        every { builder.useTransportSecurity() } returns builder
        every { builder.usePlaintext() } returns builder
    }

    @Test
    fun `transport security is the default channel security`() {
        builder.applyGrpcChannelSecurity(GrpcChannelSecurity.TRANSPORT_SECURITY, "api.example.com")

        verify(exactly = 1) { builder.useTransportSecurity() }
        verify(exactly = 0) { builder.usePlaintext() }
    }

    @Test
    fun `local plaintext opt-in applies plaintext only for loopback hosts`() {
        listOf("localhost", "127.0.0.1", "::1", "[::1]").forEach { host ->
            clearMocks(builder)
            every { builder.usePlaintext() } returns builder

            builder.applyGrpcChannelSecurity(GrpcChannelSecurity.LOCAL_PLAINTEXT, host)

            verify(exactly = 1) { builder.usePlaintext() }
            verify(exactly = 0) { builder.useTransportSecurity() }
        }
    }

    @Test
    fun `local plaintext opt-in rejects remote hosts`() {
        assertFailsWith<IllegalArgumentException> {
            builder.applyGrpcChannelSecurity(GrpcChannelSecurity.LOCAL_PLAINTEXT, "api.example.com")
        }

        verify(exactly = 0) { builder.usePlaintext() }
        verify(exactly = 0) { builder.useTransportSecurity() }
    }

    @Test
    fun `abstract client host port constructor rejects remote plaintext opt-in`() {
        assertFailsWith<IllegalArgumentException> {
            TestClient("api.example.com", 50051, GrpcChannelSecurity.LOCAL_PLAINTEXT)
        }
    }

    @Test
    fun `abstract client host port constructor allows local plaintext opt-in`() {
        val client = TestClient("localhost", 50051, GrpcChannelSecurity.LOCAL_PLAINTEXT)

        client.close()
    }
}
