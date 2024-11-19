package io.bluetape4k.grpc

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder

/**
 * [ManagedChannel]를 생성합니다.
 *
 * ```
 * val channel = managedChannel("localhost", 8080) {
 *    usePlaintext()
 *    executor(VirtualThreadExecutor)
 * }
 * ```
 *
 * @param host 호스트
 * @param port 포트
 * @param initializer [ManagedChannelBuilder] 초기화 람다
 * @return [ManagedChannel] 인스턴스
 */
inline fun managedChannel(
    host: String,
    port: Int,
    initializer: ManagedChannelBuilder<*>.() -> Unit,
): ManagedChannel =
    ManagedChannelBuilder
        .forAddress(host, port)
        .apply(initializer)
        .build()

/**
 * [ManagedChannel] 를 생성합니다.
 *
 * ```
 * val channel = managedChannel("localhost:8080") {
 *     usePlaintext()
 *     executor(VirtualThreadExecutor)
 * }
 * ```
 *
 * @param target 타겟 (호스트:포트)
 * @param initializer [ManagedChannelBuilder] 초기화 람다
 * @return [ManagedChannel] 인스턴스
 */
inline fun managedChannel(
    target: String,
    initializer: ManagedChannelBuilder<*>.() -> Unit,
): ManagedChannel = ManagedChannelBuilder
    .forTarget(target)
    .apply(initializer)
    .build()
