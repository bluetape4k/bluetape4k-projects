# Module bluetape4k-bloomfilter

## NOTE

Use redisson bloomfilter

## 소개

[Bloom Filter](https://ko.wikipedia.org/wiki/%EB%B8%94%EB%A3%B8_%ED%95%84%ED%84%B0)는 특정 요소가 집합에 속하는지 여부를 검사하는데 사용하는 확률적
자료구조이다.
블룸 필터에 의해 어떤 원소가 집합에 속한다고 판단된 경우 실제로는 원소가 집합에 속하지 않는 긍정 오류가 발생하는 것이 가능하지만, 반대로 원소가 집합에 속하지 않는 것으로 판단되었는데 실제로는 원소가 집합에
속하는 부정 오류는 절대로 발생하지 않는다는 특성이 있다.
집합에 원소를 추가하는 것은 가능하나, 집합에서 원소를 삭제하는 것은 불가능하다.
집합 내 원소의 숫자가 증가할수록 긍정 오류 발생 확률도 증가한다.

image::../doc/720px-Bloom_filter.svg.png[Bloom Filter]

NOTE: 즉 집합에 특정 요소가 없다는 판단인 부정오류는 절대 발생하지 않으므로, 이 판단을 믿을 수 있다.
이를 활용하면, 이력 정보를 담은 집합에 특정 이력이 포함되지 않는다는 판단은 신뢰할 수 있으므로, 중복 여부를 판단하기에 효과적인 알고리즘이라 할 수 있다.

### 활용 예

다양한 시스템에서 응용할 수 있는데, 이미 수행한 작업에 대해 중복 작업을 피하기 위한 목적으로 다음과 같이 응용할 수 있다

* Event Sourcing 시스템에서 이벤트를 중복 없이 전파
* 알림 서비스에서 중복 없이 알림 발송
* ID 생성 알고리즘 중 Time 기반 시 분산 시스템에서 중복된 Id 생성 방지

== 설정

Gradle Build 에서 `bluetape4k-bloomfilter` 라이브러를 참조에 추가하시면 됩니다.

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-bloomfilter:${bluetape4k_version}")
}
```

## 사용법

집합에 요소를 추가하고, 원하는 요소가 집합에 속하지 않았음을 검증할 수 있다

### InMemoryBloomFilter

로컬 메모리에서 `BitSet` 자료구조를 이용하여 BloomFilter를 구현했습니다.

InMemoryBloomFilter 예제

```kotlin
val count = 100_000
private val bloomFilter = InMemoryBloomFilter()

repeat(count) {
    bloomFilter.add(it.toString())  // Add elements
}

(0 until count).all { bloomFilter.contains(it.toString()) }.shouldBeTrue()

bloomFilter.contains((-1L).toString()).shouldBeFalse()        // Verify not exists
bloomFilter.contains((count + 1L).toString()).shouldBeFalse() // Verify not exists
```

### RedisBloomFilter

Redis 의 bit operation을 이용하여 BloomFilter를 구현하여, 분산 컴퓨팅 환경에서도 사용할 수 있습니다.
RedisBloomFilter를 사용하기 위해서는 [Redisson](https://github.com/redisson/redisson) 라이브러리를 사용합니다.

RedisBloomFilter example

```kotlin
val count = 100_000
private val bloomFilter = RedisBloomFilter(
    redisson = RedisProvider.redisson,
    bloomKey = "bluetape4k:bloomfilter:test",
    maxNum = DEFAULT_MAX_NUM,
    errorRate = DEFAULT_ERROR_RATE
)

repeat(count) {
    bloomFilter.add(it.toString())
}

(0 until count).all { bloomFilter.contains(it.toString()) }.shouldBeTrue()

bloomFilter.contains((-1L).toString()).shouldBeFalse()
bloomFilter.contains((count + 1L).toString()).shouldBeFalse()
```

## TODO

* RedisMutableBloomFilter - Redis를 사용하는 요소 삭제가 가능한 BloomFilter
* RedisBloomFilter에 비동기 방식 제공  
