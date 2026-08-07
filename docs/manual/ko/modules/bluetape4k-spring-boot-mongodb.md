---
manualId: bluetape4k-spring-boot-mongodb
title: "Spring Data MongoDB 코루틴 지원"
description: "ReactiveMongoOperations를 Flow와 suspend 함수로 사용하고 Criteria, Query, Update를 Kotlin 문법으로 조립하는 방법을 설명합니다."
kind: library
group: spring
learningOrder: 920
---

# Spring Data MongoDB 코루틴 지원

## 제공하는 기능 {#problem}

`bluetape4k-spring-boot-mongodb`는 Spring Data MongoDB의 `ReactiveMongoOperations`를 Kotlin coroutine 코드에서 쓰기 좋은 형태로 감쌉니다. 여러 문서를 읽거나 쓰는 작업은 `Flow<T>`, 한 번의 결과를 내는 작업은 suspend 함수로 바꿉니다. `Criteria`, `Query`, `Update`를 짧게 조립하는 Kotlin 확장도 함께 제공합니다.

이 모듈은 MongoDB client나 connection pool을 직접 만들지 않습니다. Spring Boot가 구성한 `ReactiveMongoDatabaseFactory`와 `MongoConverter`를 사용하고, MongoDB driver가 실제 연결·프로토콜·서버 오류를 처리합니다. 모듈의 자동 설정은 `ReactiveMongoOperations` bean이 없을 때 `ReactiveMongoTemplate`을 보완하는 작은 fallback입니다.

## 사용하기 전에 결정할 것 {#when-to-use}

- 애플리케이션이 Spring Data MongoDB의 mapping과 `ReactiveMongoOperations`를 표준 persistence 경계로 사용할지 정합니다.
- 0건을 `null`로 받을지, 0건도 오류인 필수 조회로 다룰지 구분합니다.
- 여러 결과와 다건 insert는 `Flow`를 언제 어디서 수집할지 정합니다.
- `save`, `updateFirst`, `updateMulti`, `upsert`의 서로 다른 쓰기 의미를 구분합니다.
- transaction, retryable writes, read concern, write concern, pool과 timeout은 Spring Boot·driver 구성에서 소유합니다.

Spring Data 계층 없이 MongoDB Kotlin driver를 직접 사용하려면 [`bluetape4k-mongodb`](./bluetape4k-mongodb.md)를 먼저 확인합니다.

## 의존성 추가 {#coordinates}

