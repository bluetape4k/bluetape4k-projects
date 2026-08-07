---
manualId: bluetape4k-jdbc
title: "JDBC와 SQL 확장"
description: "JDBC의 연결·statement·ResultSet·트랜잭션 수명주기를 유지하면서 Kotlin 코드의 반복을 줄이는 방법을 설명합니다."
kind: library
group: data
learningOrder: 600
---

# JDBC와 SQL 확장

## 제공하는 기능 {#problem}

`bluetape4k-jdbc`는 표준 JDBC 위에 Kotlin 확장 함수를 더합니다. `DataSource`에서 연결을 빌리고 닫는 범위, prepared statement 실행, `ResultSet` 변환, 트랜잭션 상태 복원을 짧은 코드로 표현할 수 있습니다. SQL과 연결 풀, 데이터베이스 드라이버의 동작을 감추는 ORM은 아닙니다.

이 모듈이 유용한 경우는 SQL을 직접 제어하면서 JDBC 자원 수명주기를 반복해서 작성하고 싶지 않을 때입니다. 반대로 테이블·컬럼을 타입 안전한 DSL로 모델링하거나 엔티티 생명주기를 관리해야 한다면 [다음 기술 선택](./bluetape4k-jdbc/ecosystem-paths.md)까지 읽고 Exposed나 Hibernate를 검토합니다.

## 사용하기 전에 결정할 것 {#when-to-use}

- `DataSource`와 connection pool을 누가 만들고 닫는지 정합니다.
- 트랜잭션 경계를 service에서 소유할지, 이미 사용 중인 Spring transaction manager에 맡길지 정합니다.
- 결과를 블록 안에서 모두 소비할지, 열린 `ResultSet`에 기대는 지연 sequence가 정말 필요한지 정합니다.
- SQL 값은 문자열 보간 대신 prepared statement parameter로 분리합니다.
- 대량 쓰기는 한 번에 담을 행 수와 JDBC batch 크기를 운영 환경에 맞게 제한합니다.

## 의존성 추가 {#coordinates}

사용자는 개별 라이브러리 버전 대신 중앙 BOM 버전만 관리합니다. 데이터베이스 드라이버와 HikariCP 같은 pool 구현은 애플리케이션이 별도로 선택합니다.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-jdbc")

    runtimeOnly("org.postgresql:postgresql") // 사용하는 driver로 교체
}
```

## 첫 쿼리 {#quick-start}

`withConnect`는 `DataSource`에서 얻은 connection을 블록이 끝날 때 닫습니다. `executeQuery`는 prepared statement와 `ResultSet`도 같은 범위에서 닫습니다.

```kotlin
import io.bluetape4k.jdbc.sql.executeQuery
import io.bluetape4k.jdbc.sql.mapSingle
import javax.sql.DataSource

data class AccountSummary(
    val id: Long,
    val name: String,
)

fun findAccount(dataSource: DataSource, id: Long): AccountSummary =
    dataSource.executeQuery(
        "SELECT id, name FROM accounts WHERE id = ?",
        id,
    ) { rs ->
        rs.mapSingle { row ->
            AccountSummary(
                id = row.getLong("id"),
                name = row.getString("name"),
            )
        }
    }
