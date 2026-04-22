package io.bluetape4k.redis.lettuce.benchmark

import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecs
import io.bluetape4k.redis.lettuce.codec.LettuceJsonCodecs
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import java.util.concurrent.TimeUnit

/**
 * Lettuce Codec 직렬화/역직렬화 처리량 벤치마크.
 *
 * ## 비교 그룹
 * - **JSON**: Jackson3, Fastjson2 ([LettuceJsonCodecs])
 * - **Binary (non-compressed)**: Fory, Kryo, Jdk ([LettuceBinaryCodecs])
 * - **Binary + LZ4**: lz4Fory, lz4Kryo — 빠른 압축 조합
 * - **Binary + Zstd**: zstdFory, zstdKryo — 고압축률 조합
 *
 * ## 실행 방법
 * ```bash
 * ./gradlew :bluetape4k-lettuce:benchmark
 * ```
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(1)
class LettuceCodecBenchmark {

    /**
     * 벤치마크용 데이터 클래스.
     * 실제 캐시 엔티티를 모사하기 위해 다양한 필드 타입을 포함합니다.
     */
    data class BenchmarkData(
        val id: Long,
        val name: String,
        val description: String,
        val tags: List<String>,
        val metadata: Map<String, String>,
    ): java.io.Serializable

    private lateinit var testData: BenchmarkData

    // JSON Codecs
    private val jackson3Codec = LettuceJsonCodecs.jackson3<BenchmarkData>()
    private val fastjson2Codec = LettuceJsonCodecs.fastjson2<BenchmarkData>()

    // Binary Codecs (non-compressed)
    private val foryCodec = LettuceBinaryCodecs.fory<BenchmarkData>()
    private val kryoCodec = LettuceBinaryCodecs.kryo<BenchmarkData>()
    private val jdkCodec = LettuceBinaryCodecs.jdk<BenchmarkData>()

    // Binary + LZ4 압축
    private val lz4ForyCodec = LettuceBinaryCodecs.lz4Fory<BenchmarkData>()
    private val lz4KryoCodec = LettuceBinaryCodecs.lz4Kryo<BenchmarkData>()

    // Binary + Zstd 압축
    private val zstdForyCodec = LettuceBinaryCodecs.zstdFory<BenchmarkData>()
    private val zstdKryoCodec = LettuceBinaryCodecs.zstdKryo<BenchmarkData>()

    @Setup
    fun setup() {
        testData = BenchmarkData(
            id = 12345L,
            name = "benchmark-test",
            description = "A".repeat(512),
            tags = listOf("redis", "codec", "benchmark", "json"),
            metadata = mapOf("env" to "test", "version" to "1.0"),
        )
    }

    // -------------------------------------------------------------------------
    // JSON Codecs
    // -------------------------------------------------------------------------

    /** Jackson 3 JSON encode/decode roundtrip */
    @Benchmark
    fun jackson3EncodeDecode() {
        val encoded = jackson3Codec.encodeValue(testData)
        jackson3Codec.decodeValue(encoded)
    }

    /** Fastjson2 JSONB encode/decode roundtrip */
    @Benchmark
    fun fastjson2EncodeDecode() {
        val encoded = fastjson2Codec.encodeValue(testData)
        fastjson2Codec.decodeValue(encoded)
    }

    // -------------------------------------------------------------------------
    // Binary Codecs (non-compressed)
    // -------------------------------------------------------------------------

    /** Apache Fory 직렬화 encode/decode roundtrip */
    @Benchmark
    fun foryEncodeDecode() {
        val encoded = foryCodec.encodeValue(testData)
        foryCodec.decodeValue(encoded)
    }

    /** Kryo 직렬화 encode/decode roundtrip */
    @Benchmark
    fun kryoEncodeDecode() {
        val encoded = kryoCodec.encodeValue(testData)
        kryoCodec.decodeValue(encoded)
    }

    /** JDK 직렬화 encode/decode roundtrip */
    @Benchmark
    fun jdkEncodeDecode() {
        val encoded = jdkCodec.encodeValue(testData)
        jdkCodec.decodeValue(encoded)
    }

    // -------------------------------------------------------------------------
    // LZ4 압축 Codecs
    // -------------------------------------------------------------------------

    /** Fory + LZ4 압축 encode/decode roundtrip */
    @Benchmark
    fun lz4ForyEncodeDecode() {
        val encoded = lz4ForyCodec.encodeValue(testData)
        lz4ForyCodec.decodeValue(encoded)
    }

    /** Kryo + LZ4 압축 encode/decode roundtrip */
    @Benchmark
    fun lz4KryoEncodeDecode() {
        val encoded = lz4KryoCodec.encodeValue(testData)
        lz4KryoCodec.decodeValue(encoded)
    }

    // -------------------------------------------------------------------------
    // Zstd 압축 Codecs
    // -------------------------------------------------------------------------

    /** Fory + Zstd 압축 encode/decode roundtrip */
    @Benchmark
    fun zstdForyEncodeDecode() {
        val encoded = zstdForyCodec.encodeValue(testData)
        zstdForyCodec.decodeValue(encoded)
    }

    /** Kryo + Zstd 압축 encode/decode roundtrip */
    @Benchmark
    fun zstdKryoEncodeDecode() {
        val encoded = zstdKryoCodec.encodeValue(testData)
        zstdKryoCodec.decodeValue(encoded)
    }
}
