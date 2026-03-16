package io.bluetape4k.aws.core

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class SdkBytesSupportTest {

    @Test
    fun `String toSdkBytes는 UTF-8 문자열을 보존한다`() {
        val value = "안녕하세요 aws"
        val sdkBytes = value.toSdkBytes()

        sdkBytes.asUtf8String() shouldBeEqualTo value
    }

    @Test
    fun `ByteArray toSdkBytes는 바이트 배열을 보존한다`() {
        val bytes = byteArrayOf(1, 2, 3, 4)
        val sdkBytes = bytes.toSdkBytes()

        sdkBytes.asByteArray().toList() shouldBeEqualTo bytes.toList()
    }

    @Test
    fun `ByteBuffer toSdkBytes는 버퍼 내용을 보존한다`() {
        val buffer = ByteBuffer.wrap(byteArrayOf(7, 8, 9))
        val sdkBytes = buffer.toSdkBytes()

        sdkBytes.asByteArray().toList() shouldBeEqualTo listOf(7.toByte(), 8.toByte(), 9.toByte())
    }
}

