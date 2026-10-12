# bluetape4k-assertions

[한국어](README.ko.md)

JUnit 5-native assertion DSL with the bluetape4k assertion API surface. It keeps api-scope dependencies minimal:
JUnit Jupiter API and Kotlin coroutines are exposed only where the public DSL requires them, while Turbine support remains `compileOnly`.

## Architecture

![assertions Architecture diagram](../../docs/images/readme-diagrams/testing-assertions-diagram-01.png)

## Features

- **bluetape4k assertion API surface** — familiar infix names with explicit equality semantics and paired positive/negative conditions
- **Basic**: `shouldBe` (ref ===), `shouldBeEqualTo` (value ==), `should`/`shouldNot`, and `shouldNotBeNull` with smart cast contract
- **Numerical**: comparisons, sign checks, finite checks, signed and unsigned range containment
- **Collections / Arrays / Maps**: paired size, content, containment, and predicate checks
- **CharSequences**: paired prefix, suffix, substring, regex, case-insensitive, empty, and blank checks
- **Exceptions**: `invoking { }` / `shouldThrow`, message matching, cause inspection
- **Async Exceptions**: `coInvoking { }` / `shouldThrow` — CancellationException-safe coroutine support
- **Reflection**: `shouldBeInstanceOf<T>` with smart cast contract
- **DateTimes**: paired after/before and inclusive comparison checks for 7 java.time types
- **Durations**: paired positive and zero-or-positive checks for `java.time.Duration` and `kotlin.time.Duration`
- **Softly**: `assertSoftly { add { } }` — virtual-thread safe, `MultipleFailuresError` aggregation
- **Flow assertions**: `assertEmpty`, `assertResult`, `assertResultSet`, `assertFailure`, `assertError`
- **Turbine integration**: `awaitItemAndAssert`, `awaitItemMatching`, `awaitErrorOfType` (optional, via compileOnly)

## Quick Start

### Gradle

