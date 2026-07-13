---
manualId: bluetape4k-jdbc
title: "Module bluetape4k-jdbc"
description: "JDBC(Java Database Connectivity) 사용 시 반복 코드를 줄이는 Kotlin 확장 라이브러리입니다. Connection 처리, Statement 실행, ResultSet 매핑, 트랜잭션 블록을 간결하게 만들면서도 JDBC의 기본 동작은 그대로 유지합니다."
kind: library
group: data
---

# Module bluetape4k-jdbc

## 해결하는 문제 {#problem}

JDBC(Java Database Connectivity) 사용 시 반복 코드를 줄이는 Kotlin 확장 라이브러리입니다. Connection 처리, Statement 실행, ResultSet 매핑, 트랜잭션 블록을 간결하게 만들면서도 JDBC의 기본 동작은 그대로 유지합니다. 이 매뉴얼은 README의 기능 목록을 반복하지 않고 현재 build, source entry point, test, 설정 resource, lifecycle 근거를 연결합니다.

## 사용 시점 {#when-to-use}

애플리케이션에 transaction boundary, connection ownership, query 동작, serialization이 필요할 때 `bluetape4k-jdbc`를 선택합니다. 아래 source entry point에서 시작해 ownership과 failure 계약이 caller lifecycle에 맞는지 확인합니다. 표준 API나 이미 도입한 더 작은 모듈이 같은 계약을 만족한다면 그쪽을 우선합니다.

