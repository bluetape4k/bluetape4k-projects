# bluetape4k Probabilistic

In-memory probabilistic data structures for JVM applications.

This module currently provides a dependency-free Bloom Filter implementation. It does not use Guava or Eclipse Collections internally.

## Gradle

```kotlin
dependencies {
    implementation(project(":bluetape4k-probabilistic"))
}
```

## Bloom Filter

```kotlin
import io.bluetape4k.probabilistic.bloomfilter.bloomFilter

val filter = bloomFilter<String>(
    expectedInsertions = 1_000_000L,
    fpp = 0.01,
)

if (!filter.mightContain(url)) {
    filter.put(url)
    crawl(url)
}
```

## Coroutine API

```kotlin
import io.bluetape4k.probabilistic.bloomfilter.suspendBloomFilter

val filter = suspendBloomFilter<String>(expectedInsertions = 100_000L, fpp = 0.01)

filter.put("https://example.com")
val exists = filter.mightContain("https://example.com")
```

The suspend implementation is still an in-memory, non-blocking operation. It does not switch dispatchers.

## Notes

- `mightContain(false)` means the element is definitely absent.
- `mightContain(true)` means the element may exist; false positives are possible.
- `put(false)` means all target bits were already set. It does not prove the element was previously inserted.
- The implementation is not thread-safe. Synchronize externally for concurrent writes.
- `putAll` only merges filters created with compatible settings and hashers.
- For distributed Redis-backed filters, use `infra/lettuce`.
