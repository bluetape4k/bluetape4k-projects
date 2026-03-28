package io.bluetape4k.exposed.core.inet

import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.net.InetAddress

/**
 * InetAddress / CIDR 컬럼 타입 통합 테스트.
 *
 * H2 및 PostgreSQL 다이얼렉트에서 IPv4/IPv6 주소 및 CIDR 블록 저장/조회를 검증한다.
 */
class InetColumnTypeTest : AbstractExposedTest() {

    companion object : KLogging()

    object NetworkTable : LongIdTable("inet_networks") {
        val ip = inetAddress("ip")
        val network = cidr("network")
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `IPv4 주소 저장 및 조회`(testDB: TestDB) {
        val addr = InetAddress.getByName("192.168.1.1")
        withTables(testDB, NetworkTable) {
            NetworkTable.insert {
                it[ip] = addr
                it[network] = "192.168.0.0/24"
            }

            val row = NetworkTable.selectAll().single()
            val result = row[NetworkTable.ip]
            result.shouldNotBeNull()
            result.hostAddress shouldBeEqualTo "192.168.1.1"
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `IPv6 주소 저장 및 조회`(testDB: TestDB) {
        val addr = InetAddress.getByName("::1")
        withTables(testDB, NetworkTable) {
            NetworkTable.insert {
                it[ip] = addr
                it[network] = "::1/128"
            }

            val row = NetworkTable.selectAll().single()
            val result = row[NetworkTable.ip]
            result.shouldNotBeNull()
            result shouldBeEqualTo addr
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `CIDR 문자열 저장 및 조회`(testDB: TestDB) {
        withTables(testDB, NetworkTable) {
            NetworkTable.insert {
                it[ip] = InetAddress.getByName("10.0.0.1")
                it[network] = "10.0.0.0/8"
            }

            val row = NetworkTable.selectAll().single()
            val result = row[NetworkTable.network]
            result shouldBeEqualTo "10.0.0.0/8"
        }
    }

    @Test
    fun `InetAddressColumnType 과 CidrColumnType 은 dialect 에 따라 sqlType 과 marker 를 선택한다`() {
        val inetType = InetAddressColumnType()
        val cidrType = CidrColumnType()
        val addr = InetAddress.getByName("192.168.1.1")

        withDb(TestDB.H2) {
            inetType.sqlType() shouldBeEqualTo "VARCHAR(45)"
            inetType.parameterMarker(addr) shouldBeEqualTo "?"
            cidrType.sqlType() shouldBeEqualTo "VARCHAR(50)"
            cidrType.parameterMarker("192.168.0.0/24") shouldBeEqualTo "?"
        }

        withDb(TestDB.POSTGRESQL) {
            inetType.sqlType() shouldBeEqualTo "INET"
            inetType.parameterMarker(addr) shouldBeEqualTo "?::inet"
            cidrType.sqlType() shouldBeEqualTo "CIDR"
            cidrType.parameterMarker("192.168.0.0/24") shouldBeEqualTo "?::cidr"
        }
    }

    private fun withDb(testDB: TestDB, block: () -> Unit) {
        transaction(db = testDB.connect()) {
            block()
        }
    }
}
