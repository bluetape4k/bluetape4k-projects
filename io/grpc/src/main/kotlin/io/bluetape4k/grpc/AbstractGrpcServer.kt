package io.bluetape4k.grpc

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.logging.warn
import io.bluetape4k.support.ifTrue
import io.bluetape4k.utils.ShutdownQueue
import io.grpc.BindableService
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.ServerServiceDefinition
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.reentrantLock
import java.util.concurrent.TimeUnit
import kotlin.concurrent.withLock

/**
 * 포트 기반 gRPC 서버를 관리하는 추상 베이스 클래스입니다.
 *
 * ## 동작/계약
 * - [start]는 서버를 시작하고 JVM shutdown hook 큐에 종료 작업을 등록합니다.
 * - [stop]은 `shutdown + awaitTermination(5s)`를 수행합니다.
 * - [services]는 서버 생성 시 builder에 일괄 등록됩니다.
 *
 * ```kotlin
 * server.start()
 * // server.isRunning == true
 * ```
 */
abstract class AbstractGrpcServer(
    protected val builder: ServerBuilder<*>,
    protected val services: List<BindableService>,
): GrpcServer {
    constructor(port: Int, vararg services: BindableService):
            this(ServerBuilder.forPort(port), services.toList())

    companion object: KLogging()

    protected val server: Server by lazy { createServer() }

    private val running = atomic(false)
    private val lock = reentrantLock()

    /** 서버가 바인딩된 포트 번호입니다. */
    val port: Int get() = server.port

    /** 서버에 등록된 서비스 정의 목록입니다. */
    val serviceDefinitions: List<ServerServiceDefinition> get() = server.services

    override val isRunning: Boolean by running
    override val isShutdown: Boolean get() = server.isShutdown

    /**
     * gRPC [Server] 인스턴스를 생성합니다.
     *
     * ## 동작/계약
     * - [builder]에 [services]를 모두 등록한 뒤 `build()`를 호출합니다.
     * - 서브클래스에서 override하여 추가 설정을 적용할 수 있습니다.
     *
     * ```kotlin
     * // 서브클래스에서 override 예시
     * override fun createServer(): Server =
     *     builder.addService(myService).build()
     * ```
     */
    protected open fun createServer(): Server {
        log.debug { "Create gRPC server..." }
        return builder.apply { services.forEach { addService(it) } }.build()
    }

    override fun start() {
        lock.withLock {
            log.debug { "Starting gRPC Server..." }
            server.start()
            running.value = true
            log.info { "Start gRPC Server. port=$port, services=$serviceDefinitions" }

            ShutdownQueue.register {
                if (!isShutdown) {
                    log.debug { "Shutdown gRPC server since JVM is shutting down." }
                    stop()
                    log.info { "gRPC Server is shutdowned." }
                }
            }
        }
    }

    override fun stop() {
        lock.withLock {
            if (!isShutdown) {
                log.debug { "Shutdown gRPC server..." }
                running.value = false
                runCatching { server.shutdown() }
                runCatching { server.awaitTermination(5, TimeUnit.SECONDS) }
                    .isFailure
                    .ifTrue {
                        log.warn { "Timed out waiting for server shutdown" }
                    }
            }
        }
    }
}
