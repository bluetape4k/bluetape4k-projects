package io.bluetape4k.grpc

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.grpc.examples.helloworld.GreeterService
import io.bluetape4k.grpc.inprocess.AbstractGrpcInprocessServer
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.inprocess.InProcessServerBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GrpcServerTest: AbstractGrpcTest() {

    @Test
    fun `port server starts stops and lets a replacement reuse the bound port`() {
        val server = PortLifecycleServer(0)
        var boundPort = 0

        try {
            server.start()
            server.isRunning.shouldBeTrue()
            server.isShutdown.shouldBeFalse()
            server.serviceDefinitions.size shouldBeEqualTo 1
            boundPort = server.port
            (boundPort > 0).shouldBeTrue()
        } finally {
            server.stop()
        }
        server.isRunning.shouldBeFalse()
        server.isShutdown.shouldBeTrue()

        val replacement = PortLifecycleServer(boundPort)
        try {
            replacement.start()
            replacement.isRunning.shouldBeTrue()
            replacement.port shouldBeEqualTo boundPort
        } finally {
            replacement.stop()
        }
    }

    @Test
    fun `port conflict fails during start and leaves the second server stopped`() {
        val first = PortLifecycleServer(0)
        try {
            first.start()

            val second = PortLifecycleServer(first.port)
            try {
                assertFailsWith<IOException> {
                    second.start()
                }
                second.isRunning.shouldBeFalse()
            } finally {
                second.stop()
            }
        } finally {
            first.stop()
        }
    }

    @Test
    fun `port server stop uses graceful shutdown when termination completes`() {
        val recordingServer = RecordingServer(terminatesGracefully = true)
        val server = RecordingPortLifecycleServer(recordingServer)

        server.start()
        server.stop()

        server.isRunning.shouldBeFalse()
        server.isShutdown.shouldBeTrue()
        recordingServer.shutdownCalls shouldBeEqualTo 1
        recordingServer.awaitTimedCalls shouldBeEqualTo 1
        recordingServer.shutdownNowCalls shouldBeEqualTo 0
    }

    @Test
    fun `port server stop forces shutdown when graceful termination times out`() {
        val recordingServer = RecordingServer(terminatesGracefully = false)
        val server = RecordingPortLifecycleServer(recordingServer)

        server.start()
        server.stop()

        server.isRunning.shouldBeFalse()
        server.isShutdown.shouldBeTrue()
        recordingServer.shutdownCalls shouldBeEqualTo 1
        recordingServer.awaitTimedCalls shouldBeEqualTo 2
        recordingServer.shutdownNowCalls shouldBeEqualTo 1
    }

    @Test
    fun `port server stop restores interrupt status when await termination is interrupted`() {
        val recordingServer = RecordingServer(terminatesGracefully = false, interruptOnAwait = true)
        val server = RecordingPortLifecycleServer(recordingServer)

        try {
            server.start()
            server.stop()

            server.isRunning.shouldBeFalse()
            server.isShutdown.shouldBeTrue()
            recordingServer.shutdownCalls shouldBeEqualTo 1
            recordingServer.awaitTimedCalls shouldBeEqualTo 1
            recordingServer.shutdownNowCalls shouldBeEqualTo 1
            Thread.currentThread().isInterrupted.shouldBeTrue()
        } finally {
            Thread.interrupted()
        }
    }

    @Test
    fun `close delegates to stop for port servers`() {
        val recordingServer = RecordingServer(terminatesGracefully = true)
        val server = RecordingPortLifecycleServer(recordingServer)

        server.start()
        server.close()

        server.isRunning.shouldBeFalse()
        server.isShutdown.shouldBeTrue()
        recordingServer.shutdownCalls shouldBeEqualTo 1
    }

    @Test
    fun `inprocess server starts stops and lets a replacement reuse the name`() {
        val name = inprocessName()
        val server = InprocessLifecycleServer(name)

        try {
            server.start()
            server.isRunning.shouldBeTrue()
            server.isShutdown.shouldBeFalse()
        } finally {
            server.stop()
        }
        server.isRunning.shouldBeFalse()
        server.isShutdown.shouldBeTrue()

        val replacement = InprocessLifecycleServer(name)
        try {
            replacement.start()
            replacement.isRunning.shouldBeTrue()
        } finally {
            replacement.stop()
        }
    }

    @Test
    fun `inprocess name conflict fails during start and leaves the second server stopped`() {
        val name = inprocessName()
        val first = InprocessLifecycleServer(name)
        try {
            first.start()

            val second = InprocessLifecycleServer(name)
            try {
                assertFailsWith<IOException> {
                    second.start()
                }
                second.isRunning.shouldBeFalse()
            } finally {
                second.stop()
            }
        } finally {
            first.stop()
        }
    }

    @Test
    fun `inprocess server stop forces shutdown when graceful termination times out`() {
        val recordingServer = RecordingServer(terminatesGracefully = false)
        val server = RecordingInprocessLifecycleServer(recordingServer)

        server.start()
        server.stop()

        server.isRunning.shouldBeFalse()
        server.isShutdown.shouldBeTrue()
        recordingServer.shutdownCalls shouldBeEqualTo 1
        recordingServer.awaitTimedCalls shouldBeEqualTo 2
        recordingServer.shutdownNowCalls shouldBeEqualTo 1
    }

    @Test
    fun `inprocess server stop restores interrupt status when await termination is interrupted`() {
        val recordingServer = RecordingServer(terminatesGracefully = false, interruptOnAwait = true)
        val server = RecordingInprocessLifecycleServer(recordingServer)

        try {
            server.start()
            server.stop()

            server.isRunning.shouldBeFalse()
            server.isShutdown.shouldBeTrue()
            recordingServer.shutdownCalls shouldBeEqualTo 1
            recordingServer.awaitTimedCalls shouldBeEqualTo 1
            recordingServer.shutdownNowCalls shouldBeEqualTo 1
            Thread.currentThread().isInterrupted.shouldBeTrue()
        } finally {
            Thread.interrupted()
        }
    }

    private fun inprocessName(): String =
        "grpc-server-test-${UUID.randomUUID()}"

    private class PortLifecycleServer(port: Int): AbstractGrpcServer(port, GreeterService())

    private class RecordingPortLifecycleServer(
        private val recordingServer: RecordingServer,
    ): AbstractGrpcServer(ServerBuilder.forPort(1), emptyList()) {

        override fun createServer(): Server =
            recordingServer
    }

    private class InprocessLifecycleServer(name: String): AbstractGrpcInprocessServer(name, GreeterService())

    private class RecordingInprocessLifecycleServer(
        private val recordingServer: RecordingServer,
    ): AbstractGrpcInprocessServer(InProcessServerBuilder.forName(inprocessName())) {

        override fun createServer(): Server =
            recordingServer

        private companion object {
            fun inprocessName(): String =
                "recording-inprocess-${UUID.randomUUID()}"
        }
    }

    private class RecordingServer(
        private val terminatesGracefully: Boolean,
        private val interruptOnAwait: Boolean = false,
    ): Server() {
        var shutdownCalls = 0
            private set
        var shutdownNowCalls = 0
            private set
        var awaitTimedCalls = 0
            private set

        private var started = false
        private var shutdown = false
        private var terminated = false

        override fun start(): Server {
            started = true
            return this
        }

        override fun shutdown(): Server {
            shutdownCalls++
            shutdown = true
            terminated = terminatesGracefully
            return this
        }

        override fun shutdownNow(): Server {
            shutdownNowCalls++
            shutdown = true
            terminated = true
            return this
        }

        override fun isShutdown(): Boolean =
            shutdown

        override fun isTerminated(): Boolean =
            terminated

        override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean {
            awaitTimedCalls++
            if (interruptOnAwait) {
                throw InterruptedException("interrupted during graceful shutdown")
            }
            return terminated
        }

        override fun awaitTermination() {
            terminated = true
        }

        override fun getPort(): Int =
            if (started) 1 else -1
    }
}
