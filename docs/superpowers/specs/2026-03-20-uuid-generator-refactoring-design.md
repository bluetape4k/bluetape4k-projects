# UUID Generator 리팩토링 설계 Spec

**날짜**: 2026-03-20
**모듈**: `utils/idgenerators` (`io.bluetape4k.idgenerators.uuid`)
**참조**: ULID 통합 구조 (`io.bluetape4k.idgenerators.ulid.ULID`)

---

## 1. 현재 문제점 분석

### 1.1 구조적 불일치

| 항목 | 현재 상태 | 문제 |
|------|-----------|------|
| `TimebasedUuid` | `object` + nested objects (Default, Reordered, Epoch) | 패턴이 `RandomUuidGenerator`(class)와 혼재 |
| `TimebasedUuidGenerator` | `class : IdGenerator<UUID>` | `TimebasedUuid.Reordered`와 100% 중복 (같은 JUG `timeBasedReorderedGenerator`) |
| `RandomUuidGenerator` | `class(random: Random)` | 유일하게 커스텀 Random 주입 가능 |
| `NamebasedUuidGenerator` | `class : IdGenerator<UUID>` | 내부에서 random UUID를 name으로 사용 -- name-based UUID 본래 목적(결정론적)과 불일치 |

### 1.2 인코딩 불일치

- `TimebasedUuid.*`: `UUID.encodeBase62()` 사용 (BigInteger 경유, `Base62` object)
- `TimebasedUuidGenerator`, `RandomUuidGenerator`, `NamebasedUuidGenerator`: `Url62.encode()` 사용 (ByteBuffer 경유, `Url62` object)
- 두 구현체의 인코딩 결과가 **동일하지 않을 수 있음** (padding/sign 처리 차이)

### 1.3 명명 불명확

- `TimebasedUuid.Epoch` -- UUID v7을 의미하지만 이름만으로 알기 어려움
- `TimebasedUuid.Default` -- UUID v1이지만 "Default"가 버전 정보를 전달하지 않음
- `TimebasedUuid.Reordered` -- UUID v6이지만 명시적이지 않음

### 1.4 커스텀 Random 주입 불가

- `TimebasedUuid` nested objects는 모두 기본 생성자 사용, Random 주입 불가
- `RandomUuidGenerator`만 `Random` 파라미터를 받음
- JUG의 `Generators.timeBasedEpochGenerator(Random)` 오버로드가 있으나 활용 안 됨

---

## 2. 목표 설계

### 2.1 설계 원칙

1. **ULID 패턴 일관성**: `ULID` companion object + `UlidGenerator` 어댑터 구조를 따름
2. **UUID는 값 타입 불필요**: `java.util.UUID`가 이미 값 타입이므로, 생성 전략(Strategy) 통일에 집중
3. **버전 명시적 명명**: V1, V4, V5, V6, V7로 RFC 9562 버전 번호를 직접 사용
4. **인코딩 통일**: `Url62.encode()` 단일 표준
5. **하위 호환**: 기존 API는 `@Deprecated` + `typealias`/위임으로 유지

### 2.2 새 API 구조

