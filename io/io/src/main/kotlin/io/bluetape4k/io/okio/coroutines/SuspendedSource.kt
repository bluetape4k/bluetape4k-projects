package io.bluetape4k.io.okio.coroutines

import okio.Buffer
import okio.Timeout

/**
 * Coroutines 방식으로 [okio.Source] 기능을 제공하는 인터페이스
 */
interface SuspendedSource {

    /**
     * 이 소스에서 최소 1바이트 이상, 최대 `byteCount` 바이트를 제거하고 `sink`에 추가합니다.
     * 읽어들인 바이트 수를 반환하거나, 이 소스가 고갈된 경우 -1을 반환합니다.
     *
     * @param sink      읽어들일 버퍼
     * @param byteCount 읽어들일 바이트 수
     * @return 실제로 읽어들인 바이트 수
     */
    suspend fun read(sink: Buffer, byteCount: Long = sink.size): Long

    /**
     * 모든 버퍼링된 바이트를 최종 목적지로 전송하고 이 [SuspendedSource]가 보유한 리소스를 해제합니다.
     */
    suspend fun close()

    /**
     * 이 [SuspendedSource]의 [Timeout]을 반환합니다.
     */
    fun timeout(): Timeout = Timeout.NONE
}
