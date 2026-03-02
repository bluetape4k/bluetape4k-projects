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
 * in-process gRPC 채널을 사용하는 테스트용 클라이언트 베이스 클래스입니다.
 *
 * ## 동작/계약
 * - 이름 기반/주소 기반 in-process 채널 생성자를 제공합니다.
 * - [close]는 채널이 살아 있으면 `shutdown + awaitTermination(5s)`를 수행합니다.
 *
 * ```kotlin
 * client.close()
 * // channel.isShutdown == true
 * ```
 */
abstract class AbstractGrpcInprocessClient(
    protected val channel: ManagedChannel,
): Closeable {

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
            log.debug { "Close client's grpc channel... channel=$channel" }
            runCatching {
                channel.shutdown()
                channel.awaitTermination(5, TimeUnit.SECONDS)
            }
        }
    }
}
