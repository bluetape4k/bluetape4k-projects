package io.bluetape4k.io.okio.coroutines

import io.bluetape4k.io.okio.SEGMENT_SIZE
import okio.Buffer
import okio.Timeout
import java.io.IOException

/**
 * Coroutines 방식으로 [okio.Source] 기능을 제공하는 인터페이스
 */
/**
 * `SuspendedSource` 계약을 정의합니다.
 */
interface SuspendedSource {
    companion object {
        const val MAX_NO_PROGRESS_READS = 8
    }

    /**
     * 이 소스에서 최소 1바이트 이상, 최대 `byteCount` 바이트를 제거하고 `sink`에 추가합니다.
     * 읽어들인 바이트 수를 반환하거나, 이 소스가 고갈된 경우 -1을 반환합니다.
     *
     * @param sink      읽어들일 버퍼
     * @param byteCount 읽어들일 바이트 수
     * @return 실제로 읽어들인 바이트 수
     */
    suspend fun read(sink: Buffer, byteCount: Long = Long.MAX_VALUE): Long

    /**
     * 모든 버퍼링된 바이트를 최종 목적지로 전송하고 이 [SuspendedSource]가 보유한 리소스를 해제합니다.
     */
    suspend fun close()

    /**
     * 이 [SuspendedSource]의 [Timeout]을 반환합니다.
     */
    fun timeout(): Timeout = Timeout.NONE

    /**
     * Okio 코루틴에서 데이터를 읽어오는 `readAll` 함수를 제공합니다.
     */
    suspend fun readAll(sink: Buffer): Long {
        var totalBytesRead = 0L
        var noProgressCount = 0
        while (true) {
            val bytesToRead = read(sink, SEGMENT_SIZE)
            if (bytesToRead == -1L) break
            if (bytesToRead == 0L) {
                noProgressCount++
                if (noProgressCount >= MAX_NO_PROGRESS_READS) {
                    throw IOException("Unable to read all bytes from suspended source: no progress.")
                }
                continue
            }
            noProgressCount = 0
            totalBytesRead += bytesToRead
        }
        return totalBytesRead
    }
}
