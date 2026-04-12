# bluetape4k-batch benchmark 재구성 설계 스펙 (kotlinx-benchmark)

- 날짜: 2026-04-12
- 작성자: Claude Code
- 모듈: `utils/batch` → `bluetape4k-batch`
- 상태: Draft v1
- 목표: 기존 `measureTimeMillis` 기반 benchmark 테스트를 `kotlinx-benchmark` 기반 측정 체계로 교체하고, 상세 결과를 `utils/batch/docs/benchmark/` 에 표와 Graph로 문서화한다.

---

## 1. 배경과 목적

현재 `utils/batch` benchmark는 JUnit 테스트 안에서 `measureTimeMillis` 로 시간을 재는 방식이다.

- `utils/batch/src/test/kotlin/io/bluetape4k/batch/jdbc/BatchJdbcBenchmarkTest.kt`
- `utils/batch/src/test/kotlin/io/bluetape4k/batch/r2dbc/BatchR2dbcBenchmarkTest.kt`

이 방식은 다음 한계가 있다.

1. JMH 기반 워밍업/반복/통계가 없어 측정 신뢰도가 낮다.
2. benchmark 실행과 결과 문서화가 느슨하게 결합되어 있어 재실행 시 갱신 흐름이 명확하지 않다.
3. 기존 README는 요약 결과는 제공하지만, 조합별 상세 row 결과와 시각화는 부족하다.
4. 기존 데이터 크기 중 `Small=100` 은 너무 작아 실제 배치 작업 비교 기준으로 약하다.

이번 변경은 `kotlinx-benchmark` 로 benchmark 러너를 표준화하고, 아래 조합을 재현 가능한 방식으로 측정하는 것이 목적이다.

- DB: H2 / PostgreSQL / MySQL
- Data Size: 1,000 / 10,000 / 100,000
- Driver: JDBC with Virtual Threads / R2DBC
- Pool Size: 10 / 30 / 60
- Parallel Query(= partition 수): 1 / 4 / 8

조합 수는 측정 대상에 따라 다르다.

- **End-to-end batch job**: `3 × 3 × 2 × 3 × 3 = 162`
- **Seed benchmark**: partition 수가 의미 없으므로 `3 × 3 × 2 × 3 = 54`

---

## 2. 범위

### 2.1 포함 범위

- `utils/batch/build.gradle.kts` 에 `kotlinx-benchmark` 설정 추가
- `src/benchmark/kotlin` source set 도입
- DB/driver별 benchmark 클래스 6개 구성
- `@Param` 기반 `dataSize` / `poolSize` / `parallelism` 조합 실행
- PostgreSQL/MySQL Testcontainers 자동 기동
- `seed` 와 `end-to-end batch job` 을 별도 benchmark 로 측정
- 상세 결과 문서를 `utils/batch/docs/benchmark/*.md` 로 생성
- `utils/batch/README.md`, `utils/batch/README.ko.md` 에 요약 + 상세 링크 추가

### 2.2 제외 범위

- 새 Gradle 모듈 추가
- 다른 모듈 benchmark 체계 통일
- CI 파이프라인 변경
- 외부 DB 직접 접속 모드 지원
- benchmark 결과를 원격 저장소나 외부 대시보드에 업로드하는 기능

---

## 3. 핵심 설계 결정

### 3.1 `kotlinx-benchmark` 사용

레포에는 이미 `kotlinx-benchmark` 플러그인/버전 상수가 준비되어 있다.

- `build.gradle.kts:27`
- `buildSrc/src/main/kotlin/Libs.kt:18`

`kotlinx-benchmark` 문서 기준으로 Kotlin/JVM 프로젝트에 별도 benchmark source set(`benchmark`)을 만들고, custom configuration profile 별로 `<target><Config>Benchmark` task 를 사용한다.

예상 task 이름 예:

- `jvmH2JdbcBenchmark`
- `jvmH2R2dbcBenchmark`
- `jvmPostgresJdbcBenchmark`
- `jvmPostgresR2dbcBenchmark`
- `jvmMysqlJdbcBenchmark`
- `jvmMysqlR2dbcBenchmark`

