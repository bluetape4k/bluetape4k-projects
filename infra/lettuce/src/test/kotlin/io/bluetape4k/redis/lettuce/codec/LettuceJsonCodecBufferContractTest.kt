package io.bluetape4k.redis.lettuce.codec

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.json.JsonSerializer
import io.netty.buffer.ByteBuf
import io.netty.buffer.PooledByteBufAllocator
import io.netty.buffer.Unpooled
import org.junit.jupiter.api.Test
import java.io.IOException
import java.io.OutputStream
import java.lang.reflect.Modifier
import java.nio.ByteBuffer

class LettuceJsonCodecBufferContractTest {

    @Test
    fun `built in JSON target encoding uses the stream path and preserves caller state`() {
        jsonTargets().forEach { (name, target) ->
            val serializer = RecordingJsonSerializer()
            val codec = LettuceJsonCodec(serializer, String::class.java)
            try {
                target.writeByte(PREFIX)
                target.markReaderIndex()
                target.markWriterIndex()
                val start = target.writerIndex()
                val referenceCount = target.refCnt()

                codec.encodeValue(VALUE, target)

                serializer.streamCalls shouldBeEqualTo 1
                serializer.arrayCalls shouldBeEqualTo 0
                target.writerIndex() shouldBeEqualTo start + JSON_WIRE.size
                target.readerIndex() shouldBeEqualTo 0
                target.refCnt() shouldBeEqualTo referenceCount
                target.bytes(0, target.writerIndex()).contentEquals(byteArrayOf(PREFIX.toByte()) + JSON_WIRE).shouldBeTrue()
                target.resetReaderIndex()
                target.resetWriterIndex()
                target.readerIndex() shouldBeEqualTo 0
                target.writerIndex() shouldBeEqualTo start
            } catch (failure: Throwable) {
                throw AssertionError("JSON target fixture failed: $name", failure)
            } finally {
                target.release()
            }
        }
    }

    @Test
    fun `one argument and target JSON encodings keep identical wire`() {
        val serializer = RecordingJsonSerializer()
        val codec = LettuceJsonCodec(serializer, String::class.java)
        val target = Unpooled.buffer(1, 64)
        try {
            codec.encodeValue(VALUE, target)

            target.bytes(0, target.writerIndex())
                .contentEquals(codec.encodeValue(VALUE).remainingBytes())
                .shouldBeTrue()
        } finally {
            target.release()
        }
    }

    @Test
    fun `null JSON target returns before serializer dispatch`() {
        val serializer = RecordingJsonSerializer()

        LettuceJsonCodec(serializer, String::class.java).encodeValue(VALUE, null)

        serializer.streamCalls shouldBeEqualTo 0
        serializer.arrayCalls shouldBeEqualTo 0
    }

    @Test
    fun `JSON count mismatch and state drift fail without commit`() {
        val mismatch = RecordingJsonSerializer().apply {
            streamBehavior = { output ->
                output.write(JSON_WIRE)
                JSON_WIRE.size - 1
            }
        }
        val mismatchTarget = Unpooled.buffer(8, 64)
        try {
            mismatchTarget.writeByte(PREFIX)
            val start = mismatchTarget.writerIndex()
            val failure = assertFailsWith<IllegalStateException> {
                LettuceJsonCodec(mismatch, String::class.java).encodeValue(VALUE, mismatchTarget)
            }
            failure.message shouldBeEqualTo
                    "Serializer reported ${JSON_WIRE.size - 1} bytes but wrote ${JSON_WIRE.size} bytes."
            mismatchTarget.writerIndex() shouldBeEqualTo start
        } finally {
            mismatchTarget.release()
        }

        val driftTarget = Unpooled.buffer(8, 64)
        try {
            driftTarget.writeBytes(byteArrayOf(PREFIX.toByte(), 0x22))
            val drift = RecordingJsonSerializer().apply {
                streamBehavior = { output ->
                    output.write(JSON_WIRE)
                    driftTarget.readerIndex(1)
                    JSON_WIRE.size
                }
            }
            val failure = assertFailsWith<IllegalStateException> {
                LettuceJsonCodec(drift, String::class.java).encodeValue(VALUE, driftTarget)
            }
            failure.message shouldBeEqualTo "Target ByteBuf state changed during serialization."
        } finally {
            driftTarget.release()
        }
    }

