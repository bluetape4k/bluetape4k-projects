package io.bluetape4k.support

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class BooleanSupportTest {

    // region ifTrue / ifFalse

    @Test
    fun `boolean ifTrue`() {
        val result = true.ifTrue { "yes" }
        result shouldBeEqualTo "yes"

        val result2 = false.ifTrue { "yes" }
        result2.shouldBeNull()
    }

    @Test
    fun `boolean supplier ifTrue`() {
        val result = { true }.ifTrue { "yes" }
        result shouldBeEqualTo "yes"

        val result2 = { false }.ifTrue { "yes" }
        result2.shouldBeNull()
    }

    @Test
    fun `boolean ifFalse`() {
        val result = false.ifFalse { "no" }
        result shouldBeEqualTo "no"

        val result2 = true.ifFalse { "no" }
        result2.shouldBeNull()
    }

    @Test
    fun `boolean supplier ifFalse`() {
        val result = { false }.ifFalse { "no" }
        result shouldBeEqualTo "no"

        val result2 = { true }.ifFalse { "no" }
        result2.shouldBeNull()
    }

    // endregion

    // region compareBoolean

    @Test
    fun `compare booleans`() {
        compareBoolean(left = true, right = true) shouldBeEqualTo 0
        compareBoolean(left = true, right = false) shouldBeEqualTo 1
        compareBoolean(left = false, right = true) shouldBeEqualTo -1
        compareBoolean(left = false, right = false) shouldBeEqualTo 0
    }

    // endregion

    // region then

    @Test
    fun `boolean then with lazy block`() {
        val result = true.then { "value" }
        result shouldBeEqualTo "value"

        val result2 = false.then { "value" }
        result2.shouldBeNull()
    }

    @Test
    fun `boolean then with eager value`() {
        val result = true.then("value")
        result shouldBeEqualTo "value"

        val result2 = false.then("value")
        result2.shouldBeNull()
    }

    @Test
    fun `boolean supplier then with lazy block`() {
        val result = { true }.then { "value" }
        result shouldBeEqualTo "value"

        val result2 = { false }.then { "value" }
        result2.shouldBeNull()
    }

    @Test
    fun `boolean supplier then with eager value`() {
        val result = { true }.then("value")
        result shouldBeEqualTo "value"

        val result2 = { false }.then("value")
        result2.shouldBeNull()
    }

    @Test
    fun `then with elvis operator`() {
        val result = true.then { "yes" } ?: "no"
        result shouldBeEqualTo "yes"

        val result2 = false.then { "yes" } ?: "no"
        result2 shouldBeEqualTo "no"
    }

    // endregion

    // region falseIfNull / trueIfNull

    @Test
    fun `falseIfNull returns false when null`() {
        val x: Boolean? = null
        x.falseIfNull().shouldBeFalse()

        true.falseIfNull().shouldBeTrue()
        false.falseIfNull().shouldBeFalse()
    }

    @Test
    fun `trueIfNull returns true when null`() {
        val x: Boolean? = null
        x.trueIfNull().shouldBeTrue()

        true.trueIfNull().shouldBeTrue()
        false.trueIfNull().shouldBeFalse()
    }

    // endregion
}
