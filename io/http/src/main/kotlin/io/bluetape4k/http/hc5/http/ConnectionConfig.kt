package io.bluetape4k.http.hc5.http

import org.apache.hc.client5.http.config.ConnectionConfig
import org.apache.hc.core5.util.TimeValue
import org.apache.hc.core5.util.Timeout

/** 기본 [ConnectionConfig] 값입니다. */
@JvmField
val defaultConnectionConfig: ConnectionConfig = ConnectionConfig.DEFAULT

/**
 * [builder]를 이용해 [ConnectionConfig]를 생성합니다.
 *
 * ```
 * val connectionConfig = connectionConfig {
 *      setConnectTimeout(TimeValue.ofSeconds(10))
 *      setSocketTimeout(TimeValue.ofSeconds(10))
 *      setValidateAfterInactivity(TimeValue.ofSeconds(10))
 * }
 * ```
 *
 * @param builder [ConnectionConfig.Builder] 설정 블록
 * @return [ConnectionConfig] 인스턴스
 */
inline fun connectionConfig(
    @BuilderInference builder: ConnectionConfig.Builder.() -> Unit,
): ConnectionConfig =
    ConnectionConfig.custom().apply(builder).build()

/**
 * 기본값을 바탕으로 [ConnectionConfig]를 생성합니다.
 *
 * ```
 * val connectionConfig = connectionConfigOf(TimeValue.ofSeconds(10)) {
 *    // custom configuration
 * }
 * ```
 *
 * @param connectTimeout 연결 타임아웃
 * @param socketTimeout 소켓 타임아웃
 * @param valiateAfterInactivity 유휴 후 유효성 검사 간격
 * @param timeToLive 연결 생존 시간
 * @param builder [ConnectionConfig.Builder] 설정 블록
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
