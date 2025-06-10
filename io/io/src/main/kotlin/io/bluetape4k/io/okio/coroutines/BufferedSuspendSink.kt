package io.bluetape4k.io.okio.coroutines

import okio.Buffer
import okio.ByteString

interface BufferedSuspendSink: SuspendSink {

    val buffer: Buffer

    suspend fun write(byteString: ByteString): BufferedSuspendSink

    suspend fun write(source: ByteArray, offset: Int = 0, byteCount: Int = source.size): BufferedSuspendSink

    suspend fun writeAll(source: SuspendSource): Long

    suspend fun write(source: SuspendSource, byteCount: Long): BufferedSuspendSink

    suspend fun writeUtf8(string: String, beginIndex: Int = 0, endIndex: Int = string.length): BufferedSuspendSink

    suspend fun writeUtf8CodePoint(codePoint: Int): BufferedSuspendSink

    suspend fun writeByte(b: Int): BufferedSuspendSink

    suspend fun writeShort(s: Int): BufferedSuspendSink

    suspend fun writeShortLe(s: Int): BufferedSuspendSink

    suspend fun writeInt(i: Int): BufferedSuspendSink

    suspend fun writeIntLe(i: Int): BufferedSuspendSink

    suspend fun writeLong(v: Long): BufferedSuspendSink

    suspend fun writeLongLe(v: Long): BufferedSuspendSink

    suspend fun writeDecimalLong(v: Long): BufferedSuspendSink

    suspend fun writeHexadecimalUnsignedLong(v: Long): BufferedSuspendSink

    override suspend fun flush()

    suspend fun emit(): BufferedSuspendSink

    suspend fun emitCompleteSegments(): BufferedSuspendSink
}
