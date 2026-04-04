package io.bluetape4k.http.hc5.protocol

import org.apache.hc.client5.http.protocol.HttpClientContext
import org.apache.hc.core5.http.protocol.HttpContext

/**
 * [HttpContext] 를 [HttpClientContext] 로 변환합니다.
 *
 * ```kotlin
 * val context: HttpContext = BasicHttpContext()
 * val clientContext = context.adapt()
 * // clientContext is HttpClientContext
 * ```
 *
 * @receiver [HttpContext]
 * @return [HttpClientContext]
 */
fun HttpContext.adapt(): HttpClientContext = HttpClientContext.castOrCreate(this)

/**
 * [HttpClientContext] 를 생성합니다.
 *
 * ```kotlin
 * val context = httpClientContextOf()
 * // context is HttpClientContext
 * ```
 *
 * @return [HttpClientContext]
 */
fun httpClientContextOf(): HttpClientContext = HttpClientContext.create()

/**
 * [HttpContext] 를 [HttpClientContext] 로 변환합니다.
 *
 * ```kotlin
 * val baseContext: HttpContext = BasicHttpContext()
 * val clientContext = httpClientContextOf(baseContext)
 * // clientContext is HttpClientContext
 * ```
 *
 * @param context [HttpContext]
 * @return [HttpClientContext]
 */
fun httpClientContextOf(context: HttpContext): HttpClientContext = HttpClientContext(context)
