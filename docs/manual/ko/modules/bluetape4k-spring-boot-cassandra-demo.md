---
manualId: bluetape4k-spring-boot-cassandra-demo
title: "Spring Data Cassandra 예제"
description: "Apache Cassandra와 Spring Data Cassandra를 활용하는 종합 예제입니다 (Spring Boot 4.x)."
kind: library
group: examples
learningOrder: 1450
---

# Spring Data Cassandra 예제

## 해결하는 문제 {#problem}

Apache Cassandra와 Spring Data Cassandra를 활용하는 종합 예제입니다 (Spring Boot 4.x). 이 매뉴얼은 README의 기능 목록을 반복하지 않고 현재 build, source entry point, test, 설정 resource, lifecycle 근거를 연결합니다.

## 사용 시점 {#when-to-use}

애플리케이션에 auto-configuration condition, bean ownership, property binding, application lifecycle이 필요할 때 `bluetape4k-spring-boot-cassandra-demo`를 선택합니다. 아래 source entry point에서 시작해 ownership과 failure 계약이 caller lifecycle에 맞는지 확인합니다. 표준 API나 이미 도입한 더 작은 모듈이 같은 계약을 만족한다면 그쪽을 우선합니다.

## 의존성 좌표 {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-spring-boot-cassandra-demo")
}
```

Gradle project path는 `:bluetape4k-spring-boot-cassandra-demo`, source directory는 `spring-boot/cassandra-demo`입니다.

## 핵심 개념 {#concepts}

이 모듈은 설정 또는 platform metadata이며 index할 Kotlin/Java source type이 없습니다.

## 빠른 시작 {#quick-start}

위 좌표를 추가하고 Gradle을 refresh한 뒤 필요한 작업을 소유한 가장 작은 entry point에서 시작합니다. Kotlin/Java source entry point가 없는 모듈이므로 Gradle model과 README를 확인합니다.

## 작업별 API {#api-by-task}

이 모듈에는 등록된 Kotlin/Java source file이 없습니다. build model과 README가 public surface입니다.

## 권장 패턴 {#patterns}

README 근거는 **예제 아키텍처**, **예제 목록**, **기본 (basic/)**, **Kotlin DSL (kotlin/)**, **Reactive (reactive/)**, **감사 (auditing/)**, **Entity 정의**, **Repository**, **Coroutines 지원**, **실행 방법** 순서로 탐색할 수 있습니다. 이 항목으로 방향을 잡고 source와 test에서 동작을 확인합니다. 도입 범위는 좁게 유지하고 소유한 resource를 caller lifecycle에 연결합니다.

## 연동 {#integrations}

현재 build에 선언된 integration edge는 다음과 같습니다.

```kotlin
implementation(platform(libs.spring.boot.dependencies))
implementation(project(":bluetape4k-cassandra"))
implementation(project(":bluetape4k-spring-boot-cassandra"))
implementation(libs.cassandra.java.driver.core)
implementation(libs.cassandra.java.driver.query.builder)
implementation(libs.cassandra.java.driver.mapper.runtime)
implementation(libs.cassandra.java.driver.metrics.micrometer)
implementation("org.springframework.boot:spring-boot-starter-aspectj")
implementation("org.springframework.boot:spring-boot-starter-data-cassandra")
implementation(project(":bluetape4k-coroutines"))
implementation(libs.kotlinx.coroutines.core)
implementation(libs.kotlinx.coroutines.reactive)
```

`compileOnly` edge는 caller가 제공해야 하는 capability이므로 API를 사용하기 전에 runtime에 실제 dependency가 있는지 확인합니다.

## 설정 {#configuration}

`src/main/resources` 아래에서 모듈 수준 설정 resource를 찾지 못했습니다. constructor, builder, function argument, 연동 framework로 설정하며 default는 source에서 확인합니다.

## 실패 동작 {#failures}

failure 의미는 artifact 이름이 아니라 아래 entry point와 test가 결정합니다. cancellation과 timeout signal을 보존하고 소유한 resource를 닫습니다. backend exception은 안정된 domain 계약을 추가할 수 있는 boundary에서만 변환합니다. retry나 fallback을 넣기 전에 test anchor로 실제 동작을 확인합니다.

## 운영 {#operations}

condition report, startup failure, pool/client health, request latency, graceful shutdown을 관찰합니다. capacity, timeout, retry, shutdown 설정은 resource를 소유한 component 가까이에 둡니다. 누가 trade-off를 받아들였는지 알 수 없는 process-wide default는 피합니다.

## 테스트 {#testing}

모듈 test task는 다음과 같습니다.

```bash
./gradlew :bluetape4k-spring-boot-cassandra-demo:test --no-configuration-cache
```

대표 test anchor는 다음과 같습니다.

- [`AbstractCassandraCoroutineTest`](../../../../spring-boot/cassandra-demo/src/test/kotlin/io/bluetape4k/examples/cassandra/AbstractCassandraCoroutineTest.kt)
- [`AbstractCassandraTest`](../../../../spring-boot/cassandra-demo/src/test/kotlin/io/bluetape4k/examples/cassandra/AbstractCassandraTest.kt)
- [`AbstractReactiveCassandraTestConfiguration`](../../../../spring-boot/cassandra-demo/src/test/kotlin/io/bluetape4k/examples/cassandra/AbstractReactiveCassandraTestConfiguration.kt)
- [`ReadmeCoroutineRepositoryContractTest`](../../../../spring-boot/cassandra-demo/src/test/kotlin/io/bluetape4k/examples/cassandra/ReadmeCoroutineRepositoryContractTest.kt)
- [`AuditedPerson`](../../../../spring-boot/cassandra-demo/src/test/kotlin/io/bluetape4k/examples/cassandra/auditing/AuditedPerson.kt)
- [`AuditedPersonRepository`](../../../../spring-boot/cassandra-demo/src/test/kotlin/io/bluetape4k/examples/cassandra/auditing/AuditedPersonRepository.kt)
- [`AuditingTest`](../../../../spring-boot/cassandra-demo/src/test/kotlin/io/bluetape4k/examples/cassandra/auditing/AuditingTest.kt)
- [`AuditingTestConfiguration`](../../../../spring-boot/cassandra-demo/src/test/kotlin/io/bluetape4k/examples/cassandra/auditing/AuditingTestConfiguration.kt)

## 워크숍 {#workshops}

manual manifest에 등록된 전용 workshop path가 없습니다. 모듈 README와 위 representative test를 실행 근거로 사용합니다.

## 제한 사항 {#limitations}

이 페이지는 연결된 source와 test가 나타내는 현재 저장소 상태를 설명합니다. optional backend를 애플리케이션 기본값으로 만들거나 benchmark artifact 없이 성능을 단정하지 않습니다. 모듈 버전이 바뀌면 호환성과 lifecycle 설명을 다시 확인해야 합니다.

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### Spring Boot Cassandra 데모 예제 아키텍처 다이어그램

[![Spring Boot Cassandra 데모 예제 아키텍처 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-cassandra-demo-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-cassandra-demo-diagram-01.svg)

_배포본 README: [`spring-boot/cassandra-demo/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/spring-boot/cassandra-demo/README.ko.md)_

