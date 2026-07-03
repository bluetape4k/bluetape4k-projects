package io.bluetape4k.cassandra

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.junit.jupiter.api.Test

/**
 * [CassandraAdmin]의 keyspace 생성/삭제 및 버전 조회 기능을 검증합니다.
 */
class CassandraAdminTest : AbstractCassandraTest() {

    companion object : KLoggingChannel() {
        private const val TEST_KEYSPACE = "admin_test_ks"
    }

    @Test
    fun `createKeyspace 는 keyspace 를 생성하고 wasApplied 를 반환한다`() {
        // 사전 정리
        CassandraAdmin.dropKeyspace(session, TEST_KEYSPACE)

        val applied = CassandraAdmin.createKeyspace(session, TEST_KEYSPACE)
        applied.shouldBeTrue()
    }

    @Test
    fun `createKeyspace 는 이미 존재하는 keyspace 에 대해 예외 없이 완료된다`() {
        CassandraAdmin.createKeyspace(session, TEST_KEYSPACE)
        // IF NOT EXISTS 이므로 두 번째 호출도 예외 없이 완료됨 (Cassandra wasApplied 반환값은 구현에 따라 다를 수 있음)
        CassandraAdmin.createKeyspace(session, TEST_KEYSPACE)
        // 예외가 발생하지 않으면 성공
    }

    @Test
    fun `dropKeyspace 는 존재하는 keyspace 를 삭제한다`() {
        CassandraAdmin.createKeyspace(session, TEST_KEYSPACE)
        val applied = CassandraAdmin.dropKeyspace(session, TEST_KEYSPACE)
        applied.shouldBeTrue()
    }

    @Test
    fun `dropKeyspace 는 존재하지 않는 keyspace 에 대해 예외 없이 완료된다`() {
        CassandraAdmin.dropKeyspace(session, TEST_KEYSPACE)
        // IF EXISTS 이므로 두 번째 호출(이미 없는 상태)도 예외 없이 완료됨
        CassandraAdmin.dropKeyspace(session, TEST_KEYSPACE)
        // 예외가 발생하지 않으면 성공
    }

    @Test
    fun `getReleaseVersion 은 null 이 아닌 버전을 반환한다`() {
        val version = CassandraAdmin.getReleaseVersion(session)
        version.shouldNotBeNull()
    }

    @Test
    fun `createKeyspace 는 blank keyspace 를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            CassandraAdmin.createKeyspace(session, " ")
        }
    }

    @Test
    fun `createKeyspace 는 양수가 아닌 replicationFactor 를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            CassandraAdmin.createKeyspace(session, TEST_KEYSPACE, replicationFactor = 0)
        }
    }

    @Test
    fun `dropKeyspace 는 blank keyspace 를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            CassandraAdmin.dropKeyspace(session, " ")
        }
    }
}
