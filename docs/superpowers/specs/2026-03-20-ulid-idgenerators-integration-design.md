# ULID idgenerators 통합 설계 Spec

**날짜**: 2026-03-20
**모듈**: `utils/idgenerators` (`bluetape4k-idgenerators`)
**소스**: `bluetape4k-experimental/utils/ulid/`

---

## 1. 배경 및 목표

### 배경

ULID(Universally Unique Lexicographically Sortable Identifier)는 UUID의 대안으로, 128비트 크기에 48비트 밀리초 타임스탬프 + 80비트 랜덤으로 구성된다. Crockford Base32 인코딩으로 26자 문자열 표현이 가능하며, 시간순 정렬이 보장되어 DB PK로 적합하다.

현재 `bluetape4k-experimental/utils/ulid/`에 완전한 ULID 구현이 존재하며, 이를 `bluetape4k-idgenerators` 모듈에 통합하여 정식 API로 승격한다.

### 목표

1. experimental ULID 소스를 `idgenerators` 모듈로 마이그레이션
2. 기존 `IdGenerator<ID>` 계약에 맞는 `UlidGenerator` 제공
3. Monotonic 모드를 기본으로 하여 DB PK B-tree 성능 최적화
4. 기존 ULID 계층 구조(Factory, Monotonic, StatefulMonotonic)를 유지하되 패키지만 변경

---

## 2. 설계 결정 및 근거

### 2.1 IdGenerator 타입: `UlidGenerator: IdGenerator<String>`

**근거**: Ksuid 패턴과 동일하게 `String` 타입을 반환한다. ULID의 26자 Crockford Base32 문자열이 사실상 표준 표현이며, DB 컬럼 타입으로도 `CHAR(26)` 또는 `VARCHAR(26)`이 일반적이다.

```kotlin
class UlidGenerator(
    factory: ULID.Factory = ULID,
): IdGenerator<String> {
    private val monotonic: ULID.StatefulMonotonic = ULID.statefulMonotonic(factory)

    override fun nextId(): String = monotonic.nextULID().toString()
    override fun nextIdAsString(): String = nextId()
}
```

### 2.2 기본 모드: Monotonic (StatefulMonotonic)

**근거**: DB PK 용도에서는 같은 밀리초 내 순서 보장이 중요하다. `StatefulMonotonic`은 내부적으로 이전 ULID를 추적하여 random 부분을 increment하므로, B-tree 삽입 성능이 최적화된다.

### 2.3 Overflow 정책: spin-wait (next millisecond)

**근거**: `ULIDStatefulMonotonic`의 `nextULID()`가 overflow 시 `nextULIDStrict()`가 null을 반환하면 다음 밀리초까지 대기한다. 이는 실무에서 거의 발생하지 않으며(같은 밀리초에 2^80 이상 생성 불가), 발생하더라도 1ms 미만 대기로 해결된다.

### 2.4 ULID 인터페이스: 계층 구조 유지

**근거**: experimental 코드의 `ULID` 인터페이스 설계가 이미 잘 구조화되어 있다.

- `ULID` (interface) - 값 타입, Comparable
- `ULID.Factory` - 생성 팩토리
- `ULID.Monotonic` - 상태 없는 단조 증가 생성
- `ULID.StatefulMonotonic` - 상태 기반 단조 증가 생성 (atomicfu CAS)

`internal` 구현체(`ULIDValue`, `ULIDFactory`, `ULIDMonotonic`, `ULIDStatefulMonotonic`)는 internal 가시성을 유지한다.

### 2.5 Exposed 통합: 이번 PR에서 제외

**근거**: `exposed-core`의 컬럼 타입 추가는 별도 태스크로 분리하여 PR 크기를 관리 가능하게 유지한다.

---

## 3. 파일 구조 (신규 파일 목록)

```
utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/ulid/
├── ULID.kt                          # ULID 인터페이스 (Factory, Monotonic, StatefulMonotonic)
├── UlidGenerator.kt                 # IdGenerator<String> 구현
├── JavaUUIDSupport.kt               # ULID <-> java.util.UUID 변환
├── KotlinUuidSupport.kt             # ULID <-> kotlin.uuid.Uuid 변환
└── internal/
    ├── ULIDValue.kt                 # data class, ULID 구현체
    ├── ULIDFactory.kt               # ULID.Factory 구현
    ├── ULIDMonotonic.kt             # ULID.Monotonic 구현
    ├── ULIDStatefulMonotonic.kt     # ULID.StatefulMonotonic 구현 (atomicfu CAS)
    ├── Crockford.kt                 # Crockford Base32 인코딩/디코딩
    ├── Constants.kt                 # 상수 정의
    ├── Time.kt                      # 시간 유틸리티
    └── ByteArraySupport.kt          # ByteArray 확장

utils/idgenerators/src/test/kotlin/io/bluetape4k/idgenerators/ulid/
├── AbstractULIDTest.kt              # 공통 테스트 베이스
├── ULIDTest.kt                      # ULID 기본 테스트
├── ULIDFactoryTest.kt               # Factory 테스트
├── ULIDMonotonicTest.kt             # Monotonic 테스트
├── ULIDStatefulMonotonicTest.kt     # StatefulMonotonic 테스트
├── ULIDConcurrencyTest.kt           # 동시성 테스트 (MultithreadingTester)
├── UlidGeneratorTest.kt             # [신규] UlidGenerator IdGenerator 계약 테스트
├── UlidGeneratorConcurrencyTest.kt  # [신규] UlidGenerator 동시성 테스트
├── JavaUUIDSupportTest.kt           # UUID 변환 테스트
├── KotlinUuidSupportTest.kt         # Kotlin Uuid 변환 테스트
├── internal/
│   └── CrockfordTest.kt             # Base32 인코딩 테스트
└── utils/
    ├── MockRandom.kt                # 테스트용 Mock Random
    └── TestConstants.kt             # 테스트 상수
```

