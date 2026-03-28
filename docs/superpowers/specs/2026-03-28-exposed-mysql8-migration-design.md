# exposed-mysql8-gis -> exposed-mysql8 이관 설계 Spec

**날짜**: 2026-03-28
**소스**: `bluetape4k-experimental/data/exposed-mysql8-gis/`
**대상**: `bluetape4k-projects/data/exposed-mysql8/`
**참고 패턴**: `bluetape4k-projects/data/exposed-postgresql/`

---

## 1. 이관 범위 분석

### 1.1 이관 대상 소스 파일 (main: 6개, test: 7개)

**main** (`src/main/kotlin/io/bluetape4k/exposed/mysql8/gis/`):

| 파일                      | 역할                                                         | LOC (약) |
|-------------------------|------------------------------------------------------------|---------|
| `MySqlWkbUtils.kt`      | MySQL Internal Geometry Format <-> JTS 변환, `SRID_WGS84` 상수 | 52      |
| `GeoColumnTypes.kt`     | `GeometryColumnType<T>` + 8개 팩토리 함수                        | 120     |
| `GeoExtensions.kt`      | `Table.geoPoint()` 등 8개 테이블 확장 함수                          | 119     |
| `JtsHelpers.kt`         | `wgs84Point()`, `wgs84Polygon()` 등 JTS 헬퍼                  | 87      |
| `SpatialExpressions.kt` | `StContainsOp`, `StDistanceExpr` 등 17개 SQL Expression 클래스  | 417     |
| `SpatialFunctions.kt`   | `Column.stContains()` 등 18개 확장 함수                          | 381     |

**test** (`src/test/kotlin/io/bluetape4k/exposed/mysql8/gis/`):

| 파일                          | 역할                                             |
|-----------------------------|------------------------------------------------|
| `AbstractMySqlGisTest.kt`   | Testcontainers MySQL 8 + 공통 `withGeoTables` 헬퍼 |
| `GeometryColumnTypeTest.kt` | 8가지 geometry 타입 CRUD 테스트                       |
| `MySqlWkbUtilsTest.kt`      | WKB 변환 유틸 단위 테스트                               |
| `SpatialFunctionTest.kt`    | 공간 속성/변환 함수 테스트                                |
| `SpatialMeasurementTest.kt` | 거리/길이/넓이 측정 함수 테스트                             |
| `SpatialRelationTest.kt`    | 9개 관계 함수 테스트                                   |
| `SpikeWritePathTest.kt`     | 쓰기 경로 spike 테스트                                |

**기타**:

- `README.md` — 상세한 API 문서 (381줄)
- `build.gradle.kts` — 빌드 설정

### 1.2 외부 의존성

| 의존성                             | Libs.kt (projects) 존재 여부 | 비고                                     |
|---------------------------------|--------------------------|----------------------------------------|
| `exposed_core`                  | O                        | `exposed_bom` 사용으로 전환                  |
| `exposed_dao`                   | O                        | 사용하지 않음 — 제거 가능                        |
| `exposed_jdbc`                  | O                        | `compileOnly`로 전환                      |
| `jts_core`                      | **X — 추가 필요**            | `org.locationtech.jts:jts-core:1.20.0` |
| `mysql_connector_j`             | O                        |                                        |
| `testcontainers_mysql`          | O                        |                                        |
| `bluetape4k_exposed_core`       | O (project 참조로 전환)       |                                        |
| `bluetape4k_junit5`             | O (project 참조로 전환)       |                                        |
| `bluetape4k_exposed_jdbc_tests` | O (project 참조로 전환)       |                                        |
| `bluetape4k_testcontainers`     | O (project 참조로 전환)       |                                        |

### 1.3 이관하지 않는 것

- experimental 저장소의 다른 모듈
- experimental 저장소의 settings.gradle.kts / buildSrc 변경 (삭제만)

---

## 2. 패키지명 결정

**결론: `io.bluetape4k.exposed.mysql8.gis` 유지**

근거:

- exposed-postgresql 패턴: `io.bluetape4k.exposed.postgresql.postgis` (DB이름.기능명)
- mysql8 + gis 는 동일한 네이밍 컨벤션: DB이름 + 기능 서브패키지
- 향후 `mysql8` 아래에 다른 기능 (예: `fulltext`, `json`) 추가 가능성 고려
- 기존 코드의 import 변경 없음 -> 마이그레이션 비용 최소화

