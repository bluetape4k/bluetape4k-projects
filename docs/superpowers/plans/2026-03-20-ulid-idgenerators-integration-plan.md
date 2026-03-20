# ULID idgenerators 통합 구현 계획

**날짜**: 2026-03-20
**설계 Spec**: `docs/superpowers/specs/2026-03-20-ulid-idgenerators-integration-design.md`
**대상 모듈**: `utils/idgenerators` (`bluetape4k-idgenerators`)
**소스**: `bluetape4k-experimental/utils/ulid/`

---

## 태스크 개요

| # | 태스크 | Complexity | 병렬 가능 | 의존성 |
|---|--------|------------|-----------|--------|
| 1 | Internal 소스 마이그레이션 | low | - | 없음 |
| 2 | 공개 API 소스 마이그레이션 | low | - | T1 |
| 3 | UlidGenerator 구현 | medium | - | T2 |
| 4 | 테스트 마이그레이션 | low | T1,T2 완료 후 | T2 |
| 5 | UlidGenerator 신규 테스트 | medium | T4와 병렬 가능 | T3 |
| 6 | 빌드 검증 및 정리 | low | - | T4, T5 |

---

## 태스크 상세

### T1. Internal 소스 마이그레이션 `complexity: low`

**설명**: `bluetape4k-experimental/utils/ulid/src/main/kotlin/io/bluetape4k/ulid/internal/` 전체를 `idgenerators` 모듈로 복사하고 패키지명을 변경한다.

**의존성**: 없음

**작업 내용**:
1. 디렉토리 생성: `utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/ulid/internal/`
2. 8개 파일 복사 + 패키지 변경 (`io.bluetape4k.ulid.internal` -> `io.bluetape4k.idgenerators.ulid.internal`)
3. import 경로 수정 (`io.bluetape4k.ulid.ULID` -> `io.bluetape4k.idgenerators.ulid.ULID`)

**변경 파일**:
| 원본 (experimental) | 대상 (idgenerators) |
|---|---|
| `internal/Constants.kt` | `ulid/internal/Constants.kt` |
| `internal/Time.kt` | `ulid/internal/Time.kt` |
| `internal/ByteArraySupport.kt` | `ulid/internal/ByteArraySupport.kt` |
| `internal/Crockford.kt` | `ulid/internal/Crockford.kt` |
| `internal/ULIDValue.kt` | `ulid/internal/ULIDValue.kt` |
| `internal/ULIDFactory.kt` | `ulid/internal/ULIDFactory.kt` |
| `internal/ULIDMonotonic.kt` | `ulid/internal/ULIDMonotonic.kt` |
| `internal/ULIDStatefulMonotonic.kt` | `ulid/internal/ULIDStatefulMonotonic.kt` |

**수용 기준**:
- 모든 internal 파일의 패키지가 `io.bluetape4k.idgenerators.ulid.internal`
- 컴파일 에러 없음 (T2 완료 후 확인)

---

### T2. 공개 API 소스 마이그레이션 `complexity: low`

**설명**: ULID 인터페이스와 UUID 변환 확장 함수를 마이그레이션한다.

**의존성**: T1

**작업 내용**:
1. 3개 파일 복사 + 패키지 변경 (`io.bluetape4k.ulid` -> `io.bluetape4k.idgenerators.ulid`)
2. import 경로 수정 (internal 패키지 참조)

**변경 파일**:
| 원본 (experimental) | 대상 (idgenerators) |
|---|---|
| `ULID.kt` | `ulid/ULID.kt` |
| `JavaUUIDSupport.kt` | `ulid/JavaUUIDSupport.kt` |
| `KotlinUuidSupport.kt` | `ulid/KotlinUuidSupport.kt` |

**수용 기준**:
- `ULID` 인터페이스가 `io.bluetape4k.idgenerators.ulid` 패키지에 위치
- `ULID.Companion`이 `Factory`를 delegate하여 `ULID.nextULID()`, `ULID.parseULID()` 등 직접 호출 가능
- `./gradlew :bluetape4k-idgenerators:compileKotlin` 성공

---

### T3. UlidGenerator 구현 `complexity: medium`

