package io.bluetape4k.grpc

import java.io.Closeable

/**
 * Server for gRPC Service
 */
interface GrpcServer: Closeable {

    val isRunning: Boolean
    val isShutdown: Boolean

    /**
     * Start gRPC Server
     */
    fun start()

    /**
     * Stop gRPC Server
     */
    fun stop()

    /**
     * Close gRPC Server
     */
    override fun close() {
        runCatching { stop() }
    }
}
