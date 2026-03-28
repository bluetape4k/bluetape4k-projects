# exposed-inet / exposed-phone -> exposed-core 이관 설계 Spec

**날짜**: 2026-03-28
**소스**: `bluetape4k-experimental/data/exposed-inet/`, `bluetape4k-experimental/data/exposed-phone/`
**대상**: `bluetape4k-projects/data/exposed-core/`

---

## 1. 개요

bluetape4k-experimental의 `exposed-inet`(INET/CIDR 컬럼 타입)과 `exposed-phone`(PhoneNumber 컬럼 타입)을
bluetape4k-projects의 `exposed-core` 모듈로 이관한다.

두 모듈 모두 커스텀 컬럼 타입이며, exposed-core의 기존 `compress/`, `encrypt/`, `serializable/` 컬럼 타입 패키지와
동일한 관심사에 속한다. 별도 모듈로 분리할 만큼의 복잡도가 없으므로(각각 소스 2개) exposed-core에 통합한다.

### 1.1 이관 대상 요약

| 모듈 | main 파일 | test 파일 | 핵심 클래스 |
|------|----------|----------|-----------|
| exposed-inet | 2개 | 2개 | `InetAddressColumnType`, `CidrColumnType`, `InetContainedByOp` |
| exposed-phone | 2개 | 1개 | `PhoneNumberTransformer`, `PhoneNumberColumnType`, `PhoneNumberStringColumnType` |

---

## 2. 현재 상태

### 2.1 exposed-core 패키지 구조 (현재)

```
io.bluetape4k.exposed.core/
├── compress/          ← CompressedBinaryColumnType, CompressedBlobColumnType
├── encrypt/           ← 암호화 컬럼 타입
├── serializable/      ← 직렬화 컬럼 타입
├── dao/id/            ← 커스텀 IdTable 확장
├── statements/api/    ← Blob 확장
├── ColumnExtensions.kt
├── ExposedPage.kt
├── HasIdentifier.kt
└── ...
```

### 2.2 exposed-core 의존성 패턴

- 핵심 의존성: `api(Libs.exposed_core)`, `api(project(":bluetape4k-idgenerators"))`
- 선택적 의존성: `compileOnly(project(":bluetape4k-io"))`, `compileOnly(project(":bluetape4k-crypto"))`
- 테스트: `testImplementation(Libs.testcontainers_postgresql)` **이미 포함됨**

### 2.3 experimental 소스 분석

**exposed-inet**:
- `InetAddressColumnType`: PostgreSQL `INET` / 기타 `VARCHAR(45)` fallback
- `CidrColumnType`: PostgreSQL `CIDR` / 기타 `VARCHAR(50)` fallback
- `InetContainedByOp`: PostgreSQL `<<` 연산자 (`ComparisonOp` 상속)
- `Table.inetAddress()`, `Table.cidr()`: 컬럼 등록 확장
- `Column<InetAddress>.isContainedBy()`: PostgreSQL 전용, `check(currentDialect is PostgreSQLDialect)`로 런타임 보호
- 외부 의존성: **없음** (JDK `java.net.InetAddress`만 사용)

**exposed-phone**:
- `PhoneNumberTransformer`: `ColumnTransformer<String, PhoneNumber>` 구현
- `PhoneNumberColumnType`: `ColumnWithTransform<String, PhoneNumber>` — VARCHAR(20) + E.164
- `PhoneNumberStringColumnType`: `ColumnType<String>` — E.164 정규화 저장
- `Table.phoneNumber()`, `Table.phoneNumberString()`: 컬럼 등록 확장
- 외부 의존성: **`com.googlecode.libphonenumber:libphonenumber:8.13.52`** (Google)

---

## 3. 설계 결정

### 3.1 패키지 구조

기존 exposed-core의 패턴(`compress/`, `encrypt/`, `serializable/`)을 따라 기능별 하위 패키지로 배치:

```
io.bluetape4k.exposed.core/
├── inet/              ← InetAddressColumnType, CidrColumnType, InetContainedByOp, 확장함수
├── phone/             ← PhoneNumberTransformer, PhoneNumberColumnType, PhoneNumberStringColumnType, 확장함수
├── compress/          ← (기존)
├── encrypt/           ← (기존)
├── serializable/      ← (기존)
└── ...
```

**결정 근거**: 각 패키지가 하나의 도메인 컬럼 타입을 담당하는 일관된 패턴. `inet`, `phone` 모두 "커스텀 컬럼 타입"이라는 exposed-core의 핵심 관심사에 속한다.

### 3.2 libphonenumber 의존성: `compileOnly`

```kotlin
compileOnly(Libs.libphonenumber)
```

