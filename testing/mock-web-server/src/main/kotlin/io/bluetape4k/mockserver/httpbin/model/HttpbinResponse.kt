package io.bluetape4k.mockserver.httpbin.model

import io.bluetape4k.logging.KLogging
import java.io.Serializable

/**
 * httpbin API 응답 모델.
 *
 * httpbin.org의 응답 형식을 모사한다.
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
