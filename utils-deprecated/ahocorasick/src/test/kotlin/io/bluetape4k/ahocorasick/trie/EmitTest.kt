package io.bluetape4k.ahocorasick.trie

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.jupiter.api.Test

class EmitTest {

    companion object: KLogging()

    @Test
    fun `emit equals`() {
        val e1 = Emit(13, 42, null)
        val e2 = Emit(13, 42, null)
        val e3 = Emit(13, 42, "keyword")

        e1 shouldBeEqualTo e2
        e1 shouldBeEqualTo e3
    }

    @Test
    fun `emit not equals`() {
        val e1 = Emit(13, 42, null)
        val e2 = Emit(0, 1, null)
        val e3 = Emit(13, 31, null)

        e1 shouldNotBeEqualTo e2
        e1 shouldNotBeEqualTo e3
    }
}
