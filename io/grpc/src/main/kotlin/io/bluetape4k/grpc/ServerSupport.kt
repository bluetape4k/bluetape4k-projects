package io.bluetape4k.grpc

import io.grpc.Server
import io.grpc.ServerBuilder

/**
 * 포트 기반 [ServerBuilder]를 생성하고 초기화 블록을 적용합니다.
 *
 * ## 동작/계약
 * - 매 호출마다 `ServerBuilder.forPort(port)`로 새 builder를 생성합니다.
 * - [builder]에서 서비스/인터셉터/executor 등을 추가 구성할 수 있습니다.
 *
 * ```kotlin
 * val b = grpcServerBuilder(8080) { addService(service) }
 * // b != null
 * ```
 */
inline fun grpcServerBuilder(
    port: Int,
    builder: ServerBuilder<*>.() -> Unit,
): ServerBuilder<*> =
    ServerBuilder.forPort(port).apply(builder)

/**
 * [ServerBuilder] 설정을 적용해 즉시 [Server]를 빌드합니다.
 *
 * ## 동작/계약
 * - [grpcServerBuilder]에 위임한 뒤 `build()`를 호출합니다.
 * - 서버 시작은 자동으로 하지 않으며, 호출자가 `start()`를 수행해야 합니다.
 *
 * ```kotlin
 * val server = grpcServer(8080) { addService(service) }
 * // server.port == 8080
 * ```
 */
inline fun grpcServer(
    port: Int,
    builder: ServerBuilder<*>.() -> Unit,
): Server =
    grpcServerBuilder(port, builder).build()
