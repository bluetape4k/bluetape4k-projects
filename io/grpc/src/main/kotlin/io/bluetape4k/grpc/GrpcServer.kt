package io.bluetape4k.grpc

import java.io.Closeable

/**
 * gRPC 서버 수명주기(start/stop/close)를 정의하는 공통 인터페이스입니다.
 *
 * ## 동작/계약
 * - [start]는 서버 구동을 시작합니다.
 * - [stop]은 서버를 중지하며 구현체 정책에 따라 graceful shutdown을 수행할 수 있습니다.
 * - [close] 기본 구현은 [stop]을 호출합니다.
 *
 * ```kotlin
 * server.start()
 * server.stop()
 * // server.isShutdown == true
 * ```
 */
interface GrpcServer: Closeable {

    val isRunning: Boolean
    val isShutdown: Boolean

    fun start()

    fun stop()

    override fun close() {
        runCatching { stop() }
    }
}
