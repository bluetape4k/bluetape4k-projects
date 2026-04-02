package io.bluetape4k.testcontainers.graphdb

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.AbstractContainerTest
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.neo4j.driver.AuthTokens
import org.neo4j.driver.GraphDatabase
import kotlin.test.assertFailsWith

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MemgraphServerTest: AbstractContainerTest() {

    companion object: KLogging()

    private val memgraph = MemgraphServer.Launcher.memgraph

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
    fun `Neo4j Driver로 Bolt 연결 후 쿼리를 실행할 수 있어야 한다`() {
        GraphDatabase.driver(memgraph.boltUrl, AuthTokens.none()).use { driver ->
            driver.session().use { session ->
                val result = session.run("RETURN 1 AS n")
                val record = result.single()
                val value = record["n"].asInt()
                log.debug { "RETURN 1 AS n => $value" }
                assert(value == 1) { "예상 결과는 1이지만 실제 값은 $value 입니다." }
            }
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
}