디렉토리 구조:

```
data/exposed-mysql8/
├── build.gradle.kts
├── README.md
└── src/
    ├── main/kotlin/io/bluetape4k/exposed/mysql8/gis/
    │   ├── GeoColumnTypes.kt
    │   ├── GeoExtensions.kt
    │   ├── JtsHelpers.kt
    │   ├── MySqlWkbUtils.kt
    │   ├── SpatialExpressions.kt
    │   └── SpatialFunctions.kt
    └── test/kotlin/io/bluetape4k/exposed/mysql8/gis/
        ├── AbstractMySqlGisTest.kt
        ├── GeometryColumnTypeTest.kt
        ├── MySqlWkbUtilsTest.kt
        ├── SpatialFunctionTest.kt
        ├── SpatialMeasurementTest.kt
        ├── SpatialRelationTest.kt
        └── SpikeWritePathTest.kt
```

---

## 3. build.gradle.kts 변경 사항

### 3.1 현재 (experimental)

```kotlin
plugins { kotlin("jvm") }
dependencies {
    api(Libs.exposed_core)
    api(Libs.exposed_dao)
    api(Libs.exposed_jdbc)
    api(Libs.bluetape4k_exposed_core)
    api(Libs.jts_core)

    testImplementation(Libs.bluetape4k_junit5)
    testImplementation(Libs.bluetape4k_exposed_jdbc_tests)
    testImplementation(Libs.mysql_connector_j)
    testImplementation(Libs.bluetape4k_testcontainers)
    testImplementation(Libs.testcontainers)
    testImplementation(Libs.testcontainers_mysql)
}
```

### 3.2 변경 후 (projects, exposed-postgresql 패턴 적용)

```kotlin
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // Exposed
    implementation(platform(Libs.exposed_bom))
    api(project(":bluetape4k-exposed-core"))
    compileOnly(Libs.exposed_jdbc)

    // Logging
    implementation(project(":bluetape4k-logging"))

    // JTS (공간 데이터 처리 — 사용자가 런타임에 추가)
    api(Libs.jts_core)

    // Database Drivers
    compileOnly(Libs.mysql_connector_j)

    // Testing
    testImplementation(project(":bluetape4k-exposed-jdbc-tests"))
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.testcontainers_junit_jupiter)
    testImplementation(Libs.testcontainers_mysql)

    testRuntimeOnly(Libs.mysql_connector_j)
    testRuntimeOnly(Libs.hikaricp)
}
```

### 3.3 주요 변경 포인트

| 항목                               | experimental             | projects                                     |
|----------------------------------|--------------------------|----------------------------------------------|
| Exposed 의존성                      | 개별 `api(Libs.exposed_*)` | `platform(Libs.exposed_bom)` + `compileOnly` |
| `exposed_dao`                    | `api`                    | **제거** (소스코드에서 미사용)                          |
| `exposed_jdbc`                   | `api`                    | `compileOnly` (사용자가 런타임에 추가)                 |
| bluetape4k 모듈                    | `Libs.*` (artifact 참조)   | `project(":...")` (프로젝트 참조)                  |
| mysql_connector_j                | `testImplementation`     | `compileOnly` + `testRuntimeOnly`            |
| `bluetape4k-logging`             | 없음                       | `implementation` 추가                          |
| `testImplementation extendsFrom` | 없음                       | `compileOnly`, `runtimeOnly` 확장              |
| `hikaricp`                       | 없음                       | `testRuntimeOnly` 추가                         |
| `testcontainers_junit_jupiter`   | 없음 (`testcontainers` 사용) | 추가                                           |

---

## 4. Libs.kt 변경 사항

### 4.1 추가 필요한 상수

```kotlin
// buildSrc/src/main/kotlin/Libs.kt 에 추가
// https://mvnrepository.com/artifact/org.locationtech.jts/jts-core
const val jts_core = "org.locationtech.jts:jts-core:1.20.0"
```

### 4.2 기존 상수 확인 (이미 존재)

- `mysql_connector_j` = `"com.mysql:mysql-connector-j:9.6.0"` -- OK
- `testcontainers_mysql` = `testcontainersModule("mysql")` -- OK
- `exposed_bom` -- OK
- `exposed_jdbc` -- OK
- `hikaricp` -- OK
- `testcontainers_junit_jupiter` -- OK

---

## 5. settings.gradle.kts 자동 등록

