package io.bluetape4k.io.okio.coroutines

import okio.Buffer
import okio.ByteString

@Deprecated(
    "Use SuspendSink instead.",
    ReplaceWith("SuspendSink")
)
interface BufferedAsyncSink: AsyncSink {

    val buffer: Buffer

    suspend fun write(byteString: ByteString): BufferedAsyncSink

    suspend fun write(source: ByteArray, offset: Int = 0, byteCount: Int = source.size): BufferedAsyncSink

    suspend fun writeAll(source: AsyncSource): Long

    suspend fun write(source: AsyncSource, byteCount: Long): BufferedAsyncSink

    suspend fun writeUtf8(string: String, beginIndex: Int = 0, endIndex: Int = string.length): BufferedAsyncSink

    suspend fun writeUtf8CodePoint(codePoint: Int): BufferedAsyncSink

    suspend fun writeByte(b: Int): BufferedAsyncSink

    suspend fun writeShort(s: Int): BufferedAsyncSink

    suspend fun writeShortLe(s: Int): BufferedAsyncSink

    suspend fun writeInt(i: Int): BufferedAsyncSink

    suspend fun writeIntLe(i: Int): BufferedAsyncSink

    suspend fun writeLong(v: Long): BufferedAsyncSink

    suspend fun writeLongLe(v: Long): BufferedAsyncSink

    suspend fun writeDecimalLong(v: Long): BufferedAsyncSink

    suspend fun writeHexadecimalUnsignedLong(v: Long): BufferedAsyncSink

    override suspend fun flush()

    suspend fun emit(): BufferedAsyncSink

    suspend fun emitCompleteSegments(): BufferedAsyncSink
}
