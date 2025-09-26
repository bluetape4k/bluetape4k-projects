package io.bluetape4k.io.okio.coroutines

import okio.Buffer
import okio.ByteString

interface BufferedSuspendedSink: SuspendedSink {

    val buffer: Buffer

    suspend fun write(byteString: ByteString): BufferedSuspendedSink

    suspend fun write(source: ByteArray, offset: Int = 0, byteCount: Int = source.size): BufferedSuspendedSink

    suspend fun writeAll(source: SuspendedSource): Long

    suspend fun write(source: SuspendedSource, byteCount: Long): BufferedSuspendedSink

    suspend fun writeUtf8(string: String, beginIndex: Int = 0, endIndex: Int = string.length): BufferedSuspendedSink

    suspend fun writeUtf8CodePoint(codePoint: Int): BufferedSuspendedSink

    suspend fun writeByte(b: Int): BufferedSuspendedSink

    suspend fun writeShort(s: Int): BufferedSuspendedSink

    suspend fun writeShortLe(s: Int): BufferedSuspendedSink

    suspend fun writeInt(i: Int): BufferedSuspendedSink

    suspend fun writeIntLe(i: Int): BufferedSuspendedSink

    suspend fun writeLong(v: Long): BufferedSuspendedSink

    suspend fun writeLongLe(v: Long): BufferedSuspendedSink

    suspend fun writeDecimalLong(v: Long): BufferedSuspendedSink

    suspend fun writeHexadecimalUnsignedLong(v: Long): BufferedSuspendedSink

    override suspend fun flush()

    suspend fun emit(): BufferedSuspendedSink

    suspend fun emitCompleteSegments(): BufferedSuspendedSink
}
