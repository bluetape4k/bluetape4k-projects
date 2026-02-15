package io.bluetape4k.http.hc5.http

import org.apache.hc.client5.http.impl.RequestSupport
import org.apache.hc.core5.http.HttpRequest

/**
 * [HttpRequest]에서 경로 접두사를 추출합니다.
 *
 * ```
 * val pathPrefix = request.extractPathPrefix()  // "/api/v1"
 * ```
 *
 * @return 추출된 경로 접두사
 */
fun HttpRequest.extractPathPrefix(): String = RequestSupport.extractPathPrefix(this)