```

결과가 없으면 `mapSingle`은 `NoSuchElementException`, 두 행 이상이면 `IllegalStateException`을 던집니다. 0~1행 계약이라면 `mapFirst`를 사용합니다.

## API 선택 지도 {#api-by-task}

| 필요한 작업 | 시작할 API | 기억할 경계 |
| --- | --- | --- |
| connection을 빌려 한 작업 수행 | `DataSource.withConnect` | 블록이 끝나면 connection을 닫습니다. |
| 단순 SQL 직접 실행 | `runQuery`, `executeUpdate`, `executeInsert` | SQL 문자열과 resource 범위를 호출 코드가 소유합니다. |
| parameter가 있는 쿼리·갱신 | `Connection.executeQuery`, `executeUpdate` | 내부에서 prepared statement를 만들고 닫습니다. |
| 여러 parameter row 쓰기 | `executeBatch`, `executeLargeBatch` | 모든 row의 parameter 수가 같아야 합니다. |
| SQL NULL을 Kotlin nullable로 읽기 | `getIntOrNull`, `getLongOrNull` 등 | JDBC getter 직후 `wasNull()`을 확인합니다. |
| 행을 collection이나 객체로 변환 | `mapFirst`, `mapSingle`, `toList`, `extract` | 함수마다 cursor를 어디까지 소비하는지 확인합니다. |
| commit·rollback과 상태 복원 | `withTransaction`, `withReadOnlyTransaction` | 기존 auto-commit, isolation, read-only 상태를 복원합니다. |
| HikariCP 구성 | `hikariConfigOf`, `hikariDataSourceOf` | HikariCP는 `compileOnly`이므로 애플리케이션이 dependency와 종료를 소유합니다. |

## 학습 경로 {#concepts}

각 장은 기능 목록보다 실제 사용 중 틀리기 쉬운 경계를 중심으로 설명합니다. 코드 예제 뒤에는 1.12.1 배포 소스와 대표 테스트를 연결했으므로, 설명을 읽은 뒤 곧바로 구현과 검증 근거까지 내려갈 수 있습니다.

1. [Connection과 DataSource 수명주기](./bluetape4k-jdbc/connection-lifecycle.md) — 연결을 누가 닫는지, Hikari helper를 어디까지 맡길지 정합니다.
2. [Prepared statement와 batch](./bluetape4k-jdbc/statements-batches.md) — parameter binding, 생성 key, batch row 계약을 다룹니다.
3. [ResultSet 읽기와 mapping](./bluetape4k-jdbc/resultset-mapping.md) — SQL NULL, 단일 행, collection, cursor 이동과 지연 sequence를 구분합니다.
4. [트랜잭션과 상태 복원](./bluetape4k-jdbc/transactions.md) — commit·rollback뿐 아니라 재사용 connection의 원래 상태를 복원하는 과정을 설명합니다.
5. [JDBC 다음의 기술 선택](./bluetape4k-jdbc/ecosystem-paths.md) — 직접 JDBC를 유지할지, Exposed나 Hibernate로 올라갈지 선택 기준을 정리합니다.

처음 도입한다면 1→2→3→4 순서로 읽고, 프로젝트의 persistence 계층을 정하는 단계라면 5장을 먼저 읽어도 됩니다.

## 권장 패턴 {#patterns}

자원은 만든 계층이 닫고, 조회 결과는 가능한 한 `ResultSet`이 열린 블록 안에서 값 객체로 바꿉니다. SQL 값은 parameter로 바인딩하고, 트랜잭션은 여러 statement가 함께 성공하거나 실패해야 하는 가장 작은 service 경계에 둡니다. connection pool을 쓴다면 pool 크기, timeout과 종료 시점은 애플리케이션 설정으로 남깁니다.

## 연동 {#integrations}

모듈은 `bluetape4k-core`를 API dependency로 사용합니다. HikariCP, Tomcat JDBC, Agroal과 Spring JDBC는 optional `compileOnly` 연동입니다. helper가 보인다고 해서 해당 구현이 runtime에 자동으로 들어오는 것은 아닙니다.

Spring이 트랜잭션을 관리하는 애플리케이션에서는 `withTransaction`을 중첩해 새 경계를 만들기 전에 현재 transaction manager와 connection binding을 확인합니다. 직접 JDBC 경계와 framework 경계를 한 호출 흐름에 섞으면 commit 책임이 모호해집니다.

## 설정 {#configuration}

JDBC URL, driver, 사용자 이름, credential, pool 크기, connection·statement timeout과 isolation 기본값은 애플리케이션이 관리합니다. 이 모듈은 별도 설정 파일이나 process-wide 기본값을 설치하지 않습니다. HikariCP를 사용한다면 `hikariDataSourceOf` lambda 또는 애플리케이션 framework의 datasource 설정에서 값을 명시합니다.

## 실패 동작 {#failures}

JDBC driver가 던진 `SQLException`은 기본적으로 호출자에게 전파됩니다. 정확히 한 행을 요구하는 mapping은 결과 개수가 맞지 않으면 별도 예외를 던집니다. 트랜잭션 block이나 commit이 실패하면 rollback을 시도하며, rollback·상태 복원 실패는 원래 예외의 suppressed exception으로 붙습니다.

## 운영 {#operations}

pool 포화, connection 획득 시간, query latency, rollback 수, batch 크기와 database timeout을 함께 관찰합니다. 지연 `Sequence`나 JDBC 자원을 block 밖으로 반환하지 않고, slow query와 pool timeout을 같은 operation context에서 추적합니다.

## 테스트 {#testing}

1.12.1 배포본의 대표 테스트는 H2 기반 API 테스트와 MySQL Testcontainers 경로를 포함합니다. Docker가 필요한 테스트가 있으므로 다른 heavy integration suite와 병렬 실행하지 않습니다.

```bash
./gradlew :bluetape4k-jdbc:test --no-build-cache --no-configuration-cache
```

## 워크숍 {#workshops}

전용 workshop 저장소는 아직 등록되지 않았습니다. 대신 각 장에 연결한 `JdbcTemplateTest`, `TransactionExtensionsTest`, `ResultSetMappingExtensionsTest`가 실행 가능한 예제 역할을 합니다. 작은 H2 schema로 조회→mapping→transaction rollback→batch 순서로 따라 하면 이 모듈의 핵심 경계를 한 번에 확인할 수 있습니다.

## 1.12.1 범위 {#limitations}

이 매뉴얼은 `bluetape4k-projects` 1.12.1 태그의 소스를 기준으로 합니다. 배포 뒤 `develop`에 추가된 API는 1.11 문서에 포함하지 않습니다. 이 모듈은 schema migration, query DSL, entity dirty checking, coroutine-friendly non-blocking database driver도 제공하지 않습니다.

## Source와 tests {#sources}

- [`DataSourceExtensions.kt`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/DataSourceExtensions.kt)
- [`PreparedStatementExtensions.kt`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/PreparedStatementExtensions.kt)
- [`ResultSetExtensions.kt`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/ResultSetExtensions.kt)
- [`ResultSetMappingExtensions.kt`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/ResultSetMappingExtensions.kt)
- [`TransactionExtensions.kt`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/TransactionExtensions.kt)
- [`JdbcTemplateTest.kt`](../../../../data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/sql/JdbcTemplateTest.kt)
- [`TransactionExtensionsTest.kt`](../../../../data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/sql/TransactionExtensionsTest.kt)
- [`ResultSetMappingExtensionsTest.kt`](../../../../data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/sql/ResultSetMappingExtensionsTest.kt)

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### Extension Function API 개요

[![Extension Function API 개요](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-jdbc-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-jdbc-diagram-01.svg)

_배포본 README: [`data/jdbc/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/data/jdbc/README.ko.md)_

### Core API 구조도

[![Core API 구조도](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-jdbc-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-jdbc-diagram-02.svg)

_배포본 README: [`data/jdbc/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/data/jdbc/README.ko.md)_

### JDBC Query Execution 처리 흐름

[![JDBC Query Execution 처리 흐름](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-jdbc-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-jdbc-sequence-01.svg)

_배포본 README: [`data/jdbc/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/data/jdbc/README.ko.md)_

<!-- release-readme-diagrams:end -->
