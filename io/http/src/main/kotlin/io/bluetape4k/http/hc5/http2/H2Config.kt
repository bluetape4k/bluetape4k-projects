package io.bluetape4k.http.hc5.http2

import org.apache.hc.core5.http2.config.H2Config

/**
 * 기본 [H2Config] 를 제공합니다.
 */
fun h2ConfigOf(): H2Config = H2Config.DEFAULT

/**
 * [H2Config] 를 생성합니다.
 *
 * ```
 * val config = h2Config {
 *    setPushEnabled(true)
 *    setHeaderTableSize(4096)
 *    setInitialWindowSize(65535)
 * }
 * ```
 *
 * @param initlializer [H2Config.Builder] 초기화 람다
 * @return [H2Config] 인스턴스
 */
inline fun h2Config(
    initlializer: H2Config.Builder.() -> Unit,
): H2Config {
    return H2Config.custom().apply(initlializer).build()
}

/**
 * [H2Config] 를 생성합니다.
 *
 * ```
 * val source = h2ConfigOf()
 * val config = h2Config(source) {
 *    setPushEnabled(true)
 *    setHeaderTableSize(4096)
 *    setInitialWindowSize(65535)
 * }
 * ```
 *
 * @param source [H2Config] 기존 설정
 * @param initlializer [H2Config.Builder] 초기화 람다
 * @return [H2Config] 인스턴스
 */
inline fun h2Config(
    source: H2Config,
    initlializer: H2Config.Builder.() -> Unit = {},
): H2Config =
    H2Config.copy(source).apply(initlializer).build()

/**
 * [H2Config] 를 생성합니다.
 *
 * ```
 * val config = h2Config(
 *    pushEnabled = true,
 *    headerTableSize = 4096,
 *    initialWindowSize = 65535,
 * ) {
 *       // additional settings
 * }
 * ```
 *
 * @param pushEnabled pushEnabled 설정
 * @param headerTableSize headerTableSize 설정
 * @param initialWindowSize initialWindowSize 설정
 * @param compressionEnabled compressionEnabled 설정
 * @param initlializer [H2Config.Builder] 초기화 람다
 * @return [H2Config] 인스턴스
 */
inline fun h2Config(
    pushEnabled: Boolean = H2Config.DEFAULT.isPushEnabled,
    headerTableSize: Int = H2Config.DEFAULT.headerTableSize,
    initialWindowSize: Int = H2Config.DEFAULT.initialWindowSize,
    compressionEnabled: Boolean = H2Config.DEFAULT.isCompressionEnabled,
    initlializer: H2Config.Builder.() -> Unit = {},
): H2Config = h2Config {
    setPushEnabled(pushEnabled)
    setHeaderTableSize(headerTableSize)
    setInitialWindowSize(initialWindowSize)
    setCompressionEnabled(compressionEnabled)
    initlializer()
}
