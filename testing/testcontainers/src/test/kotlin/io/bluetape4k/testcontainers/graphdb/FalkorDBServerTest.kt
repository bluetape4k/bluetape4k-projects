package io.bluetape4k.testcontainers.graphdb

import com.falkordb.FalkorDB
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.AbstractContainerTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeBlank
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertFailsWith

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FalkorDBServerTest : AbstractContainerTest() {

    companion object : KLogging() {
        private const val GRAPH_NAME = "social"
    }

    private val falkordb = FalkorDBServer.Launcher.falkordb

    @Test
    fun `FalkorDB 서버가 실행 중이어야 한다`() {
        falkordb.isRunning.shouldBeTrue()
    }

    @Test
    fun `Redis 포트가 0보다 커야 한다`() {
        falkordb.port shouldBeGreaterThan 0
    }

    @Test
    fun `url 은 redis 스킴을 사용해야 한다`() {
        log.debug { "falkordb.url=${falkordb.url}" }
        falkordb.url shouldContain "redis://"
    }

    @Test
    fun `propertyNamespace 는 falkordb 이어야 한다`() {
        falkordb.propertyNamespace shouldBeEqualTo "falkordb"
    }

    @Test
    fun `propertyKeys 는 host port url 을 포함해야 한다`() {
        val keys = falkordb.propertyKeys()
        keys shouldContain "host"
        keys shouldContain "port"
        keys shouldContain "url"
    }

    @Test
    fun `properties 는 모든 키에 비어있지 않은 값을 반환해야 한다`() {
        val props = falkordb.properties()
        falkordb.propertyKeys().forEach { key ->
            val value = props[key]
            value.shouldNotBeNull()
            value.shouldNotBeBlank()
        }
    }

    @Test
    fun `시스템 프로퍼티에 host port url 이 등록되어야 한다`() {
        val host = System.getProperty("testcontainers.falkordb.host")
        val port = System.getProperty("testcontainers.falkordb.port")
        val url = System.getProperty("testcontainers.falkordb.url")
        log.debug { "testcontainers.falkordb.host=$host" }
        log.debug { "testcontainers.falkordb.port=$port" }
        log.debug { "testcontainers.falkordb.url=$url" }
        host.shouldNotBeNull()
        port.shouldNotBeNull()
        url.shouldNotBeNull()
    }

    @Test
    fun `jfalkordb 클라이언트로 그래프 쿼리를 실행할 수 있어야 한다`() {
        FalkorDB.driver(falkordb.host, falkordb.port).use { driver ->
            driver.graph(GRAPH_NAME).use { graph ->
                try {
                    graph.query("CREATE (:Person {name: 'Alice'})")
                    val rows = graph.query("MATCH (p:Person) RETURN p.name AS name").toList()
                    rows.shouldNotBeEmpty()
                    rows.first().getString("name") shouldBeEqualTo "Alice"
                } finally {
                    runCatching { graph.deleteGraph() }
                }
            }
        }
    }

    @Test
    fun `blank image 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { FalkorDBServer(image = " ") }
    }

    @Test
    fun `blank tag 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { FalkorDBServer(tag = " ") }
    }
}