    @Test
    fun `JSON serializer failure wins over drift and leaves attempted bytes uncommitted`() {
        val sentinel = IOException("JSON serializer failure")
        val target = Unpooled.buffer(8, 64)
        try {
            target.writeByte(PREFIX)
            val start = target.writerIndex()
            val serializer = RecordingJsonSerializer().apply {
                streamBehavior = { output ->
                    output.write(JSON_WIRE)
                    target.writerIndex(start + 1)
                    throw sentinel
                }
            }

            val actual = assertFailsWith<IOException> {
                LettuceJsonCodec(serializer, String::class.java).encodeValue(VALUE, target)
            }

            actual shouldBeSameInstanceAs sentinel
            target.writerIndex() shouldBeEqualTo start + 1
        } finally {
            target.release()
        }
    }

    @Test
    fun `retained JSON adapter is sealed after success`() {
        val serializer = RecordingJsonSerializer().apply {
            streamBehavior = { output ->
                retainedOutput = output
                output.write(JSON_WIRE)
                JSON_WIRE.size
            }
        }
        val target = Unpooled.buffer(8, 64)
        try {
            LettuceJsonCodec(serializer, String::class.java).encodeValue(VALUE, target)
            val committed = target.writerIndex()

            val failure = assertFailsWith<IOException> {
                requireNotNull(serializer.retainedOutput).write(0x33)
            }

            failure.message shouldBeEqualTo "Bounded ByteBuf output stream is sealed."
            target.writerIndex() shouldBeEqualTo committed
        } finally {
            target.release()
        }
    }

    @Test
    fun `JSON target exhaustion does not commit or expose the value`() {
        val secret = "issue756-json-secret"
        val target = Unpooled.buffer(0, 0)
        try {
            val failure = assertFailsWith<IllegalStateException> {
                LettuceJsonCodec(RecordingJsonSerializer(), String::class.java).encodeValue(secret, target)
            }

            target.writerIndex() shouldBeEqualTo 0
            (failure.message?.contains(secret) == false).shouldBeTrue()
        } finally {
            target.release()
        }
    }

    @Test
    fun `JSON codec remains final without a new target extension seam`() {
        Modifier.isFinal(LettuceJsonCodec::class.java.modifiers).shouldBeTrue()
    }

    private class RecordingJsonSerializer: JsonSerializer {
        var arrayCalls: Int = 0
            private set
        var streamCalls: Int = 0
            private set
        var retainedOutput: OutputStream? = null
        var streamBehavior: (OutputStream) -> Int = { output ->
            output.write(JSON_WIRE)
            JSON_WIRE.size
        }

        override fun serialize(graph: Any?): ByteArray = JSON_WIRE.copyOf().also { arrayCalls++ }

        override fun serializeJsonToStream(graph: Any?, target: OutputStream): Int {
            streamCalls++
            return streamBehavior(target)
        }

        override fun <T: Any> deserialize(bytes: ByteArray?, clazz: Class<T>): T? = null
    }

    private fun jsonTargets(): List<Pair<String, ByteBuf>> = listOf(
        "unpooled heap" to Unpooled.buffer(4, 64),
        "unpooled direct" to Unpooled.directBuffer(4, 64),
        "pooled heap" to PooledByteBufAllocator.DEFAULT.heapBuffer(4, 64),
        "pooled direct" to PooledByteBufAllocator.DEFAULT.directBuffer(4, 64),
        "bounded slice" to Unpooled.buffer(64, 64).slice(0, 16).clear(),
    )

    private fun ByteBuf.bytes(index: Int, length: Int): ByteArray =
        ByteArray(length).also { bytes -> getBytes(index, bytes) }

    private fun ByteBuffer.remainingBytes(): ByteArray =
        ByteArray(remaining()).also { bytes -> duplicate().get(bytes) }

    private companion object {
        const val PREFIX: Int = 0x5A
        const val VALUE: String = "json-contract"
        val JSON_WIRE: ByteArray = "\"value\"".encodeToByteArray()
    }
}