```kotlin
package io.bluetape4k.idgenerators.uuid

/**
 * UUID 생성 전략을 통합 제공하는 진입점.
 *
 * UUID는 java.util.UUID가 이미 값 타입이므로,
 * ULID와 달리 생성 전략(Generator) 통일에 집중합니다.
 */
object Uuid {

    /**
     * UUID 생성기 공통 인터페이스.
     * IdGenerator<UUID>를 확장하며 UUID 전용 편의 메서드를 추가합니다.
     */
    interface Generator : IdGenerator<UUID> {
        /** UUID를 생성합니다. nextId()의 별칭입니다. */
        fun nextUUID(): UUID = nextId()

        /** UUID를 Url62 Base62 문자열로 반환합니다. */
        fun nextBase62(): String = Url62.encode(nextId())

        /** 지정한 개수만큼 UUID를 생성합니다. */
        fun nextUUIDs(size: Int): Sequence<UUID> = nextIds(size)

        /** 지정한 개수만큼 Base62 문자열을 생성합니다. */
        fun nextBase62s(size: Int): Sequence<String> =
            generateSequence { nextBase62() }.take(size)
    }

    /** UUID v1: MAC + Gregorian timestamp 기반 */
    object V1 : Generator {
        private val generator by lazy { Generators.timeBasedGenerator() }
        override fun nextId(): UUID = generator.generate()
        override fun nextIdAsString(): String = nextBase62()
    }

    /** UUID v4: 랜덤 기반 (기본 SecureRandom) */
    object V4 : Generator {
        private val generator by lazy { Generators.randomBasedGenerator() }
        override fun nextId(): UUID = generator.generate()
        override fun nextIdAsString(): String = nextBase62()
    }

    /** UUID v5: name-based SHA-1 (결정론적) */
    object V5 : Generator {
        private val generator by lazy { Generators.nameBasedGenerator() }
        override fun nextId(): UUID = generator.generate(UUID.randomUUID().toString())
        override fun nextIdAsString(): String = nextBase62()
    }

    /** UUID v6: 재정렬 timestamp (DB PK 최적화) */
    object V6 : Generator {
        private val generator by lazy { Generators.timeBasedReorderedGenerator() }
        override fun nextId(): UUID = generator.generate()
        override fun nextIdAsString(): String = nextBase62()
    }

    /** UUID v7: Unix epoch timestamp + random (현재 표준, DB PK 최적 권장) */
    object V7 : Generator {
        private val generator by lazy { Generators.timeBasedEpochGenerator() }
        override fun nextId(): UUID = generator.generate()
        override fun nextIdAsString(): String = nextBase62()
    }

    /**
     * 커스텀 Random을 사용하는 UUID v4 생성기를 생성합니다.
     */
    fun random(random: java.util.Random = java.util.Random(System.currentTimeMillis())): Generator =
        RandomGenerator(random)

    /**
     * 커스텀 Random을 사용하는 UUID v7 생성기를 생성합니다.
     */
    fun epochRandom(random: java.util.Random): Generator =
        EpochRandomGenerator(random)

    /**
     * 고정된 name으로 결정론적 UUID v5를 생성하는 생성기입니다.
     * 동일 name 입력 시 항상 동일한 UUID를 반환합니다.
     */
    fun namebased(name: String): Generator = NamebasedGenerator(name)

    // --- 내부 구현 클래스 ---

    private class RandomGenerator(random: java.util.Random) : Generator {
        private val generator = Generators.randomBasedGenerator(random)
        override fun nextId(): UUID = generator.generate()
        override fun nextIdAsString(): String = nextBase62()
    }

    private class EpochRandomGenerator(random: java.util.Random) : Generator {
        private val generator = Generators.timeBasedEpochGenerator(random)
        override fun nextId(): UUID = generator.generate()
        override fun nextIdAsString(): String = nextBase62()
    }

    private class NamebasedGenerator(private val name: String) : Generator {
        private val generator = Generators.nameBasedGenerator()
        override fun nextId(): UUID = generator.generate(name)
        override fun nextIdAsString(): String = nextBase62()
    }
}
```

### 2.3 어댑터 클래스 (IdGenerator 연결)

```kotlin
/**
 * Uuid.Generator를 IdGenerator<UUID>로 사용하는 어댑터.
 * UlidGenerator 패턴과 동일한 구조입니다.
 */
class UuidGenerator(
    private val generator: Uuid.Generator = Uuid.V7,
) : IdGenerator<UUID> by generator
```

### 2.4 하위 호환 처리

```kotlin
// TimebasedUuid.kt -- @Deprecated 추가
@Deprecated("Use Uuid.V1, Uuid.V6, Uuid.V7 instead", ReplaceWith("Uuid"))
object TimebasedUuid {
    @Deprecated("Use Uuid.V1", ReplaceWith("Uuid.V1"))
    object Default : IdGenerator<UUID> by Uuid.V1 {
        override fun nextIdAsString(): String = nextId().encodeBase62() // 기존 동작 유지
    }
    @Deprecated("Use Uuid.V6", ReplaceWith("Uuid.V6"))
    object Reordered : IdGenerator<UUID> by Uuid.V6 {
        override fun nextIdAsString(): String = nextId().encodeBase62()
    }
    @Deprecated("Use Uuid.V7", ReplaceWith("Uuid.V7"))
    object Epoch : IdGenerator<UUID> by Uuid.V7 {
        override fun nextIdAsString(): String = nextId().encodeBase62()
    }
}

// TimebasedUuidGenerator.kt -- @Deprecated
@Deprecated("Use Uuid.V6 or UuidGenerator(Uuid.V6)", ReplaceWith("Uuid.V6"))
class TimebasedUuidGenerator : IdGenerator<UUID> by Uuid.V6 { ... }

// RandomUuidGenerator.kt -- @Deprecated
@Deprecated("Use Uuid.V4 or Uuid.random()", ReplaceWith("Uuid.V4"))
class RandomUuidGenerator(...) { ... }

// NamebasedUuidGenerator.kt -- @Deprecated
@Deprecated("Use Uuid.V5 or Uuid.namebased(name)", ReplaceWith("Uuid.V5"))
class NamebasedUuidGenerator { ... }
```

---

## 3. 인코딩 통일 전략

### 3.1 `encodeBase62()` vs `Url62.encode()` 차이

