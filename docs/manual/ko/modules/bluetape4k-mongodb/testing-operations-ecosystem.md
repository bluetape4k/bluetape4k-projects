---
title: 테스트, 운영과 생태계 경로
description: server 없는 helper test와 Testcontainers 통합 검증을 구분하고 MongoDB driver에서 Spring Data MongoDB로 이어지는 학습 경로를 안내합니다.
manualId: bluetape4k-mongodb
chapterId: testing-operations-ecosystem
---

# 테스트, 운영과 생태계 경로

## server 없이 확인할 계약

`DocumentExtensionsTest`는 pair·builder 생성, null value, reified safe cast와 type mismatch를 검증합니다. `AggregationSupportTest`는 stage BSON 구조를 검사합니다.

이 두 test는 Docker 없이 빠르게 실행할 수 있습니다. helper를 변경하거나 manual code를 검토할 때 가장 먼저 확인할 unit contract입니다.

## MongoDB가 필요한 test

client, database, collection과 examples test는 `AbstractMongoTest`를 상속합니다. fixture는 `MongoDBServer.Launcher.mongoDB` Testcontainer를 지연 시작하고 coroutine client를 만들어 test class가 끝날 때 닫습니다.

```bash
./gradlew :bluetape4k-mongodb:test --no-configuration-cache
```

Testcontainers를 사용하는 다른 module과 동시에 무겁게 실행하지 않습니다. Docker image 준비, port와 shared machine resource 때문에 실패 원인을 흐릴 수 있으므로 순차 실행합니다.

## fixture는 published API가 아니다

README의 test support 예제에 `AbstractMongoTest`가 나오지만 class는 `data/mongodb/src/test`에 있습니다. main artifact consumer가 import할 수 있는 production API가 아닙니다.

애플리케이션은 자체 test fixture에서 MongoDB container를 시작하고 coroutine `MongoClient`를 연결합니다. testcontainers helper의 sync client와 coroutine client type을 혼동하지 않습니다.

## 1.12.1 test coverage의 경계

release tests는 다음을 실제 MongoDB에서 확인합니다.

- 직접 client 생성과 provider의 같은-key identity
- database·collection 이름 수집
- `findFirst`, `exists`, `upsert`, filter·sort·skip·limit
- native suspend CRUD와 helper 조합
- 대표 aggregation 결과

반면 transaction commit·abort·cancellation, provider의 장기 cache 증가, custom codec, authentication·TLS, replica set failover와 rolling schema compatibility는 직접 검증하지 않습니다.

## 애플리케이션에서 추가할 test

1. production과 같은 credential, TLS와 codec registry로 client를 만듭니다.
2. 애플리케이션 타입과 기존 document를 양방향으로 decode합니다.
3. cancellation과 timeout 뒤 pool·session이 회수되는지 확인합니다.
4. transaction 사용 시 replica set fixture에서 commit·abort와 transient error를 검증합니다.
5. pagination sort가 동일한 값과 concurrent insert에서도 안정적인지 확인합니다.
6. 이전/신규 애플리케이션 버전 사이의 document 호환성을 검사합니다.

## 운영 지표와 장애 분류

MongoDB command failure, codec failure, pool wait timeout, server selection timeout과 application cancellation을 분리합니다. operation·database·collection은 제한된 label 집합으로 관리하고 document id나 filter 값은 metric label로 쓰지 않습니다.

새 배포 후 decode failure가 늘면 connection 장애로 뭉뚱그리지 말고 codec, field rename과 저장된 BSON type을 확인합니다. transaction abort failure는 원래 예외의 suppressed list도 함께 살펴봅니다.

## 생태계 학습 경로

- coroutine driver의 client·collection·BSON helper: 이 매뉴얼
- Spring Boot auto-configuration, reactive operation과 Criteria DSL: [`bluetape4k-spring-boot-mongodb`](../bluetape4k-spring-boot-mongodb.md)
- coroutine cancellation과 Flow 설계: [`bluetape4k-coroutines`](../bluetape4k-coroutines.md)
- Testcontainers 공통 기반: [`bluetape4k-testcontainers`](../bluetape4k-testcontainers.md)

Spring Data MongoDB는 mapping, repository와 exception translation을 추가하지만 low-level driver 책임이 사라지지는 않습니다. client ownership과 codec·query 비용을 이해한 뒤 framework 기능을 올리는 순서가 안전합니다.

## Source와 tests

- [`AbstractMongoTest.kt`](../../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/AbstractMongoTest.kt)
- [`MongoClientSupportTest.kt`](../../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/MongoClientSupportTest.kt)
- [`MongoCollectionExtensionsTest.kt`](../../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/MongoCollectionExtensionsTest.kt)
- [`DocumentExtensionsTest.kt`](../../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/bson/DocumentExtensionsTest.kt)
- [`AggregationExamples.kt`](../../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/examples/AggregationExamples.kt)
- [`BasicCrudExamples.kt`](../../../../../data/mongodb/src/test/kotlin/io/bluetape4k/mongodb/examples/BasicCrudExamples.kt)
