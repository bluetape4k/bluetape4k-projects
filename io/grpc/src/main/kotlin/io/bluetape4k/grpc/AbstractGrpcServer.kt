package io.bluetape4k.grpc

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.logging.warn
import io.bluetape4k.support.ifFalse
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
 * gRPC 서비스를 제공해주는 Server의 최상위 추상화 클래스입니다.
 *
 * @property builder  builder of grpc server
 * @property services collection of grpc services
 */
abstract class AbstractGrpcServer(
    protected val builder: ServerBuilder<*>,
    protected val services: List<BindableService>,
): GrpcServer {

    constructor(port: Int, vararg services: BindableService)
            : this(ServerBuilder.forPort(port), services.toList())

    companion object: KLogging()

    protected val server: Server by lazy { createServer() }

    private val running = atomic(false)
    private val lock = reentrantLock()

    val port: Int get() = server.port
    val serviceDefinitions: List<ServerServiceDefinition> get() = server.services

    override val isRunning: Boolean by running
    override val isShutdown: Boolean get() = server.isShutdown

    protected open fun createServer(): Server {
        log.debug { "Create gRPC server..." }
        return builder.apply { services.forEach { addService(it) } }.build()
    }

    /**
     * gRPC/Protobuf 처리에서 `start` 함수를 제공합니다.
     */
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

    /**
     * gRPC/Protobuf 처리에서 `stop` 함수를 제공합니다.
     */
    override fun stop() {
        lock.withLock {
            if (!isShutdown) {
                log.debug { "Shutdown gRPC server..." }
                runCatching {
                    running.value = false
                    server.shutdown()
                        .awaitTermination(5, TimeUnit.SECONDS)
                        .ifFalse {
                            log.warn { "Timed out waiting for server shutdown" }
                        }
                }
            }
        }
    }
}
