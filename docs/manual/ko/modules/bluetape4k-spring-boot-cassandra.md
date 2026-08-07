---
manualId: bluetape4k-spring-boot-cassandra
title: "Spring Data Cassandra 코루틴 지원"
description: "Spring Data Cassandra의 reactive·async API를 Kotlin coroutine과 Flow로 사용하고, 옵션·모델·스키마 작업을 단순화합니다."
kind: library
group: spring
learningOrder: 910
---

# Spring Data Cassandra 코루틴 지원

## 제공하는 기능 {#problem}

`bluetape4k-spring-boot-cassandra`는 Spring Data Cassandra의 reactive·async API를 Kotlin coroutine과 `Flow`로 연결하는 라이브러리입니다. `ReactiveSession`, `ReactiveCassandraOperations`, `AsyncCassandraOperations`, 저수준 CQL operations에 suspend·Flow 확장을 제공하며, 옵션 DSL과 배치 입력, `Persistable`·감사 모델 기반 클래스, 매핑 메타데이터 기반 스키마 생성 도구도 포함합니다.

이 모듈은 Spring Boot starter나 auto-configuration 모듈이 아닙니다. `CqlSession`, keyspace, contact point, authentication, driver profile, `CassandraTemplate`, repository, health indicator와 metric exporter는 Spring Boot·Spring Data Cassandra와 애플리케이션이 구성합니다. 이 모듈은 이미 구성된 객체를 받아 호출 방식을 Kotlin답게 바꿉니다.

## 사용하기 전에 결정할 것 {#when-to-use}

- Spring Data의 entity mapping과 template/repository를 사용할 때 이 모듈을 선택합니다. DataStax Java Driver를 직접 다루기만 한다면 [`bluetape4k-cassandra`](./bluetape4k-cassandra.md)가 더 작은 경계입니다.
- reactive 경로는 `Publisher`를 `Flow`나 suspend 함수로 바꿉니다. async 경로는 `CompletableFuture.await()`를 사용합니다. 한 서비스에서 두 경로를 무분별하게 섞지 않습니다.
- `Flow` 배치 확장은 입력을 `toList()`로 전부 모은 뒤 Spring Data batch에 넘깁니다. 무한 스트림이나 매우 큰 입력에는 맞지 않습니다.
- `SchemaGenerator`는 편의 도구이지 마이그레이션 시스템이 아닙니다. 운영 스키마 변경 이력과 롤백은 별도 도구가 맡아야 합니다.
- `AbstractCassandraAuditable.isNew()`는 ID가 아니라 `createdAt` 존재 여부로 판단합니다. 감사 기능을 실제로 활성화하지 않으면 기존 ID가 있어도 신규 엔티티로 보일 수 있습니다.

## 의존성 추가 {#coordinates}

