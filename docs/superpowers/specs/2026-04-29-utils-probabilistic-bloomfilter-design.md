# utils/probabilistic Bloom Filter 승격 설계

## 배경

Issue #142는 `x-obsoleted/bloomfilter`에 남아 있는 JVM 인메모리 Bloom Filter 기능 중 Redis 의존이 없는 구현만 선별해 `utils/probabilistic` 신규 모듈로 승격하는 작업이다.

현재 `infra/lettuce`에는 Redis BitSet/Lua 기반 분산 Bloom/Cuckoo Filter 구현이 있으나, 캐시 stampede 방지, 중복 방문 추적, 스팸 URL 후보 검사처럼 단일 JVM 안에서 빠르게 사용할 확률적 멤버십 자료구조가 필요하다.

## 요구사항

- 신규 모듈: `utils/probabilistic` (`:bluetape4k-probabilistic`)
- 패키지 루트: `io.bluetape4k.probabilistic`
- 1차 범위: Bloom Filter API와 인메모리 구현
- 제외 범위: Redis Lua/Redisson/Lettuce 구현, Cuckoo Filter 구현
- Guava, Eclipse Collections 등 신규 컬렉션/확률 자료구조 의존성을 추가하지 않는다.
- 공개 API는 한국어 KDoc을 제공한다.
- README.md / README.ko.md에 사용 예시와 제약을 기록한다.
- 기존 `x-obsoleted/bloomfilter`는 삭제하지 않고 참조 구현으로 둔다.

## Step 1-R 연구 요약

- `x-obsoleted/bloomfilter`는 직접 Murmur3/BitSet 기반 구현을 제공한다. 다만 `zero-allocation-hashing`, serializer 의존이 있고 mutable 구현은 `String` 전용이라 그대로 승격하지 않는다.
- `infra/lettuce`의 `BloomFilterOptions`는 `expectedInsertions > 0`, `falseProbability in (0, 1)` 입력 검증 계약을 이미 사용한다.
- repo에는 `Libs.guava`, `Libs.eclipse_collections`가 이미 있으나, 사용자 결정에 따라 이번 모듈에는 사용하지 않는다.
- Eclipse Collections는 primitive collections를 제공하지만 Bloom Filter 자체나 JVM `BitSet`보다 이 작업에 더 적합한 bit-level 저장소를 제공하지 않는다.
- JDK/Kotlin만으로도 `LongArray` 기반 bitset과 SHA-256 double hashing을 사용해 안정적인 Bloom Filter를 구현할 수 있다.

## 설계 옵션

### 옵션 A: JDK/Kotlin 직접 구현

`LongArray` bitset, SHA-256 기반 double hashing, Bloom Filter 수식으로 `m`/`k`를 계산하는 구현을 신규 모듈에 작성한다.

장점:
- 신규 외부 의존성이 없다.
- 공개 API가 Guava/Eclipse Collections 타입에 묶이지 않는다.
- 기존 `x-obsoleted`의 직접 구현 의도를 유지하면서 serializer/hash 라이브러리 의존을 제거한다.

단점:
- Guava처럼 battle-tested 구현을 위임하지 않으므로 수식/bitset/hash 테스트가 중요하다.
- SHA-256은 Murmur3보다 느릴 수 있다.

### 옵션 B: x-obsoleted 직접 구현 패키지 승격

기존 `Hasher`, `BitSet`, `LongArray` 기반 구현을 새 패키지로 옮긴다.

장점:
- 기존 코드와 테스트 일부를 거의 그대로 이전할 수 있다.

단점:
- `zero-allocation-hashing`과 serialization 의존이 남는다.
- 기존 mutable 구현은 `String` 전용이고 bucket/lock 로직 검증 비용이 크다.
- 기존 해시 offset 수식은 double hashing 표준 형태보다 검토하기 어렵다.

### 옵션 C: Eclipse Collections 내부 저장소 사용

Eclipse Collections primitive collections를 내부 bit/index 저장소로 사용한다.

장점:
- 이미 repo dependency catalog에 존재한다.

단점:
- Bloom Filter에 필요한 compact bitset에는 `LongArray`가 더 직접적이고 메모리 효율적이다.
- 신규 모듈 공개/내부 의존성을 늘리는 이점이 없다.

## 결정

옵션 A를 채택한다. `utils/probabilistic`는 Guava/Eclipse Collections 없이 직접 구현한다. 기존 `x-obsoleted` 구현에서는 API 의도, 테스트 관점, Bloom Filter 수식만 차용하고 의존성/해시 구현은 새로 단순화한다.

Rejected:
- 옵션 B: 기존 구현 전체 승격은 불필요한 외부 의존과 String 전용 mutable 구현을 함께 승격한다.
- 옵션 C: Eclipse Collections는 이 작업의 핵심인 bit-level Bloom Filter 저장소 문제를 더 단순하게 만들지 않는다.

## API 설계

```kotlin
interface BloomFilter<T: Any> {
    val expectedInsertions: Long
    val falsePositiveProbability: Double
    val bitSize: Long
    val hashFunctionCount: Int
    fun mightContain(element: T): Boolean
    fun put(element: T): Boolean
    fun approximateElementCount(): Long
    fun expectedFpp(): Double
    fun clear()
}

interface MutableBloomFilter<T: Any>: BloomFilter<T> {
    fun putAll(other: MutableBloomFilter<T>)
}

interface SuspendBloomFilter<T: Any> {
    val expectedInsertions: Long
    val falsePositiveProbability: Double
    val bitSize: Long
    val hashFunctionCount: Int
    suspend fun mightContain(element: T): Boolean
    suspend fun put(element: T): Boolean
    suspend fun approximateElementCount(): Long
    suspend fun expectedFpp(): Double
    suspend fun clear()
}
```

