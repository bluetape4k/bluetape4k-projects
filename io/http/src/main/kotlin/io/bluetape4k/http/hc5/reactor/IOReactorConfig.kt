package io.bluetape4k.http.hc5.reactor

import org.apache.hc.core5.reactor.IOReactorConfig
import org.apache.hc.core5.util.TimeValue
import org.apache.hc.core5.util.Timeout

/**
 * [initializer]를 이용해 [IOReactorConfig] 를 빌드합니다.
 *
 * ```
 * val ioReactorConfig = ioReactorConfig {
 *     setIoThreadCount(2)
 *     setSoTimeout(3000)
 * }
 * ```
 *
 * @param initializer [IOReactorConfig.Builder] DSL
 * @return [IOReactorConfig]
 */
inline fun ioReactorConfig(
    initializer: IOReactorConfig.Builder.() -> Unit,
): IOReactorConfig =
    IOReactorConfig.custom().apply(initializer).build()


/**
 * [ioReactorConfig] 를 생성합니다.
 *
 * ```
 * val ioReactorConfig = ioReactorConfigOf(
 *     ioThreadCount = 2,
 *     soTimeout = 3000,
 * ) {
 *    setTcpNoDelay(true)
 * }
 * ```
 *
 * @param ioThreadCount IO Thread Count
 * @param soTimeout Socket Timeout
 * @param soLinger Socket Linger
 * @param soKeepAlive Socket Keep Alive
 * @param initializer [IOReactorConfig.Builder] DSL
 * @return [IOReactorConfig]
 */
inline fun ioReactorConfigOf(
    ioThreadCount: Int = IOReactorConfig.DEFAULT.ioThreadCount,
    soTimeout: Timeout = IOReactorConfig.DEFAULT.soTimeout,
    soLinger: TimeValue = IOReactorConfig.DEFAULT.soLinger,
    soKeepAlive: Boolean = IOReactorConfig.DEFAULT.isSoKeepAlive,
    initializer: IOReactorConfig.Builder.() -> Unit = {},
): IOReactorConfig = ioReactorConfig {
    setIoThreadCount(ioThreadCount)
    setSoTimeout(soTimeout)
    setSoLinger(soLinger)
    setSoKeepAlive(soKeepAlive)

    initializer()
}
