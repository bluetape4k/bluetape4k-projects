package io.bluetape4k.grpc.inprocess

import io.bluetape4k.grpc.GrpcServer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.logging.warn
import io.bluetape4k.support.ifTrue
import io.bluetape4k.support.requireInRange
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.utils.ShutdownQueue
import io.grpc.BindableService
import io.grpc.Server
import io.grpc.inprocess.InProcessServerBuilder
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.reentrantLock
import java.util.concurrent.TimeUnit
import kotlin.concurrent.withLock

/**
 * in-process gRPC 서버를 관리하는 테스트용 베이스 클래스입니다.
 *
 * ## 동작/계약
 * - [start]는 서버를 시작하고 JVM shutdown hook 큐에 종료 작업을 등록합니다.
 * - [stop]은 `shutdown + awaitTermination(5s)`를 수행합니다.
 * - 생성 시 전달한 서비스들을 서버 builder에 등록합니다.
 *
 * ```kotlin
 * server.start()
 * // server.isRunning == true
 * ```
 */
abstract class AbstractGrpcInprocessServer(
    builder: InProcessServerBuilder,
    vararg services: BindableService,
) : GrpcServer {
    constructor(name: String, vararg services: BindableService) :
        this(InProcessServerBuilder.forName(name.requireNotBlank("name")), *services)

    constructor(port: Int, vararg services: BindableService) :
        this(InProcessServerBuilder.forPort(port.requireInRange(1, 65535, "port")), *services)

    companion object : KLogging()

    private val server: Server by lazy {
        builder.apply { services.forEach { addService(it) } }.build()
    }

    private val running = atomic(false)
    private val lock = reentrantLock()

    override val isRunning: Boolean by running
    override val isShutdown: Boolean get() = server.isShutdown

    override fun start() {
        lock.withLock {
            log.debug { "Starting InProcess gRPC Server..." }
            server.start()
            log.info { "Start InProcess gRPC Server." }
            running.value = true

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
