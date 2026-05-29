package io.bluetape4k.r2dbc.benchmark

import io.bluetape4k.r2dbc.pool.R2dbcPoolConfig
import io.bluetape4k.r2dbc.pool.connectionFactoryOptionsOf
import io.bluetape4k.r2dbc.pool.connectionPoolOf
import io.bluetape4k.testcontainers.database.MySQL8Server
import io.bluetape4k.testcontainers.database.PostgreSQLServer
import io.bluetape4k.testcontainers.database.getConnectionFactoryOptions
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.spi.ConnectionFactoryOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
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
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.LongAdder

/**
 * R2DBC 풀에서 커넥션을 획득하고 일정 시간 점유한 뒤 반납하는 경로의 처리량을 측정하는 공통 기반입니다.
 *
 * 측정 경로는 `Publisher`를 코루틴으로 브리지하는 실제 사용 패턴에 맞춰
 * `kotlinx-coroutines-reactor`의 `awaitSingle`과 `kotlinx-coroutines-reactive`의
 * `awaitFirstOrNull`을 사용합니다. [holdMillis]는 쿼리/트랜잭션이 커넥션을 점유하는 시간을
 * 단순 지연으로 모델링합니다.
 */
@State(Scope.Benchmark)
abstract class AbstractR2dbcPoolAcquireBenchmark {

    @Param("default", "highThroughput")
    lateinit var profile: String

    @Param("0", "1", "5")
    var holdMillis: Long = 0

    private lateinit var pool: ConnectionPool
    private lateinit var poolConfig: R2dbcPoolConfig
    private val acquired = LongAdder()
    private val failed = LongAdder()

    protected abstract fun connectionFactoryOptions(): ConnectionFactoryOptions

    protected abstract val databaseName: String

    @Setup(Level.Trial)
    fun setup() {
        poolConfig = acquireBenchmarkPoolConfig(profile)
        pool = connectionPoolOf(connectionFactoryOptions(), poolConfig)
    }

    @TearDown(Level.Trial)
    fun tearDown() {
        if (::pool.isInitialized) {
            pool.close()
        }
        println(
            "pool-acquire result: database=$databaseName, profile=$profile, holdMillis=$holdMillis, " +
                    poolConfig.describeBenchmarkPoolConfig() + ", threads=8, " +
                    "acquired=${acquired.sum()}, failed=${failed.sum()}"
        )
    }

    protected fun acquireAndClose(blackhole: Blackhole) {
        runBlocking {
            try {
                val connection = pool.create().awaitSingle()
                acquired.increment()
                try {
                    if (holdMillis > 0) {
                        delay(holdMillis)
                    }
                    blackhole.consume(connection.metadata.databaseProductName)
                } finally {
                    connection.close().awaitFirstOrNull()
                }
            } catch (e: Exception) {
                failed.increment()
                blackhole.consume(e)
                throw e
            }
        }
    }
}

/**
 * H2 R2DBC 풀 acquire/close 처리량을 측정합니다.
 *
 * ## 실행 방법
 * ```bash
 * ./gradlew :bluetape4k-r2dbc:benchmarkH2PoolAcquire
 * ```
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Warmup(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
open class H2R2dbcPoolAcquireBenchmark: AbstractR2dbcPoolAcquireBenchmark() {

    override val databaseName: String = "H2"

    override fun connectionFactoryOptions(): ConnectionFactoryOptions =
        connectionFactoryOptionsOf("r2dbc:h2:mem:///pool_acquire_${profile}_${System.nanoTime()};DB_CLOSE_DELAY=-1")

    @Benchmark
    @Threads(8)
    fun acquireAndCloseConnection(blackhole: Blackhole) {
        acquireAndClose(blackhole)
    }
}

/**
 * PostgreSQL R2DBC 풀 acquire/close 처리량을 측정합니다.
 *
 * ## 실행 방법
 * ```bash
 * ./gradlew :bluetape4k-r2dbc:benchmarkPostgresPoolAcquire
 * ```
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Warmup(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
open class PostgreSqlR2dbcPoolAcquireBenchmark: AbstractR2dbcPoolAcquireBenchmark() {

    override val databaseName: String = "PostgreSQL"

    override fun connectionFactoryOptions(): ConnectionFactoryOptions =
        PostgreSql.server.getConnectionFactoryOptions()

    @Benchmark
    @Threads(8)
    fun acquireAndCloseConnection(blackhole: Blackhole) {
        acquireAndClose(blackhole)
    }
}

/**
 * MySQL 8 R2DBC 풀 acquire/close 처리량을 측정합니다.
 *
 * ## 실행 방법
 * ```bash
 * ./gradlew :bluetape4k-r2dbc:benchmarkMysql8PoolAcquire
 * ```
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Warmup(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
open class MySql8R2dbcPoolAcquireBenchmark: AbstractR2dbcPoolAcquireBenchmark() {

    override val databaseName: String = "MySQL8"

    override fun connectionFactoryOptions(): ConnectionFactoryOptions =
        MySql8.server.getConnectionFactoryOptions()

    @Benchmark
    @Threads(8)
    fun acquireAndCloseConnection(blackhole: Blackhole) {
        acquireAndClose(blackhole)
    }
}

private object PostgreSql {
    val server: PostgreSQLServer by lazy {
        PostgreSQLServer.Launcher.postgres
    }
}

private object MySql8 {
    val server: MySQL8Server by lazy {
        MySQL8Server.Launcher.mysql
    }
}

internal const val R2DBC_BENCHMARK_VALIDATION_QUERY: String = "SELECT 1"

internal fun acquireBenchmarkPoolConfig(profile: String): R2dbcPoolConfig =
    when (profile) {
        "default"        -> R2dbcPoolConfig(
            maxValidationTime = Duration.ofSeconds(1),
            validationQuery = R2DBC_BENCHMARK_VALIDATION_QUERY,
        )
        "highThroughput" -> R2dbcPoolConfig.highThroughput(
            maxSize = 64,
            warmupSize = 16,
            poolName = "benchmark-r2dbc",
        ).copy(
            maxAcquireTime = Duration.ofSeconds(2),
            validationQuery = R2DBC_BENCHMARK_VALIDATION_QUERY,
        )
        else             -> error("Unknown profile: $profile")
    }

internal fun contentionBenchmarkPoolConfig(
    profile: String,
    maxSize: Int,
): R2dbcPoolConfig =
    when (profile) {
        "default"        -> R2dbcPoolConfig(
            maxSize = maxSize,
            initialSize = 0,
            minIdle = 0,
            maxPendingAcquire = -1,
            maxAcquireTime = Duration.ofSeconds(5),
            maxValidationTime = Duration.ofSeconds(1),
            validationQuery = R2DBC_BENCHMARK_VALIDATION_QUERY,
        )
        "highThroughput" -> R2dbcPoolConfig.highThroughput(
            maxSize = maxSize,
            poolName = "benchmark-r2dbc-contention",
        ).copy(
            maxAcquireTime = Duration.ofMillis(250),
            validationQuery = R2DBC_BENCHMARK_VALIDATION_QUERY,
        )
        else             -> error("Unknown profile: $profile")
    }

internal fun R2dbcPoolConfig.describeBenchmarkPoolConfig(): String =
    "maxSize=$maxSize, initialSize=$initialSize, minIdle=$minIdle, " +
            "maxPendingAcquire=$maxPendingAcquire, maxAcquireTime=$maxAcquireTime, " +
            "validationDepth=$validationDepth, validationQuery=${validationQuery ?: "<none>"}"
