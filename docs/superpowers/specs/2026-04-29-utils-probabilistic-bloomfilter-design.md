# utils/probabilistic Bloom Filter 승격 설계

## 배경

Issue #142는 `x-obsoleted/bloomfilter`에 남아 있는 JVM 인메모리 Bloom Filter 기능 중 Redis 의존이 없는 구현만 선별해 `utils/probabilistic` 신규 모듈로 승격하는 작업이다.

현재 `infra/lettuce`에는 Redis BitSet/Lua 기반 분산 Bloom/Cuckoo Filter 구현이 있으나, 캐시 stampede 방지, 중복 방문 추적, 스팸 URL 후보 검사처럼 단일 JVM 안에서 빠르게 사용할 확률적 멤버십 자료구조가 필요하다.

## 요구사항

- 신규 모듈: `utils/probabilistic` (`:probabilistic`)
- 패키지 루트: `io.bluetape4k.probabilistic`
- 1차 범위: Bloom Filter API와 인메모리 구현
- 제외 범위: Redis Lua/Redisson/Lettuce 구현, Cuckoo Filter 구현
- 공개 API는 한국어 KDoc을 제공한다.
- README.md / README.ko.md에 사용 예시와 제약을 기록한다.
- 기존 `x-obsoleted/bloomfilter`는 삭제하지 않고 참조 구현으로 둔다.
- Guava가 제공하는 `writeTo/readFrom` 직렬화 API는 1차 공개 API 범위에서 제외한다.

## Step 1-R 연구 요약

- Guava 공식 `BloomFilter` API는 `create(funnel, expectedInsertions, fpp)`, `put`, `mightContain`, `expectedFpp`, `approximateElementCount`, `writeTo/readFrom`을 제공한다.
- Guava 문서는 예상 삽입 수보다 훨씬 많은 원소를 넣으면 필터 포화로 FPP가 급격히 악화될 수 있음을 명시한다.
- repo에는 `Libs.guava`가 이미 `buildSrc/src/main/kotlin/Libs.kt`에 정의되어 있고 root dependency management에도 포함되어 있다.
- `infra/lettuce`의 `BloomFilterOptions`는 `expectedInsertions > 0`, `falseProbability in (0, 1)` 입력 검증 계약을 이미 사용한다.
- `x-obsoleted/bloomfilter`는 직접 Murmur3/BitSet 구현을 제공하지만 serializer/zero-allocation-hashing 의존과 자체 해시 offset 수식을 갖고 있어 신규 모듈의 1차 안정 구현으로는 리스크가 더 크다.

## 설계 옵션

### 옵션 A: Guava BloomFilter 래퍼

Guava의 검증된 BloomFilter를 내부 구현으로 사용하고 bluetape4k용 인터페이스/DSL/suspend wrapper를 제공한다.

장점:
- 해시 전략, bit size 계산, 직렬화 형식이 검증된 구현에 위임된다.
- 신규 모듈 구현량과 유지보수 리스크가 낮다.
- Issue의 "Guava BloomFilter 래퍼 또는 직접 구현" 중 1차 구현 의도와 맞다.

단점:
- Guava `Funnel`을 공개 생성 API에 노출해야 한다.
- `expectedInsertions`/`fpp`는 생성 시점 계약이며 실제 삽입 수 강제는 하지 않는다.

### 옵션 B: x-obsoleted 직접 구현 패키지 승격

기존 `Hasher`, `BitSet`, `LongArray` 기반 구현을 새 패키지로 옮긴다.

장점:
- 외부 Guava API 노출 없이 완전한 bluetape4k 구현을 제공할 수 있다.
- 기존 테스트 일부를 거의 그대로 이전할 수 있다.

단점:
- 기존 mutable 구현은 `String` 전용이고 bucket/lock 로직의 정확성 검증 비용이 크다.
- serializer와 zero-allocation-hashing 의존이 필요해 신규 유틸 모듈의 표면이 넓어진다.
- Guava가 이미 제공하는 검증된 기능을 다시 유지보수해야 한다.

### 옵션 C: Guava 래퍼 + 직접 구현 내부 백업

공개 API는 Guava 래퍼로 제공하고, 향후 최적화를 위해 내부 해시/비트셋 유틸을 함께 둔다.

장점:
- 향후 Guava 제거나 성능 최적화 여지가 생긴다.

단점:
- 당장 쓰지 않는 코드가 추가되어 테스트/문서/검토 비용이 증가한다.
- 직접 구현과 Guava 구현의 동작 차이를 계속 맞춰야 한다.

## 결정

옵션 A를 채택한다. 신규 `utils/probabilistic`는 Guava 기반 1차 구현으로 시작하고, 기존 직접 구현의 수식/테스트 관점만 차용한다.

Rejected:
- 옵션 B: 검증된 Guava 구현을 대체할 만큼의 성능/의존성 이점이 현재 요구사항에 없다.
- 옵션 C: 사용하지 않는 내부 구현을 같이 승격하면 신규 모듈의 유지보수 표면이 불필요하게 커진다.

## API 설계

