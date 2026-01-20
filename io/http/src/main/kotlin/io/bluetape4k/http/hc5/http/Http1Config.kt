package io.bluetape4k.http.hc5.http

import org.apache.hc.core5.http.config.Http1Config

/**
 * 기본 [Http1Config] 를 반환합니다.
 */
fun http1ConfigOf(): Http1Config = Http1Config.DEFAULT

/**
 * [Http1Config] 를 생성합니다.
 *
 * ```
 * val config = http1Config {
 *    setBufferSize(1024)
 *    setChunkSizeHint(1024)
 * }
 * ```
 *
 * @param builder [Http1Config.Builder] 초기화 람다
 * @return [Http1Config] 인스턴스
 */
inline fun http1Config(
    @BuilderInference builder: Http1Config.Builder.() -> Unit,
): Http1Config {
    return Http1Config.custom().apply(builder).build()
}

/**
 * [source] 를 복사하여 [Http1Config] 를 생성합니다.
 *
 * ```
 * val source = http1ConfigOf()
 * val config = http1Config(source) {
 *    setBufferSize(1024)
 *    setChunkSizeHint(1024)
 * }
 * ```
 *
 * @param source [Http1Config] 복사할 인스턴스
 * @param builder [Http1Config.Builder] 초기화 람다
 * @return [Http1Config] 인스턴스
 */
inline fun http1Config(
    source: Http1Config,
    @BuilderInference builder: Http1Config.Builder.() -> Unit = {},
): Http1Config =
    Http1Config.copy(source).apply(builder).build()
