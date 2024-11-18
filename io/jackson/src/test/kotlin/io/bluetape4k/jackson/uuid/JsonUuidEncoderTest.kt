package io.bluetape4k.jackson.uuid

import com.fasterxml.jackson.module.kotlin.readValue
import io.bluetape4k.jackson.Jackson
import io.bluetape4k.jackson.prettyWriteAsString
import io.bluetape4k.jackson.readValueOrNull
import io.bluetape4k.jackson.writeAsString
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.RepeatedTest
import java.util.*

@RandomizedTest
class JsonUuidEncodeTest {

    companion object: KLogging() {
        private const val REPEAT_COUNT = 5
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
        @field:JsonUuidEncoder
        val userId: UUID,

        @field:JsonUuidEncoder(JsonUuidEncoderType.PLAIN)
        val plainUserId: UUID,

        @field:JsonUuidEncoder(JsonUuidEncoderType.BASE62)
        val encodedUserId: UUID,

        // Faker 를 이용하여 의미있는 이름을 사용합니다.
        val username: String,
    )

    @RepeatedTest(REPEAT_COUNT)
    fun `convert uuid to base62 string`(@RandomValue user: User) {
        log.debug { mapper.prettyWriteAsString(user) }

        val jsonText = mapper.writeAsString(user)!!
        val actual = mapper.readValue<User>(jsonText)
        actual shouldBeEqualTo user
    }

    @RepeatedTest(REPEAT_COUNT)
    fun `convert random uuid`(@RandomValue expected: User) {
        log.debug { mapper.prettyWriteAsString(expected) }

        val actual = mapper.readValueOrNull<User>(mapper.writeAsString(expected)!!)
        actual shouldBeEqualTo expected
    }
}
