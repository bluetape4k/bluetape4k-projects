package io.bluetape4k.mockwebflux.httpbin.model

import io.bluetape4k.logging.KLogging
import java.io.Serializable

/**
 * httpbin API 응답 모델 (WebFlux 버전).
 *
 * httpbin.org의 응답 형식을 모사한다.
 * WebFlux + Coroutines 환경에서 사용되며, 필드 구성은 servlet 기반 모델과 동일하다.
 */
data class HttpbinResponse(
    val args: Map<String, String> = emptyMap(),
    val headers: Map<String, String> = emptyMap(),
    val origin: String = "",
    val url: String = "",
    val data: String = "",
    val files: Map<String, String> = emptyMap(),
    val form: Map<String, String> = emptyMap(),
    val json: Any? = null,
    val method: String = "",
): Serializable {
    companion object: KLogging() {
        private const val serialVersionUID = 1L
    }
}
