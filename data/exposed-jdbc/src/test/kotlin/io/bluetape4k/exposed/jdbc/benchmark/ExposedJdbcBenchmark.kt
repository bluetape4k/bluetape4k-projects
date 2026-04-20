package io.bluetape4k.exposed.jdbc.benchmark

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.database.PostgreSQLServer
import io.bluetape4k.utils.ShutdownQueue
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.Measurement
import kotlinx.benchmark.Mode
import kotlinx.benchmark.OutputTimeUnit
import kotlinx.benchmark.Param
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import kotlinx.benchmark.TearDown
import kotlinx.benchmark.Warmup
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.openjdk.jmh.annotations.Threads
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * `data/exposed-jdbc` 모듈의 CRUD 처리량과 HikariCP pool 튜닝 효과를 측정하는 JMH 벤치마크입니다.
 *
 * - 단건 CRUD (`singleInsert`/`singleFindById`/`singleUpdate`)는 `@Threads(8)` 동시성에서
 *   HikariCP pool 경합 효과를 확인합니다.
 * - `batchInsert`는 `batchSize` 파라미터로 배치 크기에 따른 처리량을 비교합니다.
 * - `joinQuery`는 다중 테이블 JOIN 처리량을 측정합니다.
 *
 * PostgreSQL Testcontainers 를 Trial 단위로 1회 시작하며, HikariCP 기본값(max=10/min=2)을 사용합니다.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 2, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 3, timeUnit = TimeUnit.SECONDS)
open class ExposedJdbcBenchmark {

    companion object: KLogging() {
        private const val SEED_USERS = 2_000
        private const val SEED_ORDERS_PER_USER = 5
    }

    object BenchmarkUsers: Table("bench_users") {
        val id = long("id").autoIncrement()
        val name = varchar("name", 128)
        val email = varchar("email", 128)
        val age = integer("age")
        override val primaryKey = PrimaryKey(id)
    }

    object BenchmarkOrders: Table("bench_orders") {
        val id = long("id").autoIncrement()
        val userId = long("user_id").references(BenchmarkUsers.id)
        val amount = integer("amount")
        val status = varchar("status", 32)
        override val primaryKey = PrimaryKey(id)
    }

    @Param("100")
    var batchSize: Int = 100

    private lateinit var postgres: PostgreSQLServer
    private lateinit var dataSource: HikariDataSource
    private lateinit var database: Database

    // seed 범위 내 PK (@Threads 병렬 접근용)
    private val findIdSeq = AtomicLong(1L)
    private val updateIdSeq = AtomicLong(1L)

    @Setup
    fun setup() {
        postgres = PostgreSQLServer().apply {
            start()
            ShutdownQueue.register(this)
        }

        val config = HikariConfig().apply {
            jdbcUrl = postgres.jdbcUrl
            username = postgres.username
            password = postgres.password
            driverClassName = PostgreSQLServer.DRIVER_CLASS_NAME
            maximumPoolSize = 10
            minimumIdle = 2
            connectionTimeout = 30_000
            idleTimeout = 600_000
            maxLifetime = 1_800_000
            poolName = "exposed-jdbc-bench"
        }
        dataSource = HikariDataSource(config)
        database = Database.connect(dataSource)

        // 스키마 초기화 + 시드
        transaction(database) {
            SchemaUtils.drop(BenchmarkOrders, BenchmarkUsers)
            SchemaUtils.create(BenchmarkUsers, BenchmarkOrders)

            BenchmarkUsers.batchInsert((1..SEED_USERS).toList()) { i ->
                this[BenchmarkUsers.name] = "user-$i"
                this[BenchmarkUsers.email] = "user$i@bench.local"
                this[BenchmarkUsers.age] = 20 + (i % 50)
            }
            val statuses = arrayOf("NEW", "PAID", "SHIPPED", "CANCELLED")
            val orders = (1..SEED_USERS).flatMap { uid ->
                (1..SEED_ORDERS_PER_USER).map { seq ->
                    Triple(uid.toLong(), (seq * 10 + uid % 7), statuses[(uid + seq) % statuses.size])
                }
            }
            BenchmarkOrders.batchInsert(orders) { (uid, amt, st) ->
                this[BenchmarkOrders.userId] = uid
                this[BenchmarkOrders.amount] = amt
                this[BenchmarkOrders.status] = st
            }
        }
    }

    @TearDown
    fun tearDown() {
        runCatching {
            transaction(database) {
                SchemaUtils.drop(BenchmarkOrders, BenchmarkUsers)
            }
        }
        runCatching { dataSource.close() }
        runCatching { postgres.stop() }
    }

    /**
     * 단건 INSERT — `@Threads(8)` 동시성에서 HikariCP pool 경합 측정.
     */
    @Benchmark
    @Threads(8)
    open fun singleInsert(): Long {
        val rnd = ThreadLocalRandom.current()
        return transaction(database) {
            val id = BenchmarkUsers.insert {
                it[name] = "bench-" + rnd.nextInt()
                it[email] = "bench-" + rnd.nextInt() + "@bench.local"
                it[age] = 20 + rnd.nextInt(50)
            } get BenchmarkUsers.id
            id
        }
    }

    /**
     * 단건 SELECT by PK — `@Threads(8)` 동시성에서 pool 경합 측정.
     */
    @Benchmark
    @Threads(8)
    open fun singleFindById(): Int {
        val pk = (findIdSeq.getAndIncrement() % SEED_USERS) + 1
        return transaction(database) {
            BenchmarkUsers
                .selectAll()
                .where { BenchmarkUsers.id eq pk }
                .count()
                .toInt()
        }
    }

    /**
     * 단건 UPDATE — `@Threads(8)` 동시성에서 pool + 락 경합 측정.
     */
    @Benchmark
    @Threads(8)
    open fun singleUpdate(): Int {
        val pk = (updateIdSeq.getAndIncrement() % SEED_USERS) + 1
        val newAge = 20 + ThreadLocalRandom.current().nextInt(50)
        return transaction(database) {
            BenchmarkUsers.update({ BenchmarkUsers.id eq pk }) {
                it[age] = newAge
            }
        }
    }

    /**
     * batchInsert — `batchSize` 파라미터에 따른 배치 INSERT 처리량.
     */
    @Benchmark
    open fun batchInsert(): Int {
        val rows = (1..batchSize).toList()
        return transaction(database) {
            BenchmarkUsers.batchInsert(rows) { i ->
                this[BenchmarkUsers.name] = "batch-$i"
                this[BenchmarkUsers.email] = "batch-$i@bench.local"
                this[BenchmarkUsers.age] = 20 + (i % 50)
            }.size
        }
    }

    /**
     * 복잡 JOIN — users INNER JOIN orders WHERE amount > X AND status = Y.
     */
    @Benchmark
    open fun joinQuery(): Int {
        return transaction(database) {
            BenchmarkUsers
                .innerJoin(BenchmarkOrders)
                .select(
                    BenchmarkUsers.id,
                    BenchmarkUsers.name,
                    BenchmarkOrders.amount,
                    BenchmarkOrders.status,
                )
                .where {
                    (BenchmarkOrders.amount greater 20) and (BenchmarkOrders.status eq "PAID")
                }
                .limit(100)
                .count()
                .toInt()
        }
    }
}
