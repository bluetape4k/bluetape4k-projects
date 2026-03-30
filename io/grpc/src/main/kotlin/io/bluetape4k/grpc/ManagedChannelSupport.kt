package io.bluetape4k.grpc

import io.bluetape4k.support.requireInRange
import io.bluetape4k.support.requireNotBlank
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder

/**
 * host/port 기반 [ManagedChannel]을 생성합니다.
 *
 * ## 동작/계약
 * - [host]가 blank이면 [IllegalArgumentException]이 발생합니다.
 * - [port]가 `1..65535` 범위를 벗어나면 [IllegalArgumentException]이 발생합니다.
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
        .forAddress(
            host.requireNotBlank("host"),
            port.requireInRange(1, 65535, "port"),
        )
        .apply(builder)
        .build()

/**
 * target 문자열 기반 [ManagedChannel]을 생성합니다.
 *
 * ## 동작/계약
 * - [target]이 blank이면 [IllegalArgumentException]이 발생합니다.
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
    .forTarget(target.requireNotBlank("target"))
    .apply(builder)
    .build()
