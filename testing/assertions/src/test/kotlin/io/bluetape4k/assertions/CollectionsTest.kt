package io.bluetape4k.assertions

import io.bluetape4k.assertions.assertFailsWith
import org.junit.jupiter.api.Test
import org.opentest4j.AssertionFailedError

class CollectionsTest {

    // ── shouldContain ─────────────────────────────────────────────────────

    @Test
    fun `shouldContain passes when element is in the collection`() {
        listOf(1, 2, 3) shouldContain 2
    }

    @Test
    fun `shouldContain fails when element is not in the collection`() {
        assertFailsWith<AssertionFailedError> {
            listOf(1, 2, 3) shouldContain 99
        }
    }

    @Test
    fun `shouldContain fails when collection is null`() {
        assertFailsWith<AssertionFailedError> {
            val c: Iterable<Int>? = null
            c shouldContain 1
        }
    }

    @Test
    fun `shouldContain supports chaining`() {
        listOf("a", "b", "c")
            .shouldContain("a")
            .shouldContain("c")
    }

    // ── shouldContainIgnoringCase ────────────────────────────────────────

    @Test
    fun `shouldContainIgnoringCase passes when matching element has different case`() {
        listOf("GET", "Post", "delete") shouldContainIgnoringCase "post"
    }

    @Test
    fun `shouldContainIgnoringCase fails when no element matches ignoring case`() {
        assertFailsWith<AssertionFailedError> {
            listOf("GET", "POST") shouldContainIgnoringCase "patch"
        }
    }

    @Test
    fun `shouldContainIgnoringCase fails when collection is null`() {
        assertFailsWith<AssertionFailedError> {
            val c: Iterable<String>? = null
            c shouldContainIgnoringCase "get"
        }
    }

    // ── shouldNotContain ──────────────────────────────────────────────────

    @Test
    fun `shouldNotContain passes when element is not in the collection`() {
        listOf(1, 2, 3) shouldNotContain 99
    }

    @Test
    fun `shouldNotContain fails when element is in the collection`() {
        assertFailsWith<AssertionFailedError> {
            listOf(1, 2, 3) shouldNotContain 2
        }
    }

    @Test
    fun `shouldNotContain passes when collection is null`() {
        val c: Iterable<Int>? = null
        c shouldNotContain 1
    }

    // ── shouldContainAll ──────────────────────────────────────────────────

    @Test
    fun `shouldContainAll passes when all elements are present`() {
        listOf(1, 2, 3, 4) shouldContainAll listOf(1, 3)
    }

    @Test
    fun `shouldContainAll fails when some elements are missing`() {
        assertFailsWith<AssertionFailedError> {
            listOf(1, 2, 3) shouldContainAll listOf(1, 99)
        }
    }

    @Test
    fun `shouldContainAll vararg passes when all elements are present`() {
        listOf(1, 2, 3).shouldContainAll(1, 2, 3)
    }

    @Test
    fun `shouldContainAll vararg fails when element is missing`() {
        assertFailsWith<AssertionFailedError> {
            listOf(1, 2).shouldContainAll(1, 2, 3)
        }
    }

    @Test
    fun `shouldContainAll fails when receiver is null`() {
        assertFailsWith<AssertionFailedError> {
            val c: Iterable<Int>? = null
            c shouldContainAll listOf(1)
        }
    }

    // ── shouldContainAny ──────────────────────────────────────────────────

    @Test
    fun `shouldContainAny passes when at least one element is present`() {
        listOf(1, 2, 3) shouldContainAny listOf(3, 99)
    }

    @Test
    fun `shouldContainAny fails when none of the elements are present`() {
        assertFailsWith<AssertionFailedError> {
            listOf(1, 2, 3) shouldContainAny listOf(10, 20)
        }
    }

    @Test
    fun `shouldContainAny vararg passes when at least one is present`() {
        listOf(1, 2, 3).shouldContainAny(99, 2)
    }

    @Test
    fun `shouldContainAny fails when receiver is null`() {
        assertFailsWith<AssertionFailedError> {
            val c: Iterable<Int>? = null
            c shouldContainAny listOf(1)
        }
    }

    // ── shouldContainNone ─────────────────────────────────────────────────

    @Test
    fun `shouldContainNone passes when none of the elements are present`() {
        listOf(1, 2, 3) shouldContainNone listOf(10, 20)
    }

    @Test
    fun `shouldContainNone fails when any element is present`() {
        assertFailsWith<AssertionFailedError> {
            listOf(1, 2, 3) shouldContainNone listOf(2, 99)
        }
    }