`includeModules("data", withBaseDir = false)` 로직에 의해:

- `data/exposed-mysql8/` 디렉토리에 `build.gradle.kts`가 존재하면
- `:bluetape4k-exposed-mysql8`로 자동 등록됨
- **별도 settings.gradle.kts 수정 불필요**

---

## 6. 소스코드 수정 사항

### 6.1 패키지 변경 없음

패키지명 `io.bluetape4k.exposed.mysql8.gis` 유지 -> 소스코드 변경 없이 파일 복사만으로 충분.

### 6.2 테스트 코드 확인 필요

`AbstractMySqlGisTest.kt`에서 사용하는 의존성:

- `io.bluetape4k.exposed.tests.AbstractExposedTest` -> `bluetape4k-exposed-jdbc-tests` 모듈에 존재 (확인됨)
- `io.bluetape4k.testcontainers.database.MySQL8Server` -> `bluetape4k-testcontainers` 모듈에 존재 (확인됨)

---

## 7. experimental 삭제 방법

1. `bluetape4k-experimental/data/exposed-mysql8-gis/` 디렉토리 전체 삭제
2. experimental의 `settings.gradle.kts`에서 해당 모듈 include 제거 (자동 등록이면 디렉토리 삭제로 충분)
3. experimental 저장소에서 커밋: `chore: exposed-mysql8-gis 모듈을 bluetape4k-projects로 이관 완료하여 삭제`

---

## 8. CLAUDE.md 업데이트

`Architecture > Module Structure > Data Modules (data/)` 섹션에 추가:

```markdown
- **exposed-mysql8**: MySQL 8.0 전용 Exposed 확장 — GIS 공간 데이터(POINT/POLYGON/LINESTRING 등 8종), JTS 기반 Geometry 컬럼 타입, ST_Contains/ST_Distance 등 18개 공간 함수; MySQL Internal Format WKB 변환
```

위치: `exposed-postgresql` 항목 바로 아래.

---

## 9. README.md 업데이트

experimental의 README.md를 그대로 복사 후, 의존성 섹션만 수정:

```markdown
## 의존성

`build.gradle.kts`:
```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-exposed-mysql8:$bluetape4kVersion")
    // JTS Core (transitively included)
    // MySQL Connector
    runtimeOnly("com.mysql:mysql-connector-j:9.6.0")
}
```

---

## 10. 태스크 목록

| #  | 태스크                            | Complexity | 설명                                                      |
|----|--------------------------------|------------|---------------------------------------------------------|
| 1  | Libs.kt에 `jts_core` 상수 추가      | **S**      | 1줄 추가                                                   |
| 2  | `data/exposed-mysql8/` 디렉토리 생성 | **S**      | mkdir                                                   |
| 3  | `build.gradle.kts` 작성          | **M**      | exposed-postgresql 패턴 적용, 의존성 재구성                       |
| 4  | main 소스 6개 파일 복사               | **S**      | 변경 없이 그대로 복사                                            |
| 5  | test 소스 7개 파일 복사               | **S**      | 변경 없이 그대로 복사                                            |
| 6  | README.md 복사 + 의존성 섹션 수정       | **S**      | 모듈명/의존성 경로만 변경                                          |
| 7  | Gradle sync + 빌드 확인            | **M**      | `./gradlew :bluetape4k-exposed-mysql8:build`            |
| 8  | 테스트 실행 확인                      | **M**      | `./gradlew :bluetape4k-exposed-mysql8:test` (Docker 필요) |
| 9  | CLAUDE.md 업데이트                 | **S**      | Data Modules 섹션에 1줄 추가                                  |
| 10 | experimental 모듈 삭제             | **S**      | 디렉토리 삭제 + 커밋                                            |

**총 예상 시간**: 30분 ~ 1시간 (테스트 실행 포함)
**위험도**: Low (패키지 변경 없음, 의존성만 재구성)

---

## 11. 검증 체크리스트

- [ ] `./gradlew :bluetape4k-exposed-mysql8:compileKotlin` 성공
- [ ] `./gradlew :bluetape4k-exposed-mysql8:test` 전체 통과 (Docker 실행 환경 필요)
- [ ] `./gradlew detekt` 에러 없음
- [ ] Libs.kt에 `jts_core` 추가 확인
- [ ] CLAUDE.md `exposed-mysql8` 항목 추가 확인
- [ ] experimental 저장소에서 모듈 삭제 확인
