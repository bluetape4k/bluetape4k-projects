---
manualId: bluetape4k-r2dbc
title: "R2DBC 코루틴 확장"
description: "Spring R2DBC를 Kotlin Coroutines와 Flow로 다루는 SQL 실행, mapping, transaction, connection pool 도구를 설명합니다."
kind: library
group: data
learningOrder: 610
---

# R2DBC 코루틴 확장

## 제공하는 기능 {#problem}

`bluetape4k-r2dbc`는 Spring R2DBC의 `DatabaseClient`와 `R2dbcEntityTemplate` 위에 Kotlin 친화적인 실행·mapping·CRUD·transaction helper를 제공합니다. connection factory와 pool을 구성하는 DSL, typed null binding, 동적 query builder, PostgreSQL JSON converter도 포함합니다.

이 모듈은 R2DBC driver나 완전한 ORM을 대신하지 않습니다. SQL과 transaction 경계는 호출 코드가 결정하며, `QueryBuilder`도 table과 column을 compile time에 검증하는 타입 안전 DSL은 아닙니다. 더 높은 수준의 table DSL과 repository가 필요하면 [R2DBC 생태계 학습 경로](./bluetape4k-r2dbc/ecosystem-paths.md)까지 읽습니다.

## 사용하기 전에 결정할 것 {#when-to-use}

- blocking JDBC를 피해야 하는 호출 경로인지 먼저 확인합니다. 단순히 coroutine 함수 안에서 JDBC를 호출한다고 non-blocking I/O가 되지는 않습니다.
- `ConnectionPool`과 직접 획득한 connection을 누가 닫는지 정합니다.
- Spring이 transaction을 소유하는지, `withTransactionSuspend`로 이 모듈이 경계를 만들지 정합니다.
- row를 domain object로 mapping할지, raw map으로 유지할지 정합니다.
- overload 때 더 기다릴지, bounded pending queue와 acquire timeout으로 빠르게 실패할지 정합니다.
- SQL 조각과 identifier를 어디에서 검증할지 정합니다.

## 의존성 추가 {#coordinates}

사용자는 개별 프로젝트 버전을 맞추지 않고 `bluetape4k-dependencies` BOM 버전만 관리합니다. Spring R2DBC와 실제 database driver는 애플리케이션이 선택합니다.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-r2dbc")

    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    runtimeOnly("org.postgresql:r2dbc-postgresql") // 사용하는 driver로 교체
}
```

## 첫 쿼리 {#quick-start}

Spring Boot가 `DatabaseClient`, `R2dbcEntityTemplate`, `MappingR2dbcConverter`를 제공하면 1.12.1의 auto-configuration이 `R2dbcClient`를 만듭니다. 타입을 지정한 `execute<T>`는 `MappingR2dbcConverter`로 row를 변환합니다.

```kotlin
import io.bluetape4k.r2dbc.R2dbcClient
import io.bluetape4k.r2dbc.core.execute
import kotlinx.coroutines.flow.Flow
import org.springframework.r2dbc.core.flow

data class AccountSummary(
    val accountId: Long,
    val name: String,
)

fun findActiveAccounts(client: R2dbcClient): Flow<AccountSummary> =
    client
        .execute<AccountSummary>(
            "SELECT account_id, name FROM accounts WHERE active = :active"
        )
        .bind("active", true)
        .fetch()
        .flow()
