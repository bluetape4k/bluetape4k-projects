# Module Examples - Mutiny

English | [한국어](./README.ko.md)

> **⚠️ Deprecated**: This example module has been deprecated and excluded from the build.

[Mutiny](https://smallrye.io/smallrye-mutiny/) is a SmallRye library for Reactive Programming. This project provides a variety of examples demonstrating how to use Mutiny.

## Examples

### Core Examples

| File                                   | Description                             |
|------------------------------------|----------------------------------------|
| `01_Basic_Uni.kt`                  | Uni basics: single-value async processing |
| `02_Basic_Multi.kt`                | Multi basics: stream processing          |
| `03_Groups.kt`                     | Grouping and batch processing            |
| `04_Composition_Transformation.kt` | Composition and transformation operators |
| `05_Failures.kt`                   | Failure handling and recovery            |
| `06_Backpressure.kt`               | Backpressure management                  |
| `07_Threading.kt`                  | Threading model and schedulers           |
| `08_Multi_CustomOperator.kt`       | Implementing custom operators            |

### Backpressure Examples (backpressure/)

| File                      | Description              |
|-----------------------|--------------------------|
| `01_Drop.kt`          | Drop strategy for overflow |
| `02_Buffer.kt`        | Buffering strategy         |
| `03_Visual_Drop.kt`   | Visual demo: drop strategy |
| `04_Visual_Buffer.kt` | Visual demo: buffer strategy |

## Key Learning Points

### Uni (Single Async Value)

```kotlin
// Create a Uni
val uni = Uni.createFrom().item("Hello")

// Transform
uni.map { it.uppercase() }
    .flatMap { processAsync(it) }

// Subscribe
uni.subscribe().with(
    { item -> println(item) },
    { failure -> println(failure.message) }
)
```

### Multi (Stream)

```kotlin
// Create a Multi
val multi = Multi.createFrom().items(1, 2, 3, 4, 5)

// Transform
multi.filter { it % 2 == 0 }
     .map { it * 2 }

// Subscribe
multi.subscribe().with { item -> println(item) }
```

### Backpressure

```kotlin
// Drop strategy
multi.onOverflow().drop()

// Buffer strategy
multi.onOverflow().buffer(100)
```

## Running the Examples

```bash
# Run all examples
./gradlew :examples:mutiny:test

# Run a specific example
./gradlew :examples:mutiny:test --tests "*01_Basic*"
```

## References

- [Mutiny Official Documentation](https://smallrye.io/smallrye-mutiny/)
- [Mutiny Getting Started](https://smallrye.io/smallrye-mutiny/getting-started/)
