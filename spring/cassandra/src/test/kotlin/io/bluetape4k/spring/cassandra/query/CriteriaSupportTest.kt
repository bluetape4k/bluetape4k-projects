package io.bluetape4k.spring.cassandra.query

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.springframework.data.cassandra.core.query.Criteria

class CriteriaSupportTest {

    @Test
    fun `eq should delegate to is`() {
        val criteria = Criteria.where("name") eq "alice"
        val expected = Criteria.where("name").`is`("alice")

        criteria.toString() shouldBeEqualTo expected.toString()
    }
}
