package io.bluetape4k

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.Test
import kotlin.collections.set

class GetterSetterTest {

    companion object: KLogging()

    @Test
    fun `getter setter operation`() {
        val map = hashMapOf<Int, String>()

        val getter = getterOperator<Int, String?> { key -> map[key] }
        val setter = setterOperator { key: Int, value: String -> map[key] = value }

        getter[1].shouldBeNull()

        setter[1] = "abc"
        getter[1] shouldBeEqualTo "abc"

        setter[2] = "가나다"
        getter[2] shouldBeEqualTo "가나다"
    }
}
