package io.bluetape4k.tokenizer.model

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.AbstractCoreTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.util.*

class TokenizeOptionsTest: AbstractCoreTest() {

    companion object: KLogging()

    @Test
    fun `default tokenize options`() {
        val options = TokenizeOptions()

        options.locale shouldBeEqualTo Locale.KOREAN
    }

    @Test
    fun `custom tokenize options`() {
        val options = TokenizeOptions(Locale.ENGLISH)

        options.locale shouldBeEqualTo Locale.ENGLISH
    }

    @Test
    fun `default tokenize options constant`() {
        val options = TokenizeOptions.DEFAULT

        options.locale shouldBeEqualTo Locale.KOREAN
    }
}
