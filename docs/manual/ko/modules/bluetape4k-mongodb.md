---
manualId: bluetape4k-mongodb
title: "MongoDB 코루틴 확장"
description: "MongoDB Kotlin Coroutine Driver의 client·collection·BSON·aggregation API를 그대로 살리면서 반복되는 구성과 조회 패턴을 줄이는 방법을 설명합니다."
kind: library
group: data
learningOrder: 630
---

# MongoDB 코루틴 확장

## 제공하는 기능 {#problem}

`bluetape4k-mongodb`는 MongoDB Kotlin Coroutine Driver 위에 작은 편의 API를 더합니다. `MongoClientSettings` DSL과 client cache, database·collection 조회 helper, `Document` builder, aggregation stage builder를 제공하며 세션과 트랜잭션의 기본 수명주기도 감쌉니다.

이 모듈은 MongoDB driver를 대체하지 않습니다. 실제 connection pool, server selection, retryable read/write, BSON codec, query 실행, `suspend`와 `Flow`는 공식 driver가 담당합니다. Spring bean, repository, `MongoTemplate`과 mapping context가 필요하다면 [`bluetape4k-spring-boot-mongodb`](./bluetape4k-spring-boot-mongodb.md)의 책임과 비교해야 합니다.

## 사용하기 전에 결정할 것 {#when-to-use}

- 공식 coroutine driver를 직접 사용하면서 반복되는 client·collection 코드를 줄일지 정합니다.
- `mongoClientOf`로 만든 client를 호출자가 닫을지, `MongoClientProvider`가 JVM 수명 동안 공유할지 선택합니다.
- `Document`를 직접 다룰지, 애플리케이션 타입과 codec registry를 구성할지 정합니다.
- 결과 전체를 `List`로 모을지, driver가 반환하는 `Flow`를 끝까지 유지할지 구분합니다.
- 세션과 트랜잭션을 쓸 topology인지 확인합니다. standalone MongoDB에서는 multi-document transaction을 사용할 수 없습니다.
- Spring Data MongoDB의 repository·mapping·transaction manager가 필요한 경우 low-level driver helper와 섞어 책임을 흐리지 않습니다.

## 의존성 추가 {#coordinates}

사용자는 MongoDB driver 세부 버전을 직접 맞추지 않고 중앙 BOM 버전만 관리합니다.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-mongodb")
}
```

Gradle project path는 `:bluetape4k-mongodb`, source directory는 `data/mongodb`입니다. kotlinx.serialization용 BSON codec은 `compileOnly`이므로 해당 codec을 사용하는 애플리케이션이 runtime dependency를 제공해야 합니다.

## 첫 client와 collection {#quick-start}

호출자가 client 수명주기를 소유하는 가장 작은 구성입니다.

```kotlin
val client = mongoClientOf("mongodb://localhost:27017")

