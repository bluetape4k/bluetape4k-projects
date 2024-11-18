package io.bluetape4k.jackson.mask

import io.bluetape4k.jackson.Jackson
import io.bluetape4k.jackson.prettyWriteAsString
import io.bluetape4k.jackson.writeAsString
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.io.Serializable
import java.math.BigDecimal

@RandomizedTest
class JsonMaskerTest {

    companion object: KLogging() {
        private const val REPEAT_COUNT = 5
    }

    data class User(
        val name: String,

        @get:JsonMasker("masked: personal information")
        val mobile: String,

        @get:JsonMasker
        val salary: BigDecimal,

        @get:JsonMasker("masked: confidential information")
        val rsu: Long = 0L,

        @get:JsonMasker
        val address: String? = null,
    ): Serializable

    private val mapper = Jackson.defaultJsonMapper

    @Test
    fun `masking field with @JsonMasker`() {
        val user = User("debop", "010-8955-5081", BigDecimal.TEN)
        log.debug { mapper.prettyWriteAsString(user) }

        val jsonText = mapper.writeAsString(user)!!
        jsonText shouldContain "masked: personal information"   // mobile
        jsonText shouldContain JsonMasker.DEFAULT_MASKED_STRING  // salary
    }

    @RepeatedTest(REPEAT_COUNT)
    fun `masking field with @JsonMasker with random object`(@RandomValue user: User) {
        log.debug { mapper.prettyWriteAsString(user) }

        val jsonText = mapper.writeAsString(user)!!
        jsonText shouldContain "masked: personal information"   // mobile
        jsonText shouldContain JsonMasker.DEFAULT_MASKED_STRING  // salary
    }
}
