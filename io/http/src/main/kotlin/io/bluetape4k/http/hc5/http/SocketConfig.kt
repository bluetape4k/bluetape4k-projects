package io.bluetape4k.http.hc5.http

import org.apache.hc.core5.http.io.SocketConfig
import org.apache.hc.core5.util.Timeout

@JvmField
val defaultSocketConfig: SocketConfig = SocketConfig.DEFAULT

/**
 * [SocketConfig] 를 생성합니다.
 *
 * @param builder [SocketConfig.Builder] 초기화 람다
 * @return [SocketConfig] 인스턴스
 */
inline fun socketConfig(
    @BuilderInference builder: SocketConfig.Builder.() -> Unit,
): SocketConfig =
    SocketConfig.custom().apply(builder).build()

/**
 * HTTP 처리에서 `socketConfigOf` 함수를 제공합니다.
 */
inline fun socketConfigOf(
    soTimeout: Timeout = Timeout.ofMinutes(3),
    soReuseStrategy: Boolean = true,
    soLinger: Timeout = Timeout.ofMinutes(3),
    @BuilderInference builder: SocketConfig.Builder.() -> Unit = {},
): SocketConfig =
    socketConfig {
        setSoTimeout(soTimeout)
        setSoReuseAddress(soReuseStrategy)
        setSoLinger(soLinger)
        builder()
    }
