# bluetape4k-assertions

[한국어](README.ko.md)

bluetape4k-assertions-compatible assertion DSL for JUnit 5. Zero `bluetape4k-*` dependencies in api scope — drop-in import replacement.

## Architecture

![Architecture 1](../../docs/images/readme-diagrams/testing-assertions-diagram-01.png)

## Features

- **bluetape4k-assertions-compatible** infix DSL — same function names, import-only migration
- **Basic**: `shouldBe` (ref ===), `shouldBeEqualTo` (value ==), `shouldNotBeNull` with smart cast contract
- **Numerical**: comparisons (`shouldBeLessThan`, `shouldBeGreaterOrEqualTo`), sign checks, signed and unsigned range containment
- **Collections / Arrays / Maps**: content equality, containment (`shouldContainAll`, `shouldNotContainAny`)
- **CharSequences**: `shouldStartWith`, `shouldEndWith`, `shouldContain`, case-insensitive checks
- **Exceptions**: `invoking { }` / `shouldThrow`, message matching, cause inspection
- **Async Exceptions**: `coInvoking { }` / `shouldThrow` — CancellationException-safe coroutine support
- **Reflection**: `shouldBeInstanceOf<T>` with smart cast contract
- **DateTimes**: `shouldBeAfter`, `shouldBeBefore`, `shouldBeOnOrAfter` for 7 java.time types
- **Softly**: `assertSoftly { add { } }` — virtual-thread safe, `MultipleFailuresError` aggregation
- **Flow assertions**: `assertEmpty`, `assertResult`, `assertResultSet`, `assertFailure`, `assertError`
- **Turbine integration**: `awaitItemAndAssert`, `awaitItemMatching`, `awaitErrorOfType` (optional, via compileOnly)

## Quick Start

### Gradle

```kotlin
// build.gradle.kts
testImplementation(project(":bluetape4k-assertions"))
// or via bluetape4k-junit5 (included transitively)
testImplementation(project(":bluetape4k-junit5"))
```

### Example Usage

```kotlin
import io.bluetape4k.assertions.*
import io.bluetape4k.assertions.coroutines.assertEmpty
import io.bluetape4k.assertions.coroutines.assertResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class MyTest {
    @Test
    fun `basic assertions`() {
        // Basic
        "hello" shouldBeEqualTo "hello"
        "hello" shouldNotBeEqualTo "world"
        
        // Smart cast after shouldNotBeNull
        val name: String? = "John"
        name.shouldNotBeNull().length shouldBeGreaterThan 0
        
        // Collections
        listOf(1, 2, 3) shouldContainAll listOf(1, 2)
        listOf(1, 2, 3) shouldNotContainAny listOf(4, 5)
        listOf("GET", "POST") shouldContainIgnoringCase "post"
        
        // CharSequences
        "hello".shouldStartWith("he")
        "hello".shouldEndWith("lo")
        
        // Numerical
        5 shouldBeLessThan 10
        5 shouldBeGreaterOrEqualTo 5
        5 shouldBeInRange 1..10
        UInt.MAX_VALUE shouldBeInRange Int.MAX_VALUE.toUInt()..UInt.MAX_VALUE
        5.0.shouldBeNear(5.1, tolerance = 0.2)
        
        // Exceptions
        invoking { error("boom") }.shouldThrow(IllegalStateException::class)
        
        // Reflection
        listOf(1, 2, 3).shouldBeInstanceOf<List<*>>()
        
        // DateTimes
        val now = LocalDateTime.now()
        now.shouldBeAfter(now.minusSeconds(1))
        now.shouldBeOnOrBefore(now.plusSeconds(1))
    }

    @Test
    fun `coroutine assertions`() = runTest {
        coInvoking { delay(10) }.shouldNotThrow()
        flowOf(1, 2, 3).assertResult(1, 2, 3)
        emptyFlow<Int>().assertEmpty()
    }
    
    @Test
    fun `softly assertions`() {
        assertSoftly {
            add { 1 shouldBeEqualTo 1 }
            add { "a" shouldStartWith "a" }
            add { listOf(1) shouldContainAll listOf(1) }
        }
        // All assertions collected, MultipleFailuresError if any fail
    }
    
}
```

## API Reference

### Basic Assertions

