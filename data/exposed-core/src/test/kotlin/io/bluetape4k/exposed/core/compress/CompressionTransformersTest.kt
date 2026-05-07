package io.bluetape4k.exposed.core.compress

import io.bluetape4k.exposed.core.statements.api.toExposedBlob
import io.bluetape4k.io.compressor.Compressor
import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeLessThan
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

/**
 * 특정 예외를 던지는 테스트용 Compressor 구현체.
 * CancellationException 재전파 검증에 사용한다.
 */
private class ThrowingCompressor(private val exception: Exception) : Compressor {
    override fun compress(plain: ByteArray?): ByteArray = throw exception
    override fun decompress(compressed: ByteArray?): ByteArray = throw exception
}

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

    @Test
    fun `Binary 압축 transformer 는 빈 바이트 배열을 압축하고 복원한다`() {
        val source = ByteArray(0)
        val transformer = CompressedBinaryTransformer(Compressors.LZ4)

        val compressed = transformer.unwrap(source)
        val restored = transformer.wrap(compressed)

        restored shouldBeEqualTo source
    }

    @Test
    fun `Blob 압축 transformer 는 빈 바이트 배열을 압축하고 복원한다`() {
        val source = ByteArray(0)
        val transformer = CompressedBlobTransformer(Compressors.LZ4)

        val compressedBlob = transformer.unwrap(source)
        val restored = transformer.wrap(compressedBlob)

        restored shouldBeEqualTo source
    }

    @Test
    fun `Binary 압축 transformer 는 대용량 데이터를 압축하고 복원한다`() {
        val source = ByteArray(100_000) { it.toByte() }
        val transformer = CompressedBinaryTransformer(Compressors.LZ4)

        val compressed = transformer.unwrap(source)
        val restored = transformer.wrap(compressed)

        restored shouldBeEqualTo source
    }

    @Test
    fun `Binary 압축 데이터는 원본보다 작거나 같아야 한다 (반복 패턴)`() {
        val source = "aaaaabbbbbccccc".repeat(500).toUtf8Bytes()
        val transformer = CompressedBinaryTransformer(Compressors.LZ4)

        val compressed = transformer.unwrap(source)

        compressed.size shouldBeLessThan source.size
    }

    /**
     * compressor 가 예외를 던질 때 Binary unwrap 이 IllegalStateException 으로 감싸는지 검증.
     *
     * catch(e: Exception) 블록이 일반 예외를 IllegalStateException 으로 래핑해야 한다.
     * CancellationException 은 제외 (재전파)이므로 RuntimeException 으로 검증한다.
     */
    @Test
    fun `Binary unwrap 은 compressor 예외를 IllegalStateException 으로 감싼다`() {
        val transformer = CompressedBinaryTransformer(ThrowingCompressor(RuntimeException("compress error")))

        assertFailsWith<IllegalStateException> {
            transformer.unwrap(ByteArray(10))
        }
    }

    /**
     * compressor 가 예외를 던질 때 Binary wrap 이 IllegalStateException 으로 감싸는지 검증.
     */
    @Test
    fun `Binary wrap 은 compressor 예외를 IllegalStateException 으로 감싼다`() {
        val transformer = CompressedBinaryTransformer(ThrowingCompressor(RuntimeException("decompress error")))

        assertFailsWith<IllegalStateException> {
            transformer.wrap(ByteArray(10))
        }
    }

    /**
     * compressor 가 예외를 던질 때 Blob unwrap 이 IllegalStateException 으로 감싸는지 검증.
     */
    @Test
    fun `Blob unwrap 은 compressor 예외를 IllegalStateException 으로 감싼다`() {
        val transformer = CompressedBlobTransformer(ThrowingCompressor(RuntimeException("compress error")))

        assertFailsWith<IllegalStateException> {
            transformer.unwrap(ByteArray(10))
        }
    }

    /**
     * compressor 가 예외를 던질 때 Blob wrap 이 IllegalStateException 으로 감싸는지 검증.
     */
    @Test
    fun `Blob wrap 은 compressor 예외를 IllegalStateException 으로 감싼다`() {
        val transformer = CompressedBlobTransformer(ThrowingCompressor(RuntimeException("decompress error")))

        assertFailsWith<IllegalStateException> {
            transformer.wrap(ByteArray(10).toExposedBlob())
        }
    }

    /**
     * CancellationException 이 catch(e: Exception) 핸들러에서 재전파되는지 검증.
     *
     * 코루틴 취소는 Exception 을 상속하므로 잘못된 catch 블록에 삼켜지면
     * structured concurrency 가 깨진다. CancellationException 재전파가 보장되어야 한다.
     */
    @Test
    fun `CancellationException 은 Binary unwrap 에서 재전파된다`() {
        val cancellation = kotlin.coroutines.cancellation.CancellationException("test cancel")
        val transformer = CompressedBinaryTransformer(ThrowingCompressor(cancellation))

        assertFailsWith<kotlin.coroutines.cancellation.CancellationException> {
            transformer.unwrap(ByteArray(10))
        }
    }

    @Test
    fun `CancellationException 은 Binary wrap 에서 재전파된다`() {
        val cancellation = kotlin.coroutines.cancellation.CancellationException("test cancel")
        val transformer = CompressedBinaryTransformer(ThrowingCompressor(cancellation))

        assertFailsWith<kotlin.coroutines.cancellation.CancellationException> {
            transformer.wrap(ByteArray(10))
        }
    }

    @Test
    fun `CancellationException 은 Blob unwrap 에서 재전파된다`() {
        val cancellation = kotlin.coroutines.cancellation.CancellationException("test cancel")
        val transformer = CompressedBlobTransformer(ThrowingCompressor(cancellation))

        assertFailsWith<kotlin.coroutines.cancellation.CancellationException> {
            transformer.unwrap(ByteArray(10))
        }
    }

    @Test
    fun `CancellationException 은 Blob wrap 에서 재전파된다`() {
        val cancellation = kotlin.coroutines.cancellation.CancellationException("test cancel")
        val transformer = CompressedBlobTransformer(ThrowingCompressor(cancellation))

        assertFailsWith<kotlin.coroutines.cancellation.CancellationException> {
            transformer.wrap(ByteArray(10).toExposedBlob())
        }
    }
}
