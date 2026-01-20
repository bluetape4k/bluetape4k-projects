package io.bluetape4k.http.hc5.http

import org.apache.hc.client5.http.ContextBuilder
import org.apache.hc.client5.http.SchemePortResolver
import org.apache.hc.client5.http.protocol.HttpClientContext

/**
 * [HttpClientContext] 를 생성합니다.
 *
 * ```
 * val context = httpClientContext {
 *    setCookieStore(cookieStore)
 *    setCredentialsProvider(credentialsProvider)
 * }
 * ```
 *
 * @param builder [ContextBuilder] 초기화 람다
 * @return [HttpClientContext]
 */
inline fun httpClientContext(
    @BuilderInference builder: ContextBuilder.() -> Unit,
): HttpClientContext =
    ContextBuilder.create().apply(builder).build()

/**
 * [ContextBuilder] 를 생성합니다.
 *
 * ```
 * val contextBuilder = contextBuilderOf()
 * ```
 *
 * @return [ContextBuilder]
 */
fun contextBuilderOf(): ContextBuilder = ContextBuilder.create()

/**
 * [ContextBuilder] 를 생성합니다.
 *
 * ```
 * val contextBuilder = contextBuilderOf(schemePortResolver)
 * ```
 *
 * @param schemePortResolver [SchemePortResolver]
 * @return [ContextBuilder]
 */
fun contextBuilderOf(schemePortResolver: SchemePortResolver): ContextBuilder =
    ContextBuilder.create(schemePortResolver)
