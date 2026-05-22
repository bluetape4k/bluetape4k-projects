package io.bluetape4k.testcontainers.graphdb

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import io.bluetape4k.testcontainers.AbstractContainerTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.Timeout.ThreadMode.SEPARATE_THREAD
import org.neo4j.driver.AuthTokens
import org.neo4j.driver.Config
import org.neo4j.driver.Driver
import org.neo4j.driver.GraphDatabase
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@TestInstance(Lifecycle.PER_CLASS)
@Timeout(value = 5, unit = TimeUnit.MINUTES, threadMode = SEPARATE_THREAD)
class MemgraphServerTest: AbstractContainerTest() {

    companion object: KLogging()

    private lateinit var memgraph: MemgraphServer

    @BeforeAll
    fun beforeAll() {
        memgraph = MemgraphServer().apply { start() }
    }

    @AfterAll
    fun afterAll() {
        if (this::memgraph.isInitialized && memgraph.isRunning) {
            memgraph.close()
        }
    }

    @Test
    fun `Memgraph 서버가 실행 중이어야 한다`() {
        memgraph.isRunning.shouldBeTrue()
    }

    @Test
    fun `Bolt 포트가 0보다 커야 한다`() {
        memgraph.port shouldBeGreaterThan 0
    }

    @Test
    fun `시스템 프로퍼티에 host 정보가 등록되어야 한다`() {
        val host = System.getProperty("testcontainers.memgraph.host")
        log.debug { "testcontainers.memgraph.host=$host" }
        host.shouldNotBeNull()
    }

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES, threadMode = SEPARATE_THREAD)
    fun `Neo4j Driver로 Bolt 연결 후 쿼리를 실행할 수 있어야 한다`() {
        val driver = GraphDatabase.driver(memgraph.boltUrl, AuthTokens.none(), memgraphDriverConfig())
        try {
            driver.verifyConnectivity()
            driver.session().use { session ->
                val result = session.run("RETURN 1 AS n")
                val record = result.single()
                val value = record["n"].asInt()
                log.debug { "RETURN 1 AS n => $value" }
                value shouldBeEqualTo 1
            }
        } finally {
            driver.closeWithin(10, TimeUnit.SECONDS)
        }
    }

    @Test
    fun `blank image 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { MemgraphServer(image = " ") }
    }

    @Test
    fun `blank tag 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { MemgraphServer(tag = " ") }
    }

    private fun memgraphDriverConfig(): Config =
        Config.builder()
            .withoutEncryption()
            .withTelemetryDisabled(true)
            .withAutoCommitRetriesDisabled(true)
            // Issue #602: keep Memgraph Bolt test shutdown surface minimal on GitHub Ubuntu runners.
            .withEventLoopThreads(1)
            .withMaxConnectionPoolSize(1)
            .withConnectionTimeout(10, TimeUnit.SECONDS)
            .withConnectionAcquisitionTimeout(10, TimeUnit.SECONDS)
            .withConnectionLivenessCheckTimeout(5, TimeUnit.SECONDS)
            .withMaxTransactionRetryTime(5, TimeUnit.SECONDS)
            .build()

    private fun Driver.closeWithin(timeout: Long, unit: TimeUnit) {
        val closeFuture = closeAsync().toCompletableFuture()
        try {
            closeFuture.get(timeout, unit)
        } catch (e: TimeoutException) {
            // Memgraph compatibility is verified by the query; do not let driver shutdown hang Nightly.
            closeFuture.cancel(true)
            log.warn(e) { "Neo4j Driver close timed out after Memgraph query verification." }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw e
        } catch (e: ExecutionException) {
            log.warn(e) { "Neo4j Driver close failed after Memgraph query verification." }
        }
    }
}