사용자는 개별 Spring Data, Cassandra Driver 또는 bluetape4k 라이브러리 버전을 맞추지 않고 `bluetape4k-dependencies` BOM 버전만 관리합니다.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-spring-boot-cassandra")
}
```

이 artifact는 Spring Data Cassandra starter를 구현 의존성으로 포함합니다. 다만 접속 정보와 session 생성 정책은 애플리케이션 설정에 남습니다. DataStax mapper runtime과 Micrometer driver 연동은 `compileOnly`이므로 해당 기능을 쓰는 애플리케이션이 runtime 의존성을 추가해야 합니다.

## 첫 사용 예제 {#quick-start}

Spring Boot가 만든 `ReactiveCassandraOperations`를 주입받아 결과 스트림은 `Flow`, 단건 결과는 suspend 함수로 읽습니다.

```kotlin
class UserReader(
    private val operations: ReactiveCassandraOperations,
) {
    fun findAll(): Flow<User> =
        operations.selectAsFlow<User>("SELECT * FROM users")

    suspend fun findOrNull(id: UUID): User? =
        operations.selectOneOrNullByIdSuspending<User>(id)
}
```

`selectAsFlow`는 cold `Flow`입니다. 실제 CQL 실행과 row mapping 오류는 반환 시점이 아니라 `collect` 시점에 발생합니다. 단건이 없을 수 있다면 `selectOneOrNull...` 계열을 선택해야 합니다.

## 작업별 API {#api-by-task}

| 필요한 작업 | API | 중요한 경계 |
| --- | --- | --- |
| reactive 결과 스트리밍 | `selectAsFlow`, `queryForFlow`, `queryForRowsFlow` | subscription과 오류는 `Flow.collect` 시점에 발생합니다. |
| reactive 단건·쓰기 | `selectOneSuspending`, `insertSuspending`, `updateSuspending`, `deleteSuspending` | `awaitSingle` 계열은 빈 publisher를 허용하지 않는 경우가 있습니다. |
| driver session 호출 | `ReactiveSession.executeSuspending`, `prepareSuspending` | session을 만들거나 닫지 않으며 전달된 statement 옵션을 그대로 사용합니다. |
| async template 호출 | `AsyncCassandraOperations.*Suspending` | `CompletableFuture.await()`로 기다리며 Spring Data 예외를 그대로 전파합니다. |
| 저수준 CQL mapping | `AsyncCqlOperations.querySuspending`, `ReactiveCqlOperations.queryForFlow` | row mapper·extractor와 bind marker 타입은 호출자가 책임집니다. |
| 옵션 생성 | `queryOptions`, `insertOptions`, `updateOptions`, `writeOptions`, `deleteOptions` | Spring Data builder 검증과 의미를 바꾸지 않습니다. |
| Flow 배치 입력 | `insertFlow`, `updateFlow`, `deleteFlow` | 전체 입력을 메모리에 수집합니다. 실행은 후속 batch `execute()` 단계입니다. |
| 모델 기반 클래스 | `AbstractCassandraPersistable`, `AbstractCassandraAuditable` | ID·감사 시각에 따른 `isNew`, equality 규칙을 먼저 정합니다. |
| 조건 DSL | `Criteria.eq` | Spring Data의 `Criteria.is(value)` 별칭입니다. |
| 스키마 보조 | `SchemaGenerator` | 현재 keyspace metadata를 보고 UDT·table 생성 또는 truncate를 수행합니다. |

## 학습 경로 {#concepts}

각 장은 1.12.1 배포 소스와 테스트에서 확인한 동작을 설명합니다. API 목록에 그치지 않고 session 소유권, cold stream, 빈 결과, 메모리 사용, 스키마 변경 책임을 함께 다룹니다.

1. [구성과 객체 소유권](./bluetape4k-spring-boot-cassandra/configuration-and-ownership.md) — auto-configuration이 아닌 범위와 `CqlSession`·template·repository 책임을 구분합니다.
2. [Reactive operations와 coroutine](./bluetape4k-spring-boot-cassandra/reactive-coroutine-operations.md) — `Publisher`를 `Flow`·suspend로 바꿀 때의 실행 시점과 빈 결과를 설명합니다.
3. [Async와 저수준 CQL operations](./bluetape4k-spring-boot-cassandra/async-and-cql-operations.md) — future await, row mapper, extractor, prepared statement 경계를 다룹니다.
4. [WriteOptions와 batch](./bluetape4k-spring-boot-cassandra/write-options-and-batches.md) — TTL·timestamp·LWT 옵션과 Flow 전체 수집 비용을 연결합니다.
5. [모델, 변환과 스키마](./bluetape4k-spring-boot-cassandra/models-schema-and-converters.md) — 신규 판단, 감사 필드, converter, UDT·table 생성 범위를 정리합니다.
6. [실패, 테스트와 생태계 경로](./bluetape4k-spring-boot-cassandra/failures-testing-and-ecosystem.md) — cancellation, driver 오류, Testcontainers 검증과 다음 학습 경로를 안내합니다.

처음 도입한다면 1→2장을 읽습니다. 직접 CQL을 많이 쓴다면 3→4장, entity와 schema를 함께 설계한다면 5→6장까지 이어서 읽는 편이 좋습니다.

## 권장 패턴 {#patterns}

애플리케이션 configuration이 `CqlSession`과 Spring Data template을 소유하고, service는 하나의 접근 경로를 선택합니다. streaming 조회는 reactive+`Flow`, Spring Data async template을 이미 쓰는 코드는 future+suspend 경로로 통일합니다. 단건 부재가 정상인 조회는 이름에 `OrNull`이 들어간 API를 사용해 계약을 드러냅니다.

prepared statement와 typed mapping을 우선하고, CQL 문자열의 값 연결은 피합니다. TTL, timestamp, consistency, timeout과 LWT는 호출 코드에 흩뿌리지 않고 use case 가까이에서 options 객체로 만듭니다.

## 연동 {#integrations}

```text
Spring Boot / application configuration
       └── CqlSession + keyspace + driver config
                    ↓
       Spring Data Cassandra mapping layer
       ├── ReactiveSession / ReactiveCassandraOperations
       ├── AsyncCqlOperations / AsyncCassandraOperations
       ├── CassandraTemplate and repositories
       └── MappingContext / converter
                    ↓
       bluetape4k coroutine, Flow, option,
       model, and schema helper APIs
