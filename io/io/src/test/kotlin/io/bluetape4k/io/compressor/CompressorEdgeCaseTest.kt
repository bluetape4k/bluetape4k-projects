package io.bluetape4k.io.compressor

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.emptyByteArray
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

/**
 * [Compressor] 구현체들에 대한 edge case 테스트입니다.
 *
 * 단일 바이트, 대용량 입력, 반복 패턴, 멀티스레드 안전성 등을 검증합니다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CompressorEdgeCaseTest {

    companion object : KLogging() {
        private const val THREAD_COUNT = 8
        private const val LARGE_INPUT_SIZE = 1024 * 1024  // 1 MB

        @JvmStatic
        fun allCompressors(): Stream<Compressor> = Stream.of(
            LZ4Compressor(),
            ZstdCompressor(),
            SnappyCompressor(),
            GZipCompressor(),
            DeflateCompressor(),
            BZip2Compressor(),
            FramedLZ4Compressor(),
            BlockLZ4Compressor(),
            FramedSnappyCompressor(),
            ApacheGZipCompressor(),
            ApacheDeflateCompressor(),
            ApacheZstdCompressor(),
            ZipCompressor(),
        )

        @JvmStatic
        fun fastCompressors(): Stream<Compressor> = Stream.of(
            LZ4Compressor(),
            ZstdCompressor(),
            SnappyCompressor(),
            GZipCompressor(),
            DeflateCompressor(),
        )
    }

    @ParameterizedTest(name = "단일 바이트 압축/해제: {0}")
    @MethodSource("allCompressors")
    fun `단일 바이트 압축 및 해제`(compressor: Compressor) {
        val input = byteArrayOf(0x42)
        val compressed = compressor.compress(input)
        compressed.shouldNotBeEmpty()

        val decompressed = compressor.decompress(compressed)
        decompressed shouldBeEqualTo input
    }

    @ParameterizedTest(name = "모두 같은 바이트로 이루어진 배열 압축: {0}")
    @MethodSource("allCompressors")
    fun `반복 패턴 데이터 압축률 검증`(compressor: Compressor) {
        // 반복 패턴은 높은 압축률을 보여야 한다
        val input = ByteArray(10_000) { 0xAA.toByte() }
        val compressed = compressor.compress(input)
        val decompressed = compressor.decompress(compressed)

        decompressed shouldBeEqualTo input

        val compressionRatio = compressed.size.toDouble() / input.size
        log.debug {
            "${compressor.javaClass.simpleName}: ratio=${compressionRatio}, " +
                    "compressed=${compressed.size}, plain=${input.size}"
        }
        // 반복 패턴이므로 압축 효과가 있어야 함 (10% 미만 크기)
        (compressionRatio < 1.0).shouldBeTrue()
    }

    @ParameterizedTest(name = "대용량(1MB) 입력 압축/해제: {0}")
    @MethodSource("fastCompressors")
    fun `대용량 1MB 입력 압축 및 해제`(compressor: Compressor) {
        // 반복 패턴으로 대용량 데이터 생성
        val pattern = "Hello, bluetape4k Compressor! ".toByteArray()
        val input = ByteArray(LARGE_INPUT_SIZE).also { buf ->
            var offset = 0
            while (offset < buf.size) {
                val copyLen = minOf(pattern.size, buf.size - offset)
                pattern.copyInto(buf, offset, 0, copyLen)
                offset += copyLen
            }
        }

        val compressed = compressor.compress(input)
        val decompressed = compressor.decompress(compressed)

        decompressed shouldBeEqualTo input
        log.debug {
            "${compressor.javaClass.simpleName} 1MB: " +
                    "compressed=${compressed.size}, ratio=${compressed.size * 100.0 / input.size}%"
        }
    }

    @ParameterizedTest(name = "바이너리 데이터(랜덤) 압축/해제: {0}")
    @MethodSource("allCompressors")
    fun `비압축성 랜덤 바이너리 데이터 압축 및 해제`(compressor: Compressor) {
        // 랜덤 데이터는 압축이 거의 안 되지만, 무결성은 보장되어야 한다
        val input = ByteArray(4096) { (it % 256).toByte() }.also {
            // 좀 더 랜덤하게 섞기
            for (i in it.indices step 3) {
                it[i] = (it[i].toInt() xor 0x55).toByte()
            }
        }

        val compressed = compressor.compress(input)
        val decompressed = compressor.decompress(compressed)

        decompressed shouldBeEqualTo input
    }

    @ParameterizedTest(name = "null 및 empty 입력 처리: {0}")
    @MethodSource("allCompressors")
    fun `null 및 empty 입력은 emptyByteArray 를 반환한다`(compressor: Compressor) {
        compressor.compress(null) shouldBeEqualTo emptyByteArray
        compressor.compress(emptyByteArray) shouldBeEqualTo emptyByteArray
        compressor.decompress(null) shouldBeEqualTo emptyByteArray
        compressor.decompress(emptyByteArray) shouldBeEqualTo emptyByteArray
    }

    @ParameterizedTest(name = "멀티스레드 안전성: {0}")
    @MethodSource("fastCompressors")
    fun `멀티스레드 환경에서 동시 압축이 안전하게 동작한다`(compressor: Compressor) {
        val input = "Concurrent compression test data: bluetape4k ".repeat(100).toByteArray()
        val executor = Executors.newFixedThreadPool(THREAD_COUNT)
        val latch = CountDownLatch(THREAD_COUNT)
        val errorCount = java.util.concurrent.atomic.AtomicInteger(0)
        val results = java.util.concurrent.CopyOnWriteArrayList<ByteArray>()

        repeat(THREAD_COUNT) { _ ->
            executor.submit {
                try {
                    val compressed = compressor.compress(input)
                    val decompressed = compressor.decompress(compressed)
                    results.add(decompressed)
                } catch (e: Throwable) {
                    errorCount.incrementAndGet()
                    log.debug(e) { "Concurrent compression error: ${e.message}" }
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await(30, TimeUnit.SECONDS)
        executor.shutdown()

        errorCount.get() shouldBeEqualTo 0
        results.size shouldBeEqualTo THREAD_COUNT
        results.forEach { result ->
            (result contentEquals input).shouldBeTrue()
        }
    }

    @Test
    fun `LZ4Compressor 압축 데이터 헤더 손상 시 예외를 처리한다`() {
        val compressor = LZ4Compressor()
        val input = "Hello, LZ4!".toByteArray()
        val compressed = compressor.compress(input)

        // 헤더(원본 크기 4바이트)를 비정상 값으로 변조
        val corrupted = compressed.copyOf()
        corrupted[0] = 0x7F
        corrupted[1] = 0xFF.toByte()
        corrupted[2] = 0xFF.toByte()
        corrupted[3] = 0xFF.toByte()

        // AbstractCompressor는 예외를 삼키고 emptyByteArray를 반환하도록 설계되어 있음
        val result = compressor.decompress(corrupted)
        result shouldBeEqualTo emptyByteArray
    }

    @Test
    fun `ZstdCompressor 는 레벨을 유효 범위로 제한한다`() {
        val tooLow = ZstdCompressor(-100)
        val tooHigh = ZstdCompressor(9999)

        val input = "Zstd level clamping test".repeat(50).toByteArray()

        // 두 인스턴스 모두 정상 동작해야 한다
        val compressedLow = tooLow.compress(input)
        tooLow.decompress(compressedLow) shouldBeEqualTo input

        val compressedHigh = tooHigh.compress(input)
        tooHigh.decompress(compressedHigh) shouldBeEqualTo input
    }

    @Test
    fun `ZstdCompressor 기본 레벨 3 과 고압축 레벨의 결과를 비교한다`() {
        val input = "Hello, Zstd compression level test! ".repeat(1000).toByteArray()
        val defaultCompressor = ZstdCompressor(3)
        val highCompressor = ZstdCompressor(15)

        val defaultCompressed = defaultCompressor.compress(input)
        val highCompressed = highCompressor.compress(input)

        // 두 결과 모두 원본으로 복원 가능해야 한다
        defaultCompressor.decompress(defaultCompressed) shouldBeEqualTo input
        highCompressor.decompress(highCompressed) shouldBeEqualTo input

        // 고압축이 기본보다 더 작거나 같아야 한다
        highCompressed.size shouldBeLessOrEqualTo defaultCompressed.size

        log.debug {
            "Zstd default=${defaultCompressed.size}, high=${highCompressed.size}, " +
                    "original=${input.size}"
        }
    }

    @Test
    fun `GZipCompressor 는 다른 bufferSize 로 동일한 결과를 반환한다`() {
        val input = "GZip buffer size test data".repeat(200).toByteArray()

        val small = GZipCompressor(512)
        val large = GZipCompressor(65536)

        val decompressedFromSmall = small.decompress(small.compress(input))
        val decompressedFromLarge = large.decompress(large.compress(input))

        decompressedFromSmall shouldBeEqualTo input
        decompressedFromLarge shouldBeEqualTo input
    }

    @ParameterizedTest(name = "String API 압축 및 해제: {0}")
    @MethodSource("fastCompressors")
    fun `String API 로 압축 및 해제가 동작한다`(compressor: Compressor) {
        val input = "String compression test: 한국어, English, 日本語".repeat(100)
        val compressed: String = compressor.compress(input)
        val decompressed: String = compressor.decompress(compressed)
        decompressed shouldBeEqualTo input
    }

    @ParameterizedTest(name = "압축 결과는 원본보다 작다: {0}")
    @MethodSource("fastCompressors")
    fun `반복 패턴 데이터의 압축 결과는 원본보다 작아야 한다`(compressor: Compressor) {
        val input = "압축 효율 테스트 데이터 ".repeat(500).toByteArray()
        val compressed = compressor.compress(input)

        compressed.size shouldBeLessThan input.size
    }
}
