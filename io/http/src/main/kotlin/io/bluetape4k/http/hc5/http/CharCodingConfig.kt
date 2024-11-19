package io.bluetape4k.http.hc5.http

import org.apache.hc.core5.http.config.CharCodingConfig

/**
 * 기본 [CharCodingConfig] 를 생성합니다.
 */
fun charCodingConfigOf(): CharCodingConfig =
    CharCodingConfig.DEFAULT

/**
 * [CharCodingConfig] 를 생성합니다.
 *
 * ```
 * val config = charCodingConfig {
 *     setCharset("UTF-8")
 *     setMalformedInputAction(CodingErrorAction.REPLACE)
 *     setUnmappableInputAction(CodingErrorAction.REPLACE)
 * }
 * ```
 *
 * @param initializer [CharCodingConfig.Builder] 초기화 람다
 * @return [CharCodingConfig]
 */
inline fun charCodingConfig(
    initializer: CharCodingConfig.Builder.() -> Unit,
): CharCodingConfig =
    CharCodingConfig.custom().apply(initializer).build()

/**
 * [source] 를 복사하여 [CharCodingConfig] 를 생성합니다.
 *
 * ```
 * val config = charCodingConfig(source) {
 *    setCharset("UTF-8")
 * }
 * ```
 *
 * @param source [CharCodingConfig] 복사할 대상
 * @param initializer [CharCodingConfig.Builder] 초기화 람다
 * @return [CharCodingConfig]
 */
inline fun charCodingConfig(
    source: CharCodingConfig,
    initializer: CharCodingConfig.Builder.() -> Unit,
): CharCodingConfig =
    CharCodingConfig.copy(source).apply(initializer).build()