```

Flow는 수집할 때 query를 실행합니다. connection은 R2DBC/Spring publisher 수명주기 안에서 관리되므로, 임의로 block하거나 별도 connection을 섞지 않습니다.

## API 선택 지도 {#api-by-task}

| 필요한 작업 | 시작할 API | 기억할 경계 |
| --- | --- | --- |
| client 묶음 사용 | `R2dbcClient` | 기존 Spring 객체를 보관하는 holder이며 pool을 만들거나 닫지 않습니다. |
| connection option과 pool 구성 | `connectionFactoryOptionsOf`, `r2dbcConnectionPool` | driver dependency와 pool 종료는 애플리케이션이 소유합니다. |
| raw SQL 실행 | `DatabaseClient.execute`, `R2dbcClient.execute` | SQL 문자열과 결과 cardinality를 호출자가 결정합니다. |
| named/indexed parameter binding | `bindMap`, `bindIndexedMap`, `bindNullable` | map의 raw null은 거부되므로 typed null을 사용합니다. |
| 타입 mapping | `execute<T>`, `MappingR2dbcConverter.read<T>` | column과 property가 맞지 않으면 converter 예외가 전파됩니다. |
| raw table CRUD | `insert`, `update`, `delete` | table·column identifier는 검사하지만 raw where SQL은 호출자가 소유합니다. |
| 동적 SQL 구성 | `query`, `queryWithCount`, `QueryBuilder` | 타입 안전 DSL이 아니며 `queryWithCount`는 block을 두 번 실행합니다. |
| suspend transaction | `withTransactionSuspend` | 같은 `DatabaseClient`의 connection factory를 transaction manager에 연결합니다. |
| schema/data 초기화 | `connectionFactoryInitializer`, `resourceDatabasePopulatorOf` | 초기화 시점과 실패 정책을 애플리케이션 lifecycle에 연결합니다. |

## 학습 경로 {#concepts}

각 장은 기능 목록만 나열하지 않고, 실제로 틀리기 쉬운 ownership과 failure 경계를 예제와 함께 설명합니다. 모든 예제는 1.12.1 배포 소스와 대표 테스트를 근거로 하므로 설명을 읽은 뒤 구현과 검증 코드까지 바로 확인할 수 있습니다.

1. [Connection과 pool](./bluetape4k-r2dbc/connections-and-pools.md) — connection option, pool 구성, 종료 책임과 overload 정책을 정합니다.
2. [SQL 실행과 parameter binding](./bluetape4k-r2dbc/sql-and-binding.md) — `DatabaseClient`, typed mapping, named/indexed parameter와 typed null을 다룹니다.
3. [CRUD와 row mapping](./bluetape4k-r2dbc/crud-and-mapping.md) — raw table·entity CRUD, identifier 검증, `Readable`과 PostgreSQL JSON 변환을 설명합니다.
4. [동적 query](./bluetape4k-r2dbc/dynamic-queries.md) — `QueryBuilder`, 중첩 조건, count query와 1.12.1의 검증 범위를 확인합니다.
5. [Transaction과 lifecycle](./bluetape4k-r2dbc/transactions-and-lifecycle.md) — commit·rollback, transaction-aware connection과 schema 초기화를 연결합니다.
6. [R2DBC 생태계 학습 경로](./bluetape4k-r2dbc/ecosystem-paths.md) — 직접 Spring R2DBC에서 이 모듈, Spring coroutine 확장, Exposed R2DBC와 workshop으로 발전하는 순서를 정리합니다.

처음 도입한다면 1→2→3→4→5 순서로 읽습니다. persistence 기술 자체를 고르는 단계라면 6장을 먼저 읽고 필요한 수준으로 돌아옵니다.

## 권장 패턴 {#patterns}

connection pool은 애플리케이션 시작 시 만들고 종료 시 한 번 닫습니다. query parameter는 SQL 문자열과 분리하고, nullable 값은 타입 정보를 가진 `Parameter`로 바인딩합니다. 여러 쓰기가 함께 성공해야 하면 가장 작은 service 경계에 `withTransactionSuspend`를 둡니다. Flow는 끝까지 non-blocking 경로로 유지하고, blocking bridge는 명시적인 adapter 경계 밖으로 새지 않게 합니다.

## 연동 {#integrations}

`bluetape4k-coroutines`와 `r2dbc-pool`은 API dependency입니다. Spring Data R2DBC, Spring Boot auto-configuration, Reactor, Jackson3와 database driver는 optional 또는 `compileOnly`이므로 사용하는 기능에 맞게 애플리케이션 runtime에 추가해야 합니다.

더 많은 Spring Data R2DBC coroutine CRUD 확장이 필요하면 [`bluetape4k-spring-boot-r2dbc`](./bluetape4k-spring-boot-r2dbc.md)를 사용합니다. table·column DSL과 repository abstraction이 필요하면 `bluetape4k-exposed`의 R2DBC 모듈로 올라갑니다.

## 설정 {#configuration}

Spring Boot를 사용하면 `spring.r2dbc.*`가 driver connection 설정을 담당합니다. 직접 구성할 때는 `R2dbcConnectionConfig`와 `R2dbcPoolConfig`에서 driver, SSL, timeout, pool 크기, warmup, pending queue와 validation 방식을 명시합니다.

1.12.1의 `R2dbcClientAutoConfiguration`은 `DatabaseClient` class가 있으면 활성화되며, 사용자 정의 `R2dbcClient` bean이 있어도 자동으로 물러나는 back-off 조건이 없습니다. 같은 타입의 bean을 직접 구성할 때는 자동 설정 충돌을 별도로 피해야 합니다.

## 실패 동작 {#failures}

`bindMap`, `bindIndexedMap`과 map 기반 update에 raw `null`을 넣으면 `IllegalArgumentException`이 발생합니다. nullable 값은 `typedNullParameter<T>()`, `bindNullable<T>()`, `nullValue`를 사용합니다. mapping과 PostgreSQL JSON 변환 실패는 값을 조용히 대체하지 않고 Spring conversion 예외로 전파합니다.

pool 크기와 JMX 설정이 잘못되면 구성 또는 실제 pool 변환 시점에 검증 예외가 발생합니다. transaction block의 예외는 rollback 신호가 되며, cancellation도 일반 실패로 삼켜서는 안 됩니다.

## 운영 {#operations}

active·idle connection, pending acquire, acquire timeout, connection hold time, query p95/p99와 rollback을 함께 관찰합니다. bounded queue의 acquire 실패는 무조건 pool을 키우라는 뜻이 아니라 DB가 감당할 수 없는 부하를 빠르게 드러내는 신호일 수 있습니다. `validationQuery`는 획득마다 DB 왕복을 추가하므로 driver의 LOCAL validation으로 충분한지 먼저 확인합니다.

## 테스트 {#testing}

1.12.1 배포본의 대표 테스트는 H2 기반 SQL·CRUD·transaction과 pool 포화 시나리오를 포함합니다.

```bash
./gradlew :bluetape4k-r2dbc:test --no-build-cache --no-configuration-cache
```

pool benchmark는 일반 회귀 테스트가 아닙니다. PostgreSQL·MySQL benchmark는 Testcontainers를 사용하므로 다른 heavy database suite와 병렬 실행하지 않습니다.

## 워크숍 {#workshops}

모듈 내부에서는 `ExecuteTest`, `InsertTest`, `TransactionSupportTest`가 가장 작은 실행 예제 역할을 합니다. 더 높은 수준의 Kotlin SQL DSL과 repository를 학습하려면 [Exposed R2DBC Workshop](https://github.com/bluetape4k/exposed-r2dbc-workshop)에서 SQL DSL→DDL/DML→coroutine→Spring WebFlux→운영 패턴 순서로 이어갈 수 있습니다.

## 1.12.1 범위 {#limitations}

이 매뉴얼은 `bluetape4k-projects` 1.12.1 태그를 기준으로 합니다. 배포 뒤 추가된 auto-configuration back-off와 `QueryBuilder.limit`·`offset` 사전 검증은 1.11 기능으로 설명하지 않습니다. 1.11에서는 호출자가 `limit > 0`, `offset >= 0`을 확인해야 합니다.

1.12.1 README에 등장하지만 실제 배포 소스에는 없는 `sqlInsert`, `sqlUpdate`, `sqlDelete`, `awaitGeneratedKey`, `awaitSingleAsMap`, `awaitCount`, `awaitExists`, `awaitList`는 예제에서 제외했습니다. 이 매뉴얼은 실제 source와 test에 존재하는 API만 사용합니다.

## Source와 tests {#sources}

- [`R2dbcClient.kt`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/R2dbcClient.kt)
- [`ConnectionPoolSupport.kt`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/pool/ConnectionPoolSupport.kt)
- [`R2dbcPoolConfig.kt`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/pool/R2dbcPoolConfig.kt)
- [`DatabaseClientSupport.kt`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/support/DatabaseClientSupport.kt)
- [`Execute.kt`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/core/Execute.kt)
- [`QueryBuilder.kt`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/query/QueryBuilder.kt)
- [`TransactionSupport.kt`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/support/TransactionSupport.kt)
- [`ExecuteTest.kt`](../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/core/ExecuteTest.kt)
- [`ConnectionPoolSupportTest.kt`](../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/pool/ConnectionPoolSupportTest.kt)
- [`TransactionSupportTest.kt`](../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/support/TransactionSupportTest.kt)

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### Extension Function API 개요

[![Extension Function API 개요](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-r2dbc-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-r2dbc-diagram-01.svg)

_배포본 README: [`data/r2dbc/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/data/r2dbc/README.ko.md)_

### Core API 클래스 구조도

[![Core API 클래스 구조도](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-r2dbc-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-r2dbc-diagram-02.svg)

_배포본 README: [`data/r2dbc/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/data/r2dbc/README.ko.md)_

### JDBC vs R2DBC Comparison 다이어그램

[![JDBC vs R2DBC Comparison 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-r2dbc-diagram-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-r2dbc-diagram-03.svg)

_배포본 README: [`data/r2dbc/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/data/r2dbc/README.ko.md)_

### R2DBC Query Execution 처리 흐름

[![R2DBC Query Execution 처리 흐름](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-r2dbc-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-r2dbc-sequence-01.svg)

_배포본 README: [`data/r2dbc/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/data/r2dbc/README.ko.md)_

<!-- release-readme-diagrams:end -->
