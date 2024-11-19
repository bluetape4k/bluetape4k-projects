package io.bluetape4k.grpc.inprocess

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.grpc.ManagedChannel
import io.grpc.inprocess.InProcessChannelBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import java.io.Closeable
import java.util.concurrent.TimeUnit

/**
 * 테스트를 위해 gRPC Inprocess server를 사용하는 Client 클래스입니다.
 *
 * @property channel gRPC Channel instance.
 */
abstract class AbstractGrpcInprocessClient(protected val channel: ManagedChannel): Closeable {

    constructor(name: String): this(buildChannelByName(name))
    constructor(host: String, port: Int): this(buildChannelByAddress(host, port))

    companion object: KLogging() {
        @JvmStatic
        private fun buildChannelByName(name: String): ManagedChannel {
            return InProcessChannelBuilder
                .forName(name)
                .usePlaintext()
                .executor(Dispatchers.IO.asExecutor())
                .build()
        }

        @JvmStatic
        private fun buildChannelByAddress(host: String, port: Int): ManagedChannel {
            return InProcessChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .executor(Dispatchers.IO.asExecutor())
                .build()
        }
    }

    override fun close() {
        if (!channel.isShutdown) {
            log.debug { "Close client's grpc channel..." }
            runCatching {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
            }
        }
    }
}
