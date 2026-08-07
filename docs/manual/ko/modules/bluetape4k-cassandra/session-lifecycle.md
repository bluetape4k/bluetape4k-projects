---
title: CqlSession 수명주기와 캐시 경계
description: 직접 만든 세션과 provider가 관리하는 세션의 소유권, identity와 1.12.1 bootstrap 제한을 설명합니다.
manualId: bluetape4k-cassandra
chapterId: session-lifecycle
---

# CqlSession 수명주기와 캐시 경계

## 문제

`CqlSession`은 연결 풀과 드라이버 실행 자원을 소유합니다. 요청마다 세션을 만들면 종료 책임은 분명하지만 연결 비용이 커집니다. 반대로 세션을 재사용하면서 keyspace만 캐시 key로 삼으면 서로 다른 접속 지점이나 설정 집합이 같은 세션을 받을 수 있습니다. 먼저 직접 소유할지 `CqlSessionProvider`에 맡길지 결정해야 합니다.

## 직접 생성

한 작업이나 수명이 제한된 컴포넌트가 세션을 소유한다면 `cqlSessionOf`로 만들고 `use`로 닫습니다. `InetSocketAddress`를 명시하면 접속 대상도 호출 지점에서 확인할 수 있습니다.

```kotlin
import io.bluetape4k.cassandra.cqlSessionOf
import java.net.InetSocketAddress

fun readReleaseVersion(): String? {
    val contactPoint = InetSocketAddress("127.0.0.1", 9042)

    return cqlSessionOf(
        contactPoint = contactPoint,
        localDatacenter = "datacenter1",
        keyspaceName = "system",
    ).use { session ->
        session.execute("SELECT release_version FROM system.local")
            .one()
            ?.getString("release_version")
    }
}
```

이 방식에서는 함수가 세션을 만들었으므로 함수가 닫습니다. 반환한 `Row`나 페이지 처리 상태를 `use` 밖에서 계속 읽지 않습니다.

## Provider identity

애플리케이션 범위에서 세션을 재사용한다면 `CqlSessionIdentity`에 수가 제한된 설정 차원만 넣습니다. `of`는 각 `contextParts` 원소의 앞뒤 공백과 빈 값을 제거한 뒤 정렬해서 안정된 `context` 문자열을 만듭니다. 같은 identity는 같은 열린 세션을 재사용하고, 다른 identity는 같은 keyspace라도 별도 세션을 만듭니다.

```kotlin
import com.datastax.oss.driver.api.core.CqlSession
import io.bluetape4k.cassandra.CqlSessionIdentity
import io.bluetape4k.cassandra.CqlSessionProvider
import java.net.InetSocketAddress

fun tenantOrdersSession(
    username: String,
    password: String,
): CqlSession {
    val contactPoint = InetSocketAddress("cassandra-a.example.com", 9042)
    val localDatacenter = "dc-a"
    val clientId = "order-reader"
    val identity = CqlSessionIdentity.of(
        keyspace = "tenant_orders",
        contextParts = listOf(
            "contactPoint=${contactPoint.hostString}:${contactPoint.port}",
            "localDatacenter=$localDatacenter",
            "routingProfile=tenant-a-primary",
            "credentialVersion=orders-v3",
            "clientId=$clientId",
        ),
    )

    return CqlSessionProvider.getOrCreateSession(
        identity = identity,
        builderSupplier = {
            CqlSession.builder()
                .addContactPoint(contactPoint)
                .withLocalDatacenter(localDatacenter)
                .withAuthCredentials(username, password)
        },
    ) {
        withApplicationName("order-reader")
    }
}
```

`builderSupplier`는 호출될 때마다 새 builder를 반환해야 합니다. 1.12.1은 새 세션을 만들 때 `${identity.context}`를 INFO 로그에 기록합니다. 따라서 비밀번호, 토큰, 실제 사용자 이름, 테넌트나 고객 식별자는 `context`에 넣지 않습니다. 로그에 노출해도 되는 불투명한 라우팅 프로필 ID나 자격 증명 버전만 사용합니다.

요청 ID, 상관관계 ID, 임의 UUID처럼 요청마다 달라지는 값도 identity에 넣으면 안 됩니다. 1.12.1 캐시에는 크기 제한이나 유휴 제거(`idle eviction`)가 없어서 값이 달라질 때마다 세션이 늘어나며, 닫거나 프로세스를 종료할 때까지 남습니다. 위 예제의 `clientId`, `routingProfile`, `credentialVersion`은 배포 설정에서 가져오는 수가 제한된 값입니다.

## 1.12.1 bootstrap 제한

1.12.1에서 `CqlSessionProvider`는 keyspace가 없을 때를 대비해 두 세션을 순서대로 만듭니다.