try {
    val database = client.getDatabase("catalog")
    val products = database.getCollectionOf<Document>("products")

    products.insertOne(documentOf("sku" to "A-100", "stock" to 3))
    val product = products.findFirst(Filters.eq("sku", "A-100"))
} finally {
    client.close()
}
```

`getCollectionOf<T>`는 `T::class.java`를 driver에 넘겨주는 reified helper입니다. `T`를 자동 직렬화하지 않으므로 해당 type을 처리할 codec이 database의 codec registry에 있어야 합니다.

## API 선택 지도 {#api-by-task}

| 필요한 작업 | 시작할 API | 기억할 경계 |
| --- | --- | --- |
| client를 직접 생성 | `mongoClient`, `mongoClientOf` | 반환된 client는 호출자가 닫습니다. |
| client를 JVM에서 공유 | `MongoClientProvider.getOrCreate` | 1.12.1에는 명시적 제거·전체 종료 API가 없습니다. |
| database·collection 이름 수집 | `listDatabaseNamesAsList`, `listCollectionNamesList` | 모든 이름을 메모리의 `List`로 모읍니다. |
| typed collection 획득 | `getCollectionOf<T>` | 실제 encode/decode는 driver codec registry가 담당합니다. |
| 첫 문서·존재 여부·upsert | `findFirst`, `exists`, `upsert` | driver 호출을 조합한 편의 함수입니다. |
| filter·sort·page를 `Flow`로 조회 | `findAsFlow` | 실행과 backpressure 계약은 coroutine driver를 따릅니다. |
| 세션·트랜잭션 범위 | `withClientSession`, `inTransaction` | session을 native collection API에 명시적으로 전달합니다. |
| BSON 문서 구성 | `documentOf`, `Document.getAs<T>` | safe cast만 하며 숫자 변환이나 schema 검증은 하지 않습니다. |
| aggregation stage 구성 | `pipeline`, `matchStage` 등 | pipeline 실행은 native `aggregate`가 담당합니다. |

## 학습 경로 {#concepts}

아래 장은 1.12.1 배포 소스와 테스트를 따라 client 소유권에서 transaction, codec과 운영 검증까지 이어집니다. 공식 driver가 제공하는 기능과 bluetape4k helper가 추가하는 기능을 계속 나눠서 설명합니다.

1. [모듈 경계와 client 수명주기](./bluetape4k-mongodb/module-boundary-client-lifecycle.md) — 직접 생성과 provider cache의 소유권, 1.12.1 cache key 제약을 확인합니다.
2. [Database·Collection과 Flow](./bluetape4k-mongodb/database-collection-flow.md) — typed collection, 첫 조회, 존재 확인, upsert와 지연 실행 결과를 다룹니다.
3. [Document, codec과 query 경계](./bluetape4k-mongodb/documents-codecs-queries.md) — `Document` DSL, safe cast, codec registry와 공식 query DSL의 역할을 구분합니다.
4. [세션과 트랜잭션](./bluetape4k-mongodb/sessions-transactions.md) — session 전달, commit·abort·close 순서와 취소 경계를 설명합니다.
5. [Aggregation pipeline 구성](./bluetape4k-mongodb/aggregation-pipelines.md) — stage builder가 하는 일과 native `aggregate`가 맡는 실행을 나눕니다.
6. [테스트, 운영과 생태계 경로](./bluetape4k-mongodb/testing-operations-ecosystem.md) — unit·Testcontainers 검증과 Spring Data MongoDB로 발전하는 경로를 안내합니다.

처음 도입한다면 1→2→3 순서로 client 하나와 collection 하나를 구성합니다. transaction이 필요하면 4장을 먼저 읽고 replica set 또는 sharded cluster 테스트 환경을 준비합니다.

## 권장 패턴 {#patterns}

client는 operation마다 만들지 말고 애플리케이션 컴포넌트의 수명에 맞춰 공유합니다. 직접 만든 client는 해당 컴포넌트가 닫고, provider가 반환한 client는 공유 객체라는 사실을 호출부에 드러냅니다. 1.12.1 provider는 같은 URL의 builder 설정을 별도 cache key로 보지 않으므로 설정이 다른 client가 필요하면 완성된 `MongoClientSettings` overload를 사용합니다.

조회 결과가 클 수 있으면 `listDatabaseNamesAsList` 같은 eager helper보다 driver `Flow`를 그대로 소비합니다. `findAsFlow`에는 안정적인 sort와 범위를 함께 지정하고, page가 반복되는 API라면 skip 비용과 cursor 기반 pagination도 비교합니다.

## 연동 {#integrations}

모듈은 MongoDB Kotlin Coroutine Driver, Kotlin query extensions와 BSON Kotlin API를 노출합니다. KProperty 기반 filter·sort·update·projection은 공식 `mongodb-driver-kotlin-extensions`의 기능이며 이 모듈이 새 DSL을 만드는 것이 아닙니다.

Spring Boot의 reactive repository, `ReactiveMongoOperations`, auto-configuration과 Criteria DSL이 필요하면 [`bluetape4k-spring-boot-mongodb`](./bluetape4k-spring-boot-mongodb.md)를 사용합니다. 두 모듈을 함께 쓸 때 low-level `MongoClient`와 Spring이 관리하는 client를 중복 생성하지 않도록 ownership을 하나로 정합니다.

## 설정 {#configuration}

모듈 전용 property나 resource는 없습니다. endpoint, credential, TLS, application name, timeout, pool과 codec registry는 `MongoClientSettings.Builder`에서 공식 driver 방식으로 설정합니다.

```kotlin
val client = mongoClientOf(connectionString) {
    applicationName("catalog-api")
    applyToConnectionPoolSettings { pool ->
        pool.maxSize(32)
    }
}
```

credential이 포함된 connection string을 log에 남기지 않습니다. provider cache는 문자열 자체나 `MongoClientSettings`를 key로 사용하므로 tenant별 credential을 무제한 생성하면 client와 pool도 계속 늘어납니다.

## 실패 동작 {#failures}

helper는 MongoDB 예외를 domain 예외로 바꾸거나 자동 retry하지 않습니다. `findFirst`는 결과가 없으면 `null`, `Document.getAs<T>`는 key가 없거나 runtime type이 다르면 `null`을 반환합니다. 두 경우를 데이터 오류와 구분해야 한다면 호출부에서 명시적으로 검사합니다.

`inTransaction`은 성공하면 commit하고 예외가 나면 abort를 시도한 뒤 원래 예외를 다시 던집니다. abort도 실패하면 그 예외를 suppressed exception으로 붙입니다. 1.12.1 구현은 취소 경로의 abort를 `NonCancellable` context에서 실행하지 않으므로, 강한 취소 상황까지 cleanup을 보장한다고 가정하지 않습니다.

## 운영 {#operations}

MongoDB driver의 command latency, server selection timeout, connection pool wait, retry와 transaction abort를 관찰합니다. operation 이름과 collection은 제한된 metric label로 기록하고 filter 값이나 전체 `Document`를 고유 label에 넣지 않습니다.

provider 사용 시 client 생성 횟수와 설정 cardinality를 확인합니다. 1.12.1에는 cache 제거 API가 없으므로 동적으로 늘어나는 tenant·credential별 client registry로 사용하기에는 맞지 않습니다.

## 테스트 {#testing}

`DocumentExtensionsTest`와 `AggregationSupportTest`는 MongoDB 없이 실행할 수 있습니다. 나머지 module test는 `MongoDBServer` Testcontainer를 사용하므로 다른 heavy suite와 순차 실행합니다.

```bash
./gradlew :bluetape4k-mongodb:test --no-configuration-cache
```

`AbstractMongoTest`는 `src/test`의 내부 fixture이지 published artifact의 API가 아닙니다. 애플리케이션 테스트에서는 자체 Testcontainers fixture를 만들고 sync client가 아니라 coroutine `MongoClient`를 연결합니다.

## 워크숍 {#workshops}

`BasicCrudExamples`는 native suspend CRUD와 `findFirst`, `exists`, `upsert`, `findAsFlow`를 한 흐름에서 보여 줍니다. `AggregationExamples`는 `pipeline`으로 stage를 만든 뒤 native `aggregate(...).toList()`로 실행합니다.

Spring Data MongoDB의 mapping, Criteria·Query·Update와 reactive operation을 학습하려면 [`bluetape4k-spring-boot-mongodb`](./bluetape4k-spring-boot-mongodb.md) 매뉴얼로 이동합니다. driver-level helper를 이해한 뒤 framework가 추가하는 lifecycle과 exception translation을 비교하면 경계가 선명해집니다.

## 1.12.1 범위 {#limitations}

이 매뉴얼은 release commit `7cf0b73646af05c0f8872cc4f6a16983949c4e3e`의 1.12.1 소스와 테스트를 기준으로 합니다. production API는 일곱 Kotlin source file의 client·database·collection·BSON·aggregation helper입니다.

repository, object mapping framework, schema migration, auto-configuration, health indicator, metrics와 transaction manager는 제공하지 않습니다. provider cache의 명시적 종료 API와 `NonCancellable` transaction cleanup도 1.12.1 범위에는 없습니다.

## Source와 tests {#sources}

- [`build.gradle.kts`](../../../../data/mongodb/build.gradle.kts)
- [`MongoClientSupport.kt`](../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoClientSupport.kt)
- [`MongoClientProvider.kt`](../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoClientProvider.kt)
- [`MongoClientExtensions.kt`](../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoClientExtensions.kt)
- [`MongoCollectionExtensions.kt`](../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoCollectionExtensions.kt)
- [`MongoDatabaseExtensions.kt`](../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoDatabaseExtensions.kt)
- [`DocumentExtensions.kt`](../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/bson/DocumentExtensions.kt)
- [`AggregationSupport.kt`](../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/aggregation/AggregationSupport.kt)
- [`MongoClientSupportTest.kt`](../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/MongoClientSupportTest.kt)
- [`MongoCollectionExtensionsTest.kt`](../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/MongoCollectionExtensionsTest.kt)
- [`AggregationSupportTest.kt`](../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/aggregation/AggregationSupportTest.kt)
- [`BasicCrudExamples.kt`](../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/examples/BasicCrudExamples.kt)

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### Core 클래스 구조도

[![Core 클래스 구조도](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-mongodb-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-mongodb-diagram-01.svg)

_배포본 README: [`data/mongodb/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/data/mongodb/README.ko.md)_

### 모듈 API 구조도

[![모듈 API 구조도](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-mongodb-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-mongodb-diagram-02.svg)

_배포본 README: [`data/mongodb/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/data/mongodb/README.ko.md)_

### Aggregation 파이프라인 Data 처리 흐름

[![Aggregation 파이프라인 Data 처리 흐름](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-mongodb-diagram-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-mongodb-diagram-03.svg)

_배포본 README: [`data/mongodb/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/data/mongodb/README.ko.md)_

<!-- release-readme-diagrams:end -->