```

`bluetape4k-cassandra`는 driver 수준 statement, paging과 CQL helper를 제공합니다. 이 모듈은 그 기반과 Spring Data Cassandra를 함께 사용합니다. Spring Data repository interface 자체는 이 모듈이 새로 정의하지 않습니다.

## 설정 {#configuration}

1.12.1 배포 소스에는 `src/main/resources`, `AutoConfiguration.imports`, `@ConfigurationProperties`, auto-configuration class가 없습니다. 따라서 이 모듈 전용 property prefix나 activation condition도 없습니다.

접속 주소, local datacenter, keyspace, authentication, request timeout, pooling과 driver metric은 Spring Boot의 Cassandra 설정이나 애플리케이션의 `AbstractCassandraConfiguration`에서 관리합니다. 테스트도 `AbstractCassandraTestConfiguration`이 `CqlSession`을 직접 구성하고 공유합니다. `CqlSession`은 비용이 큰 thread-safe 객체이므로 요청마다 생성하지 않습니다.

## 실패 동작 {#failures}

Reactive 확장은 `awaitSingle`, `awaitSingleOrNull`, `asFlow`를 사용합니다. non-null 단건 API가 빈 publisher를 받으면 실패하며, nullable API는 `null`을 반환합니다. `Flow`는 수집 중 driver·mapping 오류를 그대로 내보냅니다.

Async 확장은 `CompletableFuture.await()`를 사용하므로 Spring Data와 driver 예외를 그대로 전파합니다. 이 확장들은 retry, timeout, fallback 또는 exception translation을 추가하지 않습니다. coroutine cancellation은 대기를 취소하지만 이미 Cassandra에 전달된 쿼리가 서버에서 중단됐다고 단정할 수는 없습니다.

`SchemaGenerator`는 등록되지 않은 entity metadata가 필요하면 실패하고, `truncate`는 table이 존재할 때 전체 데이터를 지웁니다. 운영 호출 경로에는 두지 않습니다.

## 운영 {#operations}

이 모듈은 health indicator나 observation bean을 등록하지 않습니다. Spring Boot Actuator와 Cassandra driver metric을 애플리케이션이 구성하고, session 연결 상태, request latency, timeout, unavailable·overloaded 오류, pool 사용량을 관찰합니다.

`Flow` 조회는 consumer가 감당할 수 있는 속도로 수집하고 불필요한 `toList()`를 피합니다. 반대로 batch `insertFlow`·`updateFlow`·`deleteFlow`는 의도적으로 전부 수집하므로 입력 크기를 제한합니다. schema 생성과 truncate는 배포·테스트 단계로 격리하고 감사 로그를 남깁니다.

## 테스트 {#testing}

가벼운 adapter·모델·options 검증과 실제 Cassandra 검증을 나눕니다.

```bash
# mock과 순수 객체를 사용하는 빠른 검증
./gradlew :bluetape4k-spring-boot-cassandra:test \
  --tests '*UnitTest' --tests '*OptionsSupportTest' --tests '*AbstractCassandraModelTest'

