# exposed-mysql8-gis -> exposed-mysql8 이관 실행 계획

**날짜**: 2026-03-28
**Spec**: `docs/superpowers/specs/2026-03-28-exposed-mysql8-migration-design.md`
**소스**: `bluetape4k-experimental/data/exposed-mysql8-gis/`
**대상**: `bluetape4k-projects/data/exposed-mysql8/`

---

## 사전 검증 결과

- **Exposed 버전**: 양쪽 모두 `1.1.1` -- 호환성 문제 없음
- **Import 패턴**: 양쪽 모두 `org.jetbrains.exposed.v1.*` -- 소스 변경 불필요
- **`jts_core`**: projects Libs.kt에 미존재 -- 추가 필요 (`org.locationtech.jts:jts-core:1.20.0`)
- **settings.gradle.kts**: `includeModules("data", withBaseDir = false)` 자동 등록 -- 별도 수정 불필요
- **패키지명**: `io.bluetape4k.exposed.mysql8.gis` 유지 (exposed-postgresql의 `postgis` 패턴과 일관)

---

## 태스크 목록

### Task 1: Libs.kt에 `jts_core` 상수 추가

- **complexity: low**
- **파일**: `buildSrc/src/main/kotlin/Libs.kt`
- **작업**: `jts_core` 상수 1줄 추가 (experimental과 동일한 값)
  ```kotlin
  // https://mvnrepository.com/artifact/org.locationtech.jts/jts-core
  const val jts_core = "org.locationtech.jts:jts-core:1.20.0"
  ```
- **위치**: 기존 라이브러리 상수들 사이 적절한 위치 (알파벳순 또는 GIS 관련 라이브러리 근처)
- **검증**: `./gradlew buildSrc:build` 또는 Gradle sync 성공

### Task 2: `data/exposed-mysql8/` 디렉토리 구조 생성

- **complexity: low**
- **작업**: 디렉토리 생성
  ```
  data/exposed-mysql8/
  └── src/
      ├── main/kotlin/io/bluetape4k/exposed/mysql8/gis/
      ├── test/kotlin/io/bluetape4k/exposed/mysql8/gis/
      └── test/resources/
  ```
- **검증**: 디렉토리 존재 확인

### Task 3: `build.gradle.kts` 작성

- **complexity: medium**
- **파일**: `data/exposed-mysql8/build.gradle.kts`
- **작업**: exposed-postgresql 패턴을 기반으로 MySQL 8 전용 빌드 설정 작성
- **주요 변경점** (experimental 대비):
    - `exposed_dao` 제거 (소스에서 미사용)
    - `exposed_jdbc` -> `compileOnly` (JDBC는 사용자가 런타임에 추가)
    - bluetape4k 모듈 -> `project(":...")` 참조로 전환
    - `mysql_connector_j` -> `compileOnly` + `testRuntimeOnly`
    - `bluetape4k-logging` `implementation` 추가
    - `testcontainers_junit_jupiter` 추가
    - `hikaricp` `testRuntimeOnly` 추가
    - `configurations { testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get()) }` 추가
    - `exposed_java_time` -> `compileOnly` 추가 (exposed-postgresql 패턴과 일관성 위해 포함, 현재 미사용이므로 주석 명시)
- **검증**: Gradle sync 성공

### Task 4: main 소스 6개 파일 복사

- **complexity: low**
- **소스**: `bluetape4k-experimental/data/exposed-mysql8-gis/src/main/kotlin/io/bluetape4k/exposed/mysql8/gis/`
- **대상**: `bluetape4k-projects/data/exposed-mysql8/src/main/kotlin/io/bluetape4k/exposed/mysql8/gis/`
- **파일 목록**:
    1. `MySqlWkbUtils.kt`
    2. `GeoColumnTypes.kt`
    3. `GeoExtensions.kt`
    4. `JtsHelpers.kt`
    5. `SpatialExpressions.kt`
    6. `SpatialFunctions.kt`
- **소스 변경**: 없음 (패키지명 동일, import 호환)
- **검증**: 파일 존재 + 패키지 선언 확인

### Task 5: test 소스 7개 파일 + resources 복사

- **complexity: low**
- **소스**: `bluetape4k-experimental/data/exposed-mysql8-gis/src/test/`
- **대상**: `bluetape4k-projects/data/exposed-mysql8/src/test/`
- **kotlin 파일 목록** (→ `test/kotlin/io/bluetape4k/exposed/mysql8/gis/`):
    1. `AbstractMySqlGisTest.kt`
    2. `GeometryColumnTypeTest.kt`
    3. `MySqlWkbUtilsTest.kt`
    4. `SpatialFunctionTest.kt`
    5. `SpatialMeasurementTest.kt`
    6. `SpatialRelationTest.kt`
    7. `SpikeWritePathTest.kt` (spike 테스트이나 그대로 포함)
- **resources 파일** (→ `test/resources/`):
    - `junit-platform.properties` (테스트 lifecycle=per_class, 병렬 설정)
    - `logback-test.xml` (DEBUG 로깅 설정)
    - **참고**: `exposed-postgresql`은 이 파일들이 없이 `bluetape4k-exposed-jdbc-tests`의 설정을 상속. 동일 패턴 적용 가능하나, 일관성 위해 복사 포함
- **소스 변경**: 없음
- **검증**: 파일 존재 확인

### Task 6: 빌드 검증 (compileKotlin)

- **complexity: medium**
- **명령**: `./gradlew :bluetape4k-exposed-mysql8:compileKotlin :bluetape4k-exposed-mysql8:compileTestKotlin`
- **검증 기준**: 컴파일 성공, 경고만 허용 (에러 0)
- **실패 시 대응**:
    - import 미해결 -> Libs.kt 의존성 누락 확인
    - 클래스 미발견 -> exposed-core 프로젝트 참조 확인

