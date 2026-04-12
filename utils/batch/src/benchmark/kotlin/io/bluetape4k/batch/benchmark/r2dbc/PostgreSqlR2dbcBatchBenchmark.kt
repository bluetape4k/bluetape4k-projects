package io.bluetape4k.batch.benchmark.r2dbc

import io.bluetape4k.batch.benchmark.support.BenchmarkDatabase
import io.bluetape4k.exposed.tests.Containers
import io.bluetape4k.exposed.tests.TestDBConfig
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

/** PostgreSQL 대상 R2DBC 배치 벤치마크입니다. */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
class PostgreSqlR2dbcBatchBenchmark {

    @State(Scope.Benchmark)
    class SeedState : AbstractR2dbcSeedState() {
        override val database: BenchmarkDatabase get() = BenchmarkDatabase.POSTGRESQL
        override fun connectionUrl(): String {
            if (TestDBConfig.useTestcontainers) {
                Containers.Postgres.port
            }
            return postgreSqlR2dbcUrl()
        }

        @Param("1000", "10000", "100000")
        override var dataSize: Int = 1000

        @Param("10", "30", "60")
        override var poolSize: Int = 10

        @Setup(Level.Trial)
        fun setup() = trialSetup()

        @Setup(Level.Iteration)
        fun iterSetup() = iterationSetup()

        @TearDown(Level.Trial)
        fun tearDown() = trialTearDown()
    }

    @State(Scope.Benchmark)
    class JobState : AbstractR2dbcJobState() {
        override val database: BenchmarkDatabase get() = BenchmarkDatabase.POSTGRESQL
        override fun connectionUrl(): String {
            if (TestDBConfig.useTestcontainers) {
                Containers.Postgres.port
            }
            return postgreSqlR2dbcUrl()
        }

        @Param("1000", "10000", "100000")
        override var dataSize: Int = 1000

        @Param("10", "30", "60")
        override var poolSize: Int = 10

        @Param("1", "4", "8")
        override var parallelism: Int = 1

        @Setup(Level.Trial)
        fun setup() = trialSetup()

        @Setup(Level.Iteration)
        fun iterSetup() = iterationSetup()

        @TearDown(Level.Trial)
        fun tearDown() = trialTearDown()
    }

    @Benchmark
    fun seedBenchmark(state: SeedState): Int = state.insertSourceRows()

    @Benchmark
    fun endToEndBatchJobBenchmark(state: JobState): Int = state.runJob()
}
