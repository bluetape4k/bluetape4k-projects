---
manualId: bluetape4k-cassandra
title: "Cassandra 코루틴 클라이언트"
description: "Apache Cassandra Java Driver를 Kotlin의 세션 수명주기, 코루틴 쿼리와 타입 변환 관점에서 사용하는 방법을 설명합니다."
kind: library
group: data
learningOrder: 620
---

# Cassandra 코루틴 클라이언트

<span id="이-라이브러리가-맡는-일"></span>

## 제공하는 기능 {#problem}

`bluetape4k-cassandra`는 Apache Cassandra Java Driver 위에 Kotlin용 세션 생성 함수, 세션 재사용 경계, 코루틴 쿼리, row mapping과 statement 확장을 제공합니다. 짧은 작업에서 세션을 직접 만들고 닫는 흐름부터 여러 페이지를 비동기로 읽고 드라이버 값을 Kotlin 타입으로 옮기는 작업까지, 애플리케이션 코드에서 반복되는 부분을 줄여 줍니다.

이 모듈이 Cassandra cluster나 schema를 운영하는 것은 아닙니다. 접속 주소, 인증 정보, keyspace, consistency와 세션 종료 시점은 애플리케이션이 결정해야 합니다.

## 사용하기 전에 결정할 것 {#when-to-use}

- 한 작업 안에서 세션을 만들고 닫을지, 애플리케이션 전체에서 재사용할지 정합니다.
- 재사용한다면 keyspace뿐 아니라 접속 지점, 데이터센터, 라우팅 프로필, 자격 증명 버전, client ID처럼 수가 제한된 설정 값을 캐시 경계에 반영합니다.
- 동기 `execute`와 코루틴용 `executeSuspending` 가운데 호출 계층에 맞는 API를 고릅니다.
- keyspace 생성 권한을 애플리케이션에 줄지, 배포 단계에서 별도로 관리할지 정합니다.

## 의존성 추가 {#coordinates}

