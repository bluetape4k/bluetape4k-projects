package io.bluetape4k.io.okio

import io.bluetape4k.logging.KLogging
import okio.Buffer
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class BufferCursorKotlinTest: AbstractOkioTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5

        private val _factories = BufferFactory.factories
        private val _buffers by lazy { _factories.map { it.newBuffer() } }
    }

    fun factories() = _factories
    fun buffers() = _buffers

    @ParameterizedTest
    @MethodSource("factories")
    fun `read unsafe cursor`(factory: BufferFactory) {
        val cursor = Buffer.UnsafeCursor()

        val buffer = factory.newBuffer()
        buffer.readUnsafe(cursor)
        cursor.buffer shouldBeEqualTo buffer
        cursor.readWrite.shouldBeFalse()

        cursor.close()
        cursor.buffer.shouldBeNull()
    }

    @ParameterizedTest
    @MethodSource("factories")
    fun `read write unsafe cursor`(factory: BufferFactory) {
        val cursor = Buffer.UnsafeCursor()

        val buffer = factory.newBuffer()
        buffer.readAndWriteUnsafe(cursor)
        cursor.buffer shouldBeEqualTo buffer
        cursor.readWrite.shouldBeTrue()

        cursor.close()
        cursor.buffer.shouldBeNull()
    }
}
