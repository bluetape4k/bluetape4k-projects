# bluetape4k-assertions

[한국어](README.ko.md)

Kluent-compatible assertion DSL for JUnit 5. Zero `bluetape4k-*` dependencies in api scope — drop-in import replacement.

## Architecture

```mermaid
graph TD
    A["bluetape4k-assertions"] --> B["junit-jupiter-api"]
    A --> C["opentest4j"]
    A --> D["kotlinx-coroutines-core"]
    A -.->|compileOnly| E["turbine"]
    F["bluetape4k-junit5"] -->|api| A
    G["bluetape4k-coroutines"] -.->|@Deprecated bridge| A
    
    classDef api fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef optional fill:#F3E5F5,stroke:#CE93D8,color:#6A1B9A
    classDef consumer fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
    
    class A,B,C,D api
    class E optional
    class F,G consumer
```

## Features

- **Kluent-compatible** infix DSL — same function names, import-only migration
- **Basic**: `shouldBe` (ref ===), `shouldBeEqualTo` (value ==), `shouldNotBeNull` with smart cast contract
- **Numerical**: comparisons (`shouldBeLessThan`, `shouldBeGreaterThanOrEqualTo`), sign checks, range containment
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
import io.bluetape4k.junit5.BlueConf
import org.junit.jupiter.api.Test

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
        
        // CharSequences
        "hello".shouldStartWith("he")
        "hello".shouldEndWith("lo")
        
        // Numerical
        5 shouldBeLessThan 10
        5 shouldBeGreaterThanOrEqualTo 5
        5.0 shouldBeNear 5.1 tolerance 0.2
        
        // Exceptions
        invoking { error("boom") } shouldThrow IllegalStateException::class
        coInvoking { delay(100) } shouldNotThrow
        
        // Reflection
        listOf(1, 2, 3).shouldBeInstanceOf<List<*>>()
        
        // DateTimes
        val now = LocalDateTime.now()
        now.shouldBeAfter(now.minusSeconds(1))
        now.shouldBeOnOrBefore(now.plusSeconds(1))
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
    
    @Test
    fun `flow assertions`() {
        flowOf(1, 2, 3).assertResult(1, 2, 3)
        flowOf(1, 2).assertNotEmpty()
        flow<Int> { }.assertEmpty()
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
| `shouldBeLessThanOrEqualTo(bound)` | <= |
| `shouldBeGreaterThan(bound)` | > |
| `shouldBeGreaterThanOrEqualTo(bound)` | >= |
| `shouldBePositive()` | > 0 |
| `shouldBeNegative()` | < 0 |
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
| `shouldHaveSize(size)` | Size check |
| `shouldContain(element)` | Contains single element |
| `IntArray shouldBeEqualTo expected` | Primitive array content equality (`contentEquals`) |
| `ByteArray shouldBeEqualTo expected` | Primitive array content equality (`contentEquals`) |
| `Array<T> shouldBeEqualTo expected` | Object array deep content equality (`contentDeepEquals`) |

### Exceptions

| Function | Description |
|----------|-------------|
| `invoking { }.shouldThrow<E>()` | Sync block throws E |
| `invoking { }.shouldNotThrow()` | Sync block throws nothing |
| `coInvoking { }.shouldThrow<E>()` | Async block throws E |
| `.withMessage(msg)` | Exact message match (chain) |
| `.withMessageContaining(substring)` | Message contains (chain) |

### Reflection

| Function | Description |
|----------|-------------|
| `shouldBeInstanceOf<T>()` | Instance check (smart cast) |
| `shouldNotBeInstanceOf<T>()` | Negative instance check |

## Migration from Kluent

Replace `import org.amshove.kluent.*` with `import io.bluetape4k.assertions.*`.

| Kluent | bluetape4k-assertions | Notes |
|--------|----------------------|-------|
| `shouldBe` (value ==) | `shouldBeEqualTo` | Different semantics in bluetape4k |
| `shouldBe` (ref ===) | `shouldBe` | Same behavior |
| `shouldNotBeNull()` | `shouldNotBeNull()` | + smart cast contract |
| `shouldThrow<E>()` | `invoking { }.shouldThrow(E::class)` | Explicit block wrapper |
| `shouldHaveMessage()` | `.withMessage()` | Chain on InvokingBlock |
| `coInvoking { }` | `coInvoking { }` | Full coroutine support |

### Critical Semantic Change

**Kluent's `shouldBe` uses `==` (structural equality) for all types.**
**bluetape4k-assertions's `shouldBe` uses `===` (referential equality).**

This is intentional to provide clear semantics for both value and reference equality. Always use `shouldBeEqualTo` when migrating Kluent's `shouldBe` for value equality.

```kotlin
// Kluent
"a" shouldBe "a"  // Passes (== comparison)

// bluetape4k-assertions
"a" shouldBe "a"  // Fails! (=== comparison)
"a" shouldBeEqualTo "a"  // Passes (== comparison)
```

---

**Maintainer**: Sunghyouk Bae (@sunghyouk.bae@gmail.com)
**License**: Apache 2.0
