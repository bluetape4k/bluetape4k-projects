package io.bluetape4k.grpc

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import io.grpc.ManagedChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import java.io.Closeable
import java.util.concurrent.TimeUnit

/**
 * Base gRPC client that owns a [ManagedChannel] lifecycle.
 *
 * ## Contract
 * - The host/port constructor uses [GrpcChannelSecurity.TRANSPORT_SECURITY] by default.
 * - Plaintext channels require explicit [GrpcChannelSecurity.LOCAL_PLAINTEXT] opt-in and are limited to loopback hosts.
 * - [close] calls `shutdown + awaitTermination(5s)` while the channel is alive.
 * - Shutdown failures are swallowed after logging the shutdown attempt.
 *
 * ```kotlin
 * val channel = ManagedChannelBuilder.forAddress("api.example.com", 443)
 *     .useTransportSecurity()
 *     .build()
 * val client = object: AbstractGrpcClient(channel) {}
 * ```
 */
abstract class AbstractGrpcClient(
    protected val channel: ManagedChannel,
): Closeable {

    constructor(
        host: String = DEFAULT_HOST,
        port: Int = DEFAULT_PORT,
        channelSecurity: GrpcChannelSecurity = GrpcChannelSecurity.TRANSPORT_SECURITY,
    ): this(buildForAddress(host, port, channelSecurity))

    companion object: KLogging() {
        const val DEFAULT_HOST = "localhost"
        const val DEFAULT_PORT = 50051

        private fun buildForAddress(
            host: String,
            port: Int,
            channelSecurity: GrpcChannelSecurity,
        ): ManagedChannel =
            managedChannel(host, port) {
                applyGrpcChannelSecurity(channelSecurity, host)
                executor(Dispatchers.IO.asExecutor())
            }
    }

    override fun close() {
        if (!channel.isShutdown) {
            log.debug { "Shutdown GrpcClient channel. channel=$channel" }
            runCatching {
                channel.shutdown()
                if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn { "Channel did not terminate in time, forcing shutdownNow. channel=$channel" }
                    channel.shutdownNow()
                }
            }
        }
    }
}
