package io.bluetape4k.io.okio

import io.bluetape4k.logging.KLogging
import okio.Buffer
import okio.ByteString.Companion.decodeHex
import okio.Deflater
import okio.Inflater
import okio.buffer
import okio.deflate
import okio.inflate
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class DeflateKotlinTest: AbstractOkioTest() {

    companion object: KLogging()

    @Test
    fun deflate() {
        val data = Buffer()
        val deflater = data.deflate()
        deflater.buffered().writeUtf8("Hi!").close()
        data.readByteString().hex() shouldBeEqualTo "789cf3c854040001ce00d3"
    }

    @Test
    fun `deflate with Deflater`() {
        val data = Buffer()
        val deflater = data.deflate(Deflater(0, true))
        deflater.buffered().writeUtf8("Hi!").close()
        data.readByteString().hex() shouldBeEqualTo "010300fcff486921"
    }

    @Test
    fun inflate() {
        val buffer = bufferOf("789cf3c854040001ce00d3".decodeHex())
        val inflated = buffer.inflate()
        inflated.buffer().readUtf8() shouldBeEqualTo "Hi!"
    }

    @Test
    fun `inflate with Inflater`() {
        val buffer = bufferOf("010300fcff486921".decodeHex())
        val inflated = buffer.inflate(Inflater(true))
        inflated.buffer().readUtf8() shouldBeEqualTo "Hi!"
    }
}
