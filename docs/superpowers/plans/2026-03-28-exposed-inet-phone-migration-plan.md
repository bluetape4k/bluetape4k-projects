# exposed-inet / exposed-phone -> exposed-core 이관 실행 계획

**날짜**: 2026-03-28
**Spec**: `docs/superpowers/specs/2026-03-28-exposed-inet-phone-migration-design.md`
**소스**: `bluetape4k-experimental/data/exposed-inet/`, `bluetape4k-experimental/data/exposed-phone/`
**대상**: `bluetape4k-projects/data/exposed-core/`

---

## 사전 검증 결과

- **Exposed 버전**: 양쪽 모두 `org.jetbrains.exposed.v1.*` import 패턴 -- 호환성 문제 없음
- **패키지 변경**: `io.bluetape4k.exposed.inet` -> `io.bluetape4k.exposed.core.inet`, `io.bluetape4k.exposed.phone` -> `io.bluetape4k.exposed.core.phone`
- **`libphonenumber`**: projects Libs.kt에 미존재 -- 추가 필요 (`com.googlecode.libphonenumber:libphonenumber:8.13.52`)
- **`testcontainers_postgresql`**: exposed-core에 **이미 포함** -- 추가 불필요
- **`exposed_java_time`**: exposed-phone에서 `api(Libs.exposed_java_time)` 선언되어 있으나, 소스에서 **미사용** -- 이관 시 제외
- **`KLogging()` companion**: `InetColumnTypes.kt`(2곳), `PhoneNumberColumnType.kt`(1곳)에서 로그 호출 없이 선언만 존재 -- 이관 시 제거
- **`configurations { testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get()) }`**: exposed-core에 **이미 설정** -- `compileOnly(Libs.libphonenumber)` 추가만으로 테스트에도 자동 전이, `testRuntimeOnly` 불필요

---

## 태스크 목록

### Task 1: Libs.kt에 `libphonenumber` 상수 추가

- **complexity: low**
- **파일**: `buildSrc/src/main/kotlin/Libs.kt`
- **작업**: `libphonenumber` 상수 추가
  ```kotlin
  // Google libphonenumber -- 전화번호 E.164 정규화 (exposed-core phone column types)
  // https://mvnrepository.com/artifact/com.googlecode.libphonenumber/libphonenumber
  const val libphonenumber = "com.googlecode.libphonenumber:libphonenumber:8.13.52"
  ```
- **위치**: 기존 라이브러리 상수들 사이 적절한 위치
- **검증**: Gradle sync 성공

### Task 2: `exposed-core/build.gradle.kts`에 libphonenumber 의존성 추가

- **complexity: low**
- **파일**: `data/exposed-core/build.gradle.kts`
- **작업**: `compileOnly(Libs.libphonenumber)` 추가 (Phone number column types 전용)
  ```kotlin
  // Phone number column types (사용자가 필요 시 런타임에 추가)
  compileOnly(Libs.libphonenumber)
  ```
- **위치**: 기존 `compileOnly(project(":bluetape4k-crypto"))` 바로 아래
- **참고**: `testRuntimeOnly(Libs.libphonenumber)` **불필요** -- `configurations { testImplementation.get().extendsFrom(compileOnly.get()) }` 패턴으로 자동 전이
- **검증**: Gradle sync 성공

### Task 3: inet 패키지 main 소스 이관 (2개 파일)

- **complexity: medium**
- **대상 디렉토리**: `data/exposed-core/src/main/kotlin/io/bluetape4k/exposed/core/inet/`
- **파일 목록**:
    1. `InetColumnTypes.kt` -- 패키지명 `io.bluetape4k.exposed.core.inet`으로 변경
    2. `InetExtensions.kt` -- 패키지명 변경, import 경로 `io.bluetape4k.exposed.core.inet.*` 반영
