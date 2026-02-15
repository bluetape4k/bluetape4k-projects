package io.bluetape4k.http.hc5.reactor

import org.apache.hc.core5.reactor.IOReactorConfig
import org.apache.hc.core5.util.TimeValue
import org.apache.hc.core5.util.Timeout

/**
 * [builder]를 이용해 [IOReactorConfig] 를 빌드합니다.
 *
 * ```
 * val ioReactorConfig = ioReactorConfig {
 *     setIoThreadCount(2)
 *     setSoTimeout(3000)
 * }
 * ```
 *
 * @param builder [IOReactorConfig.Builder] 설정 블록
 * @return [IOReactorConfig]
 */
inline fun ioReactorConfig(
    @BuilderInference builder: IOReactorConfig.Builder.() -> Unit,
): IOReactorConfig =
    IOReactorConfig.custom().apply(builder).build()

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
 * @param ioThreadCount IO 스레드 수
 * @param soTimeout 소켓 타임아웃
 * @param soLinger 소켓 linger 시간
 * @param soKeepAlive 소켓 Keep-Alive 여부
 * @param builder [IOReactorConfig.Builder] 설정 블록
 * @return [IOReactorConfig]
 */
inline fun ioReactorConfigOf(
    ioThreadCount: Int = IOReactorConfig.DEFAULT.ioThreadCount,
    soTimeout: Timeout = IOReactorConfig.DEFAULT.soTimeout,
    soLinger: TimeValue = IOReactorConfig.DEFAULT.soLinger,
    soKeepAlive: Boolean = IOReactorConfig.DEFAULT.isSoKeepAlive,
    @BuilderInference builder: IOReactorConfig.Builder.() -> Unit = {},
): IOReactorConfig =
    ioReactorConfig {
        setIoThreadCount(ioThreadCount)
        setSoTimeout(soTimeout)
        setSoLinger(soLinger)
        setSoKeepAlive(soKeepAlive)

        builder()
    }