> 참고: `kotlinx-benchmark` 에서는 custom configuration profile `h2Jdbc` 를 등록하면 JVM target 기준 `jvmH2JdbcBenchmark` task 가 생성된다. 실제 task 이름은 구현 후 검증해서 README 에 반영한다.

### 3.2 source set 분리

Benchmark 코드는 테스트 코드와 분리한다.

```text
utils/batch/
├── build.gradle.kts
├── README.md
├── README.ko.md
├── docs/
│   └── benchmark/
│       ├── README.md
│       ├── README.ko.md
│       ├── h2.md
│       ├── postgresql.md
│       └── mysql.md
└── src/
    ├── main/kotlin/
    ├── test/kotlin/
    └── benchmark/kotlin/io/bluetape4k/batch/benchmark/
        ├── jdbc/
        ├── r2dbc/
        └── support/
```

분리 이유:

- benchmark와 회귀 테스트의 목적이 다르다.
- `kotlinx-benchmark` lifecycle 은 JUnit 테스트 lifecycle 과 다르다.
- 측정 보조 코드와 테스트 보조 코드의 역할을 섞지 않기 위함이다.

### 3.3 DB/driver별 클래스 분리

하나의 benchmark 클래스가 DB/driver까지 모두 바꾸는 구조 대신, DB/driver를 클래스 단위로 고정한다.

#### JDBC
- `H2JdbcBatchBenchmark`
- `PostgreSqlJdbcBatchBenchmark`
- `MySqlJdbcBatchBenchmark`

#### R2DBC
- `H2R2dbcBatchBenchmark`
- `PostgreSqlR2dbcBatchBenchmark`
- `MySqlR2dbcBatchBenchmark`

이 구조를 택하는 이유:

- DB lifecycle 과 driver lifecycle 을 독립적으로 제어할 수 있다.
- Testcontainers 준비 코드를 DB별로 단순화할 수 있다.
- benchmark 클래스는 DB/driver 단위로 분리하되, 결과 문서는 DB 단위로 묶어 JDBC/R2DBC 비교를 한눈에 보여줄 수 있다.
- 사용자 요구인 “DB/driver별 개별 프로파일, smoke 없음” 과 가장 잘 맞는다.

### 3.4 `@Param` 은 조합 변수만 담당

DB 와 driver 는 클래스/프로파일이 고정하고, 측정 대상별로 별도 scenario state 를 둔다.

#### Seed benchmark state
- `dataSize = 1000, 10000, 100000`
- `poolSize = 10, 30, 60`

#### End-to-end batch job state
- `dataSize = 1000, 10000, 100000`
- `poolSize = 10, 30, 60`
- `parallelism = 1, 4, 8`

즉 `parallelism` 은 seed benchmark 에는 적용하지 않는다.

이렇게 하면 조합 수는 다음처럼 분리된다.

- Seed: 클래스당 `9`개 조합 → 전체 `6 × 9 = 54`
- End-to-end: 클래스당 `27`개 조합 → 전체 `6 × 27 = 162`

---

## 4. benchmark 실행 모델

### 4.1 측정 대상 2종 분리

각 DB/driver 클래스는 아래 benchmark 메서드를 가진다.

1. `seedBenchmark(seedState)`
2. `endToEndBatchJobBenchmark(jobState)`

여기서 `seedState` 와 `jobState` 는 서로 다른 `@State` 객체이며, seed 쪽에는 `parallelism` 파라미터를 두지 않는다.

두 값을 섞지 않는 이유는 다음과 같다.

- source 적재(batchInsert) 성능과 실제 배치 처리 성능은 병목이 다르다.
- seed 비용이 큰 DB(MySQL, PostgreSQL)와 job 비용이 큰 드라이버(R2DBC)에서 해석 포인트가 다르다.
- README 요약은 end-to-end 중심으로 쓰되, 상세 문서는 seed도 함께 보여줘야 전체 추세를 설명할 수 있다.

### 4.2 측정 경계

#### 측정 밖
- PostgreSQL/MySQL Testcontainers 기동
- connection pool 생성 (`HikariCP`, `r2dbc-pool`)
- schema 생성 및 dialect 초기화
- benchmark helper 객체 생성

#### seed benchmark 측정 안
- source/target/job tables truncate 완료 상태
- pool/factory 준비 완료 상태

#### seed benchmark 측정 본문
- source table 에 `dataSize` 만큼 적재