- **소스 변경 사항**:
    - 패키지 선언: `io.bluetape4k.exposed.inet` -> `io.bluetape4k.exposed.core.inet`
    - `InetColumnTypes.kt`: `InetAddressColumnType`, `CidrColumnType`의 `companion object: KLogging()` **제거** (로그 호출 없음)
    - `InetColumnTypes.kt`: `import io.bluetape4k.logging.KLogging` 제거
    - `InetExtensions.kt`: `import io.bluetape4k.exposed.inet.InetAddressColumnType` -> `import io.bluetape4k.exposed.core.inet.InetAddressColumnType` (동일 패키지이므로 실제로는 import 불필요할 수 있음)
- **검증**: 파일 존재 + 패키지 선언 확인

### Task 4: phone 패키지 main 소스 이관 (2개 파일)

- **complexity: medium**
- **대상 디렉토리**: `data/exposed-core/src/main/kotlin/io/bluetape4k/exposed/core/phone/`
- **파일 목록**:
    1. `PhoneNumberColumnType.kt` -- 패키지명 `io.bluetape4k.exposed.core.phone`으로 변경
    2. `PhoneNumberExtensions.kt` -- 패키지명 변경, import 경로 반영
- **소스 변경 사항**:
    - 패키지 선언: `io.bluetape4k.exposed.phone` -> `io.bluetape4k.exposed.core.phone`
    - `PhoneNumberColumnType.kt`: `PhoneNumberTransformer`의 `companion object: KLogging()` **제거** (로그 호출 없음)
    - `PhoneNumberColumnType.kt`: `import io.bluetape4k.logging.KLogging` 제거
    - `PhoneNumberExtensions.kt`: import 경로 `io.bluetape4k.exposed.core.phone.*` 반영
- **검증**: 파일 존재 + 패키지 선언 확인

### Task 5: inet 패키지 test 소스 이관 (2개 파일)

- **complexity: medium**
- **대상 디렉토리**: `data/exposed-core/src/test/kotlin/io/bluetape4k/exposed/core/inet/`
- **파일 목록**:
    1. `InetColumnTypeTest.kt` -- 패키지명 변경, H2 + PostgreSQL 다이얼렉트 테스트
    2. `InetPostgresTest.kt` -- 패키지명 변경, PostgreSQL 전용 `<<` 연산자 테스트
- **소스 변경 사항**:
    - 패키지 선언: `io.bluetape4k.exposed.inet` -> `io.bluetape4k.exposed.core.inet`
    - `InetColumnTypeTest.kt`: `import io.bluetape4k.exposed.inet.*` -> `import io.bluetape4k.exposed.core.inet.*` (내부 참조 클래스: `InetAddressColumnType`, `CidrColumnType`, `inetAddress`, `cidr`)
    - `InetPostgresTest.kt`: 동일 패턴 import 변경 (`inetAddress`, `cidr`, `isContainedBy`)
- **검증**: 파일 존재 + 패키지 선언 확인

### Task 6: phone 패키지 test 소스 이관 (1개 파일)

- **complexity: medium**
- **대상 디렉토리**: `data/exposed-core/src/test/kotlin/io/bluetape4k/exposed/core/phone/`
- **파일 목록**:
    1. `PhoneNumberColumnTypeTest.kt` -- 패키지명 변경
- **소스 변경 사항**:
    - 패키지 선언: `io.bluetape4k.exposed.phone` -> `io.bluetape4k.exposed.core.phone`
    - `import io.bluetape4k.exposed.phone.*` -> `import io.bluetape4k.exposed.core.phone.*` (내부 참조: `PhoneNumberTransformer`, `PhoneNumberStringColumnType`, `phoneNumber`, `phoneNumberString`)
    - `ContactTable` 객체: 파일 최상위에 선언되어 있으므로 패키지 변경만으로 충분
- **검증**: 파일 존재 + 패키지 선언 확인

### Task 7: 빌드 검증 (compileKotlin)

