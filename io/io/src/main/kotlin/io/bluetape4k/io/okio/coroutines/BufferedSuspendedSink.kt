package io.bluetape4k.io.okio.coroutines

import okio.Buffer
import okio.ByteString

/**
 * `BufferedSuspendedSink` 계약을 정의합니다.
 */
interface BufferedSuspendedSink: SuspendedSink {

    val buffer: Buffer

    /**
     * Okio 코루틴에서 데이터를 기록하는 `write` 함수를 제공합니다.
     */
    suspend fun write(byteString: ByteString): BufferedSuspendedSink

    /**
     * Okio 코루틴에서 데이터를 기록하는 `write` 함수를 제공합니다.
     */
    suspend fun write(source: ByteArray, offset: Int = 0, byteCount: Int = source.size): BufferedSuspendedSink

    /**
     * Okio 코루틴에서 데이터를 기록하는 `writeAll` 함수를 제공합니다.
     */
    suspend fun writeAll(source: SuspendedSource): Long

    /**
     * Okio 코루틴에서 데이터를 기록하는 `write` 함수를 제공합니다.
     */
    suspend fun write(source: SuspendedSource, byteCount: Long): BufferedSuspendedSink

    /**
     * Okio 코루틴에서 데이터를 기록하는 `writeUtf8` 함수를 제공합니다.
     */
    suspend fun writeUtf8(string: String, beginIndex: Int = 0, endIndex: Int = string.length): BufferedSuspendedSink

    /**
     * Okio 코루틴에서 데이터를 기록하는 `writeUtf8CodePoint` 함수를 제공합니다.
     */
    suspend fun writeUtf8CodePoint(codePoint: Int): BufferedSuspendedSink

    /**
     * Okio 코루틴에서 데이터를 기록하는 `writeByte` 함수를 제공합니다.
     */
    suspend fun writeByte(b: Int): BufferedSuspendedSink

    /**
     * Okio 코루틴에서 데이터를 기록하는 `writeShort` 함수를 제공합니다.
     */
    suspend fun writeShort(s: Int): BufferedSuspendedSink

    /**
     * Okio 코루틴에서 데이터를 기록하는 `writeShortLe` 함수를 제공합니다.
     */
    suspend fun writeShortLe(s: Int): BufferedSuspendedSink

    /**
     * Okio 코루틴에서 데이터를 기록하는 `writeInt` 함수를 제공합니다.
     */
    suspend fun writeInt(i: Int): BufferedSuspendedSink

    /**
     * Okio 코루틴에서 데이터를 기록하는 `writeIntLe` 함수를 제공합니다.
     */
    suspend fun writeIntLe(i: Int): BufferedSuspendedSink

    /**
     * Okio 코루틴에서 데이터를 기록하는 `writeLong` 함수를 제공합니다.
     */
    suspend fun writeLong(v: Long): BufferedSuspendedSink

    /**
     * Okio 코루틴에서 데이터를 기록하는 `writeLongLe` 함수를 제공합니다.
     */
    suspend fun writeLongLe(v: Long): BufferedSuspendedSink

    /**
     * Okio 코루틴에서 데이터를 기록하는 `writeDecimalLong` 함수를 제공합니다.
     */
    suspend fun writeDecimalLong(v: Long): BufferedSuspendedSink

    /**
     * Okio 코루틴에서 데이터를 기록하는 `writeHexadecimalUnsignedLong` 함수를 제공합니다.
     */
    suspend fun writeHexadecimalUnsignedLong(v: Long): BufferedSuspendedSink

    /**
     * Okio 코루틴 버퍼의 데이터를 실제 출력 대상으로 반영합니다.
     */
    override suspend fun flush()

    /**
     * Okio 코루틴에서 `emit` 함수를 제공합니다.
     */
    suspend fun emit(): BufferedSuspendedSink

    /**
     * Okio 코루틴에서 `emitCompleteSegments` 함수를 제공합니다.
     */
    suspend fun emitCompleteSegments(): BufferedSuspendedSink
}
