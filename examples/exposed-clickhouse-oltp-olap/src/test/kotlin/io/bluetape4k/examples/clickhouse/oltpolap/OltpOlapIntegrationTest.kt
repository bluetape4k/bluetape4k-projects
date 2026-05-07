package io.bluetape4k.examples.clickhouse.oltpolap

import io.bluetape4k.examples.clickhouse.oltpolap.domain.AnalyticsRepository
import io.bluetape4k.examples.clickhouse.oltpolap.domain.Order
import io.bluetape4k.examples.clickhouse.oltpolap.domain.OrderEvents
import io.bluetape4k.examples.clickhouse.oltpolap.domain.Orders
import io.bluetape4k.examples.clickhouse.oltpolap.domain.OrdersRepository
import io.bluetape4k.exposed.clickhouse.ClickHouseDatabase
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.database.ClickHouseServer
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldNotBeNull
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer

/**
 * PostgreSQL OLTP + ClickHouse OLAP 통합 예제.
 *
 * 시나리오:
 * 1. PostgreSQL에 트랜잭션 단위로 주문을 적재 (OLTP)
 * 2. 적재된 주문을 ClickHouse로 배치 forwarding (Pipeline)
 * 3. ClickHouse에서 리전별 집계 분석 수행 (OLAP)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OltpOlapIntegrationTest {

    companion object: KLogging()

    private val postgresContainer: PostgreSQLContainer<*> =
        PostgreSQLContainer("postgres:16-alpine")

    private val clickhouseServer: ClickHouseServer = ClickHouseServer.Launcher.clickhouse

    private lateinit var pgDb: Database
    private lateinit var chDb: Database

    private val ordersRepo = OrdersRepository()
    private val analyticsRepo = AnalyticsRepository()

    @BeforeAll
    fun setup() {
        postgresContainer.start()

        pgDb = Database.connect(
            url = postgresContainer.jdbcUrl,
            driver = "org.postgresql.Driver",
            user = postgresContainer.username,
            password = postgresContainer.password,
        )

        chDb = ClickHouseDatabase.connect(
            host = clickhouseServer.host,
            port = clickhouseServer.port,
            database = "default",
            user = clickhouseServer.username ?: "test",
            password = clickhouseServer.password ?: "test",
        )

        // PostgreSQL OLTP 스키마
        transaction(pgDb) {
            SchemaUtils.create(Orders)
        }

        // ClickHouse OLAP 스키마
        transaction(chDb) {
            SchemaUtils.create(OrderEvents)
        }
    }

    @AfterAll
    fun teardown() {
        runCatching {
            transaction(pgDb) { SchemaUtils.drop(Orders) }
        }
        runCatching {
            transaction(chDb) { SchemaUtils.drop(OrderEvents) }
        }
        runCatching { postgresContainer.stop() }
    }

    @Test
    fun `OLTP에 주문을 삽입하고 OLAP으로 전달하여 집계 분석한다`() {
        val regions = listOf("ASIA", "EUROPE", "AMERICAS")
        val orders = (1..100).map { i ->
            Order(
                customerId = "customer-${i % 10}",
                productId = "product-${i % 5}",
                amount = (i * 10).toDouble(),
                region = regions[i % 3],
            )
        }

        // 1. OLTP — PostgreSQL 트랜잭션으로 주문 적재
        val insertedOrders = transaction(pgDb) {
            orders.map { order ->
                val id = ordersRepo.insert(order)
                order.copy(id = id)
            }
        }
        insertedOrders.size shouldBeGreaterThan 0
        log.debug { "Inserted ${insertedOrders.size} orders into PostgreSQL OLTP" }

        // 2. Pipeline — OLTP에서 OLAP으로 배치 forwarding
        transaction(chDb) {
            analyticsRepo.batchInsertOrders(insertedOrders)
        }
        log.debug { "Forwarded ${insertedOrders.size} events to ClickHouse OLAP" }

        // 3. OLAP — ClickHouse 집계 분석
        val asiaResult = transaction(chDb) {
            analyticsRepo.analyzeByRegion(this, "ASIA")
        }

        log.debug { "ASIA analytics: $asiaResult" }
        asiaResult.shouldNotBeNull()
        asiaResult.totalOrders shouldBeGreaterThan 0L
        asiaResult.uniqueCustomers shouldBeGreaterThan 0L
        asiaResult.p95Amount shouldBeGreaterThan 0.0
        asiaResult.latestProductId.shouldNotBeNull()
    }
}
