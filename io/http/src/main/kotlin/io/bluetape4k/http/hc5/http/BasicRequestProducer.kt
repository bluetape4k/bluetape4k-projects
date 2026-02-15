package io.bluetape4k.http.hc5.http

import org.apache.hc.core5.http.Method
import org.apache.hc.core5.http.message.BasicHttpRequest
import org.apache.hc.core5.http.nio.AsyncEntityProducer
import org.apache.hc.core5.http.nio.support.BasicRequestProducer
import java.net.URI

/**
 * [BasicHttpRequest]를 [BasicRequestProducer]로 변환합니다.
 */
fun BasicHttpRequest.toProducer(): BasicRequestProducer = basicRequestProducerOf(this)

/**
 * [BasicRequestProducer]를 빌드합니다.
 *
 * ```
 * val request = BasicHttpRequest(Method.GET, URI.create("http://localhost:8080"))
 * val producer = basicRequestProducerOf(request)
 * ```
 *
 * @param request [BasicHttpRequest] 기본 요청 정보
 * @param dataProducer 요청 본문 생성기
 * @return [BasicRequestProducer] 인스턴스
 */
fun basicRequestProducerOf(
    request: BasicHttpRequest,
    dataProducer: AsyncEntityProducer? = null,
): BasicRequestProducer =
    BasicRequestProducer(request, dataProducer)

/**
 * [BasicRequestProducer]를 빌드합니다.
 *
 * ```
 * val producer = basicRequestProducerOf(Method.GET, URI.create("http://localhost:8080"))
 * ```
 *
 * @param method [Method] HTTP 메서드
 * @param uri 요청 URI
 * @param dataProducer 요청 본문 생성기
 * @return [BasicRequestProducer] 인스턴스
 */
fun basicRequestProducerOf(
    method: Method,
    uri: URI,
    dataProducer: AsyncEntityProducer? = null,
): BasicRequestProducer =
    BasicRequestProducer(method.name, uri, dataProducer)

/**
 * [BasicRequestProducer]를 빌드합니다.
 *
 * ```
 * val entityProducer = asyncEntityProducerOf("Hello, World!")
 * val producer = basicRequestProducerOf("GET", URI.create("http://localhost:8080"), entityProducer)
 * ```
 *
 * @param methodName HTTP 메서드 이름
 * @param uri 요청 URI
 * @param dataProducer 요청 본문 생성기
 * @return [BasicRequestProducer] 인스턴스
 */
fun basicRequestProducerOf(
    methodName: String,
    uri: URI,
    dataProducer: AsyncEntityProducer? = null,
): BasicRequestProducer =
    BasicRequestProducer(methodName, uri, dataProducer)
