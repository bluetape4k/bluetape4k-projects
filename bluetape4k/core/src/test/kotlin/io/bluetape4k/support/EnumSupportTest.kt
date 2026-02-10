package io.bluetape4k.support

import io.bluetape4k.AbstractCoreTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.enums.enumEntries

class EnumSupportTest: AbstractCoreTest() {

    companion object: KLogging()

    enum class Color { RED, GREEN, BLUE }

    private val expectedNameMap = mapOf("RED" to Color.RED, "GREEN" to Color.GREEN, "BLUE" to Color.BLUE)
    private val expectedList = Color.entries.toList()

    @Nested
    inner class UseEnumEntries {

        private val colorEntries = enumEntries<Color>()

        @Test
        fun `build emup map by enumEntries`() {
            val map = colorEntries.enumMap()
            map shouldBeEqualTo expectedNameMap
        }

        @Test
        fun `get enum list`() {
            val list = colorEntries.enumList()
            list shouldBeEqualTo expectedList
        }

        @Test
        fun `get by name or null`() {
            colorEntries.findByNameOrNull("BLUE", ignoreCase = true) shouldBeEqualTo Color.BLUE
            colorEntries.findByNameOrNull("Blue", ignoreCase = true) shouldBeEqualTo Color.BLUE
            colorEntries.findByNameOrNull("blue", ignoreCase = true) shouldBeEqualTo Color.BLUE

            colorEntries.findByNameOrNull("BLUE", ignoreCase = false) shouldBeEqualTo Color.BLUE
            colorEntries.findByNameOrNull("Blue", ignoreCase = false).shouldBeNull()
            colorEntries.findByNameOrNull("blue", ignoreCase = false).shouldBeNull()
        }

        @Test
        fun `check valid name`() {
            colorEntries.isValidName("BLUE", ignoreCase = true).shouldBeTrue()
            colorEntries.isValidName("Blue", ignoreCase = true).shouldBeTrue()
            colorEntries.isValidName("blue", ignoreCase = true).shouldBeTrue()

            colorEntries.isValidName("BLUE", ignoreCase = false).shouldBeTrue()
            colorEntries.isValidName("Blue", ignoreCase = false).shouldBeFalse()
            colorEntries.isValidName("blue", ignoreCase = false).shouldBeFalse()
        }
    }

    @Nested
    inner class UseReflection {

        @Test
        fun `get emun map`() {
            val map = Color::class.enumMap()
            map shouldBeEqualTo expectedNameMap
        }

        @Test
        fun `get enum list`() {
            val list = Color::class.enumList()
            list shouldBeEqualTo expectedList
        }

        @Test
        fun `get by name or null`() {
            Color::class.findByNameOrNull("BLUE", ignoreCase = true) shouldBeEqualTo Color.BLUE
            Color::class.findByNameOrNull("Blue", ignoreCase = true) shouldBeEqualTo Color.BLUE
            Color::class.findByNameOrNull("blue", ignoreCase = true) shouldBeEqualTo Color.BLUE

            Color::class.findByNameOrNull("BLUE", ignoreCase = false) shouldBeEqualTo Color.BLUE
            Color::class.findByNameOrNull("Blue", ignoreCase = false).shouldBeNull()
            Color::class.findByNameOrNull("blue", ignoreCase = false).shouldBeNull()
        }

        @Test
        fun `check valid name of enum`() {
            Color::class.isValidName("BLUE", ignoreCase = true).shouldBeTrue()
            Color::class.isValidName("Blue", ignoreCase = true).shouldBeTrue()
            Color::class.isValidName("blue", ignoreCase = true).shouldBeTrue()

            Color::class.isValidName("BLUE", ignoreCase = false).shouldBeTrue()
            Color::class.isValidName("Blue", ignoreCase = false).shouldBeFalse()
            Color::class.isValidName("blue", ignoreCase = false).shouldBeFalse()
        }
    }

}
