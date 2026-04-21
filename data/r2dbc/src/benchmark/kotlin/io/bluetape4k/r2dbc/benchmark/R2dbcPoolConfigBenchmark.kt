package io.bluetape4k.r2dbc.benchmark

import io.bluetape4k.r2dbc.pool.R2dbcPoolConfig
import io.bluetape4k.r2dbc.pool.toConnectionPoolConfiguration
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import io.r2dbc.spi.Option
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
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * R2DBC 풀 설정 생성과 [io.r2dbc.pool.ConnectionPoolConfiguration] 변환 처리량을 측정합니다.
 *
 * ## 실행 방법
 * ```bash
 * ./gradlew :bluetape4k-r2dbc:benchmarkPoolConfig
 * ```
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Warmup(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
open class R2dbcPoolConfigBenchmark {

    @Param("default", "highThroughput")
    lateinit var profile: String

    private lateinit var connectionFactory: ConnectionFactory
    private lateinit var poolConfig: R2dbcPoolConfig

    @Setup(Level.Trial)
    fun setup() {
        val options = h2ConnectionFactoryOptions("pool_benchmark_${profile}_${System.nanoTime()}")
        connectionFactory = ConnectionFactories.get(options)
        poolConfig = when (profile) {
            "default" -> R2dbcPoolConfig()
            "highThroughput" -> R2dbcPoolConfig.highThroughput(
                maxSize = 64,
                warmupSize = 16,
                poolName = "benchmark-r2dbc",
            ).copy(
                maxAcquireTime = Duration.ofSeconds(2),
            )
            else -> error("Unknown profile: $profile")
        }
    }

    @Benchmark
    fun convertToConnectionPoolConfiguration(blackhole: Blackhole) {
        blackhole.consume(poolConfig.toConnectionPoolConfiguration(connectionFactory))
    }

    private fun h2ConnectionFactoryOptions(databaseName: String): ConnectionFactoryOptions =
        ConnectionFactoryOptions.builder()
            .option(ConnectionFactoryOptions.DRIVER, "h2")
            .option(ConnectionFactoryOptions.PROTOCOL, "mem")
            .option(ConnectionFactoryOptions.DATABASE, databaseName)
            .option(Option.valueOf("DB_CLOSE_DELAY"), "-1")
            .build()
}
