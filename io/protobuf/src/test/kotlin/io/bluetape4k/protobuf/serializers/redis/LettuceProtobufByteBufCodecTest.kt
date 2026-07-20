package io.bluetape4k.protobuf.serializers.redis

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.protobuf.ProtoAny
import io.bluetape4k.protobuf.redis.messages.redisSimpleMessage
import io.bluetape4k.protobuf.serializers.ProtobufSerializer
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.junit.jupiter.api.Test
import java.lang.reflect.Modifier
import java.lang.reflect.Proxy

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
            val shortCodec = injectedCodec(serializer) { buffer, index ->
                buffer.setByte(index, 0x7F)
                expectedSize - 1
            }

            assertFailsWith<IllegalStateException> {
                shortCodec.encodeValue(message, target)
            }
            target.writerIndex() shouldBeEqualTo start
            target.getUnsignedByte(start) shouldBeEqualTo 0x7F

            target.setZero(start, expectedSize)
            val failedCodec = injectedCodec(serializer) { buffer, index ->
                buffer.setByte(index, 0x5E)
                throw IllegalArgumentException("injected write failure")
            }
            assertFailsWith<IllegalArgumentException> {
                failedCodec.encodeValue(message, target)
            }
            target.writerIndex() shouldBeEqualTo start
            target.getUnsignedByte(start) shouldBeEqualTo 0x5E
        } finally {
            target.release()
        }
    }

    @Test
    fun `null target is a no-op`() {
        LettuceProtobufCodecs.protobuf<Any>().encodeValue(Any(), null)
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
}
