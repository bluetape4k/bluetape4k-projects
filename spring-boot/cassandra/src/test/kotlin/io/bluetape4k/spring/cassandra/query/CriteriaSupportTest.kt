package io.bluetape4k.spring.cassandra.query

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.junit.jupiter.api.Test
import org.springframework.data.cassandra.core.query.Criteria

class CriteriaSupportTest {

    companion object: KLoggingChannel()

    @Test
    fun `eq should delegate to is`() {
        val criteria = Criteria.where("name") eq "alice"
        val expected = Criteria.where("name").`is`("alice")

        log.debug { "criteria=$criteria" }
        criteria.toString() shouldBeEqualTo expected.toString()
    }
}