---

## 4. 패키지 구조

### 변경 전 (experimental)
```
io.bluetape4k.ulid
io.bluetape4k.ulid.internal
```

### 변경 후 (idgenerators 통합)
```
io.bluetape4k.idgenerators.ulid
io.bluetape4k.idgenerators.ulid.internal
```

기존 idgenerators 모듈의 패키지 관례(`io.bluetape4k.idgenerators.{feature}`)를 따른다.

---

## 5. 핵심 API 설계

### 5.1 공개 API

```kotlin
// ULID 값 타입 (인터페이스)
interface ULID : Comparable<ULID> {
    val mostSignificantBits: Long
    val leastSignificantBits: Long
    val timestamp: Long

    fun toByteArray(): ByteArray
    fun increment(): ULID

    interface Factory { ... }
    interface Monotonic { ... }
    interface StatefulMonotonic : Factory { ... }

    companion object : Factory by ULIDFactory.Default {
        fun factory(random: Random = Random): Factory
        fun monotonic(factory: Factory = ULID): Monotonic
        fun statefulMonotonic(factory: Factory = ULID): StatefulMonotonic
    }
}

// IdGenerator 어댑터
class UlidGenerator(
    factory: ULID.Factory = ULID,
) : IdGenerator<String> {
    override fun nextId(): String
    override fun nextIdAsString(): String
}

// UUID 변환 확장
fun ULID.toUUID(): UUID
fun ULID.Companion.fromUUID(uuid: UUID): ULID
fun ULID.toUuid(): Uuid          // @ExperimentalUuidApi
fun ULID.Companion.fromUuid(uuid: Uuid): ULID
```

### 5.2 Internal API (변경 없음)

- `ULIDValue`: `data class`, `ULID` 인터페이스 구현, `Serializable`
- `ULIDFactory`: `ULID.Factory` 구현, `SecureRandom` 기반
- `ULIDMonotonic`: 상태 없는 monotonic 생성, overflow 시 spin-wait
- `ULIDStatefulMonotonic`: atomicfu CAS 기반 상태 관리, thread-safe
- `Crockford`: Base32 인코딩/디코딩 유틸리티

---

## 6. 테스트 전략

### 6.1 마이그레이션 테스트 (experimental에서 가져오기)

| 테스트 파일 | 검증 대상 |
|---|---|
| `ULIDTest.kt` | ULID 생성, 파싱, toString, compareTo |
| `ULIDFactoryTest.kt` | Factory를 통한 생성, 바이트배열 변환 |
| `ULIDMonotonicTest.kt` | 같은 밀리초 내 순서 보장, overflow 동작 |
| `ULIDStatefulMonotonicTest.kt` | atomicfu CAS, 내부 상태 추적 |
| `ULIDConcurrencyTest.kt` | MultithreadingTester 동시성 검증 |
| `JavaUUIDSupportTest.kt` | ULID <-> UUID 왕복 변환 |
| `KotlinUuidSupportTest.kt` | ULID <-> Kotlin Uuid 왕복 변환 |
| `CrockfordTest.kt` | Base32 인코딩/디코딩 정확성 |

### 6.2 신규 테스트

| 테스트 파일 | 검증 대상 |
|---|---|
| `UlidGeneratorTest.kt` | `IdGenerator<String>` 계약 준수, nextId/nextIdAsString/nextIds |
| `UlidGeneratorConcurrencyTest.kt` | StructuredTaskScopeTester, SuspendedJobTester 동시성 |

### 6.3 동시성 테스트 도구

- `MultithreadingTester`: 기존 experimental 테스트에서 사용 중 (유지)
- `StructuredTaskScopeTester`: Virtual Thread 기반 동시성 (신규 추가)
- `SuspendedJobTester`: Coroutine Job 기반 동시성 (신규 추가)

---

## 7. 수용 기준 (Acceptance Criteria)

### 필수 (Must Have)

- [ ] 모든 ULID 소스가 `io.bluetape4k.idgenerators.ulid` 패키지로 마이그레이션됨
- [ ] `UlidGenerator`가 `IdGenerator<String>` 인터페이스를 구현함
- [ ] `UlidGenerator.nextId()`가 26자 Crockford Base32 문자열을 반환함
- [ ] Monotonic 모드에서 같은 밀리초 내 ULID가 단조 증가함
- [ ] 마이그레이션된 모든 테스트가 통과함 (패키지 변경 후)
- [ ] `UlidGeneratorTest`, `UlidGeneratorConcurrencyTest` 신규 테스트 통과
- [ ] `./gradlew :bluetape4k-idgenerators:test` 전체 통과
- [ ] build.gradle.kts에 추가 의존성 변경 불필요 (bluetape4k-core만 사용)

### 금지 (Must NOT Have)

- [ ] Exposed 컬럼 타입 추가 (별도 태스크)
- [ ] 기존 idgenerators 코드 변경 (Ksuid, Snowflake, UUID 등)
- [ ] external 라이브러리 의존성 추가
- [ ] `ULID` 인터페이스의 API 변경 (패키지명만 변경)