```kotlin
builderSupplier().build().use { adminSession ->
    CassandraAdmin.createKeyspace(adminSession, identity.keyspace)
}

builderSupplier()
    .withKeyspace(identity.keyspace)
    .apply(builder)
    .build()
```

따라서 `builderSupplier`는 관리 세션과 최종 세션에 모두 필요한 접속 지점, `localDatacenter`, 인증, TLS 설정을 제공해야 합니다. 마지막 builder 블록은 최종 세션에만 적용됩니다. 위 예제의 `withApplicationName("order-reader")`가 bootstrap 세션에는 적용되지 않는 이유입니다.

이 제약은 1.12.1 뒤에 병합된 bootstrap builder 수정 전 동작입니다. 애플리케이션이 keyspace DDL을 맡지 않거나 bootstrap에 별도 설정이 필요하면 배포 단계에서 keyspace를 관리하고, 직접 소유하는 세션을 `cqlSessionOf`로 여는 방식을 선택합니다.

## 종료

직접 만든 세션은 `use` 또는 명시적 `close`로 닫습니다. `CqlSessionProvider`가 만든 최종 세션은 `ShutdownQueue`에 등록되므로 일반 호출마다 `use`로 감싸지 않습니다. 그렇게 하면 첫 호출이 공유 세션을 닫아 다른 호출의 재사용을 깨뜨립니다.

`CqlSessionProvider`가 관리하는 세션의 기본 수명은 프로세스나 이를 소유한 컴포넌트의 수명과 같습니다. 특정 identity를 명시적으로 닫거나 폐기하려면 먼저 그 identity를 사용하는 모든 호출을 중지하고 진행 중인 작업이 끝날 때까지 기다려야 합니다. `CqlSessionProvider`에는 원자적인 `retire`/`evict` API가 없습니다. 세션을 닫아도 캐시 항목은 즉시 제거되지 않으며, 다음 `getOrCreateSession` 호출이 닫힌 항목을 발견해야 제거됩니다. 종료 시점의 일괄 정리는 `ShutdownQueue`가 맡습니다.

## 실패 표

| 상황 | 1.12.1 동작 | 대응 |
| --- | --- | --- |
| 빈 keyspace | `CqlSessionIdentity`와 keyspace 오버로드가 `IllegalArgumentException`을 던집니다. | 입력 경계에서 keyspace를 검증합니다. |
| 빈 local datacenter | `newCqlSessionBuilder`가 `IllegalArgumentException`을 던집니다. | 드라이버 설정을 읽은 직후 빈 값을 거부합니다. |
| 같은 keyspace, 다른 connection context | 명시적 identity가 다르면 세션을 재사용하지 않습니다. | 로그에 안전하고 수가 제한된 설정 ID를 `context`에 넣습니다. |
| 요청마다 다른 값을 identity에 사용 | 캐시에 크기 제한이나 유휴 제거가 없어 세션 수가 계속 늘어납니다. | 요청 ID나 임의 UUID를 제외하고 배포 설정 차원만 사용합니다. |
| 캐시에 닫힌 세션이 남음 | 다음 `CqlSessionProvider` 조회가 `isClosed` 항목을 제거하고 해당 identity를 다시 생성합니다. | 모든 사용자를 먼저 정지합니다. 원자적인 `retire`/`evict` API가 없음을 전제로 전환합니다. |
| bootstrap에 필요한 설정을 builder 블록에만 둠 | admin 세션에는 블록이 적용되지 않아 연결이나 인증이 먼저 실패할 수 있습니다. | 공통 설정을 `builderSupplier`로 옮기거나 keyspace를 별도로 관리합니다. |

## Source와 tests

- [`CqlSessionProvider.kt`](../../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/CqlSessionProvider.kt): identity 생성, INFO `context` 로그, 열린 세션 재사용, 닫힌 캐시 제거, bootstrap과 최종 세션 생성, `ShutdownQueue` 등록
- [`CqlSessionSupport.kt`](../../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/CqlSessionSupport.kt): `cqlSession`과 `cqlSessionOf`
- [`CassandraAdmin.kt`](../../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/CassandraAdmin.kt): `CREATE KEYSPACE IF NOT EXISTS` 실행
- [`ShutdownQueue.kt`](../../../../../bluetape4k/core/src/main/kotlin/io/bluetape4k/utils/ShutdownQueue.kt): 프로세스 종료 시 등록 자원 정리
- [`CqlSessionProviderTest.kt`](../../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/CqlSessionProviderTest.kt): 같은 identity 재사용, 다른 `context` 분리, 빈 keyspace와 `localDatacenter` 거부
- [`CqlSessionSupportTest.kt`](../../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/CqlSessionSupportTest.kt): 직접 만든 세션의 생성과 종료

## 다음 읽을 장

세션 소유권을 정했다면 [코루틴 쿼리](./coroutine-queries.md)에서 suspend 실행과 페이지 처리 경계를 확인합니다.
