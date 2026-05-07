package io.bluetape4k.assertions

import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Test
import org.opentest4j.AssertionFailedError

class MapsTest {

    private val sampleMap = mapOf("a" to 1, "b" to 2, "c" to 3)

    // ── shouldContainKey ─────────────────────────────────────────────────────

    @Test
    fun `shouldContainKey passes when key exists`() {
        sampleMap shouldContainKey "a"
    }

    @Test
    fun `shouldContainKey fails when key is absent`() {
        assertFailsWith<AssertionFailedError> {
            sampleMap shouldContainKey "z"
        }
    }

    @Test
    fun `shouldContainKey fails when map is null`() {
        assertFailsWith<AssertionFailedError> {
            val map: Map<String, Int>? = null
            map shouldContainKey "a"
        }
    }

    @Test
    fun `shouldContainKey returns receiver for chaining`() {
        val result = sampleMap shouldContainKey "b"
        result shouldBeEqualTo sampleMap
    }

    // ── shouldNotContainKey ──────────────────────────────────────────────────

    @Test
    fun `shouldNotContainKey passes when key is absent`() {
        sampleMap shouldNotContainKey "z"
    }

    @Test
    fun `shouldNotContainKey fails when key exists`() {
        assertFailsWith<AssertionFailedError> {
            sampleMap shouldNotContainKey "a"
        }
    }

    @Test
    fun `shouldNotContainKey passes when map is null`() {
        val map: Map<String, Int>? = null
        map shouldNotContainKey "a"
    }

    // ── shouldContainValue ───────────────────────────────────────────────────

    @Test
    fun `shouldContainValue passes when value exists`() {
        sampleMap shouldContainValue 1
    }

    @Test
    fun `shouldContainValue fails when value is absent`() {
        assertFailsWith<AssertionFailedError> {
            sampleMap shouldContainValue 99
        }
    }

    @Test
    fun `shouldContainValue fails when map is null`() {
        assertFailsWith<AssertionFailedError> {
            val map: Map<String, Int>? = null
            map shouldContainValue 1
        }
    }

    @Test
    fun `shouldContainValue returns receiver for chaining`() {
        val result = sampleMap shouldContainValue 2
        result shouldBeEqualTo sampleMap
    }

    // ── shouldNotContainValue ────────────────────────────────────────────────

    @Test
    fun `shouldNotContainValue passes when value is absent`() {
        sampleMap shouldNotContainValue 99
    }

    @Test
    fun `shouldNotContainValue fails when value exists`() {
        assertFailsWith<AssertionFailedError> {
            sampleMap shouldNotContainValue 1
        }
    }

    @Test
    fun `shouldNotContainValue passes when map is null`() {
        val map: Map<String, Int>? = null
        map shouldNotContainValue 1
    }

    // ── shouldContain (Pair) ─────────────────────────────────────────────────

    @Test
    fun `shouldContain passes when key-value pair exists`() {
        sampleMap shouldContain ("a" to 1)
    }

    @Test
    fun `shouldContain fails when key exists but value differs`() {
        assertFailsWith<AssertionFailedError> {
            sampleMap shouldContain ("a" to 99)
        }
    }

    @Test
    fun `shouldContain fails when key is absent`() {
        assertFailsWith<AssertionFailedError> {
            sampleMap shouldContain ("z" to 1)
        }
    }

    @Test
    fun `shouldContain fails when map is null`() {
        assertFailsWith<AssertionFailedError> {
            val map: Map<String, Int>? = null
            map shouldContain ("a" to 1)
        }
    }

    @Test
    fun `shouldContain returns receiver for chaining`() {
        val result = sampleMap shouldContain ("b" to 2)
        result shouldBeEqualTo sampleMap
    }

    // ── shouldNotContain (Pair) ──────────────────────────────────────────────

    @Test
    fun `shouldNotContain passes when pair is absent`() {
        sampleMap shouldNotContain ("z" to 99)
    }

    @Test
    fun `shouldNotContain passes when key exists but value differs`() {
        sampleMap shouldNotContain ("a" to 99)
    }

    @Test
    fun `shouldNotContain fails when exact key-value pair exists`() {
        assertFailsWith<AssertionFailedError> {
            sampleMap shouldNotContain ("a" to 1)
        }
    }

    @Test
    fun `shouldNotContain passes when map is null`() {
        val map: Map<String, Int>? = null
        map shouldNotContain ("a" to 1)
    }

    // ── shouldHaveSize ───────────────────────────────────────────────────────

    @Test
    fun `shouldHaveSize passes when size matches`() {
        sampleMap.shouldHaveSize(3)
    }

    @Test
    fun `shouldHaveSize fails when size differs`() {
        assertFailsWith<AssertionFailedError> {
            sampleMap.shouldHaveSize(5)
        }
    }

    @Test
    fun `shouldHaveSize fails when map is null`() {
        assertFailsWith<AssertionFailedError> {
            val map: Map<String, Int>? = null
            map.shouldHaveSize(0)
        }
    }

    @Test
    fun `shouldHaveSize passes for empty map with size 0`() {
        emptyMap<String, Int>().shouldHaveSize(0)
    }

    @Test
    fun `shouldHaveSize returns receiver for chaining`() {
        val result = sampleMap.shouldHaveSize(3)
        result shouldBeEqualTo sampleMap
    }

    // ── shouldBeEmpty ────────────────────────────────────────────────────────

    @Test
    fun `shouldBeEmpty passes for empty map`() {
        emptyMap<String, Int>().shouldBeEmpty()
    }

    @Test
    fun `shouldBeEmpty fails for non-empty map`() {
        assertFailsWith<AssertionFailedError> {
            sampleMap.shouldBeEmpty()
        }
    }

    @Test
    fun `shouldBeEmpty passes for null map`() {
        val map: Map<String, Int>? = null
        map.shouldBeEmpty()
    }

    // ── shouldNotBeEmpty ─────────────────────────────────────────────────────

    @Test
    fun `shouldNotBeEmpty passes for non-empty map`() {
        sampleMap.shouldNotBeEmpty()
    }

    @Test
    fun `shouldNotBeEmpty fails for empty map`() {
        assertFailsWith<AssertionFailedError> {
            emptyMap<String, Int>().shouldNotBeEmpty()
        }
    }

    @Test
    fun `shouldNotBeEmpty fails for null map`() {
        assertFailsWith<AssertionFailedError> {
            val map: Map<String, Int>? = null
            map.shouldNotBeEmpty()
        }
    }

    @Test
    fun `shouldNotBeEmpty returns receiver for chaining`() {
        val result = sampleMap.shouldNotBeEmpty()
        result shouldBeEqualTo sampleMap
    }

    // ── chaining ─────────────────────────────────────────────────────────────

    @Test
    fun `assertion functions support method chaining`() {
        sampleMap
            .shouldNotBeEmpty()
            .shouldHaveSize(3)
            .shouldContainKey("a")
            .shouldContainValue(1)
            .shouldContain("c" to 3)
    }
}
