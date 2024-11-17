package io.bluetape4k.support

import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.fail
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test

@RandomizedTest
class StandardFuncSupportTest {


    companion object: KLogging() {
        private const val REPEAT_SIZE = 3
    }

    @Test
    fun `run safeLet with null`() {
        val p1: String? = null
        val p2: String? = null

        safeLet(p1, p2) { _, _ ->
            fail("p1, p2 둘 다 null 이므로 실행되어서는 안됩니다")
        }

        safeLet(null, "b") { _, _ ->
            fail("p1 이 null 이므로 실행되어서는 안됩니다")
        }
        safeLet("a", null) { _, _ ->
            fail("p2 이 null 이므로 실행되어서는 안됩니다")
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `run safeLet`(@RandomValue p1: String?, @RandomValue p2: String?) {
        safeLet(p1, p2) { a, b ->
            a.shouldNotBeNull()
            b.shouldNotBeNull()
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `when all not null, should execute`(@RandomValue(type = String::class) strs: List<String?>) {
        strs.whenAllNotNull {
            it.size shouldBeEqualTo strs.filterNotNull().size
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `coalesce - find first not null element`(@RandomValue p1: String?, @RandomValue p2: String?) {
        coalesce(p1, p2) shouldBeEqualTo when {
            p1 != null -> p1
            p2 != null -> p2
            else       -> null
        }

        coalesce("a", null) shouldBeEqualTo "a"
        coalesce(null, "b") shouldBeEqualTo "b"
        coalesce("a", "b") shouldBeEqualTo "a"
        coalesce<Any>(null, null).shouldBeNull()
    }
}
