---
title: 테스트, 운영과 생태계
description: MongoDB Testcontainer 통합 테스트, 운영 관찰 지점과 Spring Data 및 native driver 선택 기준을 정리합니다.
manualId: bluetape4k-spring-boot-mongodb
chapterId: testing-operations-ecosystem
---

# 테스트, 운영과 생태계

## 현재 테스트 구조 (Spring Boot 4.1+)

`AbstractReactiveMongoTest`는 `MongoDBServer` Testcontainer를 지연 시작하고 `spring.mongodb.uri`를 동적으로 등록합니다. Spring Boot 4.1+에서 `spring.data.mongodb.uri`는 legacy namespace이며 유일한 URI key로 남으면 즉시 실패합니다. `AbstractReactiveMongoCoroutineTest`는 여기에 `Dispatchers.IO`와 `CoroutineName`을 가진 test scope를 더합니다.

`ReactiveMongoOperationsCoroutinesTest`는 실제 Spring Boot context와 MongoDB를 사용해 다음 흐름을 검증합니다.

- insert와 save
- Flow·nullable·필수 단건 조회
- count와 exists
- update first·multi와 upsert
- remove, find-and-modify, find-and-remove
- distinct와 aggregation
- collection 존재·생성·삭제

Query DSL 세 테스트는 server 없이 BSON 구조만 비교합니다. 빠른 DSL 검증을 먼저 실행하고 Docker 통합 테스트는 다른 Testcontainers 작업과 직렬로 실행합니다.

## 테스트 data 격리

통합 테스트는 `test_users` 전용 collection을 사용합니다. 테스트마다 고유한 email 같은 business key를 만들고, 자신이 만든 document만 삭제합니다. 공유 collection의 전체 개수에 의존하면 실행 순서와 병렬성에 따라 깨지기 쉽습니다.

```bash
./gradlew :bluetape4k-spring-boot-mongodb:test \
    --no-build-cache --no-configuration-cache
```

## 운영에서 볼 신호

이 모듈은 metric을 추가하지 않으므로 Spring Boot와 MongoDB driver의 관찰 기능을 사용합니다.

| 신호 | 확인할 문제 |
| --- | --- |
| server selection 시간 | topology 변화, DNS·network, primary 부재 |
| pool wait와 in-use connection | pool 고갈, 긴 query, 느린 Flow consumer |
| command latency와 error code | index 누락, timeout, duplicate key, write concern |
| cancellation·request latency | client disconnect, deadline, 과도한 결과 |
| JVM shutdown 시 active subscription | tailable cursor와 scope 정리 누락 |

## 어떤 MongoDB 계층을 선택할까

| 필요한 것 | 시작점 |
| --- | --- |
| Spring Data mapping + `ReactiveMongoOperations` + coroutine | 이 모듈 |
| Spring Data repository interface | Spring Data reactive/coroutine repository |
| MongoDB Kotlin driver, codec, collection API 직접 제어 | [`bluetape4k-mongodb`](../bluetape4k-mongodb.md) |
| Spring Boot client·property·health 구성 | Spring Boot MongoDB auto-configuration |

두 bluetape4k 모듈을 함께 사용할 수 있지만 같은 repository에서 Spring Data mapping과 native codec mapping을 무분별하게 섞지 않습니다. document wire format과 transaction/session 경계를 한 계층이 명확히 소유하도록 정합니다.

## Source와 tests

- [`AbstractReactiveMongoTest.kt`](../../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/AbstractReactiveMongoTest.kt)
- [`AbstractReactiveMongoCoroutineTest.kt`](../../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/AbstractReactiveMongoCoroutineTest.kt)
- [`MongoTestApplication.kt`](../../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/MongoTestApplication.kt)
- [`ReactiveMongoOperationsCoroutinesTest.kt`](../../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/coroutines/ReactiveMongoOperationsCoroutinesTest.kt)
- [`User.kt`](../../../../../spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/model/User.kt)

## 다음 단계

도입할 계층을 정했다면 [자동 설정과 구성 경계](./auto-configuration-boundaries.md)로 돌아가 애플리케이션이 소유할 bean과 설정을 고정합니다.