## 의존성 좌표 {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-jdbc")
}
```

Gradle project path는 `:bluetape4k-jdbc`, source directory는 `data/jdbc`입니다.

## 핵심 개념 {#concepts}

먼저 확인할 소스 개념은 `JdbcDrivers`, `HikariSupport`, `ArgumentSetter`, `ConnectionExtensions`, `DataSourceExtensions`, `DataSourceTransactionExtensions`, `GetColumnToken`입니다. 파일 이름은 탐색용 기준점일 뿐입니다. 공개 계약으로 사용하기 전에 선언과 테스트를 함께 확인합니다.

## 빠른 시작 {#quick-start}

위 좌표를 추가하고 Gradle을 refresh한 뒤 필요한 작업을 소유한 가장 작은 entry point에서 시작합니다. 먼저 [`JdbcDrivers`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/JdbcDrivers.kt)를 확인합니다. 이 파일이 모듈의 구체적인 source entry point입니다.

## 작업별 API {#api-by-task}

| Entry point | 확인할 내용 |
| --- | --- |
| [`JdbcDrivers`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/JdbcDrivers.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`HikariSupport`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/hikari/HikariSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`ArgumentSetter`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/ArgumentSetter.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`ConnectionExtensions`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/ConnectionExtensions.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`DataSourceExtensions`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/DataSourceExtensions.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`DataSourceTransactionExtensions`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/DataSourceTransactionExtensions.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`GetColumnToken`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/GetColumnToken.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`PrepareStatementSupport`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/PrepareStatementSupport.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`PreparedStatementArgumentSetter`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/PreparedStatementArgumentSetter.kt) | constructor, function, ownership 계약을 확인합니다. |

## 권장 패턴 {#patterns}

README는 **특징**, **아키텍처 다이어그램**, **확장 함수 API 개요**, **주요 API 구조**, **JDBC 쿼리 실행 흐름**, **의존성 추가**, **주요 기능**, **1. DataSource/Connection 관리**, **2. Statement 실행** 순서로 살펴볼 수 있습니다. 이 항목으로 방향을 잡고 소스와 테스트에서 실제 동작을 확인합니다. 도입 범위는 좁게 유지하고, 소유한 리소스의 수명 주기를 호출자와 연결합니다.

## 연동 {#integrations}

현재 build에 선언된 integration edge는 다음과 같습니다.

```kotlin
implementation(platform(libs.spring.boot.dependencies))
api(project(":bluetape4k-core"))
compileOnly(libs.hikaricp)
compileOnly(libs.tomcat.jdbc)
compileOnly(libs.agroal.spring.boot.starter)
compileOnly("org.springframework.boot:spring-boot-starter-jdbc")
```

`compileOnly` edge는 caller가 제공해야 하는 capability이므로 API를 사용하기 전에 runtime에 실제 dependency가 있는지 확인합니다.

## 설정 {#configuration}

`src/main/resources` 아래에서 모듈 수준 설정 resource를 찾지 못했습니다. constructor, builder, function argument, 연동 framework로 설정하며 default는 source에서 확인합니다.

## 실패 동작 {#failures}

failure 의미는 artifact 이름이 아니라 아래 entry point와 test가 결정합니다. cancellation과 timeout signal을 보존하고 소유한 resource를 닫습니다. backend exception은 안정된 domain 계약을 추가할 수 있는 boundary에서만 변환합니다. retry나 fallback을 넣기 전에 test anchor로 실제 동작을 확인합니다.

## 운영 {#operations}

pool 포화, query latency, retry, transaction rollback, schema 호환성을 관찰합니다. capacity, timeout, retry, shutdown 설정은 resource를 소유한 component 가까이에 둡니다. 누가 trade-off를 받아들였는지 알 수 없는 process-wide default는 피합니다.

## 테스트 {#testing}

모듈 test task는 다음과 같습니다.

```bash
./gradlew :bluetape4k-jdbc:test --no-configuration-cache
```

대표 test anchor는 다음과 같습니다.

- [`AbstractJdbcTest`](../../../../data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/AbstractJdbcTest.kt)
- [`JdbcDriversTest`](../../../../data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/JdbcDriversTest.kt)
- [`HikariSupportTest`](../../../../data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/hikari/HikariSupportTest.kt)
- [`Actor`](../../../../data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/model/Actor.kt)
- [`TestBean`](../../../../data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/model/TestBean.kt)
- [`AbstractJdbcSqlTest`](../../../../data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/sql/AbstractJdbcSqlTest.kt)
- [`ConnectionExtensionsTest`](../../../../data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/sql/ConnectionExtensionsTest.kt)

## 워크숍 {#workshops}

manual manifest에 등록된 전용 workshop path가 없습니다. 모듈 README와 위 representative test를 실행 근거로 사용합니다.

## 제한 사항 {#limitations}

이 페이지는 연결된 source와 test가 나타내는 현재 저장소 상태를 설명합니다. optional backend를 애플리케이션 기본값으로 만들거나 benchmark artifact 없이 성능을 단정하지 않습니다. 모듈 버전이 바뀌면 호환성과 lifecycle 설명을 다시 확인해야 합니다.

## 근거 {#sources}

- [모듈 README](../../../../data/jdbc/README.ko.md)
- [모듈 build](../../../../data/jdbc/build.gradle.kts)
- [`JdbcDrivers`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/JdbcDrivers.kt)
- [`HikariSupport`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/hikari/HikariSupport.kt)
- [`ArgumentSetter`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/ArgumentSetter.kt)
- [`ConnectionExtensions`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/ConnectionExtensions.kt)
- [`DataSourceExtensions`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/DataSourceExtensions.kt)
- [`DataSourceTransactionExtensions`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/DataSourceTransactionExtensions.kt)
- [`GetColumnToken`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/GetColumnToken.kt)
- [`PrepareStatementSupport`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/PrepareStatementSupport.kt)
- [`PreparedStatementArgumentSetter`](../../../../data/jdbc/src/main/kotlin/io/bluetape4k/jdbc/sql/PreparedStatementArgumentSetter.kt)
- [`AbstractJdbcTest`](../../../../data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/AbstractJdbcTest.kt)
- [`JdbcDriversTest`](../../../../data/jdbc/src/test/kotlin/io/bluetape4k/jdbc/JdbcDriversTest.kt)
