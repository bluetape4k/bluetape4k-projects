package io.bluetape4k.protobuf.serializers.redis

import com.google.protobuf.Message
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.io.serializer.BinarySerializationException
import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.protobuf.ProtoAny
import io.bluetape4k.protobuf.redis.messages.redisSimpleMessage
import io.bluetape4k.protobuf.serializers.ProtobufSerializer
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.netty.buffer.ByteBuf
import io.netty.buffer.SwappedByteBuf
import io.netty.buffer.Unpooled
import io.netty.util.IllegalReferenceCountException
import org.junit.jupiter.api.Test
import java.lang.reflect.Modifier
import java.lang.reflect.Proxy
import java.nio.ByteBuffer

class LettuceProtobufByteBufCodecTest {

    @Test
    fun `uncompressed factories use a private optimized subtype`() {
        val strict = LettuceProtobufCodecs.protobuf<Any>()
        val trusted = LettuceProtobufCodecs.trustedInternalProtobuf<Any>()
        val compressed = LettuceProtobufCodecs.gzipProtobuf<Any>()

        strict.javaClass shouldBeEqualTo trusted.javaClass
        (strict.javaClass != compressed.javaClass) shouldBeEqualTo true
        Modifier.isPrivate(strict.javaClass.modifiers) shouldBeEqualTo true
        strict.javaClass.declaredConstructors.none {
            Modifier.isPublic(it.modifiers) || Modifier.isProtected(it.modifiers)
        } shouldBeEqualTo true
    }

    @Test
    fun `protobuf value is written into caller owned heap and direct targets`() {
        val codec = LettuceProtobufCodecs.protobuf<Any>()
        val message = redisSimpleMessage {
            id = 757
            name = "bounded-bytebuf"
            description = "absolute-index"
        }
        val expected = ProtoAny.pack(message).toByteArray()

        val targets = listOf(
            Unpooled.buffer(8, 4096),
            Unpooled.directBuffer(8, 4096),
            Unpooled.compositeBuffer().apply {
                addComponents(true, Unpooled.buffer(4, 2048), Unpooled.buffer(4, 2048))
            },
            Unpooled.buffer(4096).apply { writerIndex(1) }.slice(0, 4096),
        )

        targets.forEach { target ->
            assertCallerOwnedWrite(codec, message, expected, target)
        }
    }

    @Test
    fun `short or failed absolute write does not commit writer index`() {
        val serializer = ProtobufSerializer()
        val message = redisSimpleMessage {
            id = 757
            name = "failure-aftercare"
        }
        val expectedSize = ProtoAny.pack(message).serializedSize
        val target = Unpooled.buffer(expectedSize + 8)
        try {
            target.writeByte(0x33)
            val start = target.writerIndex()
            target.markReaderIndex()
            target.markWriterIndex()
            val shortCodec = injectedCodec(serializer) { buffer, index ->
                buffer.setByte(index, 0x7F)
                expectedSize - 1
            }

            assertFailsWith<IllegalStateException> {
                shortCodec.encodeValue(message, target)
            }
            target.refCnt() shouldBeEqualTo 1
            target.readerIndex() shouldBeEqualTo 0
            target.writerIndex() shouldBeEqualTo start
            target.getUnsignedByte(start) shouldBeEqualTo 0x7F
            target.resetReaderIndex()
            target.resetWriterIndex()
            target.writerIndex() shouldBeEqualTo start

            target.setZero(start, expectedSize)
            target.markReaderIndex()
            target.markWriterIndex()
            val failedCodec = injectedCodec(serializer) { buffer, index ->
                buffer.setByte(index, 0x5E)
                throw IllegalArgumentException("injected write failure")
            }
            assertFailsWith<IllegalArgumentException> {
                failedCodec.encodeValue(message, target)
            }
            target.refCnt() shouldBeEqualTo 1
            target.readerIndex() shouldBeEqualTo 0
            target.writerIndex() shouldBeEqualTo start
            target.getUnsignedByte(start) shouldBeEqualTo 0x5E
            target.resetReaderIndex()
            target.resetWriterIndex()
            target.writerIndex() shouldBeEqualTo start
        } finally {
            target.release()
        }
    }

    @Test
    fun `capacity and target accessibility failures preserve committed indices`() {
        val codec = LettuceProtobufCodecs.protobuf<Any>()
        val message = redisSimpleMessage { id = 757 }
        val size = ProtoAny.pack(message).serializedSize

        val bounded = Unpooled.buffer(0, size - 1)
        try {
            bounded.markReaderIndex()
            bounded.markWriterIndex()
            assertFailsWith<IndexOutOfBoundsException> {
                codec.encodeValue(message, bounded)
            }
            assertUncommittedState(bounded, 0)
        } finally {
            bounded.release()
        }

        val readOnly = Unpooled.unmodifiableBuffer(Unpooled.buffer(size, size))
        try {
            readOnly.markReaderIndex()
            readOnly.markWriterIndex()
            assertFailsWith<UnsupportedOperationException> {
                codec.encodeValue(message, readOnly)
            }
            assertUncommittedState(readOnly, 0)
        } finally {
            readOnly.release()
        }

        val released = Unpooled.buffer(size, size)
        released.release()
        assertFailsWith<IllegalReferenceCountException> {
            codec.encodeValue(message, released)
        }
        released.refCnt() shouldBeEqualTo 0
    }

