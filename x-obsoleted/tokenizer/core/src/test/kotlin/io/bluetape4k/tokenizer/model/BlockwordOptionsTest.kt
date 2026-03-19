package io.bluetape4k.tokenizer.model

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.AbstractCoreTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.util.*

class BlockwordOptionsTest: AbstractCoreTest() {

    companion object: KLogging()

    @Test
    fun `default options`() {
        val options = BlockwordOptions.DEFAULT

        options.mask shouldBeEqualTo "*"
        options.locale shouldBeEqualTo Locale.KOREAN
        options.severity shouldBeEqualTo Severity.DEFAULT
    }

    @Test
    fun `custom options`() {
        val options =
            BlockwordOptions(
                mask = "#",
                locale = Locale.ENGLISH,
                severity = Severity.HIGH,
            )

        options.mask shouldBeEqualTo "#"
        options.locale shouldBeEqualTo Locale.ENGLISH
        options.severity shouldBeEqualTo Severity.HIGH
    }

    @Test
    fun `factory function with all parameters`() {
        val options =
            blockwordOptionsOf(
                mask = "X",
                locale = Locale.JAPANESE,
                severity = Severity.MIDDLE,
            )

        options.mask shouldBeEqualTo "X"
        options.locale shouldBeEqualTo Locale.JAPANESE
        options.severity shouldBeEqualTo Severity.MIDDLE
    }

    @Test
    fun `factory function with default severity`() {
        val options =
            blockwordOptionsOf(
                mask = "*",
                locale = Locale.KOREAN,
            )

        options.severity shouldBeEqualTo Severity.DEFAULT
    }

    @Test
    fun `options equality`() {
        val options1 = BlockwordOptions()
        val options2 = BlockwordOptions()

        options1 shouldBeEqualTo options2
    }

    @Test
    fun `options data class copy`() {
        val original = BlockwordOptions(mask = "*", severity = Severity.LOW)
        val copy = original.copy(mask = "#")

        copy.mask shouldBeEqualTo "#"
        copy.severity shouldBeEqualTo Severity.LOW
    }
}