    @Test
    fun `shouldContainNone passes when receiver is null`() {
        val c: Iterable<Int>? = null
        c shouldContainNone listOf(1, 2)
    }

    // ── shouldHaveSize ────────────────────────────────────────────────────

    @Test
    fun `shouldHaveSize passes when size matches`() {
        listOf(1, 2, 3).shouldHaveSize(3)
    }

    @Test
    fun `shouldHaveSize fails when size does not match`() {
        assertFailsWith<AssertionFailedError> {
            listOf(1, 2, 3).shouldHaveSize(5)
        }
    }

    @Test
    fun `shouldHaveSize passes for empty collection with size 0`() {
        emptyList<Int>().shouldHaveSize(0)
    }

    @Test
    fun `shouldHaveSize treats null as size 0`() {
        val c: Iterable<Int>? = null
        c.shouldHaveSize(0)
    }

    @Test
    fun `shouldHaveSize fails when null and expected size is non-zero`() {
        assertFailsWith<AssertionFailedError> {
            val c: Iterable<Int>? = null
            c.shouldHaveSize(1)
        }
    }

    // ── shouldBeEmpty ─────────────────────────────────────────────────────

    @Test
    fun `shouldBeEmpty passes for empty list`() {
        emptyList<String>().shouldBeEmpty()
    }

    @Test
    fun `shouldBeEmpty fails for non-empty list`() {
        assertFailsWith<AssertionFailedError> {
            listOf(1).shouldBeEmpty()
        }
    }

    @Test
    fun `shouldBeEmpty passes for null collection`() {
        val c: Iterable<Int>? = null
        c.shouldBeEmpty()
    }

    // ── shouldNotBeEmpty ──────────────────────────────────────────────────

    @Test
    fun `shouldNotBeEmpty passes for non-empty list`() {
        listOf(1, 2).shouldNotBeEmpty()
    }

    @Test
    fun `shouldNotBeEmpty fails for empty list`() {
        assertFailsWith<AssertionFailedError> {
            emptyList<Int>().shouldNotBeEmpty()
        }
    }

    @Test
    fun `shouldNotBeEmpty fails for null collection`() {
        assertFailsWith<AssertionFailedError> {
            val c: Iterable<Int>? = null
            c.shouldNotBeEmpty()
        }
    }

    // ── shouldBeIn ────────────────────────────────────────────────────────

    @Test
    fun `shouldBeIn passes when value is in the collection`() {
        2 shouldBeIn listOf(1, 2, 3)
    }

    @Test
    fun `shouldBeIn fails when value is not in the collection`() {
        assertFailsWith<AssertionFailedError> {
            99 shouldBeIn listOf(1, 2, 3)
        }
    }

    @Test
    fun `shouldBeIn supports chaining`() {
        val result = "b" shouldBeIn listOf("a", "b", "c")
        result shouldBeEqualTo "b"
    }

    // ── shouldNotBeIn ─────────────────────────────────────────────────────

    @Test
    fun `shouldNotBeIn passes when value is not in the collection`() {
        99 shouldNotBeIn listOf(1, 2, 3)
    }

    @Test
    fun `shouldNotBeIn fails when value is in the collection`() {
        assertFailsWith<AssertionFailedError> {
            2 shouldNotBeIn listOf(1, 2, 3)
        }
    }

    // ── shouldMatchAllWith ────────────────────────────────────────────────

    @Test
    fun `shouldMatchAllWith passes when all elements satisfy predicate`() {
        listOf(2, 4, 6) shouldMatchAllWith { it % 2 == 0 }
    }

    @Test
    fun `shouldMatchAllWith fails when some elements do not satisfy predicate`() {
        assertFailsWith<AssertionFailedError> {
            listOf(2, 3, 6) shouldMatchAllWith { it % 2 == 0 }
        }
    }

    @Test
    fun `shouldMatchAllWith passes for empty collection (vacuous truth)`() {
        emptyList<Int>() shouldMatchAllWith { it > 0 }
    }

    @Test
    fun `shouldMatchAllWith passes for null receiver (vacuous truth)`() {
        val c: Iterable<Int>? = null
        c shouldMatchAllWith { it > 0 }
    }

    // ── shouldMatchAtLeastOneOf ───────────────────────────────────────────

    @Test
    fun `shouldMatchAtLeastOneOf passes when at least one element satisfies predicate`() {
        listOf(1, 2, 3) shouldMatchAtLeastOneOf { it > 2 }
    }