사용자는 개별 Spring·MongoDB driver 버전을 맞추지 않고 `bluetape4k-dependencies` BOM 버전만 관리합니다.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-spring-boot-mongodb")
}
```

이 artifact는 Spring Data MongoDB Reactive starter, coroutine·Reactor 연동과 `bluetape4k-spring-boot-core`를 함께 제공합니다.

## 첫 repository {#quick-start}

Spring Boot가 제공한 `ReactiveMongoOperations`를 주입하고 query와 결과 형태만 repository에서 결정합니다.

```kotlin
@Repository
class UserRepository(
    private val operations: ReactiveMongoOperations,
) {
    fun findAdults(city: String): Flow<User> =
        operations.findAsFlow(
            queryOf("age".criteria() gte 20, "city".criteria() eq city)
                .sortAscBy("name")
        )

    suspend fun findById(id: String): User? =
        operations.findByIdOrNullSuspending(id)

    suspend fun insert(user: User): User =
        operations.insertSuspending(user)
}
```

`findAdults`의 `Flow`는 수집할 때 Spring Data publisher를 구독합니다. 중간에서 `block()`이나 `runBlocking`으로 바꾸지 말고 WebFlux controller나 coroutine service까지 유지합니다.

## 작업별 API {#api-by-task}

| 필요한 작업 | API | 반환과 경계 |
| --- | --- | --- |
| 여러 문서 조회 | `findAsFlow`, `findAllAsFlow` | `Flow<T>`, 0건은 빈 Flow |
| nullable 단건 조회 | `findOneOrNullSuspending`, `findByIdOrNullSuspending` | 0건은 `null` |
| 필수 단건 조회 | `findOneSuspending`, `findByIdSuspending` | 0건이면 `NoSuchElementException` |
| 개수·존재 확인 | `countSuspending`, `existsSuspending` | `Long`, `Boolean` |
| insert·save | `insertSuspending`, `insertAllAsFlow`, `saveSuspending` | 저장된 entity 또는 `Flow<T>` |
| 조건 update·upsert | `updateFirstSuspending`, `updateMultiSuspending`, `upsertSuspending` | `UpdateResult` |
| 원자적 수정·삭제 | `findAndModifySuspending`, `findAndRemoveSuspending` | 대상이 없으면 `null` |
| 집계·distinct·tail | `aggregateAsFlow`, `findDistinctAsFlow`, `tailAsFlow` | `Flow<T>` |
| 컬렉션 관리 | `collectionExistsSuspending`, `createCollectionSuspending`, `dropCollectionSuspending` | `Boolean` 또는 `Unit` |

## 학습 경로 {#concepts}

각 장은 API 목록보다 실행 시점, 결과 cardinality, Spring Boot와 driver의 책임을 먼저 설명합니다. 예제 뒤의 Source와 tests 링크에서 1.12.1 구현을 바로 확인할 수 있습니다.

1. [자동 설정과 구성 경계](./bluetape4k-spring-boot-mongodb/auto-configuration-boundaries.md) — fallback bean이 만들어지는 조건과 Spring Boot·driver가 소유하는 설정을 구분합니다.
2. [Coroutine 조회와 cardinality](./bluetape4k-spring-boot-mongodb/coroutine-reads-cardinality.md) — `Flow`, nullable 단건, 필수 단건의 차이를 익힙니다.
3. [쓰기와 원자적 연산](./bluetape4k-spring-boot-mongodb/writes-and-atomic-operations.md) — insert, save, update, upsert, find-and-modify의 결과를 해석합니다.
4. [Criteria, Query, Update DSL](./bluetape4k-spring-boot-mongodb/query-dsl.md) — 조건·정렬·pagination·update를 Kotlin 문법으로 조립합니다.
5. [집계, 컬렉션과 스트리밍](./bluetape4k-spring-boot-mongodb/aggregation-collections-streaming.md) — aggregation, distinct, capped collection과 tailable cursor를 다룹니다.
6. [테스트, 운영과 생태계](./bluetape4k-spring-boot-mongodb/testing-operations-ecosystem.md) — Testcontainers 검증, 장애 관찰 지점과 native driver 경로를 연결합니다.

처음 사용한다면 1→2→3→4 순서로 읽습니다. aggregation이나 tailable cursor가 필요하면 5장을, 도입 검토나 테스트 설계가 목적이면 6장을 먼저 읽어도 됩니다.

## 권장 패턴 {#patterns}

repository는 `ReactiveMongoOperations`를 주입받아 query와 cardinality를 결정합니다. 여러 repository 호출을 묶는 transaction과 재시도는 service 경계가 소유합니다. `UpdateResult.matchedCount`와 `modifiedCount`, `DeleteResult.deletedCount`를 확인해 대상 부재와 실제 변경을 구분합니다.

필드 이름을 문자열로 받는 DSL은 간결하지만 compiler가 document schema를 검증하지 못합니다. 외부 입력으로 필드명이나 정렬 기준을 직접 받지 말고 허용 목록에 매핑합니다. `paginate`도 음수 page나 0 이하 size를 자체 검증하지 않으므로 HTTP 입력 경계에서 먼저 검사합니다.

## 연동 {#integrations}

```text
WebFlux controller / coroutine service
                  ↓
bluetape4k coroutine operations + query DSL
                  ↓
ReactiveMongoOperations / mapping converter
                  ↓