# Cassandra Testcontainers를 포함한 전체 모듈 검증
./gradlew :bluetape4k-spring-boot-cassandra:test --no-configuration-cache
```

전체 테스트는 `CassandraServer.Launcher.cassandra4`를 사용합니다. 여러 Spring context가 session을 반복 생성하면 연결이 누적되므로 테스트 configuration은 companion object의 공유 `CqlSession`을 사용합니다. container-backed 검증은 다른 무거운 테스트와 병렬로 몰아 실행하지 않습니다.

## 워크숍과 예제 {#workshops}

manual manifest에 전용 workshop은 없습니다. 대신 테스트가 단계별 실행 예제로 충분한 정보를 제공합니다. `ReactiveSessionCoroutinesExamples`에서 session 호출을 보고, `ReactiveCassandraTemplateTest`와 `AsyncCassandraTemplateTest`에서 CRUD·slice·options를 확인한 뒤, `AsyncOptimisticLockingTest`에서 version 기반 LWT 실패를 읽는 순서가 좋습니다.

더 낮은 수준의 driver API와 paging은 [`bluetape4k-cassandra`](./bluetape4k-cassandra.md), Spring의 공통 coroutine·context helper는 [`bluetape4k-spring-boot-core`](./bluetape4k-spring-boot-core.md)를 함께 봅니다.

## 1.12.1 범위 {#limitations}

이 매뉴얼은 `bluetape4k-projects` 1.12.1 배포 소스를 기준으로 합니다. artifact 이름에 `spring-boot`가 있지만 자체 auto-configuration, property binding, health·observation, repository 구현은 제공하지 않습니다. DataStax mapper runtime과 driver Micrometer integration도 자동으로 runtime에 추가되지 않습니다.

`Flow` batch는 streaming batch가 아니며 입력 전체를 메모리에 올립니다. `SchemaGenerator`는 이미 존재하는 table의 변경 사항을 비교하거나 migration history를 관리하지 않습니다. `AbstractCassandraAuditable`의 `lastModified_by` 컬럼 표기는 소스 계약 그대로이므로 기존 schema naming과 일치하는지 확인해야 합니다.

## Source와 tests {#sources}

- [모듈 README](../../../../spring-boot/cassandra/README.ko.md)
- [모듈 build](../../../../spring-boot/cassandra/build.gradle.kts)
- [`ReactiveCassandraOperationsCoroutines.kt`](../../../../spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/ReactiveCassandraOperationsCoroutines.kt)
- [`ReactiveSessionCoroutines.kt`](../../../../spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/ReactiveSessionCoroutines.kt)
- [`AsyncCassandraOperationsCoroutines.kt`](../../../../spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/AsyncCassandraOperationsCoroutines.kt)
- [`AsyncCqlOperationsCoroutines.kt`](../../../../spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/cql/AsyncCqlOperationsCoroutines.kt)
- [`OptionsSupport.kt`](../../../../spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/cql/OptionsSupport.kt)
- [`SchemaGenerator.kt`](../../../../spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/schema/SchemaGenerator.kt)
- [`AbstractCassandraPersistable.kt`](../../../../spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/model/AbstractCassandraPersistable.kt)
- [`AbstractReactiveCassandraTestConfiguration.kt`](../../../../spring-boot/cassandra/src/test/kotlin/io/bluetape4k/spring/cassandra/AbstractReactiveCassandraTestConfiguration.kt)
- [`ReactiveCassandraOperationsCoroutinesUnitTest.kt`](../../../../spring-boot/cassandra/src/test/kotlin/io/bluetape4k/spring/cassandra/ReactiveCassandraOperationsCoroutinesUnitTest.kt)
- [`SchemaGeneratorTest.kt`](../../../../spring-boot/cassandra/src/test/kotlin/io/bluetape4k/spring/cassandra/schema/SchemaGeneratorTest.kt)

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### Spring Boot Cassandra 핵심 확장 함수와 클래스 구조 다이어그램

[![Spring Boot Cassandra 핵심 확장 함수와 클래스 구조 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-cassandra-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-cassandra-diagram-01.svg)

_배포본 README: [`spring-boot/cassandra/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/spring-boot/cassandra/README.ko.md)_

### Spring Boot Cassandra 데이터 접근 계층 다이어그램

[![Spring Boot Cassandra 데이터 접근 계층 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-cassandra-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-cassandra-diagram-02.svg)

_배포본 README: [`spring-boot/cassandra/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/spring-boot/cassandra/README.ko.md)_

### Spring Boot Cassandra 코루틴 변환 시퀀스 다이어그램

[![Spring Boot Cassandra 코루틴 변환 시퀀스 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-cassandra-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-cassandra-sequence-01.svg)

_배포본 README: [`spring-boot/cassandra/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/spring-boot/cassandra/README.ko.md)_

<!-- release-readme-diagrams:end -->
