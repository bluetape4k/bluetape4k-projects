package io.bluetape4k.batch.benchmark.jdbc

import io.bluetape4k.batch.benchmark.support.BenchmarkDatabase
import io.bluetape4k.exposed.tests.Containers
import io.bluetape4k.exposed.tests.TestDBConfig
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
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
 * PostgreSQL Testcontainers 기반 JDBC 배치 벤치마크.
 *
 * ## DB 설정
 * - [TestDBConfig.useTestcontainers]가 `true`면 Testcontainers PostgreSQL 컨테이너를 사용합니다.
 * - `false`면 로컬 PostgreSQL (`localhost:5432/exposed`)에 연결합니다.
 * - 컨테이너 기동은 Trial setup 전에 완료되므로 측정에 포함되지 않습니다.
 *
 * ## JMH 라이프사이클 설계
 * 시드(INSERT)와 엔드 투 엔드 시나리오의 라이프사이클이 충돌하지 않도록 각 시나리오를
 * 별도의 `@State` 클래스([SeedState], [JobState])로 분리합니다.
 *
 * ## 측정 경계
 * - **시드 벤치마크** (`seedBenchmark`): 각 반복 전에 스키마를 초기화하고 INSERT만 측정합니다.
 * - **엔드 투 엔드 벤치마크** (`endToEndBatchJobBenchmark`): Trial 시작 시 소스 데이터를 미리 적재하고,
 *   각 반복 전에 대상/잡 테이블을 비운 뒤 배치 잡 1회 실행만 측정합니다.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
class PostgreSqlJdbcBatchBenchmark {

    companion object : KLogging()

    /**
     * PostgreSQL 시드 시나리오 전용 JMH State.
     *
     * 이 클래스에만 `@Setup(Level.Trial)`, `@Setup(Level.Iteration)`,
     * `@TearDown(Level.Trial)` 메서드가 존재하므로 [JobState]의 라이프사이클과
     * 완전히 분리됩니다.
     */
    @State(Scope.Benchmark)
    class SeedState : AbstractJdbcSeedState() {

        companion object : KLogging()

        override val database: BenchmarkDatabase get() = BenchmarkDatabase.POSTGRESQL

        /**
         * PostgreSQL JDBC URL을 반환합니다.
         *
         * Testcontainers 사용 시 컨테이너가 아직 기동되지 않았다면 이 시점에 기동됩니다.
         * 컨테이너 기동 비용은 Trial setup 내에서 발생하며 JMH 측정에 포함되지 않습니다.
         */
        override fun jdbcUrl(): String {
            val options = "?lc_messages=en_US.UTF-8"
            return if (TestDBConfig.useTestcontainers) {
                log.info { "PostgreSQL Testcontainers 컨테이너 접속 URL 조회 중 (Seed)..." }
                Containers.Postgres.jdbcUrl + options
            } else {
                "jdbc:postgresql://localhost:5432/exposed$options"
            }
        }

        override fun driverClassName(): String = io.bluetape4k.jdbc.JdbcDrivers.DRIVER_CLASS_POSTGRESQL

        override fun dbUser(): String =
            if (TestDBConfig.useTestcontainers) "test" else "exposed"

        override fun dbPassword(): String =
            if (TestDBConfig.useTestcontainers) "test" else ""

        override fun initSql(): String = "SET TIMEZONE='UTC'"

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
     * PostgreSQL 엔드 투 엔드 시나리오 전용 JMH State.
     *
     * 이 클래스에만 `@Setup(Level.Trial)`, `@Setup(Level.Iteration)`,
     * `@TearDown(Level.Trial)` 메서드가 존재하므로 [SeedState]의 라이프사이클과
     * 완전히 분리됩니다.
     */
    @State(Scope.Benchmark)
    class JobState : AbstractJdbcJobState() {

        companion object : KLogging()

        override val database: BenchmarkDatabase get() = BenchmarkDatabase.POSTGRESQL

        /**
         * PostgreSQL JDBC URL을 반환합니다.
         *
         * Testcontainers 사용 시 컨테이너가 아직 기동되지 않았다면 이 시점에 기동됩니다.
         * 컨테이너 기동 비용은 Trial setup 내에서 발생하며 JMH 측정에 포함되지 않습니다.
         */
        override fun jdbcUrl(): String {
            val options = "?lc_messages=en_US.UTF-8"
            return if (TestDBConfig.useTestcontainers) {
                log.info { "PostgreSQL Testcontainers 컨테이너 접속 URL 조회 중 (Job)..." }
                Containers.Postgres.jdbcUrl + options
            } else {
                "jdbc:postgresql://localhost:5432/exposed$options"
            }
        }

        override fun driverClassName(): String = io.bluetape4k.jdbc.JdbcDrivers.DRIVER_CLASS_POSTGRESQL

        override fun dbUser(): String =
            if (TestDBConfig.useTestcontainers) "test" else "exposed"

        override fun dbPassword(): String =
            if (TestDBConfig.useTestcontainers) "test" else ""

        override fun initSql(): String = "SET TIMEZONE='UTC'"

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
     * 시드 벤치마크: PostgreSQL 소스 테이블 INSERT 처리량을 측정합니다.
     *
     * [SeedState.iterSetup]에서 스키마를 초기화한 후 INSERT만 측정합니다.
     * [SeedState]의 라이프사이클은 이 메서드에만 적용되므로 [endToEndBatchJobBenchmark]와
     * DataSource 초기화 충돌이 발생하지 않습니다.
     */
    @Benchmark
    fun seedBenchmark(state: SeedState): Int = state.insertSourceRows()

    /**
     * 엔드 투 엔드 배치 잡 벤치마크: PostgreSQL에서 배치 잡 1회 실행 처리량을 측정합니다.
     *
     * [JobState.iterSetup]에서 대상/잡 테이블을 비운 후 배치 잡 실행만 측정합니다.
     * 소스 데이터는 [JobState.setup]에서 미리 적재됩니다.
     * 시퀀셜(`parallelism=1`)·병렬 경로 모두 `ExposedJdbcBatchJobRepository`를 사용하므로
     * `parallelism` 파라미터의 효과만을 공정하게 측정합니다.
     */
    @Benchmark
    fun endToEndBatchJobBenchmark(state: JobState): Int = state.runJob()
}