### Task 7: 테스트 실행 확인

- **complexity: medium**
- **명령**: `./gradlew :bluetape4k-exposed-mysql8:test`
- **전제 조건**: Docker 실행 중 (Testcontainers MySQL 8)
- **검증 기준**: 전체 테스트 통과
- **실패 시 대응**:
    - Testcontainers 연결 실패 -> Docker 상태 확인
    - AbstractMySqlGisTest 의존성 -> `bluetape4k-exposed-jdbc-tests`, `bluetape4k-testcontainers` 모듈 참조 확인

### Task 8: README.md 작성

- **complexity: low**
- **파일**: `data/exposed-mysql8/README.md`
- **작업**: experimental의 README.md 복사 후 전체 모듈명/artifact 참조 수정
    - 모듈명: `exposed-mysql8-gis` → `bluetape4k-exposed-mysql8`
    - artifact 이름: `exposed-mysql8-gis` → `bluetape4k-exposed-mysql8`
    - 의존성 예시 코드를 projects 기준으로 업데이트 (`project(":bluetape4k-exposed-mysql8")`)
    - 저장소 참조가 experimental을 가리키는 경우 bluetape4k-projects로 변경
- **검증**: README 내 `exposed-mysql8-gis` 문자열 잔류 없는지 확인

### Task 9: CLAUDE.md 업데이트

- **complexity: low**
- **파일**: `/Users/debop/work/bluetape4k/bluetape4k-projects/CLAUDE.md`
- **작업**: `Architecture > Module Structure > Data Modules (data/)` 섹션에 추가
  ```markdown
  - **exposed-mysql8**: MySQL 8.0 전용 Exposed 확장 — GIS 공간 데이터(POINT/POLYGON/LINESTRING 등 8종), JTS 기반 Geometry 컬럼 타입, ST_Contains/ST_Distance 등 18개 공간 함수; MySQL Internal Format WKB 변환
  ```
- **위치**: `exposed-postgresql` 항목 바로 아래
- **검증**: CLAUDE.md diff 확인

### Task 10: experimental 모듈 삭제

- **complexity: low**
- **대상**: `bluetape4k-experimental/data/exposed-mysql8-gis/` 디렉토리 전체
- **작업**:
    1. 디렉토리 삭제 (`rm -rf`)
    2. experimental 저장소에서 커밋: `chore: exposed-mysql8-gis 모듈을 bluetape4k-projects로 이관 완료하여 삭제`
- **주의**: experimental settings.gradle.kts가 자동 등록 방식이면 디렉토리 삭제만으로 충분
- **검증**: experimental 빌드 성공 확인

### Task 11: 최종 빌드 검증

- **complexity: medium**
- **명령**:
  ```bash
  ./gradlew :bluetape4k-exposed-mysql8:build
  ./gradlew detekt
  ```
- **검증 기준**:
    - [ ] `compileKotlin` 성공
    - [ ] 전체 테스트 통과
    - [ ] detekt 에러 없음
    - [ ] Libs.kt에 `jts_core` 추가 확인
    - [ ] CLAUDE.md `exposed-mysql8` 항목 추가 확인

---

## 태스크 의존성 그래프

```
Task 1 (Libs.kt) ─┐
                   ├─> Task 3 (build.gradle.kts) ─┐
Task 2 (디렉토리) ─┘                               │
                                                   ├─> Task 6 (컴파일 검증) -> Task 7 (테스트) -> Task 11 (최종 빌드)
Task 4 (main 복사) ────────────────────────────────┘
Task 5 (test 복사) ────────────────────────────────┘

Task 8 (README) ─────── 독립 (Task 2 이후 가능)
Task 9 (CLAUDE.md) ──── 독립 (Task 7 이후 권장)
Task 10 (삭제) ─────── Task 11 이후
```

## 병렬화 가능 그룹

| 그룹         | 태스크                      | 설명              |
|------------|--------------------------|-----------------|
| A (인프라 준비) | Task 1 + Task 2          | 동시 실행 가능        |
| B (파일 작성)  | Task 3 + Task 4 + Task 5 | A 완료 후 동시 실행 가능 |
| C (검증)     | Task 6 -> Task 7         | 순차 실행           |
| D (문서)     | Task 8 + Task 9          | B 완료 후 동시 실행 가능 |
| E (정리)     | Task 11 -> Task 10       | C+D 완료 후 순차 실행 (최종 빌드 검증 후 삭제)  |

---

## 위험 요소 및 완화 방안

| 위험                                      | 확률    | 완화                                      |
|-----------------------------------------|-------|-----------------------------------------|
| Docker 미실행으로 테스트 실패                     | 낮음    | Docker 상태 사전 확인, 컴파일만으로 1차 검증           |
| Testcontainers MySQL 이미지 pull 지연        | 낮음    | 이미 로컬에 캐시되어 있을 가능성 높음                   |
| exposed-core/exposed-jdbc-tests API 불일치 | 매우 낮음 | 양쪽 Exposed 1.1.1 동일, import 패턴 동일 확인 완료 |

---

## 요약

- **총 태스크**: 11개
- **complexity 분포**: high=0, medium=4, low=7
- **예상 시간**: 30분 ~ 1시간 (테스트 실행 포함)
- **위험도**: Low (패키지 변경 없음, Exposed 버전 동일, 의존성만 재구성)
- **핵심 결정**: `exposed_java_time` compileOnly 포함 (일관성), `SpikeWritePathTest.kt` 포함