<!-- release-readme-diagrams:end -->

## 근거 {#sources}

- [모듈 README](../../../../spring-boot/cassandra-demo/README.ko.md)
- [모듈 build](../../../../spring-boot/cassandra-demo/build.gradle.kts)
- [`AbstractCassandraCoroutineTest`](../../../../spring-boot/cassandra-demo/src/test/kotlin/io/bluetape4k/examples/cassandra/AbstractCassandraCoroutineTest.kt)
- [`AbstractCassandraTest`](../../../../spring-boot/cassandra-demo/src/test/kotlin/io/bluetape4k/examples/cassandra/AbstractCassandraTest.kt)
- [`AbstractReactiveCassandraTestConfiguration`](../../../../spring-boot/cassandra-demo/src/test/kotlin/io/bluetape4k/examples/cassandra/AbstractReactiveCassandraTestConfiguration.kt)
- [`ReadmeCoroutineRepositoryContractTest`](../../../../spring-boot/cassandra-demo/src/test/kotlin/io/bluetape4k/examples/cassandra/ReadmeCoroutineRepositoryContractTest.kt)
- [`AuditedPerson`](../../../../spring-boot/cassandra-demo/src/test/kotlin/io/bluetape4k/examples/cassandra/auditing/AuditedPerson.kt)
- [`AuditedPersonRepository`](../../../../spring-boot/cassandra-demo/src/test/kotlin/io/bluetape4k/examples/cassandra/auditing/AuditedPersonRepository.kt)
- [`AuditingTest`](../../../../spring-boot/cassandra-demo/src/test/kotlin/io/bluetape4k/examples/cassandra/auditing/AuditingTest.kt)
- [`AuditingTestConfiguration`](../../../../spring-boot/cassandra-demo/src/test/kotlin/io/bluetape4k/examples/cassandra/auditing/AuditingTestConfiguration.kt)
