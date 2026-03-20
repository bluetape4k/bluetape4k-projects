# UUID Generator 리팩토링 구현 계획

**날짜**: 2026-03-20
**Spec 참조**: `docs/superpowers/specs/2026-03-20-uuid-generator-refactoring-design.md`
**모듈**: `utils/idgenerators`
**패키지**: `io.bluetape4k.idgenerators.uuid`

---

## Context

`utils/idgenerators` 모듈의 UUID 생성기가 구조적으로 불일치하고 중복이 있음.
ULID 통합에서 확립된 패턴(`object` 진입점 + nested interface/objects + 어댑터 class)을 UUID에도 적용하여 통일된 API를 제공한다.

## Work Objectives

1. `Uuid` object 기반의 통일된 UUID 생성 API 제공 (V1/V4/V5/V6/V7)
2. 인코딩을 `Url62.encode()`로 통일
3. 기존 클래스를 `@Deprecated`로 하위 호환 유지
4. 외부 모듈 사용처는 이번에 마이그레이션하지 않음 (deprecation 경고만 발생)

## Guardrails

### Must Have
- 기존 `TimebasedUuid`, `TimebasedUuidGenerator`, `RandomUuidGenerator`, `NamebasedUuidGenerator`의 public API가 `@Deprecated`로 유지됨
- 기존 테스트가 모두 통과
- 새 `Uuid.*` API에 대한 테스트 추가
- KDoc 한국어 작성

### Must NOT Have
- 기존 클래스 삭제 (deprecation만)
- 외부 모듈(spring-boot3, exposed-core, jwt 등) 코드 변경
- `encodeBase62()` 확장 함수 자체를 deprecate (다른 곳에서도 사용됨)

---

## Task Flow

```
Task 1 (Uuid.kt 신규) → Task 2 (UuidGenerator.kt 신규) → Task 3 (기존 파일 @Deprecated)
                                                            ↓
                                                     Task 4 (테스트 작성)
                                                            ↓
                                                     Task 5 (빌드 검증)
```

---

## Detailed TODOs

### Task 1: `Uuid` object 신규 작성

**complexity**: medium
**의존성**: 없음
**변경 파일**: 신규 `utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/uuid/Uuid.kt`

#### 내용
- `Uuid` object 작성
  - `Uuid.Generator` interface: `IdGenerator<UUID>` 확장, `nextUUID()`, `nextBase62()`, `nextUUIDs(size)`, `nextBase62s(size)` 기본 구현
  - `Uuid.V1` object: JUG `timeBasedGenerator()`
  - `Uuid.V4` object: JUG `randomBasedGenerator()` (기본 SecureRandom)
  - `Uuid.V5` object: JUG `nameBasedGenerator()` + random UUID를 name으로 (기존 `NamebasedUuidGenerator` 동작 호환)
  - `Uuid.V6` object: JUG `timeBasedReorderedGenerator()`
  - `Uuid.V7` object: JUG `timeBasedEpochGenerator()`
- 팩토리 함수 작성
  - `Uuid.random(random: Random)`: 커스텀 Random을 사용하는 V4 생성기
  - `Uuid.epochRandom(random: Random)`: 커스텀 Random을 사용하는 V7 생성기
  - `Uuid.namebased(name: String)`: 고정 name으로 결정론적 V5 UUID 생성
- 모든 `nextIdAsString()`은 `Url62.encode()` 사용

#### Acceptance Criteria
- [x] `Uuid.V1.nextId()` 호출 시 UUID 반환
- [x] `Uuid.V7.nextBase62()` 호출 시 22자 Base62 문자열 반환
- [x] `Uuid.random(Random(42)).nextId()` 호출 가능
- [x] `Uuid.namebased("test").nextId()` 를 두 번 호출하면 동일 UUID 반환
- [x] `Uuid.V5.nextId()` 는 매번 다른 UUID 반환 (비결정론적, 기존 호환)
- [x] 컴파일 성공

---

### Task 2: `UuidGenerator` 어댑터 클래스 작성

**complexity**: low
**의존성**: Task 1
**변경 파일**: 신규 `utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/uuid/UuidGenerator.kt`

#### 내용
- `UuidGenerator(generator: Uuid.Generator = Uuid.V7) : IdGenerator<UUID>` 작성
- `ULID`의 `UlidGenerator` 패턴과 동일: 팩토리를 받아 `IdGenerator` 인터페이스로 노출
- `nextUUID()` 편의 메서드 추가

#### Acceptance Criteria
- [x] `UuidGenerator()` 기본 생성 시 V7 사용
- [x] `UuidGenerator(Uuid.V1)` 으로 전략 교체 가능
- [x] `IdGenerator<UUID>` 타입으로 사용 가능
- [x] KDoc 작성 완료

---

### Task 3: 기존 파일 `@Deprecated` 처리

**complexity**: medium
**의존성**: Task 1
**변경 파일**:
- `utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/uuid/TimebasedUuid.kt`
- `utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/uuid/TimebasedUuidGenerator.kt`
- `utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/uuid/RandomUuidGenerator.kt`
- `utils/idgenerators/src/main/kotlin/io/bluetape4k/idgenerators/uuid/NamebasedUuidGenerator.kt`