| Function | Description |
|----------|-------------|
| `shouldBe(expected)` | Referential equality (===) |
| `shouldNotBe(expected)` | Referential inequality (!==) |
| `shouldBeEqualTo(expected)` | Structural equality (==) |
| `shouldNotBeEqualTo(expected)` | Structural inequality (!=) |
| `shouldBeNull()` | Is null |
| `shouldNotBeNull()` | Is not null (smart cast) |

### Numerical

| Function | Description |
|----------|-------------|
| `shouldBeLessThan(bound)` | < |
| `shouldBeLessOrEqualTo(bound)` | <= |
| `shouldBeGreaterThan(bound)` | > |
| `shouldBeGreaterOrEqualTo(bound)` | >= |
| `shouldBePositive()` | > 0 |
| `shouldBeNegative()` | < 0 |
| `shouldBeInRange(range)` | Closed range containment |
| `shouldNotBeInRange(range)` | Closed range exclusion |
| `UInt/ULong shouldBeInRange range` | Unsigned range containment |
| `UInt/ULong shouldNotBeInRange range` | Unsigned range exclusion |
| `shouldBeNear(expected, tolerance)` | Approx equality for floats |
| `BigDecimal shouldBeEqualTo expected` | Scale-insensitive equality (`compareTo`) |
| `BigDecimal shouldNotBeEqualTo expected` | Scale-insensitive inequality (`compareTo`) |

### Collections & Arrays

| Function | Description |
|----------|-------------|
| `shouldBeEmpty()` | Empty collection |
| `shouldNotBeEmpty()` | Non-empty collection |
| `shouldContainAll(elements)` | Contains all (⊇) |
| `shouldNotContainAny(elements)` | Contains none (∩ = ∅) |
| `shouldContainIgnoringCase(element)` | String collection contains element ignoring case |
| `shouldHaveSize(size)` | Size check |
| `shouldContain(element)` | Contains single element |
| `IntArray shouldBeEqualTo expected` | Primitive array content equality (`contentEquals`) |
| `ByteArray shouldBeEqualTo expected` | Primitive array content equality (`contentEquals`) |
| `Array<T> shouldBeEqualTo expected` | Object array deep content equality (`contentDeepEquals`) |

### Exceptions

| Function | Description |
|----------|-------------|
| `invoking { }.shouldThrow(E::class)` | Sync block throws E |
| `invoking { }.shouldNotThrow()` | Sync block throws nothing |
| `coInvoking { }.shouldThrow(E::class)` | Async block throws E |
| `.withMessage(msg)` | Exact message match (chain) |
| `.withMessageContaining(substring)` | Message contains (chain) |

### Reflection

| Function | Description |
|----------|-------------|
| `shouldBeInstanceOf<T>()` | Instance check (smart cast) |
| `shouldNotBeInstanceOf<T>()` | Negative instance check |

## Migration from bluetape4k-assertions

Replace `import io.bluetape4k.assertions.*` with `import io.bluetape4k.assertions.*`.

| bluetape4k-assertions | bluetape4k-assertions | Notes |
|--------|----------------------|-------|
| `shouldBe` (value ==) | `shouldBeEqualTo` | Different semantics in bluetape4k |
| `shouldBe` (ref ===) | `shouldBe` | Same behavior |
| `shouldNotBeNull()` | `shouldNotBeNull()` | + smart cast contract |
| `shouldThrow(E::class)` | `invoking { }.shouldThrow(E::class)` | Explicit block wrapper |
| `shouldHaveMessage()` | `.withMessage()` | Chain on InvokingBlock |
| `coInvoking { }` | `coInvoking { }` | Full coroutine support |

### Critical Semantic Change

**bluetape4k-assertions's `shouldBe` uses `==` (structural equality) for all types.**
**bluetape4k-assertions's `shouldBe` uses `===` (referential equality).**

This is intentional to provide clear semantics for both value and reference equality. Always use `shouldBeEqualTo` when migrating bluetape4k-assertions's `shouldBe` for value equality.

```kotlin
// bluetape4k-assertions
"a" shouldBe "a"  // Passes (== comparison)

// bluetape4k-assertions
"a" shouldBe "a"  // Fails! (=== comparison)
"a" shouldBeEqualTo "a"  // Passes (== comparison)
```

---

**Maintainer**: Sunghyouk Bae (@sunghyouk.bae@gmail.com)
**License**: MIT
