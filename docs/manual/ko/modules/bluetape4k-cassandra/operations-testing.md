---
title: 운영 경계와 Testcontainers 검증
description: Keyspace 관리 side effect, 세션 종료, query·paging 진단과 Cassandra 통합 테스트를 설명합니다.
manualId: bluetape4k-cassandra
chapterId: operations-testing
---

# 운영 경계와 Testcontainers 검증

## CassandraAdmin은 cluster 상태를 바꾼다

`CassandraAdmin`은 편의용 조회 함수만 모아 둔 객체가 아닙니다. `createKeyspace`와 `dropKeyspace`는 실제 schema를 변경하고, `getReleaseVersion`은 `system.local`을 조회합니다.

```kotlin
import io.bluetape4k.cassandra.CassandraAdmin

val created = CassandraAdmin.createKeyspace(
    session = adminSession,
    keyspace = "orders",
    replicationFactor = 1,
)

val version = CassandraAdmin.getReleaseVersion(adminSession)
```

`createKeyspace`는 `CREATE KEYSPACE IF NOT EXISTS`와 `SimpleStrategy`를 사용합니다. 기본 replication factor는 1입니다. 이 기본값은 로컬 예제에는 편하지만 운영 cluster의 topology와 replication 정책을 대신하지 않습니다. 운영에서는 배포 도구가 만든 keyspace를 애플리케이션이 그대로 사용하도록 분리하는 편이 안전합니다.

`dropKeyspace`는 `DROP KEYSPACE IF EXISTS`를 동기 실행합니다. 테스트 정리에는 유용하지만 잘못된 keyspace를 넘기면 데이터 전체를 잃을 수 있습니다. 사용자 입력을 직접 전달하지 말고, 운영 계정에는 불필요한 schema 변경 권한을 주지 않습니다.

## 권한과 bootstrap 설정

`CqlSessionProvider` 1.12.1은 대상 keyspace를 만들기 위해 먼저 admin session을 엽니다. 이 admin session은 `builderSupplier`가 반환한 builder로 만들고, 마지막 builder 블록은 최종 session에만 적용합니다. 접속 지점, `localDatacenter`, 인증, TLS처럼 bootstrap에도 필요한 설정은 `builderSupplier`에 있어야 합니다.

```kotlin
val session = CqlSessionProvider.getOrCreateSession(
    identity = identity,
    builderSupplier = {
        CqlSessionProvider.newCqlSessionBuilder(contactPoint, localDatacenter)
            .withAuthCredentials(username, password)
    },
) {
    withApplicationName("order-reader")
}
```

admin 계정에는 keyspace 생성 권한이 필요합니다. 애플리케이션 계정에 그 권한을 줄 수 없거나 admin과 업무 접속 설정을 분리해야 한다면 배포 단계에서 keyspace를 관리하고 직접 소유하는 session을 엽니다. 이 제약은 1.12.1 뒤에 병합된 bootstrap builder 수정 전 동작입니다.

## 무엇을 관찰할까

운영 로그와 metric은 실패 지점을 구분할 수 있을 만큼만 남깁니다.

| 경계 | 관찰 항목 |
| --- | --- |
| keyspace 관리 | 작업 종류, 허용된 keyspace, replication 설정, `wasApplied`, 실패 예외 |
| session 생성 | 접속 대상의 안전한 별칭, local datacenter, identity의 불투명한 설정 ID |
| query 실행 | query shape, consistency, timeout, 성공·실패·취소 |
| paging | 소비한 row/page 수, mapper 실패, 다음 페이지 fetch 실패, collection 취소 |
| batch | batch type, partition 범위, statement 수, payload, 지연과 timeout |
| 종료 | direct/provider 소유권, close 시작·완료, 진행 중인 작업 수 |

1.12.1은 session을 만들 때 `CqlSessionIdentity.context`를 INFO로 기록합니다. 비밀번호, token, 실제 사용자·tenant·고객 식별자를 context에 넣지 않습니다. 요청 ID나 임의 UUID도 cache key의 종류를 끝없이 늘리므로 사용하지 않습니다. 로그에 허용된 수가 제한된 routing profile ID나 credential version만 사용합니다.

