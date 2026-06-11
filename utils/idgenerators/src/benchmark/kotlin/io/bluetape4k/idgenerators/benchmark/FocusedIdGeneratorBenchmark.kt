package io.bluetape4k.idgenerators.benchmark

import io.bluetape4k.idgenerators.ksuid.Ksuid
import io.bluetape4k.idgenerators.ksuid.KsuidGenerator
import io.bluetape4k.idgenerators.snowflake.SnowflakeGenerator
import io.bluetape4k.idgenerators.ulid.ULID
import io.bluetape4k.idgenerators.ulid.UlidGenerator
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Threads
import org.openjdk.jmh.annotations.Warmup
import org.openjdk.jmh.infra.Blackhole
import java.time.Instant
import java.util.concurrent.TimeUnit

private const val DefaultBatchSize = 4096 * 16

/**
 * Snowflake, ULID, KSUID 성능 개선 이슈의 baseline 전용 벤치마크입니다.
 *
 * 기존 비교 벤치마크는 여러 생성기를 한 번에 비교하기 위해 uniqueness 검증을
 * 포함합니다. 이 벤치마크는 생성-only 비용과 검증 비용을 분리해서 최적화
 * 후보가 실제 생성 경로를 개선했는지 확인합니다.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
open class FocusedSingleThreadIdGeneratorBenchmark {

    @Param(DefaultBatchSize.toString())
    var batchSize: Int = 0

    private lateinit var snowflake: SnowflakeGenerator
    private lateinit var ulidMonotonic: UlidGenerator
    private lateinit var ulidStateful: ULID.StatefulMonotonic
    private lateinit var ksuidSeconds: KsuidGenerator
    private lateinit var ksuidMillis: KsuidGenerator

    @Setup(Level.Trial)
    fun setup() {
        snowflake = SnowflakeGenerator()
        ulidMonotonic = UlidGenerator()
        ulidStateful = ULID.statefulMonotonic()
        ksuidSeconds = KsuidGenerator(Ksuid.Seconds)
        ksuidMillis = KsuidGenerator(Ksuid.Millis)
    }

    @Benchmark
    fun snowflakeDefaultGenerateOnly(bh: Blackhole) {
        var checksum = 0L
        repeat(batchSize) {
            checksum = checksum xor snowflake.nextId()
        }
        bh.consume(checksum)
    }

    @Benchmark
    fun snowflakeDefaultWithUniqueness(bh: Blackhole) {
        val ids = HashSet<Long>(batchSize * 2)
        repeat(batchSize) {
            val id = snowflake.nextId()
            check(ids.add(id)) { "Snowflake duplicate: $id" }
        }
        bh.consume(ids.size)
    }

    @Benchmark
    fun ulidMonotonicValueOnly(bh: Blackhole) {
        var checksum = 0L
        repeat(batchSize) {
            val id = ulidStateful.nextULID()
            checksum = checksum xor id.mostSignificantBits xor id.leastSignificantBits
        }
        bh.consume(checksum)
    }

    @Benchmark
    fun ulidMonotonicString(bh: Blackhole) {
        var length = 0
        repeat(batchSize) {
            length += ulidMonotonic.nextId().length
        }
        bh.consume(length)
    }

    @Benchmark
    fun ulidMonotonicWithUniqueness(bh: Blackhole) {
        val ids = HashSet<String>(batchSize * 2)
        repeat(batchSize) {
            val id = ulidMonotonic.nextId()
            check(ids.add(id)) { "ULID duplicate: $id" }
        }
        bh.consume(ids.size)
    }

    @Benchmark
    fun ksuidSecondsDefaultString(bh: Blackhole) {
        var length = 0
        repeat(batchSize) {
            length += ksuidSeconds.nextId().length
        }
        bh.consume(length)
    }

    @Benchmark
    fun ksuidSecondsFixedInstantString(bh: Blackhole) {
        var length = 0
        repeat(batchSize) {
            length += Ksuid.Seconds.generate(FixedInstant).length
        }
        bh.consume(length)
    }

    @Benchmark
    fun ksuidSecondsWithUniqueness(bh: Blackhole) {
        val ids = HashSet<String>(batchSize * 2)
        repeat(batchSize) {
            val id = ksuidSeconds.nextId()
            check(ids.add(id)) { "KSUID seconds duplicate: $id" }
        }
        bh.consume(ids.size)
    }

    @Benchmark
    fun ksuidMillisDefaultString(bh: Blackhole) {
        var length = 0
        repeat(batchSize) {
            length += ksuidMillis.nextId().length
        }
        bh.consume(length)
    }

    @Benchmark
    fun ksuidMillisFixedInstantString(bh: Blackhole) {
        var length = 0
        repeat(batchSize) {
            length += Ksuid.Millis.generate(FixedInstant).length
        }
        bh.consume(length)
    }

    @Benchmark
    fun ksuidMillisWithUniqueness(bh: Blackhole) {
        val ids = HashSet<String>(batchSize * 2)
        repeat(batchSize) {
            val id = ksuidMillis.nextId()
            check(ids.add(id)) { "KSUID millis duplicate: $id" }
        }
        bh.consume(ids.size)
    }

    companion object {
        private val FixedInstant: Instant = Instant.parse("2026-06-10T00:00:00Z")
    }
}

/**
 * 공유 생성기에서의 contention 비용을 측정합니다.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Threads(Threads.MAX)
open class FocusedConcurrentIdGeneratorBenchmark {

    @Param(DefaultBatchSize.toString())
    var batchSize: Int = 0

    private lateinit var snowflake: SnowflakeGenerator
    private lateinit var ulidMonotonic: UlidGenerator
    private lateinit var ksuidSeconds: KsuidGenerator
    private lateinit var ksuidMillis: KsuidGenerator

    @Setup(Level.Trial)
    fun setup() {
        snowflake = SnowflakeGenerator()
        ulidMonotonic = UlidGenerator()
        ksuidSeconds = KsuidGenerator(Ksuid.Seconds)
        ksuidMillis = KsuidGenerator(Ksuid.Millis)
    }

    @Benchmark
    fun snowflakeDefaultGenerateOnly(bh: Blackhole) {
        var checksum = 0L
        repeat(batchSize) {
            checksum = checksum xor snowflake.nextId()
        }
        bh.consume(checksum)
    }

    @Benchmark
    fun snowflakeDefaultWithUniqueness(bh: Blackhole) {
        val ids = HashSet<Long>(batchSize * 2)
        repeat(batchSize) {
            val id = snowflake.nextId()
            check(ids.add(id)) { "Snowflake duplicate: $id" }
        }
        bh.consume(ids.size)
    }

    @Benchmark
    fun ulidMonotonicString(bh: Blackhole) {
        var length = 0
        repeat(batchSize) {
            length += ulidMonotonic.nextId().length
        }
        bh.consume(length)
    }

    @Benchmark
    fun ulidMonotonicWithUniqueness(bh: Blackhole) {
        val ids = HashSet<String>(batchSize * 2)
        repeat(batchSize) {
            val id = ulidMonotonic.nextId()
            check(ids.add(id)) { "ULID duplicate: $id" }
        }
        bh.consume(ids.size)
    }

    @Benchmark
    fun ksuidSecondsDefaultString(bh: Blackhole) {
        var length = 0
        repeat(batchSize) {
            length += ksuidSeconds.nextId().length
        }
        bh.consume(length)
    }

    @Benchmark
    fun ksuidSecondsWithUniqueness(bh: Blackhole) {
        val ids = HashSet<String>(batchSize * 2)
        repeat(batchSize) {
            val id = ksuidSeconds.nextId()
            check(ids.add(id)) { "KSUID seconds duplicate: $id" }
        }
        bh.consume(ids.size)
    }

    @Benchmark
    fun ksuidMillisDefaultString(bh: Blackhole) {
        var length = 0
        repeat(batchSize) {
            length += ksuidMillis.nextId().length
        }
        bh.consume(length)
    }

    @Benchmark
    fun ksuidMillisWithUniqueness(bh: Blackhole) {
        val ids = HashSet<String>(batchSize * 2)
        repeat(batchSize) {
            val id = ksuidMillis.nextId()
            check(ids.add(id)) { "KSUID millis duplicate: $id" }
        }
        bh.consume(ids.size)
    }
}
