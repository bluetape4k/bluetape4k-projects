---
manualId: bluetape4k-cassandra
title: "Module bluetape4k-cassandra"
description: "Apache Cassandra Java Driver를 Kotlin의 세션 수명주기, 코루틴 쿼리와 타입 변환 관점에서 사용하는 방법을 설명합니다."
kind: library
group: data
---

# Module bluetape4k-cassandra

## 이 라이브러리가 맡는 일

`bluetape4k-cassandra`는 Apache Cassandra Java Driver 위에 Kotlin용 세션 생성 함수, 코루틴 쿼리, row와 statement 확장을 제공합니다. 이 모듈은 Cassandra cluster나 schema를 운영하지 않습니다. 애플리케이션이 접속 주소, 인증 정보, keyspace와 세션 종료 시점을 결정해야 합니다.

## 사용하기 전에 결정할 것

- 한 작업 안에서 세션을 만들고 닫을지, 애플리케이션 전체에서 재사용할지 정합니다.
- 재사용한다면 keyspace뿐 아니라 접속 지점, 데이터센터, 라우팅 프로필, 자격 증명 버전, client ID처럼 수가 제한된 설정 값을 캐시 경계에 반영합니다.
- 동기 `execute`와 코루틴용 `executeSuspending` 가운데 호출 계층에 맞는 API를 고릅니다.
- keyspace 생성 권한을 애플리케이션에 줄지, 배포 단계에서 별도로 관리할지 정합니다.

## 의존성 추가

개별 bluetape4k 버전을 반복해서 적지 않고 중앙 BOM 버전만 지정합니다.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-cassandra")
}
```

## 첫 쿼리

직접 만든 세션은 만든 코드가 닫습니다. `use` 안에 쿼리를 두면 정상 반환과 예외 모두에서 세션이 닫힙니다.

```kotlin
import io.bluetape4k.cassandra.cqlSessionOf
import java.net.InetSocketAddress

val contactPoint = InetSocketAddress("127.0.0.1", 9042)

val releaseVersion = cqlSessionOf(
    contactPoint = contactPoint,
    localDatacenter = "datacenter1",
    keyspaceName = "system",
).use { session ->
    session.execute("SELECT release_version FROM system.local")
        .one()
        ?.getString("release_version")
}
```

## API 선택 지도

| 필요한 작업 | 시작할 API | 소유권 또는 주의점 |
| --- | --- | --- |
| 짧은 범위에서 세션 생성 | `cqlSessionOf`, `cqlSession` | 호출 코드가 `use`나 `close`로 종료합니다. |
| 같은 접속 문맥의 세션 재사용 | `CqlSessionProvider`, `CqlSessionIdentity` | identity가 캐시 경계이며 provider가 종료 큐에 등록합니다. |
| 코루틴에서 쿼리 실행과 prepare | `executeSuspending`, `prepareSuspending` | 호출한 코루틴의 취소와 페이지 처리 경계를 유지합니다. |
| `Row`와 드라이버 값을 Kotlin 타입으로 변환 | `RowSupport`, `GettableSupport`, `DataTypeSupport` | null과 column type 계약을 먼저 확인합니다. |
| statement와 query builder 조립 | `StatementSupport`, `QueryBuilderSupport` | consistency, timeout, keyspace를 호출 지점에서 드러냅니다. |
| keyspace 관리와 통합 테스트 | `CassandraAdmin`, `AbstractCassandraTest` | 운영 DDL 권한과 테스트 컨테이너 수명주기를 분리합니다. |

## 학습 경로

1. [CqlSession 수명주기와 캐시 경계](./bluetape4k-cassandra/session-lifecycle.md)
2. [코루틴 쿼리](./bluetape4k-cassandra/coroutine-queries.md)
3. [Row와 data mapping](./bluetape4k-cassandra/rows-data-mapping.md)
4. [Statement와 query builder](./bluetape4k-cassandra/statements-query-builder.md)
5. [운영과 테스트](./bluetape4k-cassandra/operations-testing.md)

## 1.11.0에서 알아둘 제한

1.11.0의 `CqlSessionProvider`는 keyspace bootstrap용 관리 세션을 `builderSupplier().build()`로 만듭니다. 마지막 builder 블록은 keyspace에 연결할 최종 세션에만 적용됩니다. 따라서 두 세션에 모두 필요한 접속 지점, `localDatacenter`, 인증, TLS 설정은 `builderSupplier`에 넣어야 합니다. 이 동작은 1.11.0 뒤에 병합된 PR #986의 동작과 다릅니다.

## Source와 tests

- [`CqlSessionProvider.kt`](../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/CqlSessionProvider.kt)
- [`CqlSessionSupport.kt`](../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/CqlSessionSupport.kt)
- [`AsyncCqlSessionSupport.kt`](../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/AsyncCqlSessionSupport.kt)
- [`RowSupport.kt`](../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/RowSupport.kt)
- [`StatementSupport.kt`](../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/StatementSupport.kt)
- [`CqlSessionProviderTest.kt`](../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/CqlSessionProviderTest.kt)
- [`CqlSessionSupportTest.kt`](../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/CqlSessionSupportTest.kt)
