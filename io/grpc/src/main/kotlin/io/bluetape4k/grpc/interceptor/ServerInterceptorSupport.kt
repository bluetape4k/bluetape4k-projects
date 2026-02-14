package io.bluetape4k.grpc.interceptor

import io.bluetape4k.collections.eclipse.toUnifiedSet
import io.grpc.ForwardingServerCall
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status

/**
 * 클라이언트의 요청 헤더를 응답 헤더와 트레일러로 복사합니다.
 * 메타데이터 전파를 테스트하는 데 유용합니다.
 *
 * ```
 * val interceptor = echoRequestHeadersInterceptor(
 *    Metadata.Key.of("x-foo", Metadata.ASCII_STRING_MARSHALLER),
 *    Metadata.Key.of("x-bar", Metadata.ASCII_STRING_MARSHALLER)
 * )
 * ```
 *
 * @param keys 헤더의 키 목록
 */
fun echoRequestHeadersInterceptor(vararg keys: Metadata.Key<*>): ServerInterceptor {
    val keySet = keys.toUnifiedSet()

    return object: ServerInterceptor {
        /**
         * gRPC/Protobuf 처리에서 `interceptCall` 함수를 제공합니다.
         */
        override fun <ReqT, RespT> interceptCall(
            call: ServerCall<ReqT, RespT>,
            requestHeaders: Metadata,
            next: ServerCallHandler<ReqT, RespT>,
        ): ServerCall.Listener<ReqT> =
            next.startCall(
                /**
                 * gRPC/Protobuf 처리 관련 정적 팩토리/유틸리티를 제공하는 동반 객체입니다.
                 */
                object: ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
                    /**
                     * gRPC/Protobuf 처리에서 `sendHeaders` 함수를 제공합니다.
                     */
                    override fun sendHeaders(responseHeaders: Metadata) {
                        responseHeaders.merge(requestHeaders, keySet)
                        super.sendHeaders(responseHeaders)
                    }

                    /**
                     * gRPC/Protobuf 처리 리소스를 정리하고 닫습니다.
                     */
                    override fun close(status: Status?, trailers: Metadata) {
                        trailers.merge(requestHeaders, keySet)
                        super.close(status, trailers)
                    }
                },
                requestHeaders
            )
    }
}

/**
 * 클라이언트 요청 헤더 중 지정한 [keys] 헤더 값만 응답 헤더로 복사합니다.
 *
 * ```
 * val interceptor = echoRequestMetadataInHeaders(
 *     Metadata.Key.of("x-foo", Metadata.ASCII_STRING_MARSHALLER),
 *     Metadata.Key.of("x-bar", Metadata.ASCII_STRING_MARSHALLER)
 * }
 * ```
 *
 * @param keys 요청 헤더에서 응답 헤더로 복사할 header keys
 */
fun echoRequestMetadataInHeaders(vararg keys: Metadata.Key<*>): ServerInterceptor {
    val keySet = keys.toUnifiedSet()
    return object: ServerInterceptor {
        /**
         * gRPC/Protobuf 처리에서 `interceptCall` 함수를 제공합니다.
         */
        override fun <ReqT, RespT> interceptCall(
            call: ServerCall<ReqT, RespT>,
            requestHeaders: Metadata,
            next: ServerCallHandler<ReqT, RespT>,
        ): ServerCall.Listener<ReqT> =
            next.startCall(
                /**
                 * gRPC/Protobuf 처리 관련 정적 팩토리/유틸리티를 제공하는 동반 객체입니다.
                 */
                object: ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
                    /**
                     * gRPC/Protobuf 처리에서 `sendHeaders` 함수를 제공합니다.
                     */
                    override fun sendHeaders(responseHeaders: Metadata?) {
                        responseHeaders?.merge(requestHeaders, keySet)
                        super.sendHeaders(responseHeaders)
                    }
                },
                requestHeaders
            )
    }
}

/**
 * Echoes request headers with the specified key(s) from a client into response trailers only.
 * 클라이언트 요청 헤더 중 지정한 [keys] 헤더 값만 응답 트레일러로 복사합니다.
 *
 * ```
 * val interceptor = echoRequestMetadataInTrailers(
 *      Metadata.Key.of("x-foo", Metadata.ASCII_STRING_MARSHALLER),
 *      Metadata.Key.of("x-bar", Metadata.ASCII_STRING_MARSHALLER)
 * )
 * ```
 *
 * @param keys 요청 헤더에서 응답 트레일러로 복사할 header keys
 */
fun echoRequestMetadataInTrailers(vararg keys: Metadata.Key<*>): ServerInterceptor {
    val keySet = keys.toUnifiedSet()
    return object: ServerInterceptor {
        /**
         * gRPC/Protobuf 처리에서 `interceptCall` 함수를 제공합니다.
         */
        override fun <ReqT, RespT> interceptCall(
            call: ServerCall<ReqT, RespT>,
            requestHeaders: Metadata,
            next: ServerCallHandler<ReqT, RespT>,
        ): ServerCall.Listener<ReqT> =
            next.startCall(
                /**
                 * gRPC/Protobuf 처리 관련 정적 팩토리/유틸리티를 제공하는 동반 객체입니다.
                 */
                object: ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
                    /**
                     * gRPC/Protobuf 처리 리소스를 정리하고 닫습니다.
                     */
                    override fun close(status: Status, trailers: Metadata) {
                        trailers.merge(requestHeaders, keySet)
                        super.close(status, trailers)
                    }
                },
                requestHeaders
            )
    }
}
