package io.bluetape4k.grpc

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.grpc.ManagedChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import java.io.Closeable
import java.util.concurrent.TimeUnit

/**
 * [ManagedChannel] 수명주기를 관리하는 gRPC 클라이언트 베이스 클래스입니다.
 *
 * ## 동작/계약
 * - 기본 생성자는 `localhost:50051` 채널을 생성합니다.
 * - [close]는 채널이 살아 있으면 `shutdown + awaitTermination(5s)`를 수행합니다.
 * - 종료 실패 예외는 내부에서 무시됩니다.
 *
 * ```kotlin
 * client.close()
 * // channel.isShutdown == true
 * ```
 */
abstract class AbstractGrpcClient(
    protected val channel: ManagedChannel,
): Closeable {

    constructor(host: String = DEFAULT_HOST, port: Int = DEFAULT_PORT): this(buildForAddress(host, port))

    companion object: KLogging() {
        const val DEFAULT_HOST = "localhost"
        const val DEFAULT_PORT = 50051

        private fun buildForAddress(host: String, port: Int): ManagedChannel =
            managedChannel(host, port) {
                usePlaintext()
                executor(Dispatchers.IO.asExecutor())
            }
    }

    override fun close() {
        if (!channel.isShutdown) {
            log.debug { "Shutdown GrpcClient channel. channel=$channel" }
            runCatching {
                channel.shutdown()
                channel.awaitTermination(5, TimeUnit.SECONDS)
            }
        }
    }
}
