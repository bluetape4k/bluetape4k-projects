---
title: "구성과 객체 소유권"
description: "auto-configuration이 아닌 범위를 확인하고 CqlSession, template, repository의 소유권을 정합니다."
---

# 구성과 객체 소유권

## 이 artifact가 구성하지 않는 것

이름에 `spring-boot`가 들어가지만 1.12.1 소스에는 auto-configuration class, `AutoConfiguration.imports`, `@ConfigurationProperties`, main resource가 없습니다. classpath에 artifact를 추가해도 새 bean이나 property가 생기지 않습니다.

이 모듈이 제공하는 것은 이미 생성된 Spring Data Cassandra 객체에 대한 확장 함수입니다. 따라서 다음 객체는 Spring Boot 또는 애플리케이션 configuration이 준비해야 합니다.

- `CqlSession`과 `ReactiveSession`
- `CassandraOperations`, `ReactiveCassandraOperations`, `AsyncCassandraOperations`
- `CqlOperations`, `ReactiveCqlOperations`, `AsyncCqlOperations`
- mapping context, converter와 repository infrastructure

## CqlSession은 애플리케이션이 소유한다

`ReactiveSession.executeSuspending`과 `prepareSuspending`은 수신 객체에서 `execute`·`prepare`를 호출할 뿐 session을 만들거나 닫지 않습니다. contact point, local datacenter, keyspace, authentication, request profile도 바꾸지 않습니다.

```kotlin
@Configuration(proxyBeanMethods = false)
class CassandraConfiguration : AbstractReactiveCassandraConfiguration() {
    override fun getKeyspaceName(): String = "app"
    override fun getLocalDataCenter(): String = "datacenter1"
}
```

실제 값은 환경별 Spring Boot 설정이나 secret store에서 공급합니다. `CqlSession`은 thread-safe하고 연결 pool과 executor를 소유하므로 service나 요청마다 새로 만들지 않습니다. Spring이 만든 session은 Spring lifecycle에 맡기고 임의로 닫지 않습니다.

## Spring Data와 driver 경계

entity mapping, lifecycle callback, optimistic locking, repository query derivation은 Spring Data Cassandra 책임입니다. driver codec, prepared statement, consistency와 execution profile은 DataStax driver 계약입니다. bluetape4k 확장은 두 계층의 결과를 coroutine으로 기다리거나 Flow로 노출합니다.

`bluetape4k-cassandra`는 driver 수준 API를 보완합니다. Spring Data mapping이 필요 없다면 그 모듈과 `CqlSession`만 사용하는 편이 단순합니다. 반대로 repository와 converter가 필요하다면 이 모듈의 Spring Data 경계를 유지합니다.

## 선택 기준

| 상황 | 권장 진입점 |
| --- | --- |
| repository 중심 CRUD | Spring Data repository, 필요할 때 template coroutine 확장 |
| 동적 `Query`와 entity mapping | `ReactiveCassandraOperations` 또는 `AsyncCassandraOperations` |
| row 단위 streaming | reactive operations + `Flow` |
| driver statement와 paging 직접 제어 | `bluetape4k-cassandra` + driver API |
| 애플리케이션 schema migration | 전용 migration 절차; `SchemaGenerator`를 대체재로 쓰지 않음 |

하나의 service에서 repository, reactive template, async template과 raw session을 동시에 섞으면 transaction과 오류 변환 경계가 흐려집니다. 필요한 수준을 먼저 고르고 한 단계 아래로 내려갈 때만 명시적으로 raw API를 사용합니다.

## 실제 테스트에서 확인되는 소유권

`AbstractCassandraTestConfiguration`과 reactive 버전은 `getRequiredSession()`을 override해 companion object의 공유 session을 돌려줍니다. 여러 Spring application context가 각자 session을 만들면 Testcontainers Cassandra 연결이 빠르게 누적되기 때문입니다. 이 구조는 운영에서도 session을 장수 객체로 관리해야 한다는 점을 보여 줍니다.

## 다음 단계

구성을 정했다면 [Reactive operations와 coroutine](./reactive-coroutine-operations.md)에서 subscription과 빈 결과 계약을 확인합니다. 직접 CQL을 실행한다면 [Async와 저수준 CQL operations](./async-and-cql-operations.md)로 이동합니다.

## 근거

- [모듈 build](../../../../../spring-boot/cassandra/build.gradle.kts)
- [`ReactiveSessionCoroutines.kt`](../../../../../spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/ReactiveSessionCoroutines.kt)
- [`AbstractReactiveCassandraTestConfiguration.kt`](../../../../../spring-boot/cassandra/src/test/kotlin/io/bluetape4k/spring/cassandra/AbstractReactiveCassandraTestConfiguration.kt)
