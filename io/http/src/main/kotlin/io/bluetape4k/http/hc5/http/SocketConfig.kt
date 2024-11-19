package io.bluetape4k.http.hc5.http

import org.apache.hc.core5.http.io.SocketConfig
import org.apache.hc.core5.util.Timeout

@JvmField
val defaultSocketConfig: SocketConfig = SocketConfig.DEFAULT

/**
 * [SocketConfig] 를 생성합니다.
 *
 * @param initializer [SocketConfig.Builder] 초기화 람다
 * @return [SocketConfig] 인스턴스
 */
inline fun socketConfig(initializer: SocketConfig.Builder.() -> Unit): SocketConfig {
    return SocketConfig.custom().apply(initializer).build()
}

fun socketConfigOf(
    soTimeout: Timeout = Timeout.ofMinutes(3),
    soReuseStrategy: Boolean = true,
    soLinger: Timeout = Timeout.ofMinutes(3),
    initializer: SocketConfig.Builder.() -> Unit = {},
): SocketConfig = socketConfig {
    setSoTimeout(soTimeout)
    setSoReuseAddress(soReuseStrategy)
    setSoLinger(soLinger)
    initializer()
}
