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
    fun `Binary 압축 transformer 는 Zstd 원본을 복원한다`() {
        val source = "compress-binary-zstd".toUtf8Bytes()
        val transformer = CompressedBinaryTransformer(Compressors.Zstd)

        val compressed = transformer.unwrap(source)
        val restored = transformer.wrap(compressed)

        restored shouldBeEqualTo source
    }

    @Test
    fun `Binary 압축 transformer 는 BZip2 원본을 복원한다`() {
        val source = "compress-binary-bzip2".toUtf8Bytes()
        val transformer = CompressedBinaryTransformer(Compressors.BZip2)

        val compressed = transformer.unwrap(source)
        val restored = transformer.wrap(compressed)

        restored shouldBeEqualTo source
    }

    @Test
    fun `Binary 압축 transformer 는 Deflate 원본을 복원한다`() {
        val source = "compress-binary-deflate".toUtf8Bytes()
        val transformer = CompressedBinaryTransformer(Compressors.Deflate)

        val compressed = transformer.unwrap(source)
        val restored = transformer.wrap(compressed)

        restored shouldBeEqualTo source
    }

    @Test
    fun `Binary 압축 transformer 는 GZip 원본을 복원한다`() {
        val source = "compress-binary-gzip".toUtf8Bytes()
        val transformer = CompressedBinaryTransformer(Compressors.GZip)

        val compressed = transformer.unwrap(source)
        val restored = transformer.wrap(compressed)

        restored shouldBeEqualTo source
    }

    @Test
    fun `Blob 압축 transformer 는 LZ4 원본을 복원한다`() {
        val source = "compress-blob-lz4".toUtf8Bytes()
        val transformer = CompressedBlobTransformer(Compressors.LZ4)

        val compressedBlob = transformer.unwrap(source)
        val restored = transformer.wrap(compressedBlob)

        restored shouldBeEqualTo source
    }

    @Test
    fun `Blob 압축 transformer 는 Snappy 원본을 복원한다`() {
        val source = "compress-blob-snappy".toUtf8Bytes()
        val transformer = CompressedBlobTransformer(Compressors.Snappy)

        val compressedBlob = transformer.unwrap(source)
        val restored = transformer.wrap(compressedBlob)

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

    @Test
    fun `Blob 압축 transformer 는 BZip2 원본을 복원한다`() {
        val source = "compress-blob-bzip2".toUtf8Bytes()
        val transformer = CompressedBlobTransformer(Compressors.BZip2)

        val compressedBlob = transformer.unwrap(source)
        val restored = transformer.wrap(compressedBlob)

        restored shouldBeEqualTo source
    }

    @Test
    fun `Blob 압축 transformer 는 Deflate 원본을 복원한다`() {
        val source = "compress-blob-deflate".toUtf8Bytes()
        val transformer = CompressedBlobTransformer(Compressors.Deflate)

        val compressedBlob = transformer.unwrap(source)
        val restored = transformer.wrap(compressedBlob)

        restored shouldBeEqualTo source
    }

    @Test
    fun `Blob 압축 transformer 는 GZip 원본을 복원한다`() {
        val source = "compress-blob-gzip".toUtf8Bytes()
        val transformer = CompressedBlobTransformer(Compressors.GZip)

        val compressedBlob = transformer.unwrap(source)
        val restored = transformer.wrap(compressedBlob)

        restored shouldBeEqualTo source
    }
}
