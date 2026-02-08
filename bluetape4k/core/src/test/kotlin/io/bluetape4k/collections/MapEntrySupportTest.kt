package io.bluetape4k.collections

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.util.AbstractMap

class MapEntrySupportTest {

    @Test
    fun `pair to map entry`() {
        val entry = ("k" to 1).toMapEntry()
        entry.key shouldBeEqualTo "k"
        entry.value shouldBeEqualTo 1
    }

    @Test
    fun `map entry to pair`() {
        val entry = AbstractMap.SimpleEntry("k", 1)
        entry.toPair() shouldBeEqualTo ("k" to 1)
    }
}