```kotlin
// build.gradle.kts
testImplementation("io.github.bluetape4k:bluetape4k-assertions:${bluetape4kVersion}")
// or via bluetape4k-junit5 (included transitively)
testImplementation("io.github.bluetape4k:bluetape4k-junit5:${bluetape4kVersion}")
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
import java.time.Duration as JavaDuration
import kotlin.time.Duration as KotlinDuration
import kotlin.time.Duration.Companion.seconds

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
        "Hello World" shouldNotContainIgnoringCase "xyz"

        // Numerical
        5 shouldBeLessThan 10
        5 shouldBeGreaterOrEqualTo 5
        5.shouldBePositive()
        (-5).shouldNotBeZero()
        5 shouldBeInRange 1..10
        UInt.MAX_VALUE shouldBeInRange Int.MAX_VALUE.toUInt()..UInt.MAX_VALUE
        5.0.shouldBeNear(5.1, tolerance = 0.2)
        1.0f.shouldBeFinitePositive()
        Float.NaN.shouldNotBePositive() // sign-only negative checks are logical complements
        Float.POSITIVE_INFINITY.shouldNotBeFinite()

        // Durations
        JavaDuration.ofSeconds(1).shouldBePositive()
        val kotlinDuration: KotlinDuration = 1.seconds
        kotlinDuration.shouldBeZeroOrPositive()

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

| Function                       | Description                  |
|--------------------------------|------------------------------|
| `shouldBe(expected)`           | Referential equality (===)   |
| `shouldNotBe(expected)`        | Referential inequality (!==) |
| `shouldBeEqualTo(expected)`    | Structural equality (==)     |
| `shouldNotBeEqualTo(expected)` | Structural inequality (!=)   |
| `shouldBeNull()`               | Is null                      |
| `shouldNotBeNull()`            | Is not null (smart cast)     |
| `should(message, predicate)`   | Predicate assertion           |
| `shouldNot(message, predicate)` | Logical negation of predicate |

### Numerical

| Function                                 | Description                                |
|------------------------------------------|--------------------------------------------|
| `shouldBeLessThan(bound)`                | <                                          |
| `shouldBeLessOrEqualTo(bound)`           | <=                                         |
| `shouldBeGreaterThan(bound)`             | >                                          |
| `shouldBeGreaterOrEqualTo(bound)`        | >=                                         |
| `shouldBePositive()`                     | > 0                                        |
| `shouldNotBePositive()`                  | <= 0 or unordered for floating-point     |
| `shouldBeNegative()`                     | < 0                                        |
| `shouldNotBeNegative()`                  | >= 0 or unordered for floating-point     |
| `shouldBeZero()`                         | == 0                                       |
| `shouldNotBeZero()`                      | != 0                                       |
| `shouldBeFinite()`                       | Finite `Float`/`Double`                  |
| `shouldNotBeFinite()`                    | NaN or infinite `Float`/`Double`         |
| `shouldBeFinitePositive()`              | Finite and > 0                           |
| `shouldNotBeFinitePositive()`           | Not finite-positive                      |
| `shouldBeFiniteZeroOrNegative()`        | Finite and <= 0                          |
| `shouldNotBeFiniteZeroOrNegative()`     | Not finite-zero-or-negative              |
| `shouldBeFiniteZeroOrPositive()`       | Finite and >= 0                          |
| `shouldNotBeFiniteZeroOrPositive()`    | Not finite-zero-or-positive              |
| `shouldBeFiniteNegative()`              | Finite and < 0                           |
| `shouldNotBeFiniteNegative()`           | Not finite-negative                      |
| `shouldBeInRange(range)`                 | Closed range containment                   |
| `shouldNotBeInRange(range)`              | Closed range exclusion                     |
| `UInt/ULong shouldBeInRange range`       | Unsigned range containment                 |
| `UInt/ULong shouldNotBeInRange range`    | Unsigned range exclusion                   |
| `Float/Double shouldBeNear / shouldNotBeNear` | Approximate comparison pair using the same default tolerance |
| `BigDecimal shouldBeNear / shouldNotBeNear` | Approximate comparison pair using `BIGDECIMAL_EPSILON` |
| `BigDecimal shouldBeEqualTo expected`    | Scale-insensitive equality (`compareTo`)   |
| `BigDecimal shouldNotBeEqualTo expected` | Scale-insensitive inequality (`compareTo`) |

Sign-only `Float`/`Double` checks use the ordinary comparison operators. Therefore
`NaN` fails `shouldBePositive`, `shouldBeNegative`, and `shouldBeZero`, but passes
each corresponding `shouldNotBe...` check; positive and negative infinity follow
their sign. The `shouldBeFinite...` family rejects `NaN` and both infinities, and
the two finite-sign pairs are complements over finite values.

### Durations

| Function pair | Description |
|---------------|-------------|
| `shouldBePositive()` / `shouldNotBePositive()` | Duration is `> 0` / `<= 0` |
| `shouldBeZeroOrPositive()` / `shouldBeNegative()` | Duration is `>= 0` / `< 0` |

The duration overloads cover both `java.time.Duration` and `kotlin.time.Duration`.
Kotlin `Duration.INFINITE` and `-Duration.INFINITE` follow their sign; these
checks do not claim finite timeout semantics.

### Collections & Arrays

| Function                             | Description                                              |
|--------------------------------------|----------------------------------------------------------|
| `shouldBeEmpty()`                    | Empty collection                                         |
| `shouldNotBeEmpty()`                 | Non-empty collection                                     |
| `shouldContainAll(elements)`         | Contains all (⊇)                                         |
| `shouldNotContainAny(elements)`      | Contains none (∩ = ∅)                                    |
| `shouldContainAny(elements)`         | Contains at least one candidate                             |
| `shouldContainNone(elements)`        | Contains no candidate; `shouldNotContainAny` remains the negative alias and there is no `shouldNotContainNone` alias |
| `shouldContainAll(elements)` / `shouldNotContainAll(elements)` | All candidates present / at least one absent |
| `shouldContainIgnoringCase(element)` | String collection contains element ignoring case         |
| `shouldMatchAllWith(predicate)` / `shouldNotMatchAllWith(predicate)` | All / not all elements satisfy predicate |
| `shouldMatchAtLeastOneOf(predicate)` / `shouldNotMatchAtLeastOneOf(predicate)` | At least one / none satisfy predicate |
| `shouldHaveSize(size)`               | Size check                                               |
| `shouldNotHaveSize(size)`            | Size differs                                               |
| `shouldContentEqual(expected)` / `shouldNotContentEqual(expected)` | Ordered content equality / inequality |
| `shouldContainSame(expected)` / `shouldNotContainSame(expected)` | Same elements regardless of order |
| `shouldContain(element)`             | Contains single element                                  |
| `IntArray shouldBeEqualTo expected`  | Primitive array content equality (`contentEquals`)       |
| `ByteArray shouldBeEqualTo expected` | Primitive array content equality (`contentEquals`)       |
| `Array<T> shouldBeEqualTo expected`  | Object array deep content equality (`contentDeepEquals`) |

The size, content, and same-content pairs preserve each receiver and argument
overload: `Iterable`, `Sequence`, `Map`, generic arrays, and primitive arrays.
Array equality overloads with a non-null expected array use content semantics.
When the expected array is nullable, Kotlin may select the generic `shouldBeEqualTo`
overload, which keeps reference equality; use `shouldContentEqual` when nullable
content equality is intended. Nullable expected values are valid for the content
families, and their return type follows the receiver contract.

### CharSequences

| Function                                  | Description                                                                |
|-------------------------------------------|----------------------------------------------------------------------------|
| `shouldContainIgnoringCase(substring)`    | `CharSequence` contains a substring, ignoring case                         |
| `shouldNotContainIgnoringCase(substring)` | Substring is absent, ignoring case; a null receiver passes and returns null |
| `shouldContainAll(...)` / `shouldNotContainAll(...)` | All supplied substrings are present / at least one is absent |
| `shouldContainAny(...)` / `shouldContainNone(...)` | At least one supplied substring is present / none are present |
| `shouldMatch(Regex)` / `shouldNotMatch(Regex)` | Full regular-expression match / mismatch |
| `shouldMatch(String)` / `shouldNotMatch(String)` | String pattern match / mismatch |
| `shouldBeEqualToIgnoringCase(value)` / `shouldNotBeEqualToIgnoringCase(value)` | Case-insensitive equality / inequality |

### Exceptions

| Function                               | Description                 |
|----------------------------------------|-----------------------------|
| `invoking { }.shouldThrow(E::class)`   | Sync block throws E         |
| `invoking { }.shouldNotThrow()`        | Sync block throws nothing   |
| `coInvoking { }.shouldThrow(E::class)` | Async block throws E        |
| `.withMessage(msg)`                    | Exact message match (chain) |
| `.withMessageContaining(substring)`    | Message contains (chain)    |

### Reflection

| Function                     | Description                 |
|------------------------------|-----------------------------|
| `shouldBeInstanceOf<T>()`    | Instance check (smart cast) |
| `shouldNotBeInstanceOf<T>()` | Negative instance check     |

### Date-time comparisons

`Instant`, `ZonedDateTime`, `OffsetDateTime`, `LocalDateTime`, `LocalDate`,
`LocalTime`, and `java.util.Date` expose matching
`shouldBeAfter`/`shouldNotBeAfter`, `shouldBeBefore`/`shouldNotBeBefore`,
`shouldBeOnOrAfter`/`shouldNotBeOnOrAfter`, and
`shouldBeOnOrBefore`/`shouldNotBeOnOrBefore` overloads.

### Contract notes

- Positive and negative condition pairs return the same receiver shape as their source family; nullable receivers remain nullable where the existing API requires it.
- Lazy assertion messages and callbacks are evaluated only when needed. Predicate-based collection checks evaluate each element at most once and preserve callback exceptions.
- `Map.shouldContain(key to value)` distinguishes a missing key from an existing key mapped to `null` by checking key presence and value equality together.

## Migration Notes

The package remains `io.bluetape4k.assertions`, but equality semantics are intentionally explicit.

| Legacy expectation            | This module                          | Notes                          |
|-------------------------------|--------------------------------------|--------------------------------|
| `shouldBe` for value equality | `shouldBeEqualTo`                    | Use structural equality (`==`) |
| `shouldBe` (ref ===)          | `shouldBe`                           | Same behavior                  |
| `shouldNotBeNull()`           | `shouldNotBeNull()`                  | + smart cast contract          |
| `shouldThrow(E::class)`       | `invoking { }.shouldThrow(E::class)` | Explicit block wrapper         |
| `shouldHaveMessage()`         | `.withMessage()`                     | Chain on InvokingBlock         |
| `coInvoking { }`              | `coInvoking { }`                     | Full coroutine support         |

### Critical Semantic Change

**This module's `shouldBe` uses `===` (referential equality).**
**Use `shouldBeEqualTo` for `==` (structural equality).**

This split is intentional: tests can distinguish value equality from object identity without relying on a single overloaded name.

```kotlin
// Structural equality
"a" shouldBeEqualTo "a"  // Passes (== comparison)

// This module
"a" shouldBe "a"  // Fails! (=== comparison)
"a" shouldBeEqualTo "a"  // Passes (== comparison)
```

---

**Maintainer**: Sunghyouk Bae (@sunghyouk.bae@gmail.com)
**License**: MIT
