package io.bluetape4k.io.okio.coroutines

import okio.Buffer
import okio.Timeout

/**
 * Coroutines 방식으로 [okio.Source] 기능을 제공하는 인터페이스
 */
interface AsyncSource {

    /**
     * 이 소스에서 최소 1바이트 이상, 최대 `byteCount` 바이트를 제거하고 `sink`에 추가합니다.
     * 읽어들인 바이트 수를 반환하거나, 이 소스가 고갈된 경우 -1을 반환합니다.
     *
     * @param sink      읽어들일 버퍼
     * @param byteCount 읽어들일 바이트 수
     * @return 실제로 읽어들인 바이트 수
     */
    suspend fun read(sink: Buffer, byteCount: Long): Long

    /**
     * 모든 버퍼링된 바이트를 최종 목적지로 전송하고 이 [AsyncSource]가 보유한 리소스를 해제합니다.
     */
    suspend fun close()

    /**
     * 이 [AsyncSource]의 [Timeout]을 반환합니다.
     */
    suspend fun timeout(): Timeout
}

/**
 * `source`에서 읽은 내용을 버퍼링하는 새로운 소스를 반환합니다.
 * 반환된 소스는 메모리 버퍼로 대량 읽기를 수행합니다.
 * 데이터에 대한 편리하고 효율적인 액세스를 얻으려면 소스를 읽는 모든 곳에서 이를 사용하십시오.
 */
fun AsyncSource.buffer(): BufferedAsyncSource = RealBufferedAsyncSource(this)