#### end-to-end benchmark 측정 안
- source table 에 seed 적재 완료
- target/job execution tables 초기화 완료
- batch job DSL 구성 완료

#### end-to-end benchmark 측정 본문
- 실제 batch job 1회 실행

즉, benchmark 는 “환경 준비 비용”이 아니라 “실제 작업 비용”만 측정한다.

### 4.3 `parallelism` 의 의미

사용자 합의에 따라 `parallelism = 1 / 4 / 8` 은 **동시 job 수**가 아니라 **배치 partition 수**이다.
이 파라미터는 **end-to-end batch job benchmark 에만 적용**된다.

- `1` → sequential batch
- `4`, `8` → key-range 기반 partitioned batch

JDBC/R2DBC 모두 동일한 partition 전략을 쓴다.

구체적으로는:

1. source table 의 `min(id)` / `max(id)` 를 읽는다.
2. `parallelism` 개수만큼 key range 를 분할한다.
3. 각 partition 이 독립 reader/writer/job name 으로 실행된다.
4. 최종적으로 target row 수와 job execution 상태를 검증한다.

### 4.4 JDBC vs R2DBC 공정 비교 기준

| 항목 | JDBC | R2DBC |
|------|------|-------|
| Driver | Exposed JDBC | Exposed R2DBC |
| 동시성 모델 | Virtual Threads | Coroutine + reactive driver |
| Pool | HikariCP | r2dbc-pool |
| Pool size | 10 / 30 / 60 | 10 / 30 / 60 |
| Partition 전략 | 동일 | 동일 |
| Data set | 동일 | 동일 |
| Chunk/Page 설정 | 동일 | 동일 |

---

## 5. 코드 구조

### 5.1 support 패키지

`io.bluetape4k.batch.benchmark.support` 에 공통 보조 코드를 둔다.

#### `SeedScenarioParams`
- `@Param` 선언 보유
- `dataSize`, `poolSize`
- 사람이 읽기 쉬운 label/slug 제공

#### `JobScenarioParams`
- `@Param` 선언 보유
- `dataSize`, `poolSize`, `parallelism`
- 사람이 읽기 쉬운 label/slug 제공

#### `BenchmarkEnvironment`
- DB 준비
- schema reset
- table truncate
- source/target 검증
- resource close

#### `SeedBenchmarkSupport`
- source row 생성
- seed insert 실행
- throughput 계산

#### `BatchJobBenchmarkSupport`
- benchmark job DSL 생성
- sequential/partitioned job 생성
- result 검증

#### `BenchmarkMarkdownExporter`
- raw result 를 읽어 markdown 문서 생성
- 표와 Mermaid graph 를 함께 출력
- README 요약 테이블용 summary 모델도 생성

### 5.2 benchmark 클래스 책임

각 benchmark 클래스는 다음만 담당한다.

- 특정 DB/driver lifecycle 설정
- `@Setup` / `@TearDown`
- `@Benchmark` 메서드 정의
- support 계층 호출

즉, 실제 row 생성, Markdown 출력, throughput 계산 로직은 support 계층으로 밀어 넣는다.

### 5.3 legacy benchmark 처리

기존 `BatchJdbcBenchmarkTest`, `BatchR2dbcBenchmarkTest` 는 바로 삭제하지 않는다.

대신 다음 방향으로 축소한다.

- 이름/주석으로 `legacy` benchmark 임을 명시
- README 는 새 benchmark 체계를 기준으로 갱신
- 새 benchmark 가 충분히 자리 잡기 전까지 회귀 비교 참고 자료로만 유지

즉, benchmark 체계의 기준은 `kotlinx-benchmark` 로 전환하되, 이전 수치와의 차이를 추적할 근거는 남긴다.

---

## 6. 결과 문서화

### 6.1 문서 허브

- `utils/batch/docs/benchmark/README.md`
- `utils/batch/docs/benchmark/README.ko.md`

역할:

- benchmark 문서 인덱스
- DB별 상세 결과 링크
- 실행 환경 설명
- 측정 기준(seed / end-to-end / graph 해석법) 설명

### 6.2 상세 문서

- `utils/batch/docs/benchmark/h2.md`
- `utils/batch/docs/benchmark/postgresql.md`
- `utils/batch/docs/benchmark/mysql.md`

