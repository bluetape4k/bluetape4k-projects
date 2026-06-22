# bluetape4k Probabilistic

JVM 애플리케이션에서 사용할 인메모리 확률적 자료구조 모듈입니다.

현재는 외부 Bloom Filter 구현 의존성 없이 직접 구현한 Bloom Filter를 제공합니다. 내부 구현에 Guava 또는 Eclipse Collections를 사용하지 않습니다.

## Gradle

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-probabilistic:${bluetape4kVersion}")
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

suspend 구현은 현재 I/O 없는 인메모리 연산입니다. 별도 dispatcher 전환을 수행하지 않습니다.

## 주의 사항

- `mightContain(false)`는 미포함을 보장합니다.
- `mightContain(true)`는 포함 가능성만 의미하며 오탐이 발생할 수 있습니다.
- `put(false)`는 대상 bit가 이미 모두 켜져 있었다는 뜻입니다. 원소가 이전에 삽입되었음을 증명하지 않습니다.
- 기본 구현은 thread-safe가 아닙니다. 동시 쓰기가 필요하면 호출자가 외부에서 동기화해야 합니다.
- `putAll`은 같은 설정과 hasher로 생성된 호환 필터끼리만 병합합니다.
- Redis 기반 분산 필터가 필요하면 `infra/lettuce`를 사용하세요.
