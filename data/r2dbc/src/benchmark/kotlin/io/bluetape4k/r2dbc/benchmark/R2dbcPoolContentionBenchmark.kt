package io.bluetape4k.r2dbc.benchmark

import io.bluetape4k.r2dbc.pool.R2dbcPoolConfig
import io.bluetape4k.r2dbc.pool.connectionFactoryOptionsOf
import io.bluetape4k.r2dbc.pool.connectionPoolOf
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
 * 동시 요청 수가 풀 크기를 초과하는 상황에서 R2DBC 풀의 처리량과 acquire 실패를 측정합니다.
 *
 * ## 실행 방법
 * ```bash
 * ./gradlew :bluetape4k-r2dbc:benchmarkH2PoolContention
 * ```
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Warmup(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
open class H2R2dbcPoolContentionBenchmark {

    @Param("4", "8", "16")
    var maxSize: Int = 0

    @Param("10", "50")
    var holdMillis: Long = 0

    private lateinit var pool: ConnectionPool
    private val acquired = LongAdder()

    @Setup(Level.Trial)
    fun setup() {
        val poolConfig = R2dbcPoolConfig(
            maxSize = maxSize,
            initialSize = 0,
            minIdle = 0,
            maxPendingAcquire = -1,
            maxAcquireTime = Duration.ofSeconds(5),
            maxValidationTime = Duration.ofSeconds(1),
            validationQuery = VALIDATION_QUERY,
        )
        pool = connectionPoolOf(connectionFactoryOptions(), poolConfig)
    }

    @TearDown(Level.Trial)
    fun tearDown() {
        if (::pool.isInitialized) {
            pool.close()
        }
        println(
            "pool-contention result: maxSize=$maxSize, holdMillis=$holdMillis, " +
                    "threads=64, acquired=${acquired.sum()}"
        )
    }

    @Benchmark
    @Threads(64)
    fun acquireHoldAndClose(blackhole: Blackhole) {
        runBlocking {
            try {
                val connection = pool.create().awaitSingle()
                acquired.increment()
                try {
                    delay(holdMillis)
                    blackhole.consume(connection.metadata.databaseProductName)
                } finally {
                    connection.close().awaitFirstOrNull()
                }
            } catch (e: Exception) {
                blackhole.consume(e)
                throw e
            }
        }
    }

    private fun connectionFactoryOptions(): ConnectionFactoryOptions =
        connectionFactoryOptionsOf("r2dbc:h2:mem:///pool_contention_${maxSize}_${holdMillis};DB_CLOSE_DELAY=-1")

    companion object {
        private const val VALIDATION_QUERY = "SELECT 1"
    }
}