개별 bluetape4k 버전을 반복해서 적지 않고 중앙 BOM 버전만 지정합니다.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-cassandra")
}
```

## 첫 쿼리 {#quick-start}

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

## API 선택 지도 {#api-by-task}

| 필요한 작업 | 시작할 API | 소유권 또는 주의점 |
| --- | --- | --- |
| 짧은 범위에서 세션 생성 | `cqlSessionOf`, `cqlSession` | 호출 코드가 `use`나 `close`로 종료합니다. |
| 같은 접속 문맥의 세션 재사용 | `CqlSessionProvider`, `CqlSessionIdentity` | identity가 캐시 경계이며 provider가 종료 큐에 등록합니다. |
| 코루틴에서 쿼리 실행과 prepare | `executeSuspending`, `prepareSuspending` | 호출한 코루틴의 취소와 페이지 처리 경계를 유지합니다. |
| `Row`와 드라이버 값을 Kotlin 타입으로 변환 | `RowSupport`, `GettableSupport`, `DataTypeSupport` | null과 column type 계약을 먼저 확인합니다. |
| statement와 query builder 조립 | `StatementSupport`, `QueryBuilderSupport` | consistency, timeout, keyspace를 호출 지점에서 드러냅니다. |
| keyspace 관리와 통합 테스트 | `CassandraAdmin`, `AbstractCassandraTest` | 운영 DDL 권한과 테스트 컨테이너 수명주기를 분리합니다. |

## 학습 경로 {#concepts}

아래 다섯 장은 API 이름만 나열하지 않습니다. 각 장은 문제를 이해하는 설명에서 시작해 실행 가능한 예제, API 선택 기준, 실패와 운영 경계, 1.12.1 소스와 테스트 근거까지 연결합니다. 처음 도입한다면 순서대로 읽고, 이미 사용 중이라면 지금 해결하려는 문제에 맞는 장부터 시작해도 됩니다.

1. **[CqlSession 수명주기와 캐시 경계](./bluetape4k-cassandra/session-lifecycle.md)**
   직접 만든 세션을 `use`로 닫는 가장 작은 예제부터 `CqlSessionProvider`와 `CqlSessionIdentity`로 공유 세션을 재사용하는 방법까지 설명합니다. 세션 소유권, 캐시 identity와 1.12.1 bootstrap 설정을 어디에 둘지 판단할 수 있습니다.
2. **[코루틴 쿼리](./bluetape4k-cassandra/coroutine-queries.md)**
   `executeSuspending`, `prepareSuspending`과 `AsyncResultSet.asFlow()`로 단일 결과와 여러 페이지를 읽는 예제를 다룹니다. 취소, mapper 예외와 다음 페이지 조회가 호출자에게 어떻게 전달되는지 확인할 수 있습니다.
3. **[Row와 data mapping](./bluetape4k-cassandra/rows-data-mapping.md)**
   `Row`, collection, tuple, UDT와 `CqlDuration`을 Kotlin 값과 도메인 객체로 옮기는 예제를 제공합니다. null을 기본값으로 바꿀 때와 값이 없다는 사실을 보존할 때를 구분할 수 있습니다.
4. **[Statement와 query builder](./bluetape4k-cassandra/statements-query-builder.md)**
   raw CQL, prepared/bound statement와 QueryBuilder로 같은 작업을 표현하는 방법을 비교합니다. bind marker, consistency, timeout, page size와 keyspace를 어느 경계에서 드러낼지 선택할 수 있습니다.
5. **[운영과 테스트](./bluetape4k-cassandra/operations-testing.md)**
   keyspace 생성·삭제의 side effect, 세션 종료, paging 실패와 Testcontainers 검증을 한 흐름으로 정리합니다. 운영 권한을 애플리케이션과 배포 단계 중 어디에 둘지 결정하고 대표 장애를 진단할 수 있습니다.

## 권장 패턴 {#patterns}

직접 만든 세션은 만든 코드가 닫고, 공유 세션은 종류가 제한된 설정 값으로 구성한 `CqlSessionIdentity`를 기준으로 재사용합니다. 쿼리 값은 바인드 마커로 분리하고, `Row`는 조회 경계에서 도메인 타입으로 옮깁니다. 여러 페이지 결과는 부분 소비와 취소 가능성을 전제로 처리합니다.

## 연동 {#integrations}

Apache Cassandra Java Driver의 core, query builder, mapper runtime 위에서 동작하며 Kotlin Coroutines로 비동기 실행과 paging을 연결합니다. DataStax Mapper가 생성한 `EntityHelper`를 쓰려면 애플리케이션 빌드에도 DataStax Mapper annotation processor 설정이 필요합니다.

## 설정 {#configuration}

접속 지점, `localDatacenter`, 인증, TLS, keyspace와 statement consistency·timeout은 애플리케이션 설정입니다. provider identity에는 로그에 남겨도 되는 수가 제한된 connection/credential 설정 ID만 넣습니다.

## 실패 동작 {#failures}

빈 keyspace와 `localDatacenter`는 입력 경계에서 거부됩니다. 쿼리 준비·실행, 행 매퍼와 다음 페이지 조회 실패는 각 작업 지점에서 호출자에게 전파됩니다. bootstrap 인증 오류는 1.12.1의 admin session 설정 경계를 먼저 확인합니다.

## 운영 {#operations}

keyspace create/drop은 실제 cluster side effect입니다. 운영 권한과 replication 정책을 배포 단계와 분리하고, session 종료 책임, query·paging 실패, batch 크기와 timeout을 관찰합니다. 자세한 기준은 [운영 경계와 Testcontainers 검증](./bluetape4k-cassandra/operations-testing.md)에 정리했습니다.

## 테스트 {#testing}

실제 Cassandra 동작은 Docker가 필요한 Testcontainers 테스트로 검증합니다. 다른 heavy integration test와 병렬 실행하지 않습니다.

```bash
./gradlew :bluetape4k-cassandra:test --no-build-cache --no-configuration-cache
```

## 워크숍 {#workshops}

이 모듈 전용 워크숍은 아직 없습니다. 대신 각 장의 예제와 1.12.1으로 검증한 소스·테스트 링크를 따라가면 session, coroutine paging, mapping, QueryBuilder와 운영 경계를 순서대로 실습할 수 있습니다.

## 1.12.1에서 알아둘 제한 {#limitations}

1.12.1의 `CqlSessionProvider`는 keyspace bootstrap용 관리 세션을 `builderSupplier().build()`로 만듭니다. 마지막 builder 블록은 keyspace에 연결할 최종 세션에만 적용됩니다. 따라서 두 세션에 모두 필요한 접속 지점, `localDatacenter`, 인증, TLS 설정은 `builderSupplier`에 넣어야 합니다. 이 동작은 1.12.1 뒤에 병합된 PR #986의 동작과 다릅니다.

## Source와 tests {#sources}

- [`CqlSessionProvider.kt`](../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/CqlSessionProvider.kt)
- [`CqlSessionSupport.kt`](../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/CqlSessionSupport.kt)
- [`AsyncCqlSessionSupport.kt`](../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/AsyncCqlSessionSupport.kt)
- [`RowSupport.kt`](../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/RowSupport.kt)
- [`StatementSupport.kt`](../../../../data/cassandra/src/main/kotlin/io/bluetape4k/cassandra/cql/StatementSupport.kt)
- [`CqlSessionProviderTest.kt`](../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/CqlSessionProviderTest.kt)
- [`CqlSessionSupportTest.kt`](../../../../data/cassandra/src/test/kotlin/io/bluetape4k/cassandra/CqlSessionSupportTest.kt)

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### 확장 함수 API 개요 다이어그램

[![확장 함수 API 개요 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-cassandra-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-cassandra-diagram-01.svg)

_배포본 README: [`data/cassandra/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/data/cassandra/README.ko.md)_

### 주요 API 구조 다이어그램

[![주요 API 구조 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-cassandra-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-cassandra-diagram-02.svg)

_배포본 README: [`data/cassandra/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/data/cassandra/README.ko.md)_

### 비동기 쿼리 실행 흐름 다이어그램

[![비동기 쿼리 실행 흐름 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-cassandra-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/data-cassandra-sequence-01.svg)

_배포본 README: [`data/cassandra/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/data/cassandra/README.ko.md)_

<!-- release-readme-diagrams:end -->
