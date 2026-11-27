package io.bluetape4k.testcontainers.storage

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OwnedRedisRuntimeTest {

    @Test
    fun `start is idempotent and close stops a started handle only once`() {
        val handle = RecordingHandle()
        val runtime = OwnedRedisRuntime { handle }

        runtime.start() shouldBeEqualTo handle.endpoint
        runtime.start() shouldBeEqualTo handle.endpoint
        handle.starts shouldBeEqualTo 1
        runtime.isRunning.shouldBeTrue()

        runtime.close()
        runtime.close()

        handle.stops shouldBeEqualTo 1
        runtime.isClosed.shouldBeTrue()
    }

    @Test
    fun `failed start rolls back a partially started handle`() {
        val handle = RecordingHandle(startFailure = IllegalStateException("readiness failed"))
        val runtime = OwnedRedisRuntime { handle }

        assertThrows<IllegalStateException> { runtime.start() }

        handle.starts shouldBeEqualTo 1
        handle.stops shouldBeEqualTo 1
        runtime.isClosed.shouldBeTrue()
    }

    @Test
    fun `endpoint failure also rolls back a started handle`() {
        val handle = RecordingHandle(endpointFailure = IllegalStateException("endpoint unavailable"))
        val runtime = OwnedRedisRuntime { handle }

        assertThrows<IllegalStateException> { runtime.start() }

        handle.starts shouldBeEqualTo 1
        handle.stops shouldBeEqualTo 1
        runtime.isClosed.shouldBeTrue()
    }

    @Test
    fun `close restores interrupt status when a handle stop is interrupted`() {
        val handle = RecordingHandle(stopFailure = InterruptedException("interrupted"))
        val runtime = OwnedRedisRuntime { handle }
        runtime.start()

        try {
            assertThrows<InterruptedException> { runtime.close() }
            Thread.currentThread().isInterrupted.shouldBeTrue()
        } finally {
            Thread.interrupted()
        }
    }

    @Test
    fun `endpoint properties restore the previous values`() {
        val hostKey = "testcontainers.redis.host"
        val portKey = "testcontainers.redis.port"
        val urlKey = "testcontainers.redis.url"
        System.setProperty(hostKey, "previous-host")
        System.clearProperty(portKey)
        System.setProperty(urlKey, "redis://previous-host:6379")

        val runtime = OwnedRedisRuntime { RecordingHandle() }
        val registration = runtime.registerSystemProperties()
        try {
            System.getProperty(hostKey) shouldBeEqualTo "localhost"
            System.getProperty(portKey) shouldBeEqualTo "6379"
            System.getProperty(urlKey) shouldBeEqualTo "redis://localhost:6379"
        } finally {
            registration.close()
            runtime.close()
        }

        System.getProperty(hostKey) shouldBeEqualTo "previous-host"
        System.getProperty(portKey) shouldBeEqualTo null
        System.getProperty(urlKey) shouldBeEqualTo "redis://previous-host:6379"
        System.clearProperty(hostKey)
        System.clearProperty(urlKey)
    }

    private class RecordingHandle(
        private val startFailure: Throwable? = null,
        private val endpointFailure: Throwable? = null,
        private val stopFailure: Throwable? = null,
    ): RedisContainerHandle {

        val endpoint = RedisEndpoint("localhost", 6379, "redis://localhost:6379")
        var starts: Int = 0
        var stops: Int = 0

        override fun start() {
            starts++
            startFailure?.let { throw it }
        }

        override fun stop() {
            stops++
            stopFailure?.let { throw it }
        }

        override fun endpoint(): RedisEndpoint {
            endpointFailure?.let { throw it }
            return endpoint
        }
    }
}
