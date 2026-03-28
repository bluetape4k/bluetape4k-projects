package io.bluetape4k.exposed.core.inet

import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Test
import java.net.InetAddress

/**
 * PostgreSQL 전용 INET/CIDR 네이티브 타입 및 `<<` 연산자 테스트.
 */
class InetPostgresTest : AbstractExposedTest() {

    companion object : KLogging()

    object PgNetworkTable : LongIdTable("pg_inet_networks") {
        val ip = inetAddress("ip")
        val network = cidr("network")
    }

    @Test
    fun `PostgreSQL INET 네이티브 타입으로 IPv4 저장 및 조회`() {
        val addr = InetAddress.getByName("192.168.1.100")
        withTables(TestDB.POSTGRESQL, PgNetworkTable) {
            PgNetworkTable.insert {
                it[ip] = addr
                it[network] = "192.168.1.0/24"
            }

            val row = PgNetworkTable.selectAll().single()
            val result = row[PgNetworkTable.ip]
            result.shouldNotBeNull()
            result.hostAddress shouldBeEqualTo "192.168.1.100"
        }
    }

    @Test
    fun `PostgreSQL INET 네이티브 타입으로 IPv6 저장 및 조회`() {
        val addr = InetAddress.getByName("2001:db8::1")
        withTables(TestDB.POSTGRESQL, PgNetworkTable) {
            PgNetworkTable.insert {
                it[ip] = addr
                it[network] = "2001:db8::/32"
            }

            val row = PgNetworkTable.selectAll().single()
            val result = row[PgNetworkTable.ip]
            result.shouldNotBeNull()
            result shouldBeEqualTo addr
        }
    }

    @Test
    fun `PostgreSQL CIDR 네이티브 타입으로 네트워크 저장 및 조회`() {
        withTables(TestDB.POSTGRESQL, PgNetworkTable) {
            PgNetworkTable.insert {
                it[ip] = InetAddress.getByName("10.1.1.5")
                it[network] = "10.0.0.0/8"
            }

            val row = PgNetworkTable.selectAll().single()
            val result = row[PgNetworkTable.network]
            result shouldBeEqualTo "10.0.0.0/8"
        }
    }

    @Test
    fun `PostgreSQL isContainedBy 연산자로 IP가 네트워크에 속하는지 확인`() {
        withTables(TestDB.POSTGRESQL, PgNetworkTable) {
            PgNetworkTable.insert {
                it[ip] = InetAddress.getByName("192.168.1.50")
                it[network] = "192.168.1.0/24"
            }
            PgNetworkTable.insert {
                it[ip] = InetAddress.getByName("10.0.0.1")
                it[network] = "192.168.1.0/24"
            }

            val rows = PgNetworkTable.selectAll()
                .where { PgNetworkTable.ip.isContainedBy(PgNetworkTable.network) }
                .toList()

            rows shouldHaveSize 1
            rows.first()[PgNetworkTable.ip].hostAddress shouldBeEqualTo "192.168.1.50"
        }
    }
}
