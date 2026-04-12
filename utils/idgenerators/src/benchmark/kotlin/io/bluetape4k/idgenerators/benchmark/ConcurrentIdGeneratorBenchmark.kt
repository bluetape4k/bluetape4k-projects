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
import org.openjdk.jmh.annotations.TearDown
import org.openjdk.jmh.annotations.Threads
import org.openjdk.jmh.annotations.Warmup
import org.openjdk.jmh.infra.Blackhole
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 멀티 스레드 환경에서 각 ID 생성기의 throughput(ops/sec)을 측정합니다.
 *
 * 스레드 수는 `Runtime.getRuntime().availableProcessors() * 2`로 설정되며,
 * 모든 스레드가 하나의 생성기 인스턴스를 공유합니다.
 *
 * 각 ops는 [batchSize]개의 ID를 생성하고, 전역 Set에 추가하여 cross-thread unique를 검증합니다.
 * Trial 종료 시 전역 Set에 중복이 없었는지 최종 확인합니다.
 *
 * ## 실행 방법
 * ```bash
 * ./gradlew :bluetape4k-utils-idgenerators:benchmarkConcurrent
 * ```
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Threads(Threads.MAX)  // availableProcessors * 2 와 유사하게 JMH가 자동 설정
open class ConcurrentIdGeneratorBenchmark {

    @Param("100", "10000")
    var batchSize: Int = 0

    private lateinit var snowflakeGen: SnowflakeGenerator
    private lateinit var uuidV4Gen: UuidGenerator
    private lateinit var uuidV7Gen: UuidGenerator
    private lateinit var ulidGen: UlidGenerator
    private lateinit var ksuidSecondsGen: KsuidGenerator
    private lateinit var ksuidMillisGen: KsuidGenerator
    private lateinit var flakeGen: Flake

    // 크로스 스레드 유니크 검증용 — Trial 단위로 초기화
    private lateinit var snowflakeGlobalIds: ConcurrentHashMap.KeySetView<Long, Boolean>
    private lateinit var uuidV4GlobalIds: ConcurrentHashMap.KeySetView<Any, Boolean>
    private lateinit var uuidV7GlobalIds: ConcurrentHashMap.KeySetView<Any, Boolean>
    private lateinit var ulidGlobalIds: ConcurrentHashMap.KeySetView<String, Boolean>
    private lateinit var ksuidSecondsGlobalIds: ConcurrentHashMap.KeySetView<String, Boolean>
    private lateinit var ksuidMillisGlobalIds: ConcurrentHashMap.KeySetView<String, Boolean>
    private lateinit var flakeGlobalIds: ConcurrentHashMap.KeySetView<String, Boolean>

    @Setup(Level.Trial)
    fun setup() {
        snowflakeGen = SnowflakeGenerator()
        uuidV4Gen = UuidGenerator(Uuid.V4)
        uuidV7Gen = UuidGenerator(Uuid.V7)
        ulidGen = UlidGenerator()
        ksuidSecondsGen = KsuidGenerator()
        ksuidMillisGen = KsuidGenerator(io.bluetape4k.idgenerators.ksuid.Ksuid.Millis)
        flakeGen = Flake()

        snowflakeGlobalIds = ConcurrentHashMap.newKeySet()
        uuidV4GlobalIds = ConcurrentHashMap.newKeySet()
        uuidV7GlobalIds = ConcurrentHashMap.newKeySet()
        ulidGlobalIds = ConcurrentHashMap.newKeySet()
        ksuidSecondsGlobalIds = ConcurrentHashMap.newKeySet()
        ksuidMillisGlobalIds = ConcurrentHashMap.newKeySet()
        flakeGlobalIds = ConcurrentHashMap.newKeySet()
    }

    @TearDown(Level.Trial)
    fun verify() {
        // ConcurrentHashMap.KeySetView.add()는 이미 존재하면 false를 반환하므로
        // Benchmark 메서드 내에서 즉시 검증됨. 여기서는 최종 크기만 로깅
        println("[Concurrent] Snowflake total unique IDs: ${snowflakeGlobalIds.size}")
        println("[Concurrent] UUID V4 total unique IDs: ${uuidV4GlobalIds.size}")
        println("[Concurrent] UUID V7 total unique IDs: ${uuidV7GlobalIds.size}")
        println("[Concurrent] ULID total unique IDs: ${ulidGlobalIds.size}")
        println("[Concurrent] KSUID(seconds) total unique IDs: ${ksuidSecondsGlobalIds.size}")
        println("[Concurrent] KSUID(millis) total unique IDs: ${ksuidMillisGlobalIds.size}")
        println("[Concurrent] Flake total unique IDs: ${flakeGlobalIds.size}")
    }

    @Benchmark
    fun snowflake(bh: Blackhole) {
        repeat(batchSize) {
            val id = snowflakeGen.nextId()
            check(snowflakeGlobalIds.add(id)) { "Snowflake 크로스 스레드 중복 발생: $id" }
        }
        bh.consume(batchSize)
    }

    @Benchmark
    fun uuidV4(bh: Blackhole) {
        repeat(batchSize) {
            val id = uuidV4Gen.nextId()
            check(uuidV4GlobalIds.add(id)) { "UUID V4 크로스 스레드 중복 발생: $id" }
        }
        bh.consume(batchSize)
    }

    @Benchmark
    fun uuidV7(bh: Blackhole) {
        repeat(batchSize) {
            val id = uuidV7Gen.nextId()
            check(uuidV7GlobalIds.add(id)) { "UUID V7 크로스 스레드 중복 발생: $id" }
        }
        bh.consume(batchSize)
    }

    @Benchmark
    fun ulid(bh: Blackhole) {
        repeat(batchSize) {
            val id = ulidGen.nextId()
            check(ulidGlobalIds.add(id)) { "ULID 크로스 스레드 중복 발생: $id" }
        }
        bh.consume(batchSize)
    }

    @Benchmark
    fun ksuidSeconds(bh: Blackhole) {
        repeat(batchSize) {
            val id = ksuidSecondsGen.nextId()
            check(ksuidSecondsGlobalIds.add(id)) { "KSUID(seconds) 크로스 스레드 중복 발생: $id" }
        }
        bh.consume(batchSize)
    }

    @Benchmark
    fun ksuidMillis(bh: Blackhole) {
        repeat(batchSize) {
            val id = ksuidMillisGen.nextId()
            check(ksuidMillisGlobalIds.add(id)) { "KSUID(millis) 크로스 스레드 중복 발생: $id" }
        }
        bh.consume(batchSize)
    }

    @Benchmark
    fun flake(bh: Blackhole) {
        repeat(batchSize) {
            val id = flakeGen.nextIdAsString()
            check(flakeGlobalIds.add(id)) { "Flake 크로스 스레드 중복 발생: $id" }
        }
        bh.consume(batchSize)
    }
}