상세 문서는 **DB별로만** 구성한다. 각 문서 안에서 JDBC / R2DBC 결과를 함께 보여주고 비교한다.

각 문서는 다음 섹션을 가진다.

1. 개요
   - DB
   - 실행 일시
   - JDBC benchmark task 이름
   - R2DBC benchmark task 이름
   - 환경 (Apple M4 Pro, Testcontainers 여부 등)
2. 실행 조건
   - chunkSize
   - pageSize
   - dataSize 집합
   - poolSize 집합
   - parallelism 집합
3. Seed benchmark 결과 표
   - JDBC / R2DBC 를 같은 표 안에서 비교 가능하게 배치
4. Seed graph
   - JDBC / R2DBC 비교 중심 그래프
5. End-to-end benchmark 결과 표
   - JDBC / R2DBC 를 같은 표 안에서 비교 가능하게 배치
6. End-to-end graph
   - JDBC / R2DBC 비교 중심 그래프
7. 핵심 해석

### 6.3 상세 결과 표 형식

상세 row 결과는 모든 조합을 빠짐없이 남긴다.

필수 컬럼:

- `Driver`
- `Data Size`
- `Pool Size`
- `Parallelism`
- `Benchmark`
- `Samples`
- `Score`
- `Score Error`
- `Unit`
- `Throughput (rows/s)`

필요 시 아래 추가 컬럼을 둘 수 있다.

- `Mode`
- `Warmups`
- `Iterations`
- `Environment Note`

### 6.4 Graph 형식

사용자 요구에 따라 상세 결과 문서에는 **표와 Graph 가 모두 있어야 한다.**

Graph 는 Markdown 친화성과 저장소 이식성을 위해 **Mermaid** 로 생성한다.

그래프는 **JDBC / R2DBC 비교가 가장 중요하다**는 원칙으로 작성한다. 즉, 동일 DB 문서 안에서 두 driver를 같은 chart에 올려 비교하는 구성을 우선한다.

권장 그래프:

#### Seed
- `dataSize` 축 기준 JDBC vs R2DBC throughput 비교 bar chart
- `parallelism` 축 기준 JDBC vs R2DBC throughput 비교 line chart

#### End-to-end
- `dataSize` 축 기준 JDBC vs R2DBC throughput 비교 bar chart
- `poolSize` 변화 기준 JDBC vs R2DBC throughput 비교 line chart

문서가 과도하게 길어지는 것을 막기 위해, 한 상세 문서당 그래프는 아래 4개를 상한으로 둔다.

- Seed 2개
- End-to-end 2개

### 6.5 README 연결 방식

`utils/batch/README.md`, `utils/batch/README.ko.md` 의 Benchmarks 섹션에는 다음만 남긴다.

- DB별 요약표
- 핵심 결론
- 문서 허브 링크
- H2 / PostgreSQL / MySQL 상세 문서 direct link

즉:

- 빠른 요약: `README*`
- DB별 JDBC/R2DBC 비교 상세 분석: `docs/benchmark/*.md`

---

## 7. Gradle / 실행 설계

### 7.1 benchmark source set

Kotlin/JVM 단일 프로젝트 기준으로 아래 구조를 사용한다.

- `sourceSets { create("benchmark") }`
- `benchmark` compilation 이 `main` 과 연동되도록 설정
- `src/benchmark/kotlin` 에 benchmark 코드를 둔다

### 7.2 configuration profile

smoke profile 은 두지 않는다.

대신 아래 6개 profile 만 둔다.

- `h2Jdbc`
- `h2R2dbc`
- `postgresJdbc`
- `postgresR2dbc`
- `mysqlJdbc`
- `mysqlR2dbc`

각 profile 은 `include("<fully-qualified benchmark class>")` 방식으로 자기 클래스만 실행한다.

### 7.3 실행 예시

README 에 아래 수준의 커맨드를 문서화한다.

```bash
./gradlew :bluetape4k-batch:jvmH2JdbcBenchmark
./gradlew :bluetape4k-batch:jvmPostgresR2dbcBenchmark
./gradlew :bluetape4k-batch:jvmMysqlJdbcBenchmark
```

