package io.bluetape4k.exposed.core.compress

import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.support.toUtf8Bytes
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class CompressionTransformersTest {

    @Test
    fun `Binary 압축 transformer 는 LZ4 원본을 복원한다`() {
        val source = "compress-binary-lz4".toUtf8Bytes()
        val transformer = CompressedBinaryTransformer(Compressors.LZ4)

        val compressed = transformer.unwrap(source)
        val restored = transformer.wrap(compressed)

        restored shouldBeEqualTo source
    }

    @Test
    fun `Binary 압축 transformer 는 Snappy 원본을 복원한다`() {
        val source = "compress-binary-snappy".toUtf8Bytes()
        val transformer = CompressedBinaryTransformer(Compressors.Snappy)

        val compressed = transformer.unwrap(source)
        val restored = transformer.wrap(compressed)

        restored shouldBeEqualTo source
    }

    @Test
    fun `Blob 압축 transformer 는 Zstd 원본을 복원한다`() {
        val source = "compress-blob-zstd".toUtf8Bytes()
        val transformer = CompressedBlobTransformer(Compressors.Zstd)

        val compressedBlob = transformer.unwrap(source)
        val restored = transformer.wrap(compressedBlob)

        restored shouldBeEqualTo source
    }
}
