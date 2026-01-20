package io.bluetape4k.http.hc5.http

import org.apache.hc.client5.http.config.ConnectionConfig
import org.apache.hc.core5.util.TimeValue
import org.apache.hc.core5.util.Timeout

/**
 * Default ConnectionConfig
 */
@JvmField
val defaultConnectionConfig: ConnectionConfig = ConnectionConfig.DEFAULT

/**
 * [builder]를 이용해 [ConnectionConfig] 를 생성합니다.
 *
 * ```
 * val connectionConfig = connectionConfig {
 *      setConnectTimeout(TimeValue.ofSeconds(10))
 *      setSocketTimeout(TimeValue.ofSeconds(10))
 *      setValidateAfterInactivity(TimeValue.ofSeconds(10))
 * }
 * ```
 *
 * @param builder 환경 설정을 수행할 람다 함수
 * @return [ConnectionConfig] 인스턴스
 */
inline fun connectionConfig(
    @BuilderInference builder: ConnectionConfig.Builder.() -> Unit,
): ConnectionConfig =
    ConnectionConfig.custom().apply(builder).build()

/**
 * [ConnectionConfig] 를 생성합니다.
 *
 * ```
 * val connectionConfig = connectionConfigOf(TimeValue.ofSeconds(10)) {
 *    // custom configuration
 * }
 * ```
 *
 * @param connectTimeout connect timeout
 * @param socketTimeout socket timeout
 * @param valiateAfterInactivity validate after inactivity
 * @param timeToLive time to live
 * @param builder 환경 설정을 수행할 람다 함수
 * @return [ConnectionConfig] 인스턴스
 */
inline fun connectionConfigOf(
    connectTimeout: Timeout = defaultConnectionConfig.connectTimeout,
    socketTimeout: Timeout = defaultConnectionConfig.socketTimeout,
    valiateAfterInactivity: TimeValue = defaultConnectionConfig.validateAfterInactivity,
    timeToLive: TimeValue = defaultConnectionConfig.timeToLive,
    @BuilderInference builder: ConnectionConfig.Builder.() -> Unit = {},
): ConnectionConfig =
    connectionConfig {
        setConnectTimeout(connectTimeout)
        setSocketTimeout(socketTimeout)
        setValidateAfterInactivity(valiateAfterInactivity)
        setTimeToLive(timeToLive)

        builder()
    }
