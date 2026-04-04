package io.bluetape4k.okio.coroutines

import okio.Buffer
import okio.ByteString

/**
 * 내부 버퍼를 사용해 효율적인 쓰기를 제공하는 코루틴 Sink 인터페이스.
 *
 * ```kotlin
 * val output = Buffer()
 * val sink: BufferedSuspendedSink = (output as okio.Sink).asSuspended().buffered()
 * sink.writeUtf8("hello")
 * sink.flush()
 * val text = output.readUtf8()
 * // text == "hello"
 * ```
 */
interface BufferedSuspendedSink: SuspendedSink {

    val buffer: Buffer

    /**
     * [byteString]을 Sink 버퍼에 씁니다.
     *
     * ```kotlin
     * val output = Buffer()
     * val sink = (output as okio.Sink).asSuspended().buffered()
     * val bs = ByteString.encodeUtf8("hi")
     * sink.write(bs)
     * sink.flush()
     * val size = output.size
     * // size == 2L
     * ```
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
     * UTF-8 문자열을 Sink 버퍼에 씁니다.
     *
     * ```kotlin
     * val output = Buffer()
     * val sink = (output as okio.Sink).asSuspended().buffered()
     * sink.writeUtf8("hello world", 0, 5)
     * sink.flush()
     * val text = output.readUtf8()
     * // text == "hello"
     * ```
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
