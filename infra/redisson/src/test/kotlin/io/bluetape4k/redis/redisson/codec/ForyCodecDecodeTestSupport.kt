package io.bluetape4k.redis.redisson.codec

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.io.serializer.BinarySerializer
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import org.redisson.client.codec.BaseCodec
import org.redisson.client.handler.State
import org.redisson.client.protocol.Decoder
import org.redisson.client.protocol.Encoder
import java.nio.ByteBuffer

internal class RecordingForySerializer(
    private val copiedResult: (ByteArray) -> Any? = { error("Unexpected copied decode") },
    private val directResult: (ByteBuffer) -> Any? = { error("Unexpected direct decode") },
): BinarySerializer {

    var copiedCalls: Int = 0
        private set
    var directCalls: Int = 0
        private set
    var directBytes: ByteArray? = null
        private set

    override fun serialize(graph: Any?): ByteArray = error("Unexpected encode")

    @Suppress("UNCHECKED_CAST")
    override fun <T: Any> deserialize(bytes: ByteArray?): T? {
        copiedCalls++
        return copiedResult(bytes ?: byteArrayOf()) as? T
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T: Any> deserializeFrom(source: ByteBuffer): T? {
        directCalls++
        directBytes = ByteArray(source.remaining()).also { source.duplicate().get(it) }
        return directResult(source) as? T
    }
}

internal class RecordingFallbackCodec(
    private val decodeResult: (ByteArray) -> Any?,
): BaseCodec() {

    var decodeCalls: Int = 0
        private set
    var decodedBytes: ByteArray? = null
        private set

    private val encoder = Encoder { error("Unexpected fallback encode") }
    private val decoder = Decoder<Any> { buf: ByteBuf, _: State? ->
        decodeCalls++
        val bytes = ByteBufUtil.getBytes(buf, buf.readerIndex(), buf.readableBytes(), true)
        decodedBytes = bytes
        decodeResult(bytes)
    }

    override fun getValueEncoder(): Encoder = encoder

    override fun getValueDecoder(): Decoder<Any> = decoder
}

internal class CodecInputState private constructor(
    private val readerIndex: Int,
    private val writerIndex: Int,
    private val refCnt: Int,
    private val bytes: List<Byte>,
) {
    fun shouldRemainUnchanged(buf: ByteBuf) {
        buf.readerIndex() shouldBeEqualTo readerIndex
        buf.writerIndex() shouldBeEqualTo writerIndex
        buf.refCnt() shouldBeEqualTo refCnt
        ByteBufUtil.getBytes(buf, 0, buf.capacity(), true).toList() shouldBeEqualTo bytes

        buf.resetReaderIndex()
        buf.readerIndex() shouldBeEqualTo readerIndex
        buf.readerIndex(readerIndex)

        buf.resetWriterIndex()
        buf.writerIndex() shouldBeEqualTo writerIndex
        buf.writerIndex(writerIndex)
    }

    companion object {
        fun capture(buf: ByteBuf): CodecInputState =
            CodecInputState(
                readerIndex = buf.readerIndex(),
                writerIndex = buf.writerIndex(),
                refCnt = buf.refCnt(),
                bytes = ByteBufUtil.getBytes(buf, 0, buf.capacity(), true).toList(),
            )
    }
}

internal fun framedCodecInput(
    payload: ByteArray,
    direct: Boolean = false,
): ByteBuf {
    val prefixSize = 3
    val suffixSize = 4
    val buf =
        if (direct) {
            Unpooled.directBuffer(prefixSize + payload.size + suffixSize)
        } else {
            Unpooled.buffer(prefixSize + payload.size + suffixSize)
        }
    buf.writeBytes(byteArrayOf(11, 12, 13))
    buf.writeBytes(payload)
    buf.writeBytes(byteArrayOf(21, 22, 23, 24))
    buf.setIndex(prefixSize, prefixSize + payload.size)
    buf.markReaderIndex()
    buf.markWriterIndex()
    return buf
}
