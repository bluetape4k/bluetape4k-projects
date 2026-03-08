package io.bluetape4k.vertx.sqlclient.templates

import org.amshove.kluent.shouldBeEmpty
import org.junit.jupiter.api.Test
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull

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

        params["id0"] shouldBeEqualTo 1L
        params["name0"] shouldBeEqualTo "alice"
        params["enabled0"] shouldBeEqualTo true

        params["id1"] shouldBeEqualTo 2L
        params["name1"].shouldBeNull()
        params["enabled1"] shouldBeEqualTo false
    }

    @Test
    fun `toParameters는 빈 목록에서 빈 맵을 반환한다`() {
        val params = emptyList<UserRecord>().toParameters()
        params.shouldBeEmpty()
    }
}
