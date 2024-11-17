package io.bluetape4k.utils

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainSame
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*

class KotlinDetectorTest {

    companion object: KLogging()

    @Test
    fun `detect kotlin environment`() {
        KotlinDetector.isKotlinPresent.shouldBeTrue()
    }

    @Test
    fun `detect kotlin class`() {
        Date::class.java.isKotlinType.shouldBeFalse()
        Instant::class.java.isKotlinType.shouldBeFalse()

        Sequence::class.java.isKotlinType.shouldBeTrue()
        // LazyValue::class.java.isKotlinType.shouldBeTrue()
    }

    @Test
    fun `check method is suspendable`() {
        val klazz = Sample::class
        klazz.getSuspendableFunctions().map { it.name } shouldContainSame listOf("suspendableFunc")

        klazz.isSuspendableFunction("suspendableFunc").shouldBeTrue()
    }

    class Sample {
        fun normalFunc(): String = "normal"
        suspend fun suspendableFunc(): String = "suspendable"
    }
}