```kotlin
interface BloomFilter<T: Any> {
    val expectedInsertions: Long
    val falsePositiveProbability: Double
    fun mightContain(element: T): Boolean
    fun put(element: T): Boolean
    fun approximateElementCount(): Long
    fun expectedFpp(): Double
}

interface MutableBloomFilter<T: Any>: BloomFilter<T> {
    fun putAll(other: MutableBloomFilter<T>)
}

interface SuspendBloomFilter<T: Any> {
    val expectedInsertions: Long
    val falsePositiveProbability: Double
    suspend fun mightContain(element: T): Boolean
    suspend fun put(element: T): Boolean
    suspend fun approximateElementCount(): Long
    suspend fun expectedFpp(): Double
}
```

생성 DSL:

```kotlin
val filter = bloomFilter(
    expectedInsertions = 1_000_000L,
    fpp = 0.01,
) {
    Funnels.stringFunnel(Charsets.UTF_8)
}
```

구현 클래스:

- `GuavaBloomFilter<T>`: Guava `com.google.common.hash.BloomFilter<T>` 래퍼
- `GuavaSuspendBloomFilter<T>`: `GuavaBloomFilter<T>`에 suspend 계약을 입히는 메모리 연산 wrapper
- `BloomFilterConfig`: `expectedInsertions`, `falsePositiveProbability` 입력 검증을 한 곳에 모은 value/data class
- `putAll`은 동일한 Guava strategy/bit size/funnel 기반 필터끼리만 허용하고, 호환되지 않으면 `IllegalArgumentException`으로 실패한다.

호환 alias:

- Issue 명시 이름과 discoverability를 위해 `InMemoryBloomFilter<T>` / `InMemorySuspendBloomFilter<T>` typealias 또는 얇은 클래스를 제공한다.
- `InMemoryMutableBloomFilter`는 "삭제 가능한 mutable"이 아니라 Guava의 `putAll` 가능한 mutable 의미로 제공한다. 삭제는 Bloom Filter 특성상 1차 범위에서 지원하지 않는다.

## 모듈/의존성

`utils/probabilistic/build.gradle.kts`

- `api(project(":bluetape4k-core"))`
- `api(Libs.guava)`
- `compileOnly(Libs.kotlinx_coroutines_core)`
- `testImplementation(project(":bluetape4k-junit5"))`
- `testImplementation(Libs.kotlinx_coroutines_test)`

## 리스크와 대응

| Risk | Impact | Mitigation |
|------|--------|------------|
| Guava `Funnel` 직렬화/동등성 계약을 사용자가 잘못 이해 | compatible merge/serialization 실패 | README와 KDoc에 동일 Funnel 필요성을 명시 |
| `put` 반환값이 "신규 원소 확정"으로 오해됨 | 호출자가 중복 검출을 과신 | `put`은 bit 변경 여부이며 false면 이미 존재 확정이 아님을 KDoc에 기록 |
| 예상 삽입 수 초과로 FPP 악화 | 운영 오탐률 상승 | `expectedFpp()`/README로 모니터링 가능성을 안내하고 테스트로 설정 FPP 근방 검증 |
| suspend API가 I/O 비동기 처리로 오해됨 | 불필요한 dispatcher 전환 기대 | KDoc에 현재 구현은 비블로킹 메모리 연산이라고 명시 |
| `InMemoryMutableBloomFilter` 이름이 삭제 지원처럼 보임 | API 오용 | 삭제 API를 노출하지 않고 `putAll` 의미의 mutable로 제한, README에 Counting Bloom Filter 아님을 기록 |
| Guava 직렬화 API를 노출하면 Funnel 직렬화 호환성이 공개 계약이 됨 | 장기 호환성 부담 | 1차 구현에서는 직렬화 API를 공개하지 않고 내부 래퍼의 멤버십 계약만 제공 |

## Acceptance Criteria

- `:probabilistic` 모듈이 Gradle에 자동 포함되고 컴파일된다.
- `BloomFilter`, `MutableBloomFilter`, `SuspendBloomFilter` 공개 계약과 Guava 기반 인메모리 구현이 제공된다.
- `bloomFilter` / `suspendBloomFilter` DSL 생성 함수가 동작한다.
- 입력 검증은 `expectedInsertions > 0`, `fpp in (0, 1)`을 보장한다.
- 삽입한 원소는 `mightContain == true`를 만족한다.
- 미삽입 원소 집합의 관측 FPP가 설정값을 과도하게 넘지 않는다.
- 호환 가능한 필터끼리 `putAll` 병합이 가능하고, 호환되지 않는 필터 병합은 명시적으로 실패한다.
- suspend API는 `runTest`로 검증된다.
- README.md / README.ko.md에 사용법, FPP, 제한 사항이 기록된다.

## DoD

- `./bin/repo-test-summary -- ./gradlew :probabilistic:test` 통과
- `./bin/repo-test-summary -- ./gradlew :probabilistic:compileKotlin :probabilistic:compileTestKotlin` 통과
- 공개 API 한국어 KDoc 작성
- Step 6-R 6개 리뷰 티어 수행
- Lore commit trailer를 포함한 커밋 작성
- Issue #142를 닫는 PR 생성

## Draft Task List

1. 신규 모듈 골격과 Gradle 의존성 추가
2. Bloom Filter 공개 API/DSL/Guava 래퍼 구현
3. 입력 검증, FPP, suspend wrapper 테스트 작성
4. README.md / README.ko.md 작성
5. targeted compile/test 및 리뷰 게이트 수행
