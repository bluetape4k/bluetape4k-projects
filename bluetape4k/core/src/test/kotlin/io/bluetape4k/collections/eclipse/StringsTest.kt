package io.bluetape4k.collections.eclipse

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class StringsTest {

    @Test
    fun `char sequence to collections`() {
        val list = "abc".toFastList()
        list.size shouldBeEqualTo 3
        list.joinToString("") shouldBeEqualTo "abc"

        val set = "abca".toUnifiedSet()
        set.size shouldBeEqualTo 3
    }
}
