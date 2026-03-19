package io.bluetape4k.tokenizer.model

import com.fasterxml.jackson.module.kotlin.readValue
import io.bluetape4k.jackson.writeAsString
import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.AbstractCoreTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class TokenizeMessageTest: AbstractCoreTest() {

    companion object: KLogging()

    private fun newRequest(): TokenizeRequest = TokenizeRequest(
        text = faker.lorem().paragraph(8),
        options = TokenizeOptions.DEFAULT,
    )

    @Test
    fun `create request with empty text`() {
        assertFailsWith<IllegalArgumentException> {
            tokenizeRequestOf(text = "")
        }
    }

    @Test
    fun `create request with space`() {
        assertFailsWith<IllegalArgumentException> {
            tokenizeRequestOf(text = " ")
        }
    }

    @Test
    fun `create request with whitespace`() {
        assertFailsWith<IllegalArgumentException> {
            tokenizeRequestOf(text = "\t")
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert request to json`() {
        val expected = newRequest()
        val json = mapper.writeAsString(expected).shouldNotBeNull()
        val actual = mapper.readValue<TokenizeRequest>(json)

        actual shouldBeEqualTo expected
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert response to json`() {
        val request = newRequest()
        val expected = tokenizeResponseOf(request.text, listOf("토큰1", "토큰2"))

        val json = mapper.writeAsString(expected).shouldNotBeNull()
        val actual = mapper.readValue<TokenizeResponse>(json)

        actual shouldBeEqualTo expected
    }
}
