package io.bluetape4k.apache

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

private class SampleCtor(val name: String, val age: Int = 0)

class ApacheConstructorUtilsTest {

    @Test
    fun `invokeConstructor 는 적합한 생성자를 선택한다`() {
        val ctor = SampleCtor::class.java.getMatchingAccessibleConstructor(String::class.java, Int::class.java)
        ctor.shouldNotBeNull()

        val instance = SampleCtor::class.java.invokeConstructor(
            arrayOf<Any?>("alice", 10),
            arrayOf(String::class.java, Int::class.java)
        )
        instance.name shouldBeEqualTo "alice"
        instance.age shouldBeEqualTo 10
    }

}
