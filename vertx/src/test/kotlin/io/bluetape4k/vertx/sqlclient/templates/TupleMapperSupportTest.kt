package io.bluetape4k.vertx.sqlclient.templates

import io.bluetape4k.support.asBoolean
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

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
        params["enabled0"].asBoolean().shouldBeTrue()

        params["id1"] shouldBeEqualTo 2L
        params["name1"].shouldBeNull()
        params["enabled1"].asBoolean().shouldBeFalse()
    }

    @Test
    fun `toParameters는 빈 목록에서 빈 맵을 반환한다`() {
        val params = emptyList<UserRecord>().toParameters()
        params.shouldBeEmpty()
    }
}
