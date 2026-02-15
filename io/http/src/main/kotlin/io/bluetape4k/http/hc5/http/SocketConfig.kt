package io.bluetape4k.http.hc5.http

import org.apache.hc.core5.http.io.SocketConfig
import org.apache.hc.core5.util.Timeout

/** 기본 [SocketConfig] 값입니다. */
@JvmField
val defaultSocketConfig: SocketConfig = SocketConfig.DEFAULT

/**
 * [SocketConfig]를 생성합니다.
 *
 * @param builder [SocketConfig.Builder] 초기화 람다
 * @return [SocketConfig] 인스턴스
 */
inline fun socketConfig(
    @BuilderInference builder: SocketConfig.Builder.() -> Unit,
): SocketConfig =
    SocketConfig.custom().apply(builder).build()

/**
 * 기본 소켓 옵션을 적용한 [SocketConfig]를 생성합니다.
 *
 * @param soTimeout 소켓 타임아웃
 * @param soReuseStrategy 소켓 주소 재사용 여부
 * @param soLinger 소켓 linger 시간
 * @param builder [SocketConfig.Builder] 추가 설정 블록
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
