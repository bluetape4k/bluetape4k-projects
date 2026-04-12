package io.bluetape4k.batch.benchmark.jdbc

import io.bluetape4k.batch.benchmark.support.BenchmarkDatabase
import io.bluetape4k.logging.KLogging
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown
import java.util.concurrent.TimeUnit

/**
 * H2 인메모리 데이터베이스 대상 JDBC 배치 벤치마크.
 *
 * ## DB 설정
 * - H2 인메모리 모드 (`DB_CLOSE_DELAY=-1`): 연결이 모두 닫혀도 데이터가 유지됩니다.
 * - 별도의 Testcontainers 기동이 필요하지 않으므로 Trial setup 비용이 최소입니다.
 *
 * ## JMH 라이프사이클 설계
 * 시드(INSERT)와 엔드 투 엔드 시나리오의 라이프사이클이 충돌하지 않도록 각 시나리오를
 * 별도의 `@State` 클래스([SeedState], [JobState])로 분리합니다.
 * 이로써 동일 Trial 내에서 DataSource 이중 초기화나 스키마 조작 순서 충돌이 발생하지 않습니다.
 *
 * ## 측정 경계
 * - **시드 벤치마크** (`seedBenchmark`): 각 반복 전에 스키마를 초기화하고 INSERT만 측정합니다.
 * - **엔드 투 엔드 벤치마크** (`endToEndBatchJobBenchmark`): Trial 시작 시 소스 데이터를 미리 적재하고,
 *   각 반복 전에 대상/잡 테이블을 비운 뒤 배치 잡 1회 실행만 측정합니다.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
class H2JdbcBatchBenchmark {

    companion object : KLogging()

    /**
     * H2 시드 시나리오 전용 JMH State.
     *
     * 이 클래스에만 `@Setup(Level.Trial)`, `@Setup(Level.Iteration)`,
     * `@TearDown(Level.Trial)` 메서드가 존재하므로 [JobState]의 라이프사이클과
     * 완전히 분리됩니다.
     */
    @State(Scope.Benchmark)
    class SeedState : AbstractJdbcSeedState() {

        override val database: BenchmarkDatabase get() = BenchmarkDatabase.H2

        override fun jdbcUrl(): String =
            "jdbc:h2:mem:benchmark_h2_seed;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;"

        override fun driverClassName(): String = io.bluetape4k.jdbc.JdbcDrivers.DRIVER_CLASS_H2
        override fun dbUser(): String = "sa"
        override fun dbPassword(): String = ""

        @Param("1000", "10000", "100000")
        override var dataSize: Int = 1000

        @Param("10", "30", "60")
        override var poolSize: Int = 10

        /** Trial 시작 시 DataSource를 생성하고 스키마를 초기화합니다. */
        @Setup(Level.Trial)
        fun setup() = trialSetup()

        /** 각 반복 전에 스키마를 재초기화합니다. */
        @Setup(Level.Iteration)
        fun iterSetup() = iterationSetup()

        /** Trial 종료 시 DataSource를 닫습니다. */
        @TearDown(Level.Trial)
        fun tearDown() = trialTearDown()
    }

    /**
     * H2 엔드 투 엔드 시나리오 전용 JMH State.
     *
     * 이 클래스에만 `@Setup(Level.Trial)`, `@Setup(Level.Iteration)`,
     * `@TearDown(Level.Trial)` 메서드가 존재하므로 [SeedState]의 라이프사이클과
     * 완전히 분리됩니다.
     */
    @State(Scope.Benchmark)
    class JobState : AbstractJdbcJobState() {

        override val database: BenchmarkDatabase get() = BenchmarkDatabase.H2

        override fun jdbcUrl(): String =
            "jdbc:h2:mem:benchmark_h2_job;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;"

        override fun driverClassName(): String = io.bluetape4k.jdbc.JdbcDrivers.DRIVER_CLASS_H2
        override fun dbUser(): String = "sa"
        override fun dbPassword(): String = ""

        @Param("1000", "10000", "100000")
        override var dataSize: Int = 1000

        @Param("10", "30", "60")
        override var poolSize: Int = 10

        @Param("1", "4", "8")
        override var parallelism: Int = 1

        /** Trial 시작 시 DataSource를 생성하고 소스 데이터를 미리 적재합니다. */
        @Setup(Level.Trial)
        fun setup() = trialSetup()

        /** 각 반복 전에 대상/잡 테이블을 비웁니다. */
        @Setup(Level.Iteration)
        fun iterSetup() = iterationSetup()

        /** Trial 종료 시 DataSource를 닫습니다. */
        @TearDown(Level.Trial)
        fun tearDown() = trialTearDown()
    }

    // ── @Benchmark 메서드 ────────────────────────────────────────────────

    /**
     * 시드 벤치마크: H2 소스 테이블 INSERT 처리량을 측정합니다.
     *
     * [SeedState.iterSetup]에서 스키마를 초기화한 후 INSERT만 측정합니다.
     * [SeedState]의 라이프사이클은 이 메서드에만 적용되므로 [endToEndBatchJobBenchmark]와
     * DataSource 초기화 충돌이 발생하지 않습니다.
     */
    @Benchmark
    fun seedBenchmark(state: SeedState): Int = state.insertSourceRows()

    /**
     * 엔드 투 엔드 배치 잡 벤치마크: H2에서 배치 잡 1회 실행 처리량을 측정합니다.
     *
     * [JobState.iterSetup]에서 대상/잡 테이블을 비운 후 배치 잡 실행만 측정합니다.
     * 소스 데이터는 [JobState.setup]에서 미리 적재됩니다.
     * 시퀀셜(`parallelism=1`)·병렬 경로 모두 `ExposedJdbcBatchJobRepository`를 사용하므로
     * `parallelism` 파라미터의 효과만을 공정하게 측정합니다.
     */
    @Benchmark
    fun endToEndBatchJobBenchmark(state: JobState): Int = state.runJob()
}