query value를 그대로 기록하면 개인정보와 credential이 노출될 수 있습니다. 진단에는 CQL 구조와 marker 이름을 우선 사용하고 실제 값은 애플리케이션의 마스킹 정책을 따릅니다.

## 장애 경계표

| 증상 | 먼저 확인할 경계 |
| --- | --- |
| bootstrap 인증 또는 연결 실패 | 1.12.1 admin session에 필요한 설정이 `builderSupplier`에 있는지 확인 |
| 같은 keyspace의 잘못된 session 재사용 | `CqlSessionIdentity` context에 connection/tenant 경계가 있는지 확인 |
| Flow가 일부 row만 반환 | collection cancellation, mapper exception, next-page fetch failure 확인 |
| 종료 후 connection이 남음 | direct session과 provider-owned session의 종료 책임 구분 |
| batch 지연 또는 timeout | partition, statement 수, consistency와 timeout 확인 |

일부 row만 받은 경우 `asFlow`가 전체 결과를 원자적으로 모아 주는 API라고 가정하지 않습니다. 매퍼나 수집자가 실패하기 전에 방출된 row는 이미 처리됐을 수 있습니다. 다음 페이지 조회 실패는 현재 페이지를 다 읽은 뒤 드러납니다.

직접 만든 session은 `use`나 `close`로 닫습니다. provider가 만든 session은 공유 자원이며 `ShutdownQueue`에 등록됩니다. 일반 호출에서 `use`로 감싸지 말고, 명시적으로 닫아야 한다면 해당 identity의 신규 사용을 막고 진행 중인 작업이 끝난 뒤 처리합니다. provider에는 원자적인 retire/evict API가 없습니다.

## Testcontainers 통합 테스트 실행

모듈 테스트는 실제 Cassandra 동작이 필요한 부분을 Testcontainers로 검증합니다.

```bash
./gradlew :bluetape4k-cassandra:test --no-build-cache --no-configuration-cache
```

이 명령은 Docker runtime과 Cassandra image를 내려받거나 실행할 수 있는 환경이 필요합니다. container, port, CPU와 디스크를 공유하므로 다른 heavy Testcontainers test와 동시에 돌리지 않고 순차 실행합니다. Docker daemon 연결 실패, image pull 실패, 기동 timeout은 테스트 assertion 실패와 구분해서 보고하되 성공으로 간주하지 않습니다.

`AbstractCassandraTest`는 Cassandra 4 container session을 만들고 `@AfterAll`에서 닫으며 `SAME_THREAD`로 실행합니다. `CassandraAdminTest`는 create/drop/version과 blank keyspace 거부를, `CqlSessionProviderTest`는 identity 재사용과 connection context 분리를 확인합니다. `AsyncResultSetSupportTest`는 6,000 rows를 넣고 일반 row와 mapped row가 여러 페이지를 거쳐 모두 수집되는지 검증합니다.

운영 장애를 재현할 때는 가장 가까운 테스트부터 실행하되, 최종 확인은 위 모듈 명령으로 합니다. mock 기반 테스트만 통과했다고 해서 인증, schema 권한, paging, container lifecycle까지 검증됐다고 보지 않습니다.

## 소스와 대표 테스트

- [`CassandraAdmin.kt`](../../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/CassandraAdmin.kt): keyspace create/drop과 release version 조회
- [`CqlSessionProvider.kt`](../../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/CqlSessionProvider.kt): 1.12.1 bootstrap, identity cache, 종료 등록
- [`AbstractCassandraTest.kt`](../../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/AbstractCassandraTest.kt): Cassandra 4 Testcontainers fixture와 session 종료
- [`CassandraAdminTest.kt`](../../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/CassandraAdminTest.kt): schema side effect와 version 검증
- [`CqlSessionProviderTest.kt`](../../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/CqlSessionProviderTest.kt): identity reuse와 connection context 분리
- [`AsyncResultSetSupportTest.kt`](../../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/cql/AsyncResultSetSupportTest.kt): 6,000 rows multi-page Flow 통합 테스트

## 이어 읽기

- 이전: [Statement와 QueryBuilder 선택](./statements-query-builder.md)
- 전체 구조: [bluetape4k-cassandra 매뉴얼](../bluetape4k-cassandra.md)