**설명**: `IdGenerator<String>`을 구현하는 `UlidGenerator` 클래스를 신규 작성한다.

**의존성**: T2

**작업 내용**:
1. `UlidGenerator.kt` 신규 작성
2. Ksuid 패턴 참고: `object`가 아닌 `class`로 구현 (factory 주입 가능)
3. 내부적으로 `StatefulMonotonic` 사용 (monotonic 기본)
4. KDoc 한국어 작성

**신규 파일**:
- `utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/ulid/UlidGenerator.kt`

**핵심 설계**:
```kotlin
class UlidGenerator(
    factory: ULID.Factory = ULID,
) : IdGenerator<String> {

    private val monotonic: ULID.StatefulMonotonic = ULID.statefulMonotonic(factory)

    override fun nextId(): String = monotonic.nextULID().toString()

    override fun nextIdAsString(): String = nextId()

    fun nextULID(): ULID = monotonic.nextULID()
}
```

**수용 기준**:
- `UlidGenerator`가 `IdGenerator<String>` 인터페이스 구현
- `nextId()`가 26자 Crockford Base32 문자열 반환
- `nextULID()`로 `ULID` 값 객체 직접 획득 가능
- 기본 생성자로 즉시 사용 가능 (`UlidGenerator()`)

---

### T4. 테스트 마이그레이션 `complexity: low`

**설명**: experimental 테스트를 복사하고 패키지명을 변경한다.

**의존성**: T2

**작업 내용**:
1. 디렉토리 생성: `utils/idgenerators/src/test/kotlin/io/bluetape4k/idgenerators/ulid/`
2. 10개 테스트 파일 복사 + 패키지 변경
3. 테스트 유틸리티(MockRandom, TestConstants) 복사
4. import 경로 수정

**변경 파일**:
| 원본 (experimental) | 대상 (idgenerators) |
|---|---|
| `AbstractULIDTest.kt` | `ulid/AbstractULIDTest.kt` |
| `ULIDTest.kt` | `ulid/ULIDTest.kt` |
| `ULIDFactoryTest.kt` | `ulid/ULIDFactoryTest.kt` |
| `ULIDMonotonicTest.kt` | `ulid/ULIDMonotonicTest.kt` |
| `ULIDStatefulMonotonicTest.kt` | `ulid/ULIDStatefulMonotonicTest.kt` |
| `ULIDConcurrencyTest.kt` | `ulid/ULIDConcurrencyTest.kt` |
| `JavaUUIDSupportTest.kt` | `ulid/JavaUUIDSupportTest.kt` |
| `KotlinUuidSupportTest.kt` | `ulid/KotlinUuidSupportTest.kt` |
| `internal/CrockfordTest.kt` | `ulid/internal/CrockfordTest.kt` |
| `utils/MockRandom.kt` | `ulid/utils/MockRandom.kt` |
| `utils/TestConstants.kt` | `ulid/utils/TestConstants.kt` |

**수용 기준**:
- 모든 마이그레이션 테스트가 통과
- 패키지명이 `io.bluetape4k.idgenerators.ulid`로 변경됨

---

### T5. UlidGenerator 신규 테스트 `complexity: medium`

**설명**: `UlidGenerator`의 `IdGenerator<String>` 계약 준수와 동시성을 검증하는 테스트를 신규 작성한다.

**의존성**: T3 (T4와 병렬 실행 가능)

**신규 파일**:
- `utils/idgenerators/src/test/kotlin/io/bluetape4k/idgenerators/ulid/UlidGeneratorTest.kt`
- `utils/idgenerators/src/test/kotlin/io/bluetape4k/idgenerators/ulid/UlidGeneratorConcurrencyTest.kt`

**UlidGeneratorTest 검증 항목**:
- `nextId()` 반환값이 26자 문자열
- `nextId()` 반환값이 Crockford Base32 문자셋 (`[0-9A-HJKMNP-TV-Z]`)만 포함
- `nextIds(N)` 반환 시퀀스 크기 == N
- `nextIdsAsString(N)` 반환 시퀀스 크기 == N
- 연속 호출 시 정렬 순서 보장 (monotonic)
- `nextULID()` 반환 ULID의 timestamp가 현재 시간 부근

