package io.bluetape4k.io.okio.coroutines

import okio.Buffer
import okio.Timeout

/**
 * Coroutines 방식으로 비동기로 [okio.Sink] 기능을 제공하는 인터페이스
 */
interface AsyncSink {

    /**
     * [source]로부터 `byteCount` 바이트를 제거하고 이를 현재 Sink에 추가합니다.
     *
     * @param source    읽어드릴 버퍼
     * @param byteCount 읽어들일 바이트 수
     */
    suspend fun write(source: Buffer, byteCount: Long)

    /**
     * 모든 버퍼링된 바이트를 최종 목적지로 전송합니다.
     */
    suspend fun flush()

    /**
     * 모든 버퍼링된 바이트를 최종 목적지로 전송하고 이 [AsyncSink]가 보유한 리소스를 해제합니다.
     */
    suspend fun close()

    /**
     * 이 [AsyncSink]의 [Timeout]을 반환합니다.
     */
    suspend fun timeout(): Timeout
}

/**
 * [AsyncSink]에 버퍼링하여 쓰기를 수행합니다.
 */
fun AsyncSink.buffered(): BufferedAsyncSink = RealBufferedAsyncSink(this)

@Deprecated("Use AsyncSink.buffered() instead.", ReplaceWith("buffered()"))
fun AsyncSink.buffer(): BufferedAsyncSink = RealBufferedAsyncSink(this)