    @Test
    fun `shouldMatchAtLeastOneOf fails when no elements satisfy predicate`() {
        assertFailsWith<AssertionFailedError> {
            listOf(1, 2, 3) shouldMatchAtLeastOneOf { it > 10 }
        }
    }

    @Test
    fun `shouldMatchAtLeastOneOf fails for empty collection`() {
        assertFailsWith<AssertionFailedError> {
            emptyList<Int>() shouldMatchAtLeastOneOf { it > 0 }
        }
    }

    // ── shouldContainAll vs shouldMatchAllWith distinction ────────────────

    @Test
    fun `shouldContainAll checks element membership, shouldMatchAllWith checks a predicate`() {
        val evens = listOf(2, 4, 6)

        // shouldContainAll: checks that specific values exist in the collection
        evens shouldContainAll listOf(2, 4)

        // shouldMatchAllWith: checks that every element satisfies a condition
        evens shouldMatchAllWith { it % 2 == 0 }

        // shouldContainAll does NOT verify the predicate for all elements
        val mixed = listOf(2, 3, 4)
        mixed shouldContainAll listOf(2, 4)   // passes — 2 and 4 are present

        // shouldMatchAllWith would fail for the mixed list because 3 is odd
        assertFailsWith<AssertionFailedError> {
            mixed shouldMatchAllWith { it % 2 == 0 }
        }
    }

    // ── shouldContentEqual (Iterable) ─────────────────────────────────────

    @Test
    fun `shouldContentEqual passes when both iterables have same elements in order`() {
        listOf(1, 2, 3) shouldContentEqual listOf(1, 2, 3)
    }

    @Test
    fun `shouldContentEqual fails when order is different`() {
        assertFailsWith<AssertionFailedError> {
            listOf(1, 2, 3) shouldContentEqual listOf(3, 2, 1)
        }
    }

    @Test
    fun `shouldContentEqual fails when sizes differ`() {
        assertFailsWith<AssertionFailedError> {
            listOf(1, 2) shouldContentEqual listOf(1, 2, 3)
        }
    }

    @Test
    fun `shouldContentEqual passes when both are null`() {
        val a: Iterable<Int>? = null
        val b: Iterable<Int>? = null
        a shouldContentEqual b
    }

    @Test
    fun `shouldContentEqual fails when receiver is null and expected is not`() {
        assertFailsWith<AssertionFailedError> {
            val a: Iterable<Int>? = null
            a shouldContentEqual listOf(1, 2, 3)
        }
    }

    @Test
    fun `shouldContentEqual fails when expected is null and receiver is not`() {
        assertFailsWith<AssertionFailedError> {
            val b: Iterable<Int>? = null
            listOf(1, 2, 3) shouldContentEqual b
        }
    }

    @Test
    fun `shouldContentEqual passes for empty collections`() {
        emptyList<String>() shouldContentEqual emptyList()
    }

    @Test
    fun `shouldContentEqual fails when one is empty and other is not`() {
        assertFailsWith<AssertionFailedError> {
            emptyList<Int>() shouldContentEqual listOf(1)
        }
    }

    // ── shouldContentEqual (Sequence) ─────────────────────────────────────

    @Test
    fun `shouldContentEqual Sequence passes when both sequences have same elements in order`() {
        sequenceOf(1, 2, 3) shouldContentEqual sequenceOf(1, 2, 3)
    }

    @Test
    fun `shouldContentEqual Sequence fails when order is different`() {
        assertFailsWith<AssertionFailedError> {
            sequenceOf(1, 2, 3) shouldContentEqual sequenceOf(3, 2, 1)
        }
    }

    @Test
    fun `shouldContentEqual Sequence passes when both are null`() {
        val a: Sequence<Int>? = null
        val b: Sequence<Int>? = null
        a shouldContentEqual b
    }

    @Test
    fun `shouldContentEqual Sequence fails when receiver is null and expected is not`() {
        assertFailsWith<AssertionFailedError> {
            val a: Sequence<Int>? = null
            a shouldContentEqual sequenceOf(1, 2, 3)
        }
    }

    @Test
    fun `shouldContentEqual Sequence fails when expected is null and receiver is not`() {
        assertFailsWith<AssertionFailedError> {
            val b: Sequence<Int>? = null
            sequenceOf(1, 2, 3) shouldContentEqual b
        }
    }

    @Test
    fun `shouldContentEqual Sequence passes for empty sequences`() {
        emptySequence<String>() shouldContentEqual emptySequence()
    }
}