#### 내용
- `TimebasedUuid` object에 `@Deprecated` 추가
  - `Default` -> `ReplaceWith("Uuid.V1")`
  - `Reordered` -> `ReplaceWith("Uuid.V6")`
  - `Epoch` -> `ReplaceWith("Uuid.V7")`
  - 내부 구현은 유지 (기존 `encodeBase62()` 호출 동작 보존, 하위 호환)
- `TimebasedUuidGenerator` class에 `@Deprecated("Use Uuid.V6 or UuidGenerator(Uuid.V6)")` 추가
- `RandomUuidGenerator` class에 `@Deprecated("Use Uuid.V4 or Uuid.random()")` 추가
- `NamebasedUuidGenerator` class에 `@Deprecated("Use Uuid.V5 or Uuid.namebased(name)")` 추가
- `DeprecationLevel.WARNING` 사용 (컴파일 에러 없음)

#### Acceptance Criteria
- [x] 기존 코드에서 `TimebasedUuid.Epoch.nextId()` 호출 시 deprecation warning 발생
- [x] 기존 테스트 모두 통과 (동작 변경 없음)
- [x] `ReplaceWith` 지정으로 IDE 자동 마이그레이션 지원
- [x] 외부 모듈 빌드 에러 없음

---

### Task 4: 새 API 테스트 작성

**complexity**: medium
**의존성**: Task 1, Task 2
**변경 파일**:
- 신규 `utils/idgenerators/src/test/kotlin/io/bluetape4k/idgenerators/uuid/UuidTest.kt`
- 신규 `utils/idgenerators/src/test/kotlin/io/bluetape4k/idgenerators/uuid/UuidGeneratorTest.kt`

#### 내용

**`UuidTest.kt`** -- `Uuid` object 직접 테스트
- V1/V4/V5/V6/V7 각각 `nextId()` 유니크성 검증 (1000개 생성, Set 크기 확인)
- V1/V4/V5/V6/V7 각각 `nextBase62()` 문자열 길이/포맷 검증
- `Uuid.random(Random(42))` 커스텀 Random 동작 확인
- `Uuid.namebased("fixed-name")` 결정론적 UUID 검증 (동일 name -> 동일 UUID)
- `Uuid.V5` 비결정론적 동작 검증 (매번 다른 UUID)
- `nextUUIDs(size)` 시퀀스 크기 검증
- `nextBase62s(size)` 시퀀스 크기 검증

**`UuidGeneratorTest.kt`** -- `UuidGenerator` 어댑터 테스트
- 기본 생성자 -> V7 사용 확인
- 커스텀 전략 주입 (`UuidGenerator(Uuid.V1)`) 동작 확인
- `IdGenerator<UUID>` 인터페이스 호환 확인

#### Acceptance Criteria
- [x] 모든 테스트 통과
- [x] 유니크성, 인코딩 포맷, 결정론적 동작 검증 포함
- [x] JUnit 5 + Kluent assertions 사용

---

### Task 5: 빌드 및 회귀 테스트 검증

**complexity**: low
**의존성**: Task 1, 2, 3, 4
**변경 파일**: 없음 (검증만)

#### 내용
- `./gradlew :bluetape4k-idgenerators:test` 실행 -> 전체 통과 확인
- `./gradlew :bluetape4k-idgenerators:build` 실행 -> 빌드 성공 확인
- deprecation warning 확인 (기존 테스트에서 발생하는지)
- 외부 모듈 중 주요 사용처 빌드 확인:
  - `./gradlew :bluetape4k-exposed-core:build -x test`
  - `./gradlew :bluetape4k-spring-boot3-core:build -x test`

#### Acceptance Criteria
- [x] `bluetape4k-idgenerators` 모듈 테스트 전체 통과
- [x] 외부 모듈 빌드 에러 없음
- [x] deprecation warning만 발생 (error 없음)

---

## Success Criteria

1. `Uuid.V7.nextId()`, `Uuid.V7.nextBase62()` 등 통일된 API 사용 가능
2. `UuidGenerator(Uuid.V7)` 어댑터로 `IdGenerator<UUID>` 호환
3. 기존 4개 클래스 모두 `@Deprecated(WARNING)` + `ReplaceWith` 적용
4. 인코딩이 `Url62.encode()`로 통일 (새 API 한정)
5. 기존 테스트 + 새 테스트 모두 통과
6. 외부 모듈 빌드 에러 없음

---

## 향후 작업 (이번 스코프 제외)

- [ ] 외부 모듈의 `TimebasedUuid.Epoch` 사용처를 `Uuid.V7`로 마이그레이션
- [ ] `ColumnExtensions.kt`의 `timebasedGenerated()` 내부를 `Uuid.V7`로 변경
- [ ] deprecation level을 `WARNING` -> `ERROR` -> 삭제로 단계적 전환
- [ ] `encodeBase62()` vs `Url62.encode()` 결과 차이에 대한 마이그레이션 가이드 작성