    @Test
    fun `null target avoids protobuf work and pack failures retain their cause`() {
        val codec = LettuceProtobufCodecs.protobuf<Any>()
        val message = mockk<Message>()
        every { message.descriptorForType } throws AssertionError("protobuf work")

        codec.encodeValue(message, null)
        verify(exactly = 0) { message.descriptorForType }

        val target = Unpooled.buffer(64)
        try {
            target.writeByte(0x33)
            target.markReaderIndex()
            target.markWriterIndex()
            val failure = assertFailsWith<BinarySerializationException> {
                codec.encodeValue(message, target)
            }
            failure.message!!.startsWith("Fail to serialize. graphType=") shouldBeEqualTo true
            (failure.cause is AssertionError) shouldBeEqualTo true
            assertUncommittedState(target, 1)
        } finally {
            target.release()
        }
    }

    @Test
    fun `direct writer does not request a nio view`() {
        val codec = LettuceProtobufCodecs.protobuf<Any>()
        val message = redisSimpleMessage { id = 757 }
        val owner = Unpooled.buffer(256)
        val target = ThrowingNioByteBuf(owner)
        try {
            codec.encodeValue(message, target)
            target.nioCalls shouldBeEqualTo 0
        } finally {
            target.release()
        }
    }

    @Test
    fun `public codec ABI stays open only at the target overload`() {
        val codecClass = LettuceBinaryCodec::class.java
        Modifier.isPublic(codecClass.modifiers) shouldBeEqualTo true
        Modifier.isFinal(codecClass.modifiers) shouldBeEqualTo false
        codecClass.getDeclaredConstructor(BinarySerializer::class.java)

        val targetMethod = codecClass.getMethod(
            "encodeValue",
            Any::class.java,
            ByteBuf::class.java,
        )
        Modifier.isFinal(targetMethod.modifiers) shouldBeEqualTo false

        listOf(
            codecClass.getMethod("encodeKey", String::class.java),
            codecClass.getMethod("encodeKey", String::class.java, ByteBuf::class.java),
            codecClass.getMethod("encodeValue", Any::class.java),
            codecClass.getMethod("decodeKey", java.nio.ByteBuffer::class.java),
            codecClass.getMethod("decodeValue", java.nio.ByteBuffer::class.java),
            codecClass.getMethod("estimateSize", Any::class.java),
            codecClass.getMethod("toString"),
        ).forEach { method ->
            Modifier.isFinal(method.modifiers) shouldBeEqualTo true
        }

        codecClass.getMethod("getSerializer").returnType shouldBeEqualTo BinarySerializer::class.java
        val optimized = LettuceProtobufCodecs.protobuf<Any>().javaClass
        optimized.enclosingClass shouldBeEqualTo LettuceProtobufCodecs::class.java
        Modifier.isPrivate(optimized.modifiers) shouldBeEqualTo true
    }

    @Suppress("UNCHECKED_CAST")
    private fun injectedCodec(
        serializer: ProtobufSerializer,
        write: (ByteBuf, Int) -> Int,
    ): LettuceBinaryCodec<Any> {
        val codec = LettuceProtobufCodecs.protobuf<Any>()
        val constructor = codec.javaClass.declaredConstructors.single { candidate ->
            !candidate.isSynthetic &&
                candidate.parameterCount == 2 &&
                candidate.parameterTypes[0] == ProtobufSerializer::class.java
        }.apply { isAccessible = true }
        val writerType = constructor.parameterTypes[1]
        val writer = Proxy.newProxyInstance(
            writerType.classLoader,
            arrayOf(writerType),
        ) { proxy, method, arguments ->
            when (method.name) {
                "write" -> {
                    requireNotNull(arguments)
                    write(arguments[1] as ByteBuf, arguments[2] as Int)
                }
                "toString" -> "InjectedPackedAnyWriter"
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === arguments?.singleOrNull()
                else -> error("Unexpected writer method: ${method.name}")
            }
        }
        return constructor.newInstance(serializer, writer) as LettuceBinaryCodec<Any>
    }

    private fun assertCallerOwnedWrite(
        codec: LettuceBinaryCodec<Any>,
        message: Any,
        expected: ByteArray,
        target: ByteBuf,
    ) {
        try {
            target.clear()
            target.writeByte(0x5A)
            target.markReaderIndex()
            target.markWriterIndex()
            val beforeRefCnt = target.refCnt()
            val start = target.writerIndex()

            codec.encodeValue(message, target)

            target.refCnt() shouldBeEqualTo beforeRefCnt
            target.readerIndex() shouldBeEqualTo 0
            target.writerIndex() shouldBeEqualTo start + expected.size
            target.getUnsignedByte(0) shouldBeEqualTo 0x5A
            val actual = ByteArray(expected.size)
            target.getBytes(start, actual)
            actual.contentEquals(expected) shouldBeEqualTo true
            codec.decodeValue(codec.encodeValue(message)) shouldBeEqualTo message
            target.resetReaderIndex()
            target.resetWriterIndex()
            target.writerIndex() shouldBeEqualTo start
        } finally {
            target.release()
            target.refCnt() shouldBeEqualTo 0
        }
    }

    private fun assertUncommittedState(target: ByteBuf, expectedWriterIndex: Int) {
        target.refCnt() shouldBeEqualTo 1
        target.readerIndex() shouldBeEqualTo 0
        target.writerIndex() shouldBeEqualTo expectedWriterIndex
        target.resetReaderIndex()
        target.resetWriterIndex()
        target.writerIndex() shouldBeEqualTo expectedWriterIndex
    }

    private class ThrowingNioByteBuf(delegate: ByteBuf): SwappedByteBuf(delegate) {
        var nioCalls: Int = 0
            private set

        override fun nioBuffer(index: Int, length: Int): ByteBuffer {
            nioCalls++
            throw AssertionError("direct writer must not request a NIO view")
        }
    }
}
