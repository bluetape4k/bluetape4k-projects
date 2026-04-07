package io.bluetape4k.grpc.interceptor

import io.grpc.ForwardingServerCall
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status

/**
 * 요청 메타데이터의 지정 키를 응답 헤더와 트레일러에 모두 복사하는 인터셉터를 생성합니다.
 *
 * ## 동작/계약
 * - [keys]에 지정한 key만 `requestHeaders -> responseHeaders/trailers`로 복사합니다.
 * - 비지정 key는 복사하지 않습니다.
 *
 * ```kotlin
 * val interceptor = echoRequestHeadersInterceptor(Metadata.Key.of("x-id", Metadata.ASCII_STRING_MARSHALLER))
 * // x-id가 응답 헤더/트레일러로 전파됨
 * ```
 */
fun echoRequestHeadersInterceptor(vararg keys: Metadata.Key<*>): ServerInterceptor {
    val keySet = keys.toSet()

    return object: ServerInterceptor {
        override fun <ReqT, RespT> interceptCall(
            call: ServerCall<ReqT, RespT>,
            requestHeaders: Metadata,
            next: ServerCallHandler<ReqT, RespT>,
        ): ServerCall.Listener<ReqT> =
            next.startCall(
                object: ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
                    override fun sendHeaders(responseHeaders: Metadata) {
                        responseHeaders.merge(requestHeaders, keySet)
                        super.sendHeaders(responseHeaders)
                    }

                    override fun close(
                        status: Status,
                        trailers: Metadata,
                    ) {
                        trailers.merge(requestHeaders, keySet)
                        super.close(status, trailers)
                    }
                },
                requestHeaders
            )
    }
}

/**
 * 요청 메타데이터의 지정 키를 응답 헤더에만 복사하는 인터셉터를 생성합니다.
 *
 * ## 동작/계약
 * - [keys]에 지정한 key만 응답 헤더에 복사합니다.
 * - 트레일러에는 값을 추가하지 않습니다.
 *
 * ```kotlin
 * val interceptor = echoRequestMetadataInHeaders(Metadata.Key.of("x-id", Metadata.ASCII_STRING_MARSHALLER))
 * // x-id가 응답 헤더로 전파됨
 * ```
 */
fun echoRequestMetadataInHeaders(vararg keys: Metadata.Key<*>): ServerInterceptor {
    val keySet = keys.toSet()
    return object: ServerInterceptor {
        override fun <ReqT, RespT> interceptCall(
            call: ServerCall<ReqT, RespT>,
            requestHeaders: Metadata,
            next: ServerCallHandler<ReqT, RespT>,
        ): ServerCall.Listener<ReqT> =
            next.startCall(
                object: ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
                    override fun sendHeaders(responseHeaders: Metadata) {
                        responseHeaders.merge(requestHeaders, keySet)
                        super.sendHeaders(responseHeaders)
                    }
                },
                requestHeaders
            )
    }
}

/**
 * 요청 메타데이터의 지정 키를 응답 트레일러에만 복사하는 인터셉터를 생성합니다.
 *
 * ## 동작/계약
 * - [keys]에 지정한 key만 트레일러에 복사합니다.
 * - 헤더에는 값을 추가하지 않습니다.
 *
 * ```kotlin
 * val interceptor = echoRequestMetadataInTrailers(Metadata.Key.of("x-id", Metadata.ASCII_STRING_MARSHALLER))
 * // x-id가 응답 트레일러로 전파됨
 * ```
 */
fun echoRequestMetadataInTrailers(vararg keys: Metadata.Key<*>): ServerInterceptor {
    val keySet = keys.toSet()
    return object: ServerInterceptor {
        override fun <ReqT, RespT> interceptCall(
            call: ServerCall<ReqT, RespT>,
            requestHeaders: Metadata,
            next: ServerCallHandler<ReqT, RespT>,
        ): ServerCall.Listener<ReqT> =
            next.startCall(
                object: ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
                    override fun close(
                        status: Status,
                        trailers: Metadata,
                    ) {
                        trailers.merge(requestHeaders, keySet)
                        super.close(status, trailers)
                    }
                },
                requestHeaders
            )
    }
}
