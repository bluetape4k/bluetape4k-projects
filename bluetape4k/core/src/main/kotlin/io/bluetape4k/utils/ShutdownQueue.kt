package io.bluetape4k.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.support.closeSafe
import java.util.*

/**
 * JVM 종료 시 자동으로 정리할 객체를 관리하는 object 입니다.
 *
 * ```
 * ShutdownQueue.register(
 *    object:AutoCloseable {
 *        override fun close() {
 *          // 자원 정리 코드
 *        }
 *    }
 * )
 * // or
 *
 * // 전역적으로 redisServer를 사용하고, JVM 종료 시 자동으로 종료하도록 설정
 * val redisServer = RedisServer().apply { start() }
 * ShutdownQueue.register(redisServer)
 * ```
 */
object ShutdownQueue: KLogging() {

    private val closeables = Stack<AutoCloseable>()

    init {
        Runtimex.addShutdownHook {
            while (closeables.isNotEmpty()) {
                closeables.pop()?.let { closeable ->
                    log.debug { "Closing AutoCloseable instance ... $closeable" }
                    closeable.closeSafe()
                    log.info { "Success to close AutoCloseable instance ... $closeable" }
                }
            }
        }
    }

    /**
     * JVM 종료 시 자동으로 정리할 객체를 등록합니다.
     */
    fun register(closeable: AutoCloseable) {
        if (!closeables.contains(closeable)) {
            log.debug { "JVM Shutdown 시 자동 정리할 객체를 등록합니다. $closeable" }
            closeables.push(closeable)
        }
    }
}
