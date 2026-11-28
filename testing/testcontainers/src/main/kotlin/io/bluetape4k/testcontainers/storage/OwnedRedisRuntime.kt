package io.bluetape4k.testcontainers.storage

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import io.bluetape4k.support.requireNotBlank
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/** 명시적으로 시작·종료할 Redis endpoint의 불변 연결 정보입니다. */
data class RedisEndpoint(
    val host: String,
    val port: Int,
    val url: String,
) {

    companion object {
        private const val MIN_PORT = 1
        private const val MAX_PORT = 65_535
    }

    init {
        host.requireNotBlank("host")
        require(port in MIN_PORT..MAX_PORT) { "port must be between $MIN_PORT and $MAX_PORT" }
        url.requireNotBlank("url")
    }

    /** 기존 `testcontainers.{namespace}.host/port/url` property bridge와 호환되는 값입니다. */
    fun properties(namespace: String = RedisServer.NAME): Map<String, String> {
        namespace.requireNotBlank("namespace")
        return linkedMapOf(
            "testcontainers.$namespace.host" to host,
            "testcontainers.$namespace.port" to port.toString(),
            "testcontainers.$namespace.url" to url,
        )
    }
}

/** Testcontainers 또는 다른 Redis 실행기를 감싸는 최소 lifecycle SPI입니다. */
interface RedisContainerHandle {

    /** 실행기를 시작합니다. */
    fun start()

    /** 실행기와 그 하위 자원을 종료합니다. */
    fun stop()

    /** 시작된 실행기의 연결 endpoint를 반환합니다. */
    fun endpoint(): RedisEndpoint
}

/** Redis 실행기의 명시적 소유권과 종료 상태를 정의하는 계약입니다. */
interface RedisRuntime: AutoCloseable {

    /** 실행기를 시작하고 연결 endpoint를 반환합니다. 이미 시작된 경우 같은 endpoint를 반환합니다. */
    fun start(): RedisEndpoint

    /** 실행기가 시작되어 연결 가능한 상태인지 나타냅니다. */
    val isRunning: Boolean

    /** 실행기가 더 이상 시작될 수 없는 종료 상태인지 나타냅니다. */
    val isClosed: Boolean
}

/**
 * Redis handle의 시작·종료 소유권을 호출자에게 명시적으로 부여합니다.
 *
 * 생성자는 실행하지 않으며, `start()` 중 실패하면 이미 생성·시작된 handle을 즉시
 * 정리합니다. `close()`는 한 번만 handle을 종료하고, 종료 중 interrupt가 발생하면
 * 원래 예외를 전달하면서 현재 스레드의 interrupt 상태를 복원합니다.
 */
class OwnedRedisRuntime(
    private val factory: () -> RedisContainerHandle,
): RedisRuntime {

    companion object: KLogging()

    private enum class State { NEW, STARTED, CLOSED }

    private val lock = ReentrantLock()
    private var state: State = State.NEW
    private var activeHandle: RedisContainerHandle? = null
    private var activeEndpoint: RedisEndpoint? = null

    override val isRunning: Boolean
        get() = lock.withLock { state == State.STARTED }

    override val isClosed: Boolean
        get() = lock.withLock { state == State.CLOSED }

    @Suppress("TooGenericExceptionCaught")
    override fun start(): RedisEndpoint = lock.withLock {
        when (state) {
            State.STARTED -> return@withLock requireNotNull(activeEndpoint)
            State.CLOSED -> error("Redis runtime is already closed")
            State.NEW -> Unit
        }

        log.debug { "Starting owned Redis runtime" }
        val handle = try {
            factory()
        } catch (failure: Throwable) {
            state = State.CLOSED
            log.warn(failure) { "Redis runtime factory failed" }
            throw failure
        }

        try {
            handle.start()
            val endpoint = handle.endpoint()
            activeHandle = handle
            activeEndpoint = endpoint
            state = State.STARTED
            log.debug { "Owned Redis runtime started" }
            endpoint
        } catch (failure: Throwable) {
            state = State.CLOSED
            rollback(handle, failure)
            throw failure
        }
    }

    /** 현재 endpoint를 시스템 프로퍼티로 등록하고 이전 값을 복원하는 handle을 반환합니다. */
    fun registerSystemProperties(namespace: String = RedisServer.NAME): AutoCloseable {
        val properties = start().properties(namespace)
        val previous = properties.mapValues { (key, _) -> System.getProperty(key) }
        properties.forEach { (key, value) -> System.setProperty(key, value) }

        return AutoCloseable {
            previous.forEach { (key, value) ->
                if (value == null) System.clearProperty(key) else System.setProperty(key, value)
            }
        }
    }

    override fun close() {
        val handle = lock.withLock {
            when (state) {
                State.NEW -> {
                    state = State.CLOSED
                    null
                }

                State.STARTED -> {
                    state = State.CLOSED
                    activeHandle.also {
                        activeHandle = null
                        activeEndpoint = null
                    }
                }

                State.CLOSED -> null
            }
        } ?: return

        log.debug { "Closing owned Redis runtime" }
        try {
            handle.stop()
        } catch (interrupted: InterruptedException) {
            Thread.currentThread().interrupt()
            throw interrupted
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun rollback(handle: RedisContainerHandle, failure: Throwable) {
        try {
            handle.stop()
        } catch (interrupted: InterruptedException) {
            Thread.currentThread().interrupt()
            failure.addSuppressed(interrupted)
        } catch (rollbackFailure: Throwable) {
            log.warn(rollbackFailure) { "Redis runtime rollback failed" }
            failure.addSuppressed(rollbackFailure)
        }
    }
}
