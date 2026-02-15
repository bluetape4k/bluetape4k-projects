package io.bluetape4k.http.hc5.http

import org.apache.hc.client5.http.ContextBuilder
import org.apache.hc.client5.http.SchemePortResolver
import org.apache.hc.client5.http.protocol.HttpClientContext

/**
 * [HttpClientContext]를 생성합니다.
 *
 * ```
 * val context = httpClientContext {
 *    setCookieStore(cookieStore)
 *    setCredentialsProvider(credentialsProvider)
 * }
 * ```
 *
 * @param builder [ContextBuilder] 설정 블록
 * @return 생성된 [HttpClientContext]
 */
inline fun httpClientContext(
    @BuilderInference builder: ContextBuilder.() -> Unit,
): HttpClientContext =
    ContextBuilder.create().apply(builder).build()

/**
 * 기본 [ContextBuilder]를 생성합니다.
 *
 * ```
 * val contextBuilder = contextBuilderOf()
 * ```
 *
 * @return 생성된 [ContextBuilder]
 */
fun contextBuilderOf(): ContextBuilder = ContextBuilder.create()

/**
 * [SchemePortResolver]를 적용한 [ContextBuilder]를 생성합니다.
 *
 * ```
 * val contextBuilder = contextBuilderOf(schemePortResolver)
 * ```
 *
 * @param schemePortResolver 스킴별 포트 해석기
 * @return 생성된 [ContextBuilder]
 */
fun contextBuilderOf(schemePortResolver: SchemePortResolver): ContextBuilder =
    ContextBuilder.create(schemePortResolver)