- **complexity: medium**
- **명령**: `./gradlew :bluetape4k-exposed-core:compileKotlin :bluetape4k-exposed-core:compileTestKotlin`
- **검증 기준**: 컴파일 성공, 에러 0
- **실패 시 대응**:
    - import 미해결 -> 패키지명 변경 누락 확인 (`.inet` vs `.core.inet`)
    - `KLogging` unresolved -> companion object 제거 누락 확인
    - `PhoneNumberUtil` 미발견 -> Libs.kt `libphonenumber` 추가 및 build.gradle.kts `compileOnly` 확인
    - `requireNotBlank` 미발견 -> `bluetape4k-idgenerators` -> `bluetape4k-core` transitive 의존성 확인

### Task 8: 테스트 실행 확인

- **complexity: high**
- **명령**: `./gradlew :bluetape4k-exposed-core:test`
- **전제 조건**: Docker 실행 중 (Testcontainers PostgreSQL)
- **검증 기준**: 전체 테스트 통과 (기존 테스트 + 신규 inet/phone 테스트)
- **핵심 검증 항목**:
    - `InetColumnTypeTest`: H2 + PostgreSQL에서 IPv4/IPv6/CIDR CRUD
    - `InetPostgresTest`: PostgreSQL 네이티브 INET/CIDR + `<<` 연산자
    - `PhoneNumberColumnTypeTest`: H2(+PostgreSQL)에서 E.164 정규화 + PhoneNumber 객체 변환
    - 기존 `CompressedColumnTypeTest`, `EncryptColumnTest` 등 **회귀 테스트** 통과
- **실패 시 대응**:
    - Testcontainers 연결 실패 -> Docker 상태 확인
    - `AbstractExposedTest` 상속 문제 -> `bluetape4k-exposed-jdbc-tests` 모듈 참조 확인
    - `libphonenumber` ClassNotFoundException -> `configurations.extendsFrom` 패턴 확인

### Task 9: README.md 업데이트

- **complexity: low**
- **파일**: `data/exposed-core/README.md`
- **작업**: inet/phone 컬럼 타입 설명 추가
    - `inet` 패키지: `InetAddressColumnType`, `CidrColumnType`, `InetContainedByOp` 설명
    - `phone` 패키지: `PhoneNumberColumnType`, `PhoneNumberStringColumnType` 설명
    - **libphonenumber opt-in 사용법** 추가: 사용자가 phone 컬럼 타입 사용 시 `implementation("com.googlecode.libphonenumber:libphonenumber:8.13.52")` 추가 필요
    - PostgreSQL 전용 `isContainedBy` 확장함수 사용법
- **검증**: README 내용 정확성 확인

### Task 10: CLAUDE.md 업데이트

- **complexity: low**
- **파일**: `/Users/debop/work/bluetape4k/bluetape4k-projects/CLAUDE.md`
- **작업**: `Architecture > Module Structure > Data Modules (data/)` 섹션의 `exposed-core` 항목 설명 수정
- **현재**:
  ```markdown
  - **exposed-core**: JDBC 불필요한 핵심 기능 — 압축/암호화/직렬화 컬럼 타입, 클라이언트 ID 생성 확장(...)
  ```
- **변경 후**:
  ```markdown
  - **exposed-core**: JDBC 불필요한 핵심 기능 — 압축/암호화/직렬화/INET/PhoneNumber 컬럼 타입, 클라이언트 ID 생성 확장(...); inet 패키지(INET/CIDR, PostgreSQL `<<` 연산자), phone 패키지(E.164 정규화, `compileOnly` libphonenumber)
  ```
- **검증**: CLAUDE.md diff 확인

### Task 11: experimental 모듈 삭제

- **complexity: low**
- **대상**: `bluetape4k-experimental/data/exposed-inet/`, `bluetape4k-experimental/data/exposed-phone/` 디렉토리 전체
- **작업**:
    1. 디렉토리 삭제 (`rm -rf`)
    2. experimental settings.gradle.kts에서 include 제거 (자동 등록 방식이면 디렉토리 삭제만으로 충분)
    3. experimental 저장소에서 커밋: `chore: exposed-inet, exposed-phone 모듈을 bluetape4k-projects로 이관 완료하여 삭제`
- **검증**: experimental 빌드 성공 확인

