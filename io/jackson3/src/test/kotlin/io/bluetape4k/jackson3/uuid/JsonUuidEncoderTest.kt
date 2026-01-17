package io.bluetape4k.jackson3.uuid

import io.bluetape4k.jackson3.Jackson
import io.bluetape4k.jackson3.readValueOrNull
import io.bluetape4k.jackson3.writeAsString
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.utils.Runtimex
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
import java.util.*

@RandomizedTest
class JsonUuidEncodeTest {

    companion object: KLogging() {
        private const val REPEAT_COUNT = 5
        private val faker = Fakers.faker
    }

    private val mapper = Jackson.defaultJsonMapper

    /*
        JSON 변환 시 다음과 같이 변환됩니다.
        {
            "userId" : "413684f2-e4db-46a1-8ac7-e7225cebbfd3",
            "plainUserId" : "413684f2-e4db-46a1-8ac7-e7225cebbfd3",
            "encodedUserId" : "6gVuscij1cec8CelrpHU5h",        // base62 encoding for UUID
            "username"  : "debop"
        }
    */
    data class User(
        @get:JsonUuidEncoder
        val userId: UUID,

        @get:JsonUuidEncoder(JsonUuidEncoderType.PLAIN)
        val plainUserId: UUID,

        @get:JsonUuidEncoder(JsonUuidEncoderType.BASE62)
        val encodedUserId: UUID,

        // Faker 를 이용하여 의미있는 이름을 사용합니다.
        val username: String,
    )


    @RepeatedTest(REPEAT_COUNT)
    fun `convert uuid to base62 string`(@RandomValue user: User) {
        verifyJsonUuidEncoder(user)
    }

    @RepeatedTest(REPEAT_COUNT)
    fun `convert random uuid`(@RandomValue expected: User) {
        verifyJsonUuidEncoder(expected)
    }

    @Test
    fun `convert uuid to base62 string in multi threadings`() {
        MultithreadingTester()
            .numThreads(2 * Runtimex.availableProcessors)
            .roundsPerThread(16)
            .add {
                verifyJsonUuidEncoder(newUser())
            }
            .add {
                verifyJsonUuidEncoder(newUser())
            }
            .run()
    }

    @Test
    fun `convert uuid to base62 string in suspended jobs`() = runTest {
        SuspendedJobTester()
            .numThreads(2 * Runtimex.availableProcessors)
            .roundsPerJob(16 * 2 * Runtimex.availableProcessors)
            .add {
                verifyJsonUuidEncoder(newUser())
            }
            .add {
                verifyJsonUuidEncoder(newUser())
            }
            .run()
    }

    @EnabledOnJre(JRE.JAVA_21)
    @Test
    fun `convert uuid to base62 string in virtual threads`() {
        StructuredTaskScopeTester()
            .roundsPerTask(16 * 2 * Runtimex.availableProcessors)
            .add {
                verifyJsonUuidEncoder(newUser())
            }
            .add {
                verifyJsonUuidEncoder(newUser())
            }
            .run()
    }

    private fun newUser(): User = User(
        userId = UUID.randomUUID(),
        plainUserId = UUID.randomUUID(),
        encodedUserId = UUID.randomUUID(),
        username = faker.name().name()
    )

    private fun verifyJsonUuidEncoder(user: User) {
        val jsonText = mapper.writeAsString(user)!!
        log.debug { "jsonText=$jsonText" }

        val actual = mapper.readValueOrNull<User>(jsonText)!!
        actual shouldBeEqualTo user
    }
}
