package io.bluetape4k.testcontainers.graphdb

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.AbstractContainerTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.amshove.kluent.shouldNotBeNullOrBlank
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.neo4j.driver.GraphDatabase
import kotlin.test.assertFailsWith

/**
 * [Neo4jServer] 통합 테스트.
 *
 * [Neo4jServer.Launcher.neo4j] 싱글턴 인스턴스를 사용하여
 * Neo4j 서버의 기동, 포트, 시스템 프로퍼티, Bolt 연결을 검증합니다.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Neo4jServerTest: AbstractContainerTest() {

    companion object: KLogging()

    private val neo4j: Neo4jServer get() = Neo4jServer.Launcher.neo4j

    @Test
    @Order(1)
    fun `Neo4j 서버가 정상적으로 실행되어야 한다`() {
        neo4j.isRunning.shouldBeTrue()
        log.debug { "Neo4j isRunning=${neo4j.isRunning}" }
    }

    @Test
    @Order(2)
    fun `Bolt 포트가 0보다 커야 한다`() {
        neo4j.port shouldBeGreaterThan 0
        log.debug { "Neo4j bolt port=${neo4j.port}" }
    }

    @Test
    @Order(3)
    fun `시스템 프로퍼티에 Neo4j 호스트가 등록되어야 한다`() {
        val host = System.getProperty("testcontainers.neo4j.host")
        host.shouldNotBeNullOrBlank()
        log.debug { "testcontainers.neo4j.host=$host" }
    }

    @Test
    @Order(4)
    fun `시스템 프로퍼티에 Neo4j Bolt URL이 등록되어야 한다`() {
        val boltUrl = System.getProperty("testcontainers.neo4j.bolt.url")
        boltUrl.shouldNotBeNullOrBlank()
        log.debug { "testcontainers.neo4j.bolt.url=$boltUrl" }
    }

    @Test
    @Order(5)
    fun `Neo4j Bolt Driver로 연결을 검증할 수 있어야 한다`() {
        GraphDatabase.driver(neo4j.boltUrl).use { driver ->
            driver.verifyConnectivity()
            log.debug { "Neo4j driver connectivity verified. boltUrl=${neo4j.boltUrl}" }
        }
    }

    @Test
    @Order(6)
    fun `Neo4j 세션에서 Cypher 쿼리를 실행할 수 있어야 한다`() {
        GraphDatabase.driver(neo4j.boltUrl).use { driver ->
            driver.session().use { session ->
                val result = session.run("RETURN 1 as n")
                val record = result.single()
                record.shouldNotBeNull()
                val value = record["n"].asInt()
                log.debug { "RETURN 1 as n => $value" }
                value shouldBeEqualTo 1
            }
        }
    }

    @Test
    @Order(7)
    fun `blank image tag 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { Neo4jServer(image = " ") }
        assertFailsWith<IllegalArgumentException> { Neo4jServer(tag = " ") }
    }
}
