package io.bluetape4k.grpc

import io.grpc.Server
import io.grpc.ServerBuilder

/**
 * [ServerBuilder]를 생성하고, 추가 설정을 수행합니다.
 *
 * ```
 * val server = grpcServerBuilder(8080) {
 *    addService(service)
 *    useTransportSecurity(certChain, privateKey)
 *    executor(VirtualThreadExecutor)
 * }
 *
 *
 * @param port        서버 port
 * @param initializer 서버 빌더 초기화 람다
 * @return [ServerBuilder] 인스턴스
 */
inline fun grpcServerBuilder(
    port: Int,
    initializer: ServerBuilder<*>.() -> Unit,
): ServerBuilder<*> =
    ServerBuilder.forPort(port).apply(initializer)

/**
 * [ServerBuilder]를 이용하여 gRPC Server를 설정하고, [Server]를 빌드합니다.
 *
 * ```
 * val server = grpcServer(8080) {
 *   addService(service)
 *   useTransportSecurity(certChain, privateKey)
 *   executor(VirtualThreadExecutor)
 *   intercept(interceptor)
 * }
 * ```
 *
 * @param port        서버 port
 * @param initializer 서버 빌더 초기화 람다
 * @return [Server] 인스턴스
 */
inline fun grpcServer(
    port: Int,
    initializer: ServerBuilder<*>.() -> Unit,
): Server =
    grpcServerBuilder(port, initializer).build()
