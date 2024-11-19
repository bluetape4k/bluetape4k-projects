package io.bluetape4k.http.hc5.async.methods

import org.apache.hc.client5.http.async.methods.SimpleHttpRequest
import org.apache.hc.client5.http.async.methods.SimpleRequestProducer

/**
 * [SimpleRequestProducer]를 생성합니다.
 *
 * ```
 * val producer = simpleRequestProducerOf(simpleHttpRequest("GET"))
 * ```
 *
 * @param request [SimpleHttpRequest] 인스턴스
 * @return [SimpleRequestProducer] 인스턴스
 */
fun simpleRequestProducerOf(request: SimpleHttpRequest): SimpleRequestProducer {
    return SimpleRequestProducer.create(request)
}
