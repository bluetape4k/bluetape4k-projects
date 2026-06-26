package io.bluetape4k.grpc

import io.grpc.ManagedChannelBuilder
import java.util.Locale

/**
 * Security profile for gRPC client channels created by bluetape4k helpers.
 */
enum class GrpcChannelSecurity {
    /**
     * Uses transport security for production-facing channels.
     */
    TRANSPORT_SECURITY,

    /**
     * Uses plaintext only for local or test loopback channels.
     */
    LOCAL_PLAINTEXT,
}

private val LOCAL_PLAINTEXT_HOSTS = setOf("localhost", "127.0.0.1", "::1", "[::1]")

internal fun ManagedChannelBuilder<*>.applyGrpcChannelSecurity(
    security: GrpcChannelSecurity,
    host: String,
): ManagedChannelBuilder<*> = apply {
    when (security) {
        GrpcChannelSecurity.TRANSPORT_SECURITY -> useTransportSecurity()
        GrpcChannelSecurity.LOCAL_PLAINTEXT   -> {
            require(host.isLocalPlaintextHost()) {
                "LOCAL_PLAINTEXT is allowed only for loopback hosts. Use a ManagedChannel for custom test targets."
            }
            usePlaintext()
        }
    }
}

private fun String.isLocalPlaintextHost(): Boolean =
    trim().lowercase(Locale.ROOT) in LOCAL_PLAINTEXT_HOSTS
