package io.bluetape4k.tokenizer.model

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.AbstractCoreTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.util.*

class TokenizeMessageIntegrationTest: AbstractCoreTest() {

    companion object: KLogging()

    @Test
    fun `complete tokenize workflow`() {
        // 1. Create options
        val options = TokenizeOptions(Locale.KOREAN)

        // 2. Create request
        val request =
            tokenizeRequestOf(
                text = "한글 텍스트",
                options = options,
            )

        // 3. Verify request
        request.text shouldBeEqualTo "한글 텍스트"
        request.options.locale shouldBeEqualTo Locale.KOREAN

        // 4. Create response
        val response =
            tokenizeResponseOf(
                text = request.text,
                tokens = listOf("한글", "텍스트"),
            )

        // 5. Verify response
        response.text shouldBeEqualTo "한글 텍스트"
        response.tokens shouldBeEqualTo listOf("한글", "텍스트")
    }

    @Test
    fun `tokenize request with default options`() {
        val request = tokenizeRequestOf("test")

        request.text shouldBeEqualTo "test"
        request.options.locale shouldBeEqualTo Locale.KOREAN
    }

    @Test
    fun `tokenize response with empty tokens`() {
        val response = tokenizeResponseOf("test")

        response.text shouldBeEqualTo "test"
        response.tokens shouldBeEqualTo emptyList()
    }
}