**결정 근거**:
- exposed-core의 기존 패턴과 동일 (`compileOnly(project(":bluetape4k-io"))`, `compileOnly(project(":bluetape4k-crypto"))`)
- PhoneNumber 컬럼 타입을 사용하지 않는 프로젝트에서 불필요한 의존성 전이를 방지
- 사용자가 `phoneNumber()` 컬럼을 쓸 때만 자신의 `build.gradle.kts`에 `implementation(Libs.libphonenumber)` 추가
- `InetAddress` 컬럼은 JDK 표준이므로 추가 의존성 없음

### 3.3 PostgreSQL 전용 코드(`InetContainedByOp`): exposed-core에 포함

**결정: exposed-core에 유지, exposed-postgresql로 분리하지 않는다.**

| 기준 | 분석 |
|------|------|
| 코드 규모 | `InetContainedByOp` 1개 클래스 + `isContainedBy()` 1개 확장함수 (5줄) |
| 런타임 보호 | `check(currentDialect is PostgreSQLDialect)`로 이미 보호됨 |
| 의존성 추가 | 없음 (PostgreSQL 드라이버 불필요, Exposed Core의 dialect 체크만 사용) |
| exposed-postgresql 관심사 | PostGIS/pgvector/tsrange 등 **PostgreSQL 전용 라이브러리에 의존하는** 타입 |
| 선례 | `InetAddressColumnType`/`CidrColumnType` 자체가 이미 `currentDialect` 분기로 PostgreSQL 네이티브 타입과 VARCHAR fallback을 모두 지원 |

`InetContainedByOp`를 exposed-postgresql로 분리하면:
- 사용자가 INET 컬럼 + `<<` 연산자를 쓰려면 exposed-core + exposed-postgresql 2개 모듈 의존 필요
- INET 컬럼 타입과 연산자가 서로 다른 모듈에 분산되어 응집도 저하
- 코드 5줄을 위해 모듈 간 의존성 복잡도 증가

**결론**: INET/CIDR 컬럼 타입과 관련 연산자는 하나의 `inet/` 패키지에 응집시킨다.

### 3.4 experimental 모듈 처리: 삭제

**결정: deprecated 처리 없이 즉시 삭제한다.**

- experimental 모듈은 SNAPSHOT도 배포되지 않으므로 하위 호환성 고려 불필요
- experimental은 projects로 이관하기 위한 스테이징 영역이므로, 이관 완료 후 소스를 유지할 이유 없음
- `settings.gradle.kts`에서 `include` 제거 + 디렉토리 삭제

### 3.5 패키지명 변경

| experimental | projects (이관 후) |
|---|---|
| `io.bluetape4k.exposed.inet` | `io.bluetape4k.exposed.core.inet` |
| `io.bluetape4k.exposed.phone` | `io.bluetape4k.exposed.core.phone` |

기존 exposed-core 패키지 규칙(`io.bluetape4k.exposed.core.*`)을 따른다.

---

## 4. 의존성 변경

### 4.1 exposed-core build.gradle.kts 추가 사항

```kotlin
dependencies {
    // ... 기존 의존성 유지 ...

    // Phone number column types (사용자가 필요 시 런타임에 추가)
    compileOnly(Libs.libphonenumber)

    // Phone number test
    testRuntimeOnly(Libs.libphonenumber)
}
```

### 4.2 Libs.kt 추가 사항 (buildSrc)

```kotlin
// Google libphonenumber -- 전화번호 E.164 정규화 (exposed-core phone column types)
// https://mvnrepository.com/artifact/com.googlecode.libphonenumber/libphonenumber
const val libphonenumber = "com.googlecode.libphonenumber:libphonenumber:8.13.52"
```

### 4.3 의존성 영향도

| 의존성 | 스코프 | 비고 |
|--------|--------|------|
| `libphonenumber:8.13.52` | `compileOnly` | phone 패키지 전용, 사용자 opt-in |
| `java.net.InetAddress` | JDK 표준 | inet 패키지, 추가 의존성 없음 |
| `testcontainers_postgresql` | `testImplementation` | **이미 포함** -- 추가 불필요 |

---

## 5. 마이그레이션 절차

### Phase 1: Libs.kt 업데이트

1. `buildSrc/src/main/kotlin/Libs.kt`에 `libphonenumber` 상수 추가

### Phase 2: 소스 이관

2. `exposed-core/src/main/kotlin/io/bluetape4k/exposed/core/inet/` 디렉토리 생성
   - `InetColumnTypes.kt` 복사, 패키지명 `io.bluetape4k.exposed.core.inet`으로 변경
   - `InetExtensions.kt` 복사, 패키지명 변경
3. `exposed-core/src/main/kotlin/io/bluetape4k/exposed/core/phone/` 디렉토리 생성
   - `PhoneNumberColumnType.kt` 복사, 패키지명 `io.bluetape4k.exposed.core.phone`으로 변경
   - `PhoneNumberExtensions.kt` 복사, 패키지명 변경

### Phase 3: 테스트 이관