| 구현 | 경로 | 알고리즘 |
|------|------|----------|
| `UUID.encodeBase62()` | `Base62.encode(this.toBigInt())` | BigInteger -> Base62 변환 |
| `Url62.encode(uuid)` | ByteBuffer(16bytes) -> BigInteger -> Base62 | unsigned 보장, 고정 22자 패딩 |

`Url62.encode()`가 URL-safe하고 고정 길이(22자)를 보장하므로 이를 표준으로 채택합니다.

### 3.2 기존 `TimebasedUuid`의 `encodeBase62()` 호출

`@Deprecated` 처리된 `TimebasedUuid`는 기존 `encodeBase62()` 동작을 유지하여 하위 호환을 보장합니다.
새 `Uuid.*` API는 모두 `Url62.encode()`를 사용합니다.

---

## 4. 외부 사용처 영향 분석

### 4.1 `TimebasedUuid.Epoch` 사용처 (가장 많음)

| 모듈 | 파일 | 용도 |
|------|------|------|
| `data/exposed-core` | `ColumnExtensions.kt` | `timebasedGenerated()` 기본값 |
| `spring-boot3/core` | `StopWatchSupport.kt` | StopWatch ID 기본값 |
| `spring-boot4/core` | `StopWatchSupport.kt` | StopWatch ID 기본값 |
| `utils/jwt` | `KeyChain.kt` | KeyChain ID 기본값 |
| `infra/cache-core` | `AbstractNearJCacheTest.kt` 외 | randomKey 생성 |
| `data/exposed-jdbc-tests` | `DMLTestData.kt` | clientDefault |
| `data/exposed-r2dbc-tests` | `DMLTestData.kt`, `BoardSchema.kt` | clientDefault |
| `aws/aws` | `CustomerRepositoryTest.kt`, `UserRepositoryTest.kt` | 테스트 ID 생성 |
| `examples/coroutines-demo` | `UuidProviderCoroutineContext.kt` | CoroutineContext 예제 |

### 4.2 `TimebasedUuidGenerator` 사용처

| 모듈 | 파일 | 용도 |
|------|------|------|
| `aws/aws` | `DynamoDbEntity.kt` | companion object에서 lazy 초기화 |

### 4.3 `TimebasedUuid.Reordered` 사용처

| 모듈 | 파일 | 용도 |
|------|------|------|
| `utils/idgenerators` | `HashIdsSupportTest.kt` | 테스트에서 ID 생성 |

---

## 5. `NamebasedUuidGenerator` 설계 결정

### 현재 구현의 문제

```kotlin
// 현재: random UUID를 name으로 사용 -> 매번 다른 UUID 생성
// name-based UUID의 본래 목적(동일 입력 = 동일 출력)과 완전히 불일치
private fun nextIdInternal(): UUID {
    return namebasedUuid.generate(randomUuid.generate().toString())
}
```

### 새 설계: 이중 모드

1. **`Uuid.V5` (object)**: 현재 `NamebasedUuidGenerator`의 동작 유지 (random name -> 매번 다른 UUID). 비결정론적이지만 호환성 유지.
2. **`Uuid.namebased(name: String)`**: 고정 name을 받아 결정론적 UUID 생성. name-based UUID의 올바른 사용법.

---

## 6. 테스트 구조 계획

### 현재 테스트 파일

```
test/kotlin/.../uuid/
├── NamebasedUuidGeneratorTest.kt
├── RandomUuidGeneratorTest.kt
├── timebased/
│   ├── AbstractTimebasedUuidTest.kt
│   ├── DefaultTimebasedUuidTest.kt
│   ├── ReorderedTimebaseUuidTest.kt
│   └── EpochTimebasedUuidTest.kt
└── base62/
    ├── AbstractTimebasedUuidBase62Test.kt
    ├── DefaultTimebasedUuidBase62Test.kt
    ├── ReorderedTimebaseUuidBase62Test.kt
    └── EpochTimebasedUuidBase62Test.kt
```

### 새 테스트 구조

```
test/kotlin/.../uuid/
├── UuidGeneratorTest.kt          -- Uuid.V1/V4/V5/V6/V7 통합 테스트
├── UuidGeneratorBase62Test.kt    -- Base62 인코딩 통합 테스트
├── UuidCustomRandomTest.kt      -- Uuid.random(), Uuid.epochRandom() 테스트
├── UuidNamebasedTest.kt          -- Uuid.namebased(name) 결정론적 테스트
├── UuidGeneratorAdapterTest.kt   -- UuidGenerator 어댑터 테스트
├── NamebasedUuidGeneratorTest.kt -- 기존 (유지, @Deprecated 경고 확인)
├── RandomUuidGeneratorTest.kt    -- 기존 (유지)
├── timebased/                    -- 기존 (유지)
└── base62/                       -- 기존 (유지)
```
