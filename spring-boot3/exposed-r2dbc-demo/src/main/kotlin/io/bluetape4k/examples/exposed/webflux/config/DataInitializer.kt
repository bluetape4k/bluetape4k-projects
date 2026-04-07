package io.bluetape4k.examples.exposed.webflux.config

import io.bluetape4k.examples.exposed.webflux.domain.Products
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.math.BigDecimal

/**
 * WebFlux 예제용 초기 데이터를 비동기로 적재한다.
 *
 * startup thread를 `runBlocking`으로 붙잡지 않고,
 * 애플리케이션 준비 완료 후 별도 coroutine에서 schema 생성과 seed insert를 수행한다.
 */
@Component
class DataInitializer(
    private val r2dbcDatabase: R2dbcDatabase,
): AutoCloseable {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady(@Suppress("UNUSED_PARAMETER") event: ApplicationReadyEvent) {
        scope.launch {
            initializeData()
        }
    }

    suspend fun initializeData() {
        ensureSchema()
        suspendTransaction(r2dbcDatabase) {
            if (Products.selectAll().count() == 0L) {
                Products.insert {
                    it[name] = "Kotlin Coroutines Book"
                    it[price] = BigDecimal("39.99")
                    it[stock] = 100
                }
                Products.insert {
                    it[name] = "Spring WebFlux Guide"
                    it[price] = BigDecimal("49.99")
                    it[stock] = 50
                }
                Products.insert {
                    it[name] = "Reactive Programming"
                    it[price] = BigDecimal("29.99")
                    it[stock] = 200
                }
            }
        }
    }

    private suspend fun ensureSchema() {
        suspendTransaction(r2dbcDatabase) {
            val tables = SchemaUtils.listTables()
            val productTableExists = tables.any { it.equals(Products.tableName, ignoreCase = true) }
            if (!productTableExists) {
                SchemaUtils.create(Products)
            }
        }
    }

    override fun close() {
        scope.cancel()
    }
}
