package io.bluetape4k.http.hc5.http

import org.apache.hc.client5.http.impl.RequestSupport
import org.apache.hc.core5.http.HttpRequest

/**
 * Extract path prefix from [HttpRequest].
 *
 * ```
 * val pathPrefix = request.extractPathPrefix()  // "/api/v1"
 * ```
 *
 * @return path prefix
 */
fun HttpRequest.extractPathPrefix(): String = RequestSupport.extractPathPrefix(this)
