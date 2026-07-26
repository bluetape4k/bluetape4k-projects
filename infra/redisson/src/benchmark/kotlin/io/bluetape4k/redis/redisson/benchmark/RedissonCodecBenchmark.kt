package io.bluetape4k.redis.redisson.benchmark

import io.bluetape4k.redis.redisson.codec.RedissonCodecs
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
 * Redisson Codec 직렬화/역직렬화 처리량 벤치마크.
 *
 * ## 비교 그룹
 * - **JSON**: Jackson3, Fastjson2 — Human-readable JSON 포맷
 * - **Binary (non-compressed)**: Fory, Kryo5, Jdk — 순수 바이너리 직렬화
 * - **Binary + LZ4**: LZ4Fory, LZ4Kryo5 — 빠른 압축 + 바이너리
 * - **Binary + Zstd**: ZstdFory, ZstdKryo5 — 고압축률 + 바이너리
 *
 * ## 실행 방법
 * ```bash
 * ./gradlew :bluetape4k-redisson:benchmark
 * ```
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(1)
class RedissonCodecBenchmark {

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
    private val jackson3Codec = RedissonCodecs.Jackson3
    private val fastjson2Codec = RedissonCodecs.Fastjson2

    // Binary Codecs (non-compressed)
    private val foryCodec = RedissonCodecs.Fory
    private val kryo5Codec = RedissonCodecs.Kryo5
    private val jdkCodec = RedissonCodecs.Jdk

    // Binary + LZ4 압축
    private val lz4ForyCodec = RedissonCodecs.LZ4Fory
    private val lz4Kryo5Codec = RedissonCodecs.LZ4Kryo5

    // Binary + Zstd 압축
    private val zstdForyCodec = RedissonCodecs.ZstdFory
    private val zstdKryo5Codec = RedissonCodecs.ZstdKryo5

    // FastFory (SCHEMA_CONSISTENT) Codecs
    private val fastForyCodec = RedissonCodecs.FastFory
    private val lz4FastForyCodec = RedissonCodecs.LZ4FastFory
    private val zstdFastForyCodec = RedissonCodecs.ZstdFastFory
    private val gzipFastForyCodec = RedissonCodecs.GzipFastFory

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

    /** Jackson 3 JSON 엔벨로프 encode/decode roundtrip */
    @Benchmark
    fun jackson3EncodeDecode() {
        val buf = jackson3Codec.valueEncoder.encode(testData)
        try {
            jackson3Codec.valueDecoder.decode(buf, null)
        } finally {
            buf.release()
        }
    }

    /** Fastjson2 JSONB encode/decode roundtrip */
    @Benchmark
    fun fastjson2EncodeDecode() {
        val buf = fastjson2Codec.valueEncoder.encode(testData)
        try {
            fastjson2Codec.valueDecoder.decode(buf, null)
        } finally {
            buf.release()
        }
    }

    // -------------------------------------------------------------------------
    // Binary Codecs (non-compressed)
    // -------------------------------------------------------------------------

    /** Apache Fory 직렬화 encode/decode roundtrip */
    @Benchmark
    fun foryEncodeDecode() {
        val buf = foryCodec.valueEncoder.encode(testData)
        try {
            foryCodec.valueDecoder.decode(buf, null)
        } finally {
            buf.release()
        }
    }

    /** Kryo5 직렬화 encode/decode roundtrip */
    @Benchmark
    fun kryo5EncodeDecode() {
        val buf = kryo5Codec.valueEncoder.encode(testData)
        try {
            kryo5Codec.valueDecoder.decode(buf, null)
        } finally {
            buf.release()
        }
    }

    /** JDK 직렬화 encode/decode roundtrip */
    @Benchmark
    fun jdkEncodeDecode() {
        val buf = jdkCodec.valueEncoder.encode(testData)
        try {
            jdkCodec.valueDecoder.decode(buf, null)
        } finally {
            buf.release()
        }
    }

    // -------------------------------------------------------------------------
    // LZ4 압축 Codecs
    // -------------------------------------------------------------------------

    /** Fory + LZ4 압축 encode/decode roundtrip */
    @Benchmark
    fun lz4ForyEncodeDecode() {
        val buf = lz4ForyCodec.valueEncoder.encode(testData)
        try {
            lz4ForyCodec.valueDecoder.decode(buf, null)
        } finally {
            buf.release()
        }
    }

    /** Kryo5 + LZ4 압축 encode/decode roundtrip */
    @Benchmark
    fun lz4Kryo5EncodeDecode() {
        val buf = lz4Kryo5Codec.valueEncoder.encode(testData)
        try {
            lz4Kryo5Codec.valueDecoder.decode(buf, null)
        } finally {
            buf.release()
        }
    }

    // -------------------------------------------------------------------------
    // Zstd 압축 Codecs
    // -------------------------------------------------------------------------

    /** Fory + Zstd 압축 encode/decode roundtrip */
    @Benchmark
    fun zstdForyEncodeDecode() {
        val buf = zstdForyCodec.valueEncoder.encode(testData)
        try {
            zstdForyCodec.valueDecoder.decode(buf, null)
        } finally {
            buf.release()
        }
    }

    /** Kryo5 + Zstd 압축 encode/decode roundtrip */
    @Benchmark
    fun zstdKryo5EncodeDecode() {
        val buf = zstdKryo5Codec.valueEncoder.encode(testData)
        try {
            zstdKryo5Codec.valueDecoder.decode(buf, null)
        } finally {
            buf.release()
        }
    }

    // -------------------------------------------------------------------------
    // FastFory (SCHEMA_CONSISTENT) Codecs
    // -------------------------------------------------------------------------

    /** Apache Fory SCHEMA_CONSISTENT(FastFory) 직렬화 encode/decode roundtrip */
    @Benchmark
    fun fastForyEncodeDecode() {
        val buf = fastForyCodec.valueEncoder.encode(testData)
        try {
            fastForyCodec.valueDecoder.decode(buf, null)
        } finally {
            buf.release()
        }
    }

    /** FastFory + LZ4 압축 encode/decode roundtrip */
    @Benchmark
    fun lz4FastForyEncodeDecode() {
        val buf = lz4FastForyCodec.valueEncoder.encode(testData)
        try {
            lz4FastForyCodec.valueDecoder.decode(buf, null)
        } finally {
            buf.release()
        }
    }

    /** FastFory + Zstd 압축 encode/decode roundtrip */
    @Benchmark
    fun zstdFastForyEncodeDecode() {
        val buf = zstdFastForyCodec.valueEncoder.encode(testData)
        try {
            zstdFastForyCodec.valueDecoder.decode(buf, null)
        } finally {
            buf.release()
        }
    }

    /** FastFory + Gzip 압축 encode/decode roundtrip */
    @Benchmark
    fun gzipFastForyEncodeDecode() {
        val buf = gzipFastForyCodec.valueEncoder.encode(testData)
        try {
            gzipFastForyCodec.valueDecoder.decode(buf, null)
        } finally {
            buf.release()
        }
    }
}