### Task 12: 최종 빌드 검증

- **complexity: medium**
- **명령**:
  ```bash
  ./gradlew :bluetape4k-exposed-core:build
  ./gradlew detekt
  ```
- **검증 기준**:
    - [ ] `compileKotlin` 성공
    - [ ] 전체 테스트 통과 (기존 + inet + phone)
    - [ ] detekt 에러 없음
    - [ ] Libs.kt에 `libphonenumber` 추가 확인
    - [ ] CLAUDE.md `exposed-core` 항목 업데이트 확인
    - [ ] README.md에 libphonenumber opt-in 사용법 포함 확인

---

## 태스크 의존성 그래프

```
Task 1 (Libs.kt) ──┐
                    ├─> Task 2 (build.gradle.kts) ─┐
                    │                               │
Task 3 (inet main) ────────────────────────────────┤
Task 4 (phone main) ───────────────────────────────┤
Task 5 (inet test) ────────────────────────────────┤
Task 6 (phone test) ───────────────────────────────┤
                                                    │
                                                    ├─> Task 7 (컴파일) -> Task 8 (테스트) -> Task 12 (최종 빌드)
                                                    │
Task 9 (README) ──────── Task 8 이후 권장            │
Task 10 (CLAUDE.md) ──── Task 8 이후 권장            │
Task 11 (삭제) ────────── Task 12 이후               │
```

## 병렬화 가능 그룹

| 그룹           | 태스크                                  | 설명                              |
|--------------|--------------------------------------|---------------------------------|
| A (인프라 준비)   | Task 1 + Task 2                      | 순차 실행 (Task 2는 Task 1에 의존)     |
| B (소스 이관)    | Task 3 + Task 4 + Task 5 + Task 6   | A 완료 후 동시 실행 가능               |
| C (검증)       | Task 7 -> Task 8                     | A+B 완료 후 순차 실행                 |
| D (문서)       | Task 9 + Task 10                     | C 완료 후 동시 실행 가능               |
| E (정리)       | Task 12 -> Task 11                   | C+D 완료 후 순차 실행                 |

---

## Critic 검토 반영 추적표

| Critic 지적 사항 | 반영 태스크 | 반영 내용 |
|----------------|----------|---------|
| `KLogging()` 제거 검토 | Task 3, 4 | 로그 호출 없는 3개 companion object 제거 |
| `testRuntimeOnly(Libs.libphonenumber)` 불필요 | Task 2 | `compileOnly`만 추가, `extendsFrom` 패턴 활용 |
| `exposed_java_time` 미사용 | Task 4 | 이관 시 제외 |
| README에 libphonenumber opt-in 사용법 추가 | Task 9 | opt-in 의존성 추가 안내 포함 |

---

## 위험 요소 및 완화 방안

| 위험 | 확률 | 완화 |
|------|------|------|
| Docker 미실행으로 PostgreSQL 테스트 실패 | 낮음 | Docker 상태 사전 확인, 컴파일만으로 1차 검증 |
| `requireNotBlank` transitive 의존성 누락 | 매우 낮음 | `bluetape4k-idgenerators` -> `bluetape4k-core`가 이미 api 스코프 |
| 기존 exposed-core 테스트 회귀 | 매우 낮음 | 기존 소스 무수정, 의존성 추가만 |
| `InetPostgresTest`에서 `withDb` / `withTables` 패턴 호환성 | 낮음 | `AbstractExposedTest` 동일 사용 패턴 |

---

## 요약

- **총 태스크**: 12개
- **complexity 분포**: high=1 (테스트 실행 검증), medium=5, low=6
- **이관 파일 수**: main 4개 + test 3개 = 7개
- **예상 시간**: 30분 ~ 1시간 (테스트 실행 포함)
- **위험도**: Low (소스 규모 작음, 기존 exposed-core 패턴과 동일, 의존성 추가 최소)
- **핵심 결정**: `KLogging()` 제거, `testRuntimeOnly` 불필요 (`extendsFrom` 활용), `exposed_java_time` 제외
