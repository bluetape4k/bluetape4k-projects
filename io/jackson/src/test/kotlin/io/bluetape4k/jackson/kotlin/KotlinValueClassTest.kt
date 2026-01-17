package io.bluetape4k.jackson.kotlin

import com.fasterxml.jackson.module.kotlin.readValue
import io.bluetape4k.jackson.Jackson
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.utils.Runtimex
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
import java.io.Serializable

class KotlinValueClassTest {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
        private val faker = Fakers.faker
    }

    private val mapper = Jackson.defaultJsonMapper

    data class SomeValue(
        val id: Identifier,
        val name: String,
    ): Serializable

    @JvmInline
    value class Identifier(val id: Long): Serializable

    private fun newSomeValue(): SomeValue {
        return SomeValue(
            id = Identifier(faker.random().nextLong()),
            name = faker.name().fullName()
        )
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `json serialize value class`() {
        val someValue = SomeValue(
            id = Identifier(faker.random().nextLong()),
            name = faker.name().fullName()
        )
        verifySerializeValueClass(someValue)
    }

    @Test
    fun `json serdes value class in multi threadins`() {
        MultithreadingTester()
            .numThreads(2 * Runtimex.availableProcessors)
            .roundsPerThread(16)
            .add {
                verifySerializeValueClass(newSomeValue())
            }
            .run()
    }

    @Test
    fun `json serdes value class in suspended jobs`() = runTest {
        SuspendedJobTester()
            .numThreads(2 * Runtimex.availableProcessors)
            .roundsPerJob(16 * 2 * Runtimex.availableProcessors)
            .add {
                verifySerializeValueClass(newSomeValue())
            }
            .run()
    }

    @EnabledOnJre(JRE.JAVA_21)
    @Test
    fun `json serdes value class in virtual threads`() {
        StructuredTaskScopeTester()
            .roundsPerTask(16 * 2 * Runtimex.availableProcessors)
            .add {
                verifySerializeValueClass(newSomeValue())
            }
            .run()
    }

    private fun verifySerializeValueClass(someValue: SomeValue) {
        val json = mapper.writeValueAsString(someValue)
        val actual = mapper.readValue<SomeValue>(json)
        actual shouldBeEqualTo someValue
    }
}