> Gradle project path 는 `settings.gradle.kts` 의 `includeModules("utils", withBaseDir = false)` 규칙상 `:bluetape4k-batch` 이다. 최종 README 에는 구현 후 실제 검증한 task 이름만 남긴다.

### 7.4 문서 생성 task

benchmark 실행 후 Markdown 문서를 갱신하는 task 를 별도로 둔다.

예시 이름:

- `generateBenchmarkDocs`
- 또는 profile 별 `generateH2JdbcBenchmarkDocs`

권장 방향은 다음과 같다.

1. `jvmPostgresJdbcBenchmark` 실행
2. raw result 생성
3. `generateBenchmarkDocs` 실행
4. `utils/batch/docs/benchmark/*.md` 갱신

이 구조는 benchmark 실행과 문서 생성 책임을 분리해 디버깅을 쉽게 한다.

---

## 8. 검증 전략

### 8.1 완료 기준

이번 설계가 충족되었다고 보기 위한 최소 기준은 다음과 같다.

1. `kotlinx-benchmark` source set 이 실제로 컴파일된다.
2. 최소 1개 benchmark profile 이 실제 실행된다.
3. 가능하면 H2 1개 + 네트워크 DB 1개(PostgreSQL 또는 MySQL)를 실제 실행 확인한다.
4. 상세 Markdown 문서가 생성된다.
5. 상세 Markdown 문서에 표와 Mermaid graph 가 모두 포함된다.
6. `README.md`, `README.ko.md` 에 상세 링크가 연결된다.

### 8.2 작업 후 필수 갱신

프로젝트 지침에 따라 다음 문서를 함께 갱신한다.

- `docs/testlogs/2026-04.md`
- `docs/superpowers/index/2026-04.md`
- `docs/superpowers/INDEX.md`
- spec/plan 작성 후 `wiki-update` 실행

---

## 9. 리스크와 대응

### 리스크 1. 162조합 전체 실행 시간 증가

대응:

- smoke 는 두지 않되, profile 을 DB/driver 단위로 분리한다.
- 사용자가 필요한 축만 선택 실행할 수 있게 한다.
- README 에 “전체 실행 시 오래 걸린다”는 운영 가이드를 남긴다.

### 리스크 2. 문서 생성 로직이 benchmark 코드와 강결합

대응:

- Markdown 생성은 `BenchmarkMarkdownExporter` 로 격리한다.
- benchmark 클래스는 측정만 담당한다.

### 리스크 3. Testcontainers 기동 시간이 결과를 오염

대응:

- 컨테이너는 benchmark 측정 전 단계에서 기동한다.
- 측정 본문에서는 seed/job 실행만 수행한다.

### 리스크 4. graph 가 너무 많아 문서 가독성이 떨어짐

대응:

- 상세 문서별 그래프 수를 상한 4개로 제한한다.
- README 는 요약만 유지한다.

---

## 10. 최종 제안

추천 구현안은 다음과 같다.

1. `utils/batch` 내부에 `kotlinx-benchmark` source set(`src/benchmark/kotlin`) 추가
2. DB/driver별 benchmark 클래스 6개 구성
3. `@Param(dataSize, poolSize, parallelism)` 로 27조합씩 실행
4. PostgreSQL/MySQL 는 Testcontainers 자동 기동
5. `seed` 와 `end-to-end batch job` 을 분리 측정
6. `utils/batch/docs/benchmark/*.md` 에 상세 row 결과 표와 Mermaid graph 생성
7. `utils/batch/README.md`, `utils/batch/README.ko.md` 에 DB별 요약표와 상세 링크 제공

이 설계는 사용자가 요구한 아래 조건을 모두 충족한다.

- `kotlinx-benchmark` 사용
- DB: H2 / PostgreSQL / MySQL
- Data Size: 1,000 / 10,000 / 100,000
- Driver: JDBC with Virtual Threads / R2DBC
- Pool Size: 10 / 30 / 60
- Parallel Query: partition 수 1 / 4 / 8
- smoke 없음
- PostgreSQL/MySQL 는 Testcontainers 자동 기동
- seed / end-to-end 분리 측정
- 상세 결과는 `utils/batch/docs/benchmark/...` 에 표 + Graph 로 문서화
- README 에서 상세 문서로 링크 이동 가능
