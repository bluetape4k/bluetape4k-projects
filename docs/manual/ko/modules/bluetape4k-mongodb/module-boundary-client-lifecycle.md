---
title: 모듈 경계와 client 수명주기
description: MongoDB coroutine driver와 bluetape4k helper의 책임을 나누고 직접 생성한 client와 provider cache의 수명주기를 설명합니다.
manualId: bluetape4k-mongodb
chapterId: module-boundary-client-lifecycle
---

# 모듈 경계와 client 수명주기

## 공식 driver 위의 작은 helper

1.12.1 production source는 일곱 파일입니다. client 생성·cache·session, database와 collection extension, `Document` helper와 aggregation stage builder를 제공합니다. network protocol, pool, retry, codec 실행과 `Flow` 구현은 MongoDB Kotlin Coroutine Driver의 책임입니다.

build는 coroutine driver, Kotlin extensions와 BSON Kotlin을 API dependency로 노출합니다. 따라서 이 artifact를 추가하면 driver API도 사용할 수 있지만, bluetape4k가 driver 설정을 대신 정하거나 server를 구성하지는 않습니다.

## 직접 생성한 client

`mongoClient`는 `MongoClientSettings.Builder`를 적용해 새 client를 만들고, `mongoClientOf`는 connection string을 먼저 적용한 뒤 추가 builder를 실행합니다.

```kotlin
val client = mongoClientOf("mongodb://localhost:27017") {
    applicationName("report-worker")
}

try {
    // use client
} finally {
    client.close()
}
```

두 함수는 cache에 등록하지 않습니다. 호출자가 client와 내부 connection pool을 소유하며 application shutdown에 맞춰 닫아야 합니다.

## provider가 공유하는 client

`MongoClientProvider.getOrCreate(connectionString)`은 문자열 key의 `ConcurrentHashMap`에서 client를 공유합니다. 새 client는 `ShutdownQueue`에 등록되므로 JVM 종료 시 닫힙니다.

```kotlin
val first = MongoClientProvider.getOrCreate(url)
val second = MongoClientProvider.getOrCreate(url)
check(first === second)
```

provider에서 받은 client를 한 호출자가 임의로 닫으면 같은 instance를 공유하는 다른 호출자가 실패할 수 있습니다. 1.12.1에는 entry를 제거하거나 모두 닫는 public API가 없으므로 provider를 application-lifetime singleton으로 취급합니다.

## 1.12.1의 두 cache

release source에는 URL 문자열 cache와 `MongoClientSettings` cache가 따로 있습니다. 이 차이는 단순 구현 세부가 아니라 instance identity에 영향을 줍니다.

| 호출 | cache key | 결과 |
| --- | --- | --- |
| `getOrCreate(url)` | 원문 URL 문자열 | 같은 문자열이면 같은 instance |
| `getOrCreate(url) { ... }` | 원문 URL 문자열 | 같은 URL의 첫 builder 설정만 client 생성에 사용 |
| `getOrCreate(settings)` | `MongoClientSettings` | `equals/hashCode`가 같은 설정이면 같은 instance |

같은 URL에 timeout이나 application name이 다른 client가 필요하면 URL+builder overload를 쓰지 말고 각각 완성한 `MongoClientSettings`를 key로 전달합니다. 반대로 URL overload와 settings overload는 별도 cache이므로 논리적으로 같은 endpoint라도 두 client를 만들 수 있습니다.

## cache cardinality와 credential

connection string에 tenant나 credential이 포함되면 문자열마다 새 client와 pool이 생깁니다. 제거 API가 없는 1.12.1 provider에 동적인 tenant 목록을 계속 넣지 않습니다. tenant 수가 제한되지 않는다면 application registry에서 client의 생성과 종료를 명시적으로 관리합니다.

URL을 log에 출력하는 provider source도 함께 확인해야 합니다. credential을 connection string에 포함하는 배포에서는 log 정책과 driver 설정 방식을 검토합니다.

## Spring과 함께 쓸 때

Spring Boot가 `MongoClient`를 bean으로 관리한다면 별도의 provider client를 만들 이유가 있는지 먼저 확인합니다. Spring Data workload와 low-level driver workload가 같은 cluster와 credential을 사용한다면 client와 pool을 중복 생성하지 않는 편이 단순합니다.

repository와 mapping이 필요한 경우 [`bluetape4k-spring-boot-mongodb`](../bluetape4k-spring-boot-mongodb.md)가 더 알맞습니다. 이 모듈은 Spring lifecycle이나 auto-configuration을 제공하지 않습니다.

## Source와 tests

- [`build.gradle.kts`](../../../../../data/mongodb/build.gradle.kts)
- [`MongoClientSupport.kt`](../../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoClientSupport.kt)
- [`MongoClientProvider.kt`](../../../../../data/mongodb/src/main/kotlin/io/bluetape4k/mongodb/MongoClientProvider.kt)
- [`MongoClientSupportTest.kt`](../../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/MongoClientSupportTest.kt)