생성 DSL:

```kotlin
val filter = bloomFilter<String>(
    expectedInsertions = 1_000_000L,
    fpp = 0.01,
)
```

구현 클래스:

- `InMemoryBloomFilter<T>`: `LongArray` bitset 기반 Bloom Filter
- `InMemoryMutableBloomFilter<T>`: `putAll` 병합이 가능한 동일 구현 alias/클래스
- `InMemorySuspendBloomFilter<T>`: `InMemoryBloomFilter<T>`에 suspend 계약을 입히는 메모리 연산 wrapper
- `BloomFilterConfig`: `expectedInsertions`, `falsePositiveProbability` 입력 검증과 `bitSize`, `hashFunctionCount` 계산
- `BloomHasher<T>`: element를 hash bytes로 변환하는 전략
- `DefaultBloomHasher`: `String`, `Int`, `Long`, `ByteArray`, `Serializable`, fallback `toString()`을 지원하는 기본 hasher

## 내부 구현

- `bitSize = ceil(-n * ln(p) / ln(2)^2)`
- `hashFunctionCount = max(1, round((bitSize / n) * ln(2)))`
- SHA-256 digest에서 두 개의 64-bit 값을 뽑고 `hash1 + i * hash2` double hashing으로 offset을 생성한다.
- bitset은 `LongArray((bitSize + 63) / 64)`로 저장한다.
- `put`은 하나 이상의 bit가 새로 켜졌으면 `true`, 모두 이미 켜져 있으면 `false`를 반환한다. `false`는 "이미 존재 확정"이 아니라 Bloom Filter 특성상 "이미 존재할 가능성"이다.
- `putAll`은 `bitSize`, `hashFunctionCount`, `expectedInsertions`, `falsePositiveProbability`가 같은 직접 구현끼리만 허용한다.
- 기본 구현은 thread-safe를 보장하지 않는다. 동시 접근이 필요하면 호출자가 외부 동기화를 제공해야 한다.

## 모듈/의존성

`utils/probabilistic/build.gradle.kts`

- `api(project(":bluetape4k-core"))`
- `compileOnly(Libs.kotlinx_coroutines_core)`
- `testImplementation(project(":bluetape4k-junit5"))`
- `testImplementation(Libs.kotlinx_coroutines_test)`

## 리스크와 대응

| Risk | Impact | Mitigation |
|------|--------|------------|
| 직접 구현 수식 오류 | FPP/메모리 사용량 오류 | `BloomFilterConfig` 단위 테스트로 bit size/hash count 범위 검증 |
| hash offset 편향 | FPP 악화 | SHA-256 double hashing과 deterministic FPP 회귀 테스트 사용 |
| `put` 반환값 오해 | 호출자가 중복 검출을 과신 | KDoc/README에 bit 변경 여부라고 명시 |
| 예상 삽입 수 초과로 FPP 악화 | 운영 오탐률 상승 | `expectedFpp()`와 README 제약 문서화 |
| suspend API가 I/O 비동기 처리로 오해됨 | 불필요한 dispatcher 전환 기대 | 현재 구현은 비블로킹 메모리 연산이라고 명시 |
| 동시 접근 오해 | 데이터 경합 | thread-safe 비보장과 외부 동기화 필요성을 KDoc/README에 기록 |

## Acceptance Criteria

- `:bluetape4k-probabilistic` 모듈이 Gradle에 자동 포함되고 컴파일된다.
- Guava/Eclipse Collections 신규 의존성 없이 Bloom Filter 직접 구현이 제공된다.
- `BloomFilter`, `MutableBloomFilter`, `SuspendBloomFilter` 공개 계약과 인메모리 구현이 제공된다.
- `bloomFilter` / `suspendBloomFilter` DSL 생성 함수가 동작한다.
- 입력 검증은 `expectedInsertions > 0`, `fpp in (0, 1)`을 보장한다.
- 삽입한 원소는 `mightContain == true`를 만족한다.
- 미삽입 원소 집합의 관측 FPP가 설정값을 과도하게 넘지 않는다.
- 호환 가능한 필터끼리 `putAll` 병합이 가능하고, 호환되지 않는 필터 병합은 명시적으로 실패한다.
- suspend API는 `runTest`로 검증된다.
- README.md / README.ko.md에 사용법, FPP, 제한 사항이 기록된다.

## DoD

- `./bin/repo-test-summary -- ./gradlew :bluetape4k-probabilistic:test` 통과
- `./bin/repo-test-summary -- ./gradlew :bluetape4k-probabilistic:compileKotlin :bluetape4k-probabilistic:compileTestKotlin` 통과
- 공개 API 한국어 KDoc 작성
- Step 6-R 6개 리뷰 티어 수행
- Lore commit trailer를 포함한 커밋 작성
- Issue #142를 닫는 PR 생성

## Draft Task List

1. 신규 모듈 골격과 Gradle 의존성 추가
2. Bloom Filter 공개 API/DSL/직접 구현 작성
3. 입력 검증, bitset/hash/FPP, suspend wrapper 테스트 작성
4. README.md / README.ko.md 작성
5. targeted compile/test 및 리뷰 게이트 수행
