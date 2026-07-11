---
manualId: bluetape4k-r2dbc
title: "Module bluetape4k-r2dbc"
description: "English | 한국어"
kind: library
group: data
---

# Module bluetape4k-r2dbc

## 해결하는 문제 {#problem}

English | 한국어 이 매뉴얼은 README의 기능 목록을 반복하지 않고 현재 build, source entry point, test, 설정 resource, lifecycle 근거를 연결합니다.

## 사용 시점 {#when-to-use}

애플리케이션에 transaction boundary, connection ownership, query 동작, serialization이 필요할 때 `bluetape4k-r2dbc`를 선택합니다. 아래 source entry point에서 시작해 ownership과 failure 계약이 caller lifecycle에 맞는지 확인합니다. 표준 API나 이미 도입한 더 작은 모듈이 같은 계약을 만족한다면 그쪽을 우선합니다.

## 의존성 좌표 {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-bom:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-r2dbc")
}
```

Gradle project path는 `:bluetape4k-r2dbc`, source directory는 `data/r2dbc`입니다.

## 핵심 개념 {#concepts}

먼저 확인할 source 개념은 `R2dbcClient`, `R2dbcClientAutoConfiguration`, `ConnectionFactoryUtils`, `R2dbcTransactionManager`, `CompositeDatabasePopulator`, `ConnectionFactoryInitializer`, `ResourceDatabasePopulator`, `MappingR2dbcConverter`입니다. 파일 이름은 탐색 anchor일 뿐이므로 public 계약으로 사용하기 전에 선언과 test를 함께 읽습니다.

## 빠른 시작 {#quick-start}

위 좌표를 추가하고 Gradle을 refresh한 뒤 필요한 작업을 소유한 가장 작은 entry point에서 시작합니다. 먼저 [`R2dbcClient`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/R2dbcClient.kt)를 확인합니다. 이 파일이 모듈의 구체적인 source entry point입니다.

## 작업별 API {#api-by-task}

| Entry point | 확인할 내용 |
| --- | --- |
| [`R2dbcClient`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/R2dbcClient.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`R2dbcClientAutoConfiguration`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/config/R2dbcClientAutoConfiguration.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`ConnectionFactoryUtils`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/connection/ConnectionFactoryUtils.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`R2dbcTransactionManager`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/connection/R2dbcTransactionManager.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`CompositeDatabasePopulator`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/connection/init/CompositeDatabasePopulator.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`ConnectionFactoryInitializer`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/connection/init/ConnectionFactoryInitializer.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`ResourceDatabasePopulator`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/connection/init/ResourceDatabasePopulator.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`MappingR2dbcConverter`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/convert/MappingR2dbcConverter.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`JsonToMapConverter`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/convert/postgresql/JsonToMapConverter.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`MapToJsonConverter`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/convert/postgresql/MapToJsonConverter.kt) | constructor, function, ownership 계약을 확인합니다. |

## 권장 패턴 {#patterns}

README 근거는 **특징**, **아키텍처 다이어그램**, **확장 함수 API 개요**, **주요 API 클래스 구조**, **R2DBC 쿼리 실행 흐름**, **JDBC vs R2DBC 비교**, **의존성 추가**, **주요 기능**, **1. R2DBC 커넥션 풀 튜닝**, **실측 기반 튜닝 가이드** 순서로 탐색할 수 있습니다. 이 항목으로 방향을 잡고 source와 test에서 동작을 확인합니다. 도입 범위는 좁게 유지하고 소유한 resource를 caller lifecycle에 연결합니다.

## 연동 {#integrations}

현재 build에 선언된 integration edge는 다음과 같습니다.

```kotlin
implementation(platform(libs.spring.boot.dependencies))
api(project(":bluetape4k-core"))
compileOnly(project(":bluetape4k-jackson3"))
compileOnly(libs.jackson3.module.kotlin)
api(project(":bluetape4k-coroutines"))
api(libs.kotlinx.coroutines.core)
api(libs.kotlinx.coroutines.reactive)
api(libs.kotlinx.coroutines.reactor)
compileOnly(libs.reactor.core)
compileOnly(libs.reactor.kotlin.extensions)
api(libs.r2dbc.pool)
compileOnly("org.springframework.boot:spring-boot-starter-data-r2dbc")
```

`compileOnly` edge는 caller가 제공해야 하는 capability이므로 API를 사용하기 전에 runtime에 실제 dependency가 있는지 확인합니다.

## 설정 {#configuration}

모듈에서 찾은 설정 resource는 다음과 같습니다.

- [`spring.factories`](../../../../data/r2dbc/src/main/resources/META-INF/spring.factories)
- [`org.springframework.boot.autoconfigure.AutoConfiguration.imports`](../../../../data/r2dbc/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports)

override하기 전에 이 resource와 binding source에서 property 이름과 default를 확인합니다.

## 실패 동작 {#failures}

failure 의미는 artifact 이름이 아니라 아래 entry point와 test가 결정합니다. cancellation과 timeout signal을 보존하고 소유한 resource를 닫습니다. backend exception은 안정된 domain 계약을 추가할 수 있는 boundary에서만 변환합니다. retry나 fallback을 넣기 전에 test anchor로 실제 동작을 확인합니다.

## 운영 {#operations}

pool 포화, query latency, retry, transaction rollback, schema 호환성을 관찰합니다. capacity, timeout, retry, shutdown 설정은 resource를 소유한 component 가까이에 둡니다. 누가 trade-off를 받아들였는지 알 수 없는 process-wide default는 피합니다.

## 테스트 {#testing}

모듈 test task는 다음과 같습니다.

```bash
./gradlew :bluetape4k-r2dbc:test --no-configuration-cache
```

대표 test anchor는 다음과 같습니다.

- [`AbstractR2dbcTest`](../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/AbstractR2dbcTest.kt)
- [`R2dbcTestApplication`](../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/R2dbcTestApplication.kt)
- [`R2dbcClientAutoConfigurationTest`](../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/config/R2dbcClientAutoConfigurationTest.kt)
- [`R2dbcConfigurationTest`](../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/config/R2dbcConfigurationTest.kt)
- [`ConnectionInitTest`](../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/connection/init/ConnectionInitTest.kt)
- [`PostgresJsonConvertersTest`](../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/convert/postgresql/PostgresJsonConvertersTest.kt)
- [`DatabaseClientBuilderTest`](../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/core/DatabaseClientBuilderTest.kt)
- [`DeleteTest`](../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/core/DeleteTest.kt)

## 워크숍 {#workshops}

manual manifest에 등록된 전용 workshop path가 없습니다. 모듈 README와 위 representative test를 실행 근거로 사용합니다.

## 제한 사항 {#limitations}

이 페이지는 연결된 source와 test가 나타내는 현재 저장소 상태를 설명합니다. optional backend를 애플리케이션 기본값으로 만들거나 benchmark artifact 없이 성능을 단정하지 않습니다. 모듈 버전이 바뀌면 호환성과 lifecycle 설명을 다시 확인해야 합니다.

## 근거 {#sources}

- [모듈 README](../../../../data/r2dbc/README.ko.md)
- [모듈 build](../../../../data/r2dbc/build.gradle.kts)
- [`R2dbcClient`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/R2dbcClient.kt)
- [`R2dbcClientAutoConfiguration`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/config/R2dbcClientAutoConfiguration.kt)
- [`ConnectionFactoryUtils`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/connection/ConnectionFactoryUtils.kt)
- [`R2dbcTransactionManager`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/connection/R2dbcTransactionManager.kt)
- [`CompositeDatabasePopulator`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/connection/init/CompositeDatabasePopulator.kt)
- [`ConnectionFactoryInitializer`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/connection/init/ConnectionFactoryInitializer.kt)
- [`ResourceDatabasePopulator`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/connection/init/ResourceDatabasePopulator.kt)
- [`MappingR2dbcConverter`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/convert/MappingR2dbcConverter.kt)
- [`JsonToMapConverter`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/convert/postgresql/JsonToMapConverter.kt)
- [`MapToJsonConverter`](../../../../data/r2dbc/src/main/kotlin/io/bluetape4k/r2dbc/convert/postgresql/MapToJsonConverter.kt)
- [`AbstractR2dbcTest`](../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/AbstractR2dbcTest.kt)
- [`R2dbcTestApplication`](../../../../data/r2dbc/src/test/kotlin/io/bluetape4k/r2dbc/R2dbcTestApplication.kt)
