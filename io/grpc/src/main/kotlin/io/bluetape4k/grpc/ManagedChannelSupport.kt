package io.bluetape4k.grpc

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder

/**
 * host/port 기반 [ManagedChannel]을 생성합니다.
 *
 * ## 동작/계약
 * - `ManagedChannelBuilder.forAddress(host, port)` 경로를 사용합니다.
 * - [builder] 설정 후 즉시 `build()`를 호출합니다.
 *
 * ```kotlin
 * val channel = managedChannel("localhost", 50051) { usePlaintext() }
 * // channel.authority().contains("localhost") == true
 * ```
 */
inline fun managedChannel(
    host: String,
    port: Int,
    builder: ManagedChannelBuilder<*>.() -> Unit,
): ManagedChannel =
    ManagedChannelBuilder
        .forAddress(host, port)
        .apply(builder)
        .build()

/**
 * target 문자열 기반 [ManagedChannel]을 생성합니다.
 *
 * ## 동작/계약
 * - `ManagedChannelBuilder.forTarget(target)` 경로를 사용합니다.
 * - [builder] 설정 후 즉시 `build()`를 호출합니다.
 *
 * ```kotlin
 * val channel = managedChannel("dns:///localhost:50051") { usePlaintext() }
 * // channel != null
 * ```
 */
inline fun managedChannel(
    target: String,
    builder: ManagedChannelBuilder<*>.() -> Unit,
): ManagedChannel = ManagedChannelBuilder
    .forTarget(target)
    .apply(builder)
    .build()
