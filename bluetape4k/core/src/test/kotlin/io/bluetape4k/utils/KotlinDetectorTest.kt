package io.bluetape4k.utils

import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.delay
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class KotlinDetectorTest {

    companion object: KLogging()

    @Test
    fun `detect kotlin environment`() {
        KotlinDetector.isKotlinPresent.shouldBeTrue()
    }

    @Test
    fun `detect kotlin class`() {
        // Java Class 들은 Kotlin type 이 아니다.
        java.util.Date::class.java.isKotlinType.shouldBeFalse()
        java.time.Instant::class.java.isKotlinType.shouldBeFalse()

        // Kotlin 타입은 true 이다.
        kotlin.Int::class.java.isKotlinType.shouldBeTrue()
        kotlin.collections.List::class.java.isKotlinType.shouldBeTrue()
        kotlin.IntArray::class.java.isKotlinType.shouldBeTrue()
        kotlin.sequences.Sequence::class.java.isKotlinType.shouldBeTrue()
        kotlin.ranges.IntProgression::class.java.isKotlinType.shouldBeTrue()


        KotlinDetector::class.java.isKotlinType.shouldBeTrue()
    }

    @Test
    fun `class method is suspendable`() {
        val klazz = SampleClass::class
        klazz.getSuspendFunctions().map { it.name } shouldBeEqualTo listOf("suspendFunc")

        klazz.isSuspendFunction("suspendFunc").shouldBeTrue()

        // normal function 은 suspend function이 아니다.
        klazz.isSuspendFunction("normalFunc").shouldBeFalse()
    }

    @Test
    fun `object method is suspendable`() {
        val klazz = SampleObject::class
        klazz.getSuspendFunctions().map { it.name } shouldBeEqualTo listOf("suspendFunc")

        klazz.isSuspendFunction("suspendFunc").shouldBeTrue()

        // normal function 은 suspend function이 아니다.
        klazz.isSuspendFunction("normalFunc").shouldBeFalse()
    }

    private class SampleClass {
        fun normalFunc(): String = "normal"
        suspend fun suspendFunc(): String {
            delay(1L)
            return "suspendFunc"
        }
    }

    private object SampleObject {
        fun normalFunc(): String = "normal"
        suspend fun suspendFunc(): String {
            delay(1L)
            return "suspendFunc"
        }
    }
}
