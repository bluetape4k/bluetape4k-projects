package io.bluetape4k.io.compressor

import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class ZipCompressorTest {

    companion object: KLogging()

    @Test
    fun `ZipCompressor 는 0 이하 bufferSize 를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            ZipCompressor(0)
        }
        assertFailsWith<IllegalArgumentException> {
            ZipCompressor(-1)
        }
    }
}
