package io.bluetape4k.redis.redisson.codec

import io.bluetape4k.io.serializer.BinarySerializationException
import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import java.nio.ByteBuffer

internal object ForyCodecDecodeSupport: KLogging() {
    fun logLegacyFailure(failure: Throwable) {
        log.error(failure) { "Fail to deserialize. throw BinarySerializationException." }
    }
}

internal class ForyCodecRuntime(
    val serializerFactory: () -> BinarySerializer,
    val readableViewFactory: (ByteBuf) -> ByteBuffer? = ::readOnlyReadableNioView,
    val copiedBytesFactory: (ByteBuf) -> ByteArray = ::copiedReadableBytes,
    val fallbackBufferFactory: (ByteArray) -> ByteBuf = Unpooled::wrappedBuffer,
)

internal fun copiedReadableBytes(buf: ByteBuf): ByteArray =
    ByteBufUtil.getBytes(buf, buf.readerIndex(), buf.readableBytes(), true)

internal fun readOnlyReadableNioView(buf: ByteBuf): ByteBuffer? {
    if (buf.nioBufferCount() != 1) return null

    return buf.nioBuffer(buf.readerIndex(), buf.readableBytes())
        .slice()
        .asReadOnlyBuffer()
}

internal fun tryReadOnlyReadableNioView(
    buf: ByteBuf,
    viewFactory: (ByteBuf) -> ByteBuffer?,
): ByteBuffer? =
    try {
        viewFactory(buf)
    } catch (_: Throwable) {
        null
    }

internal fun BinarySerializer.deserializeDirectWithLegacyNormalization(
    source: ByteBuffer,
    sourceSize: Int,
): Any? =
    try {
        deserializeFrom<Any>(source)
    } catch (failure: Throwable) {
        if (failure !is BinarySerializationException) {
            ForyCodecDecodeSupport.logLegacyFailure(failure)
        }
        val legacyCause = (failure as? BinarySerializationException)?.cause ?: failure
        throw BinarySerializationException("Fail to deserialize. bytesSize=$sourceSize", legacyCause)
    }

internal inline fun <T> decodeWithFallbackBuffer(
    bytes: ByteArray,
    bufferFactory: (ByteArray) -> ByteBuf,
    block: (ByteBuf) -> T,
): T {
    val fallbackBuf = bufferFactory(bytes)
    var operationFailure: Throwable? = null
    try {
        return block(fallbackBuf)
    } catch (failure: Throwable) {
        operationFailure = failure
        throw failure
    } finally {
        try {
            fallbackBuf.release()
        } catch (cleanupFailure: Throwable) {
            operationFailure?.addSuppressed(cleanupFailure) ?: throw cleanupFailure
        }
    }
}
