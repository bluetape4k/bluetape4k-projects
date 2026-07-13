---
manualId: bluetape4k-spring-boot-hibernate-lettuce
title: "bluetape4k-spring-boot-hibernate-lettuce"
description: "Hibernate 7 2nd Level Cache (Lettuce Near Cache)를 위한 Spring Boot 4 Auto-Configuration."
kind: library
group: spring
---

# bluetape4k-spring-boot-hibernate-lettuce

## 해결하는 문제 {#problem}

Hibernate 7 2nd Level Cache (Lettuce Near Cache)를 위한 Spring Boot 4 Auto-Configuration. 이 매뉴얼은 README의 기능 목록을 반복하지 않고 현재 build, source entry point, test, 설정 resource, lifecycle 근거를 연결합니다.

## 사용 시점 {#when-to-use}

애플리케이션에 auto-configuration condition, bean ownership, property binding, application lifecycle이 필요할 때 `bluetape4k-spring-boot-hibernate-lettuce`를 선택합니다. 아래 source entry point에서 시작해 ownership과 failure 계약이 caller lifecycle에 맞는지 확인합니다. 표준 API나 이미 도입한 더 작은 모듈이 같은 계약을 만족한다면 그쪽을 우선합니다.

## 의존성 좌표 {#coordinates}

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-spring-boot-hibernate-lettuce")
}
```

Gradle project path는 `:bluetape4k-spring-boot-hibernate-lettuce`, source directory는 `spring-boot/hibernate-lettuce`입니다.

## 핵심 개념 {#concepts}

먼저 확인할 source 개념은 `LettuceNearCacheActuatorAutoConfiguration`, `LettuceNearCacheActuatorEndpoint`, `LettuceNearCacheHibernateAutoConfiguration`, `LettuceNearCacheMetricsAutoConfiguration`, `LettuceNearCacheMetricsBinder`, `LettuceNearCacheSpringProperties`입니다. 파일 이름은 탐색 anchor일 뿐이므로 public 계약으로 사용하기 전에 선언과 test를 함께 읽습니다.

## 빠른 시작 {#quick-start}

위 좌표를 추가하고 Gradle을 refresh한 뒤 필요한 작업을 소유한 가장 작은 entry point에서 시작합니다. 먼저 [`LettuceNearCacheActuatorAutoConfiguration`](../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheActuatorAutoConfiguration.kt)를 확인합니다. 이 파일이 모듈의 구체적인 source entry point입니다.

## 작업별 API {#api-by-task}

| Entry point | 확인할 내용 |
| --- | --- |
| [`LettuceNearCacheActuatorAutoConfiguration`](../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheActuatorAutoConfiguration.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`LettuceNearCacheActuatorEndpoint`](../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheActuatorEndpoint.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`LettuceNearCacheHibernateAutoConfiguration`](../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheHibernateAutoConfiguration.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`LettuceNearCacheMetricsAutoConfiguration`](../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheMetricsAutoConfiguration.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`LettuceNearCacheMetricsBinder`](../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheMetricsBinder.kt) | constructor, function, ownership 계약을 확인합니다. |
| [`LettuceNearCacheSpringProperties`](../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheSpringProperties.kt) | constructor, function, ownership 계약을 확인합니다. |

## 권장 패턴 {#patterns}

README 근거는 **Auto-Configuration 클래스 구조**, **Auto-Configuration 활성화 흐름**, **Spring Boot 4 참고 사항**, **특징**, **의존성 (Spring Boot 4)**, **빠른 시작**, **1. 의존성 추가 후 application.yml 설정**, **2. Entity에 캐시 어노테이션 추가**, **3. 실행 — 자동 설정 완료**, **설정 옵션 전체 목록** 순서로 탐색할 수 있습니다. 이 항목으로 방향을 잡고 source와 test에서 동작을 확인합니다. 도입 범위는 좁게 유지하고 소유한 resource를 caller lifecycle에 연결합니다.

## 연동 {#integrations}

현재 build에 선언된 integration edge는 다음과 같습니다.

```kotlin
implementation(platform(libs.spring.boot.dependencies))
api(project(":bluetape4k-hibernate-cache-lettuce"))
compileOnly("org.springframework.boot:spring-boot-autoconfigure")
compileOnly("org.springframework.boot:spring-boot-hibernate")
compileOnly("org.springframework.boot:spring-boot-starter-data-jpa")
compileOnly(libs.hibernate.core)
compileOnly(libs.micrometer.core)
compileOnly("org.springframework.boot:spring-boot-starter-actuator")
compileOnly("org.springframework.boot:spring-boot-micrometer-metrics")
implementation(libs.fory.kotlin)
implementation(libs.zstd.jni)
```

`compileOnly` edge는 caller가 제공해야 하는 capability이므로 API를 사용하기 전에 runtime에 실제 dependency가 있는지 확인합니다.

## 설정 {#configuration}

모듈에서 찾은 설정 resource는 다음과 같습니다.

- [`org.springframework.boot.autoconfigure.AutoConfiguration.imports`](../../../../spring-boot/hibernate-lettuce/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports)

override하기 전에 이 resource와 binding source에서 property 이름과 default를 확인합니다.

## 실패 동작 {#failures}

failure 의미는 artifact 이름이 아니라 아래 entry point와 test가 결정합니다. cancellation과 timeout signal을 보존하고 소유한 resource를 닫습니다. backend exception은 안정된 domain 계약을 추가할 수 있는 boundary에서만 변환합니다. retry나 fallback을 넣기 전에 test anchor로 실제 동작을 확인합니다.

## 운영 {#operations}

condition report, startup failure, pool/client health, request latency, graceful shutdown을 관찰합니다. capacity, timeout, retry, shutdown 설정은 resource를 소유한 component 가까이에 둡니다. 누가 trade-off를 받아들였는지 알 수 없는 process-wide default는 피합니다.

## 테스트 {#testing}

모듈 test task는 다음과 같습니다.

```bash
./gradlew :bluetape4k-spring-boot-hibernate-lettuce:test --no-configuration-cache
```

대표 test anchor는 다음과 같습니다.

- [`LettuceNearCacheAutoConfigurationTest`](../../../../spring-boot/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheAutoConfigurationTest.kt)
- [`LettuceNearCacheIntegrationTest`](../../../../spring-boot/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheIntegrationTest.kt)
- [`LettuceNearCachePropertiesCustomizerTest`](../../../../spring-boot/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCachePropertiesCustomizerTest.kt)
- [`ReadmeDependencyContractTest`](../../../../spring-boot/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/ReadmeDependencyContractTest.kt)

## 워크숍 {#workshops}

manual manifest에 등록된 전용 workshop path가 없습니다. 모듈 README와 위 representative test를 실행 근거로 사용합니다.

## 제한 사항 {#limitations}

이 페이지는 연결된 source와 test가 나타내는 현재 저장소 상태를 설명합니다. optional backend를 애플리케이션 기본값으로 만들거나 benchmark artifact 없이 성능을 단정하지 않습니다. 모듈 버전이 바뀌면 호환성과 lifecycle 설명을 다시 확인해야 합니다.

## 근거 {#sources}

- [모듈 README](../../../../spring-boot/hibernate-lettuce/README.ko.md)
- [모듈 build](../../../../spring-boot/hibernate-lettuce/build.gradle.kts)
- [`LettuceNearCacheActuatorAutoConfiguration`](../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheActuatorAutoConfiguration.kt)
- [`LettuceNearCacheActuatorEndpoint`](../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheActuatorEndpoint.kt)
- [`LettuceNearCacheHibernateAutoConfiguration`](../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheHibernateAutoConfiguration.kt)
- [`LettuceNearCacheMetricsAutoConfiguration`](../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheMetricsAutoConfiguration.kt)
- [`LettuceNearCacheMetricsBinder`](../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheMetricsBinder.kt)
- [`LettuceNearCacheSpringProperties`](../../../../spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheSpringProperties.kt)
- [`LettuceNearCacheAutoConfigurationTest`](../../../../spring-boot/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheAutoConfigurationTest.kt)
- [`LettuceNearCacheIntegrationTest`](../../../../spring-boot/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheIntegrationTest.kt)
- [`LettuceNearCachePropertiesCustomizerTest`](../../../../spring-boot/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCachePropertiesCustomizerTest.kt)
- [`ReadmeDependencyContractTest`](../../../../spring-boot/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/ReadmeDependencyContractTest.kt)
