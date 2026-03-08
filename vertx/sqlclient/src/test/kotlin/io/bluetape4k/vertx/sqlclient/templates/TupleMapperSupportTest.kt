package io.bluetape4k.vertx.sqlclient.templates

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TupleMapperSupportTest {

    data class UserRecord(
        val id: Long,
        val name: String?,
        val enabled: Boolean,
    )

    @Test
    fun `toParameters는 각 레코드 인덱스를 키 접미사로 붙여 변환한다`() {
        val users = listOf(
            UserRecord(id = 1L, name = "alice", enabled = true),
            UserRecord(id = 2L, name = null, enabled = false),
        )

        val params = users.toParameters()

        assertEquals(1L, params["id0"])
        assertEquals("alice", params["name0"])
        assertEquals(true, params["enabled0"])

        assertEquals(2L, params["id1"])
        assertEquals(null, params["name1"])
        assertEquals(false, params["enabled1"])
    }

    @Test
    fun `toParameters는 빈 목록에서 빈 맵을 반환한다`() {
        val params = emptyList<UserRecord>().toParameters()
        assertTrue(params.isEmpty())
    }
}