ReactiveMongoDatabaseFactory / MongoDB driver
                  ↓
MongoDB server
```

`bluetape4k-spring-boot-mongodb`는 위 계층을 대체하지 않습니다. coroutine adapter는 Reactor publisher를 `Flow`, `awaitSingle`, `awaitSingleOrNull`로 연결하고, query DSL은 Spring Data 객체를 그대로 만듭니다.

## 설정과 자동 구성 {#configuration}

`AutoConfiguration.imports`는 `ReactiveMongoAutoConfiguration`을 등록합니다. 이 구성은 classpath에 `ReactiveMongoOperations`가 있고 같은 타입의 bean이 없을 때만 `ReactiveMongoTemplate`을 만듭니다. 생성에 필요한 `ReactiveMongoDatabaseFactory`와 `MongoConverter`는 이미 Spring context에 있어야 합니다.

MongoDB URI, credential, database, SSL, timeout과 pool은 Spring Boot와 MongoDB driver에서 설정합니다. 이 모듈에는 자체 `@ConfigurationProperties`, client builder, custom conversion 등록이나 auditing 활성화 기능이 없습니다. 애플리케이션별 `MongoCustomConversions`와 auditing 설정은 별도 Spring configuration에 둡니다.

## 실패 동작 {#failures}

Coroutine 확장은 Spring Data와 driver 예외를 별도 domain 값으로 바꾸지 않습니다. nullable 조회는 결과 부재만 `null`로 표현합니다. connection 실패, duplicate key, mapping 오류, timeout은 예외로 전파됩니다. 필수 단건 조회는 publisher가 비어 있으면 `awaitSingle()`의 `NoSuchElementException`으로 실패합니다.

`Flow` 수집을 취소하면 subscription도 취소됩니다. cancellation을 잡아서 빈 결과나 성공으로 바꾸지 않습니다. write를 재시도하려면 전체 작업이 idempotent한지, driver의 retryable write와 중복되지 않는지 먼저 확인합니다.

## 운영 {#operations}

이 모듈은 별도 metric이나 health indicator를 제공하지 않습니다. Spring Boot Actuator, MongoDB driver command monitoring과 connection-pool metric에서 server selection, pool 대기, query latency, timeout을 관찰합니다. 느린 `Flow` consumer는 subscription과 connection 사용 시간을 늘릴 수 있으므로 결과 크기와 처리량을 함께 확인합니다.

Tailable cursor는 오래 유지되는 구독입니다. 애플리케이션 종료와 coroutine scope 취소가 cursor subscription까지 전달되는지 확인하고, 재연결 정책은 event 중복과 누락 허용 범위에 맞춰 바깥 경계에서 정합니다.

## 테스트 {#testing}

```bash
./gradlew :bluetape4k-spring-boot-mongodb:test --no-build-cache --no-configuration-cache
```

1.12.1 통합 테스트는 `MongoDBServer` Testcontainer와 실제 Spring Boot context를 사용합니다. `ReactiveMongoOperationsCoroutinesTest`가 insert, save, 조회, update, upsert, delete, aggregation과 컬렉션 관리를 검증합니다. Criteria·Query·Update DSL 테스트는 MongoDB 연결 없이 생성된 BSON 구조를 비교합니다.

Docker가 필요한 통합 테스트는 다른 Testcontainers 작업과 직렬로 실행합니다. 단위 DSL 테스트와 통합 테스트를 분리해 실패가 query 조립 문제인지 서버·driver 문제인지 먼저 좁힙니다.

## 예제와 다음 단계 {#workshops}

전용 workshop은 등록되어 있지 않습니다. 모듈의 `ReactiveMongoOperationsCoroutinesTest`가 가장 넓은 실행 예제이며, `User` document와 함께 읽으면 insert부터 aggregation까지 한 흐름으로 볼 수 있습니다.

Spring Data mapping보다 MongoDB Kotlin driver API와 codec을 직접 제어하려면 [`bluetape4k-mongodb`](./bluetape4k-mongodb.md)로 이어갑니다. Spring Data repository interface가 필요하면 이 모듈의 확장과 별개로 Spring Data의 reactive 또는 coroutine repository 기능을 선택합니다.

## 1.12.1 범위 {#limitations}

이 매뉴얼은 `bluetape4k-projects` 1.12.1 배포 소스를 기준으로 합니다. 모듈은 Spring Data repository 구현을 생성하지 않으며 custom conversion, auditing, transaction manager, MongoDB client와 pool을 구성하지 않습니다. MongoDB Kotlin sync·coroutine driver dependency가 포함되어 있어도 이 모듈의 공개 API는 `ReactiveMongoOperations` 확장과 query DSL에 집중합니다.

`tailAsFlow`는 capped collection이 필요합니다. `paginate`는 offset 기반이라 큰 page에서 효율이 떨어질 수 있으며 입력 범위를 검증하지 않습니다. 대규모 목록은 정렬된 seek pagination을 애플리케이션 query로 구현합니다.

## Source와 tests {#sources}

- [모듈 README](../../../../spring-boot/mongodb/README.ko.md)
- [모듈 build](../../../../spring-boot/mongodb/build.gradle.kts)
- [`ReactiveMongoAutoConfiguration.kt`](../../../../spring-boot/mongodb/src/main/kotlin/io/bluetape4k/spring/mongodb/config/ReactiveMongoAutoConfiguration.kt)
- [`ReactiveMongoOperationsCoroutines.kt`](../../../../spring-boot/mongodb/src/main/kotlin/io/bluetape4k/spring/mongodb/coroutines/ReactiveMongoOperationsCoroutines.kt)
- [`CriteriaExtensions.kt`](../../../../spring-boot/mongodb/src/main/kotlin/io/bluetape4k/spring/mongodb/query/CriteriaExtensions.kt)
- [`QueryExtensions.kt`](../../../../spring-boot/mongodb/src/main/kotlin/io/bluetape4k/spring/mongodb/query/QueryExtensions.kt)
- [`UpdateExtensions.kt`](../../../../spring-boot/mongodb/src/main/kotlin/io/bluetape4k/spring/mongodb/query/UpdateExtensions.kt)
- [`ReactiveMongoOperationsCoroutinesTest.kt`](../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/coroutines/ReactiveMongoOperationsCoroutinesTest.kt)
- [`CriteriaExtensionsTest.kt`](../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/query/CriteriaExtensionsTest.kt)
- [`QueryExtensionsTest.kt`](../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/query/QueryExtensionsTest.kt)
- [`UpdateExtensionsTest.kt`](../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/query/UpdateExtensionsTest.kt)

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### Spring Boot MongoDB 코루틴 확장 구조 다이어그램

[![Spring Boot MongoDB 코루틴 확장 구조 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-mongodb-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-mongodb-diagram-01.svg)

_배포본 README: [`spring-boot/mongodb/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/spring-boot/mongodb/README.ko.md)_

### ReactiveMongoOperations 코루틴 변환 흐름 다이어그램

[![ReactiveMongoOperations 코루틴 변환 흐름 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-mongodb-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-mongodb-diagram-02.svg)

_배포본 README: [`spring-boot/mongodb/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/spring-boot/mongodb/README.ko.md)_

### Criteria Query Update DSL 흐름 다이어그램

[![Criteria Query Update DSL 흐름 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-mongodb-diagram-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-mongodb-diagram-03.svg)

_배포본 README: [`spring-boot/mongodb/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/spring-boot/mongodb/README.ko.md)_

### MongoDB 코루틴 변환 시퀀스 다이어그램

[![MongoDB 코루틴 변환 시퀀스 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-mongodb-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-mongodb-sequence-01.svg)

_배포본 README: [`spring-boot/mongodb/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/spring-boot/mongodb/README.ko.md)_

<!-- release-readme-diagrams:end -->
