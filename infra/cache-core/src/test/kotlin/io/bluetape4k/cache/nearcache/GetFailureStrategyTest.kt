package io.bluetape4k.cache.nearcache

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test

class GetFailureStrategyTest {

    companion object: KLogging()

    @Test
    fun `enum 값이 2개 존재`() {
        GetFailureStrategy.entries shouldHaveSize 2
    }

    @Test
    fun `RETURN_FRONT_OR_NULL 값 확인`() {
        GetFailureStrategy.RETURN_FRONT_OR_NULL.name shouldBeEqualTo "RETURN_FRONT_OR_NULL"
    }

    @Test
    fun `PROPAGATE_EXCEPTION 값 확인`() {
        GetFailureStrategy.PROPAGATE_EXCEPTION.name shouldBeEqualTo "PROPAGATE_EXCEPTION"
    }

    @Test
    fun `valueOf 변환`() {
        GetFailureStrategy.valueOf("RETURN_FRONT_OR_NULL") shouldBeEqualTo GetFailureStrategy.RETURN_FRONT_OR_NULL
        GetFailureStrategy.valueOf("PROPAGATE_EXCEPTION") shouldBeEqualTo GetFailureStrategy.PROPAGATE_EXCEPTION
    }
}
