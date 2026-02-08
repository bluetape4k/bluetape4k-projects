package io.bluetape4k.collections.eclipse.ranges

import org.amshove.kluent.shouldBeEqualTo
import org.eclipse.collections.impl.list.mutable.primitive.CharArrayList
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList
import org.junit.jupiter.api.Test

class ProgressionExtensionsTest {

    @Test
    fun `progressions to primitive array lists`() {
        val chars = ('a'..'c').toCharArrayList()
        chars shouldBeEqualTo CharArrayList().apply { add('a'); add('b'); add('c') }

        val ints = (1..3).toIntArrayList()
        ints shouldBeEqualTo IntArrayList().apply { add(1); add(2); add(3) }

        val longs = (1L..3L).toLongArrayList()
        longs shouldBeEqualTo LongArrayList().apply { add(1L); add(2L); add(3L) }
    }
}
