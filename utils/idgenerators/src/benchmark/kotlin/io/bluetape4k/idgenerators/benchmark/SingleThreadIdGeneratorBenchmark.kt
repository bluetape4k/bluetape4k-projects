package io.bluetape4k.idgenerators.benchmark

import io.bluetape4k.idgenerators.flake.Flake
import io.bluetape4k.idgenerators.ksuid.KsuidGenerator
import io.bluetape4k.idgenerators.snowflake.SnowflakeGenerator
import io.bluetape4k.idgenerators.ulid.UlidGenerator
import io.bluetape4k.idgenerators.uuid.UuidGenerator
import io.bluetape4k.idgenerators.uuid.Uuid
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
import org.openjdk.jmh.annotations.Warmup
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

/**
 * 단일 스레드 환경에서 각 ID 생성기의 throughput(ops/sec)을 측정합니다.
 *
 * 각 ops는 [batchSize]개의 ID를 생성하고 모두 unique함을 검증합니다.
 *
 * ## 실행 방법
 * ```bash
 * ./gradlew :bluetape4k-utils-idgenerators:benchmarkSingleThread
 * ```
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
open class SingleThreadIdGeneratorBenchmark {

    @Param("100", "10000")
    var batchSize: Int = 0

    private lateinit var snowflakeGen: SnowflakeGenerator
    private lateinit var uuidV4Gen: UuidGenerator
    private lateinit var uuidV7Gen: UuidGenerator
    private lateinit var ulidGen: UlidGenerator
    private lateinit var ksuidSecondsGen: KsuidGenerator
    private lateinit var ksuidMillisGen: KsuidGenerator
    private lateinit var flakeGen: Flake

    @Setup(Level.Trial)
    fun setup() {
        snowflakeGen = SnowflakeGenerator()
        uuidV4Gen = UuidGenerator(Uuid.V4)
        uuidV7Gen = UuidGenerator(Uuid.V7)
        ulidGen = UlidGenerator()
        ksuidSecondsGen = KsuidGenerator()
        ksuidMillisGen = KsuidGenerator(io.bluetape4k.idgenerators.ksuid.Ksuid.Millis)
        flakeGen = Flake()
    }

    @Benchmark
    fun snowflake(bh: Blackhole) {
        val ids = HashSet<Long>(batchSize * 2)
        repeat(batchSize) {
            val id = snowflakeGen.nextId()
            check(ids.add(id)) { "Snowflake 중복 발생: $id" }
        }
        bh.consume(ids)
    }

    @Benchmark
    fun uuidV4(bh: Blackhole) {
        val ids = HashSet<Any>(batchSize * 2)
        repeat(batchSize) {
            val id = uuidV4Gen.nextId()
            check(ids.add(id)) { "UUID V4 중복 발생: $id" }
        }
        bh.consume(ids)
    }

    @Benchmark
    fun uuidV7(bh: Blackhole) {
        val ids = HashSet<Any>(batchSize * 2)
        repeat(batchSize) {
            val id = uuidV7Gen.nextId()
            check(ids.add(id)) { "UUID V7 중복 발생: $id" }
        }
        bh.consume(ids)
    }

    @Benchmark
    fun ulid(bh: Blackhole) {
        val ids = HashSet<String>(batchSize * 2)
        repeat(batchSize) {
            val id = ulidGen.nextId()
            check(ids.add(id)) { "ULID 중복 발생: $id" }
        }
        bh.consume(ids)
    }

    @Benchmark
    fun ksuidSeconds(bh: Blackhole) {
        val ids = HashSet<String>(batchSize * 2)
        repeat(batchSize) {
            val id = ksuidSecondsGen.nextId()
            check(ids.add(id)) { "KSUID(seconds) 중복 발생: $id" }
        }
        bh.consume(ids)
    }

    @Benchmark
    fun ksuidMillis(bh: Blackhole) {
        val ids = HashSet<String>(batchSize * 2)
        repeat(batchSize) {
            val id = ksuidMillisGen.nextId()
            check(ids.add(id)) { "KSUID(millis) 중복 발생: $id" }
        }
        bh.consume(ids)
    }

    @Benchmark
    fun flake(bh: Blackhole) {
        val ids = HashSet<String>(batchSize * 2)
        repeat(batchSize) {
            val id = flakeGen.nextIdAsString()
            check(ids.add(id)) { "Flake 중복 발생: $id" }
        }
        bh.consume(ids)
    }
}