**UlidGeneratorConcurrencyTest 검증 항목**:
- `MultithreadingTester`로 10,000회 생성 시 중복 없음
- `StructuredTaskScopeTester`로 Virtual Thread 동시성 검증
- `SuspendedJobTester`로 Coroutine 동시성 검증

**수용 기준**:
- 두 테스트 파일 모두 통과
- 동시성 테스트에서 중복 ID 없음

---

### T6. 빌드 검증 및 정리 `complexity: low`

**설명**: 전체 빌드를 실행하고 build.gradle.kts 변경 필요 여부를 확인한다.

**의존성**: T4, T5

**작업 내용**:
1. `./gradlew :bluetape4k-idgenerators:test` 실행
2. `./gradlew :bluetape4k-idgenerators:detekt` 실행
3. build.gradle.kts 변경 필요 여부 확인 (atomicfu 사용 시 플러그인 확인)
4. 불필요한 import 정리

**수용 기준**:
- `./gradlew :bluetape4k-idgenerators:test` 전체 통과
- `./gradlew :bluetape4k-idgenerators:detekt` 통과
- build.gradle.kts 변경이 필요하면 최소 범위로 수정

---

## 실행 순서 다이어그램

```
T1 (internal 소스) ──> T2 (공개 API) ──> T3 (UlidGenerator)
                                    │              │
                                    v              v
                              T4 (테스트 마이그레이션)  T5 (신규 테스트)
                                    │              │
                                    └──────┬───────┘
                                           v
                                     T6 (빌드 검증)
```

**병렬 실행 가능**: T4와 T5 (T2, T3 각각 완료 후)

---

## 예상 변경 파일 요약

### 신규 파일 (소스): 12개
```
utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/ulid/
├── ULID.kt
├── UlidGenerator.kt              [신규 작성]
├── JavaUUIDSupport.kt
├── KotlinUuidSupport.kt
└── internal/
    ├── Constants.kt
    ├── Time.kt
    ├── ByteArraySupport.kt
    ├── Crockford.kt
    ├── ULIDValue.kt
    ├── ULIDFactory.kt
    ├── ULIDMonotonic.kt
    └── ULIDStatefulMonotonic.kt
```

### 신규 파일 (테스트): 13개
```
utils/idgenerators/src/test/kotlin/io/bluetape4k/idgenerators/ulid/
├── AbstractULIDTest.kt
├── ULIDTest.kt
├── ULIDFactoryTest.kt
├── ULIDMonotonicTest.kt
├── ULIDStatefulMonotonicTest.kt
├── ULIDConcurrencyTest.kt
├── UlidGeneratorTest.kt          [신규 작성]
├── UlidGeneratorConcurrencyTest.kt [신규 작성]
├── JavaUUIDSupportTest.kt
├── KotlinUuidSupportTest.kt
├── internal/
│   └── CrockfordTest.kt
└── utils/
    ├── MockRandom.kt
    └── TestConstants.kt
```

### 기존 파일 변경: 0~1개
- `utils/idgenerators/build.gradle.kts` — atomicfu 의존성 필요 시에만 변경 (ULIDStatefulMonotonic이 atomicfu 사용)

---

## 리스크 및 주의사항

1. **atomicfu 의존성**: `ULIDStatefulMonotonic`이 `kotlinx-atomicfu`를 사용한다. 루트 `build.gradle.kts`에 atomicfu 플러그인이 이미 적용되어 있는지 확인 필요. 적용되어 있다면 추가 변경 불필요.

2. **Crockford 확장 함수**: `internal/Crockford.kt`의 `CharArray.write()` 확장이 `ULIDValue.toString()`에서 사용된다. internal 가시성이 같은 모듈 내에서 정상 동작하는지 확인.

3. **ExperimentalUuidApi**: `KotlinUuidSupport.kt`가 `@OptIn(ExperimentalUuidApi::class)`를 사용한다. Kotlin 2.3에서 stable로 전환되었을 수 있으므로 확인 필요.
