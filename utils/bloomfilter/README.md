# bluetape4k-bloomfilter

Bloom Filter는 특정 요소가 집합에 속하는지 여부를 검사하는데 사용하는 확률적 자료구조입니다.

## 특징

- **확률적 자료구조**: 특정 요소가 집합에 속한다고 판단된 경우 실제로는 원소가 집합에 속하지 않는 **긍정 오류**가 발생할 수 있습니다.
- **부정 오류 없음**: 원소가 집합에 속하지 않는 것으로 판단되었는데 실제로는 원소가 집합에 속하는 **부정 오류는 절대 발생하지 않습니다**.
- **원소 삭제 불가**: 집합에 원소를 추가하는 것은 가능하나, 기본 Bloom Filter에서는 원소를 삭제할 수 없습니다. (단, [MutableBloomFilter]는 삭제 가능)
- **공간 효율성**: 매우 적은 메모리로 대량의 데이터를 표현할 수 있습니다.

**참고**: 집합 내 원소의 숫자가 증가할수록 긍정 오류 발생 확률도 증가합니다.

![Bloom Filter](../doc/720px-Bloom_filter.svg.png)

## 활용 예시

- **캐시 필터링**: DB 조회 전 Bloom Filter로 먼저 확인하여 불필요한 DB 조회 방지
- **중복 이벤트 검사**: Event Sourcing 시스템에서 이벤트 중복 처리 방지
- **알림 중복 방지**: 알림 서비스에서 중복 알림 발송 방지
- **ID 중복 검사**: 분산 시스템에서 Time 기반 ID 생성 시 중복 방지

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-bloomfilter:${bluetape4kVersion}")
}
```

## 기본 사용법

### InMemoryBloomFilter

메모리에 BitSet을 사용하는 Bloom Filter 구현체입니다.

```kotlin
import io.bluetape4k.bloomfilter.inmemory.InMemoryBloomFilter

val bloomFilter = InMemoryBloomFilter<String>()

// 요소 추가
val items = listOf("item1", "item2", "item3")
items.forEach { bloomFilter.add(it) }

// 포함 여부 검사
bloomFilter.contains("item1")  // true
bloomFilter.contains("item4")  // false (또는 false positive)

// 전체 검사
items.all { bloomFilter.contains(it) }  // true
```

### InMemoryMutableBloomFilter

요소 삭제가 가능한 Bloom Filter 구현체입니다.

```kotlin
import io.bluetape4k.bloomfilter.inmemory.InMemoryMutableBloomFilter

val bloomFilter = InMemoryMutableBloomFilter()

bloomFilter.add("test-item")
bloomFilter.contains("test-item")  // true

// 요소 삭제
bloomFilter.remove("test-item")
bloomFilter.contains("test-item")  // false

// 대략적인 카운트 조회
bloomFilter.approximateCount("test-item")  // 0
```

### SuspendBloomFilter (Coroutines 지원)

Coroutines 환경에서 사용할 수 있는 Bloom Filter입니다.

```kotlin
import io.bluetape4k.bloomfilter.inmemory.InMemorySuspendBloomFilter
import kotlinx.coroutines.runBlocking

val bloomFilter = InMemorySuspendBloomFilter<String>()

runBlocking {
    // 요소 추가 (suspend 함수)
    bloomFilter.add("coroutine-item")
    
    // 포함 여부 검사 (suspend 함수)
    bloomFilter.contains("coroutine-item")  // true
    
    // 초기화 (suspend 함수)
    bloomFilter.clear()
}
```

### RedissonBloomFilter

Redis를 사용하는 분산 환경용 Bloom Filter입니다.

```kotlin
import io.bluetape4k.bloomfilter.redis.RedissonBloomFilter
import org.redisson.Redisson

val redisson = Redisson.create()
val bloomFilter = RedissonBloomFilter<String>(
    redisson = redisson,
    bloomName = "my-bloom-filter"
)

// 사용법은 InMemoryBloomFilter와 동일
bloomFilter.add("redis-item")
bloomFilter.contains("redis-item")  // true
```

### RedissonSuspendBloomFilter

Redis를 사용하는 Coroutines 지원 Bloom Filter입니다.

```kotlin
import io.bluetape4k.bloomfilter.redis.RedissonSuspendBloomFilter

val bloomFilter = RedissonSuspendBloomFilter<String>(
    redisson = redisson,
    bloomName = "my-suspend-bloom-filter"
)

runBlocking {
    bloomFilter.add("suspend-redis-item")
    bloomFilter.contains("suspend-redis-item")  // true
}
```

## 고급 설정

### 최대 요소 수와 오류율 설정

```kotlin
val bloomFilter = InMemoryBloomFilter<String>(
    maxNum = 100_000L,      // 최대 10만 개 요소
    errorRate = 0.001       // 0.1% 오류율
)

// 자동 계산된 파라미터 확인
println("Bit size: ${bloomFilter.m}")      // Bloom Filter 크기 (bit)
println("Hash count: ${bloomFilter.k}")    // 해시 함수 개수
```

### False Positive 확률 계산

```kotlin
val bloomFilter = InMemoryBloomFilter<String>()

// n개 요소 추가 시 bit가 0일 확률
val zeroProb = bloomFilter.getBitZeroProbability(n = 1000)

// n개 요소 추가 시 false positive 확률
val fpProb = bloomFilter.getFalsePositiveProbability(n = 1000)

// 원소당 bit 수
val bitsPerElement = bloomFilter.getBitsPerElement(n = 1000)
```

## 성능

- **시간 복잡도**: O(k) - k는 해시 함수 개수 (보통 1~10)
- **공간 복잡도**: O(m) - m은 Bloom Filter 크기 (bit)
- **해싱 알고리즘**: Murmur3 해싱 사용

## 주의사항

1. **False Positive**: Bloom Filter는 "존재하지 않는다"는 결과는 정확하지만, "존재한다"는 결과는 오류가 있을 수 있습니다.
2. **삭제 제한**: 기본 Bloom Filter는 요소 삭제가 불가능합니다. 삭제가 필요하면 MutableBloomFilter를 사용하세요.
3. **용량 계획**: 예상 요소 수와 허용 오류율을 고려하여 적절한 크기를 설정하세요.

## 참고 자료

- [Bloom Filter (Wikipedia)](https://ko.wikipedia.org/wiki/%EB%B8%94%EB%A3%B8_%ED%95%84%ED%84%B0)
- [Bloom Filter (English Wikipedia)](https://en.wikipedia.org/wiki/Bloom_filter)
- [Redisson](https://github.com/redisson/redisson)