4. `exposed-core/src/test/kotlin/io/bluetape4k/exposed/core/inet/` 디렉토리 생성
   - `InetColumnTypeTest.kt` 복사, 패키지명 변경
   - `InetPostgresTest.kt` 복사, 패키지명 변경
5. `exposed-core/src/test/kotlin/io/bluetape4k/exposed/core/phone/` 디렉토리 생성
   - `PhoneNumberColumnTypeTest.kt` 복사, 패키지명 변경

### Phase 4: 빌드 설정

6. `exposed-core/build.gradle.kts`에 `compileOnly(Libs.libphonenumber)` + `testRuntimeOnly(Libs.libphonenumber)` 추가

### Phase 5: 빌드 & 테스트

7. `./gradlew :bluetape4k-exposed-core:test` 실행하여 전체 테스트 통과 확인

### Phase 6: experimental 정리

8. `bluetape4k-experimental/settings.gradle.kts`에서 `exposed-inet`, `exposed-phone` include 제거
9. `bluetape4k-experimental/data/exposed-inet/`, `bluetape4k-experimental/data/exposed-phone/` 디렉토리 삭제

### Phase 7: 문서 업데이트

10. 루트 `CLAUDE.md`의 `exposed-core` 설명에 inet/phone 컬럼 타입 추가
11. `exposed-core` README.md 업데이트 (있다면)

---

## 6. 테스트 전략

### 6.1 exposed-core에 PostgreSQL Testcontainers 테스트 추가: 적합

**근거**:
- exposed-core의 `build.gradle.kts`에 `testcontainers_postgresql`이 **이미 포함**되어 있음
- 기존 `ColumnExtensionsTest` 등도 `@MethodSource(ENABLE_DIALECTS_METHOD)`로 다중 dialect 테스트 수행
- INET/CIDR 타입이 PostgreSQL 네이티브 타입을 사용하므로 PostgreSQL 테스트가 필수

### 6.2 테스트 구조

| 테스트 클래스 | 대상 DB | 검증 내용 |
|-------------|---------|----------|
| `InetColumnTypeTest` | H2 + PostgreSQL | IPv4/IPv6 CRUD, CIDR CRUD, dialect별 sqlType/parameterMarker |
| `InetPostgresTest` | PostgreSQL only | INET/CIDR 네이티브 타입, `<<` 연산자 |
| `PhoneNumberColumnTypeTest` | H2 (+ PostgreSQL) | E.164 정규화, PhoneNumber 객체 변환, 잘못된 번호 거부 |

### 6.3 기존 테스트 인프라 활용

- `AbstractExposedTest` (`bluetape4k-exposed-jdbc-tests`) 상속
- `withTables(testDB, ...)` / `@MethodSource(ENABLE_DIALECTS_METHOD)` 패턴
- `TestDB.H2`, `TestDB.POSTGRESQL` enum 활용

---

## 7. 파일 매핑 (전체)

### main 소스

| experimental 원본 경로 | projects 대상 경로 | 변경 사항 |
|---|---|---|
| `exposed-inet/.../inet/InetColumnTypes.kt` | `exposed-core/.../core/inet/InetColumnTypes.kt` | 패키지명 변경 |
| `exposed-inet/.../inet/InetExtensions.kt` | `exposed-core/.../core/inet/InetExtensions.kt` | 패키지명 변경, import 경로 변경 |
| `exposed-phone/.../phone/PhoneNumberColumnType.kt` | `exposed-core/.../core/phone/PhoneNumberColumnType.kt` | 패키지명 변경 |
| `exposed-phone/.../phone/PhoneNumberExtensions.kt` | `exposed-core/.../core/phone/PhoneNumberExtensions.kt` | 패키지명 변경, import 경로 변경 |

### test 소스

| experimental 원본 경로 | projects 대상 경로 | 변경 사항 |
|---|---|---|
| `exposed-inet/.../inet/InetColumnTypeTest.kt` | `exposed-core/.../core/inet/InetColumnTypeTest.kt` | 패키지명 변경 |
| `exposed-inet/.../inet/InetPostgresTest.kt` | `exposed-core/.../core/inet/InetPostgresTest.kt` | 패키지명 변경 |
| `exposed-phone/.../phone/PhoneNumberColumnTypeTest.kt` | `exposed-core/.../core/phone/PhoneNumberColumnTypeTest.kt` | 패키지명 변경, import 경로 변경 |

---

## 8. 리스크 및 고려사항

| 리스크 | 완화 방안 |
|--------|----------|
| libphonenumber 버전 충돌 | `compileOnly`이므로 사용자가 원하는 버전 지정 가능 |
| PostgreSQL 전용 `<<` 연산자 오용 | `check(currentDialect is PostgreSQLDialect)` 런타임 보호 유지 |
| exposed-core 모듈 비대화 | 소스 4개 파일 추가 (각 100줄 이하)이므로 영향 미미 |
| experimental 사용자 이관 | experimental은 미배포 상태이므로 영향 없음 |
