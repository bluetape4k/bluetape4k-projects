package io.bluetape4k.collections.eclipse

import io.bluetape4k.collections.AbstractCollectionTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.eclipse.collections.impl.tuple.Tuples
import org.junit.jupiter.api.Test

class TupleSupportTest: AbstractCollectionTest() {

    companion object: KLogging()

    @Test
    fun `Kotlin Pair to Eclipse Tuple`() {
        val pair = 1 to "one"
        val tuple = pair.toTuplePair()

        tuple.one shouldBeEqualTo 1
        tuple.two shouldBeEqualTo "one"
    }

    @Test
    fun `Eclipse Tuple to Kotlin Pair`() {
        val tuple = Tuples.pair(1, "one")
        val pair = tuple.toPair()

        pair.first shouldBeEqualTo 1
        pair.second shouldBeEqualTo "one"
    }
}
