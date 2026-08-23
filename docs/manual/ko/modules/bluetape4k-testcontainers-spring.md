---
manualId: bluetape4k-testcontainers-spring
title: "Spring Testcontainers 프로퍼티 브리지"
description: "core 서버의 생명주기나 JVM system property를 바꾸지 않고 PropertyExportingServer 프로퍼티를 Spring Test의 DynamicPropertyRegistry에 연결합니다."
kind: library
group: testing
learningOrder: 1150
---

# Spring Testcontainers 프로퍼티 브리지

## 해결하는 문제 {#problem}

Spring 통합 테스트는 컨테이너 endpoint를 애플리케이션 프로퍼티로 동적으로 주입해야 하는 경우가 많습니다. `bluetape4k-testcontainers` core 모듈은 Spring과 분리된 상태를 유지하므로, core의 프로퍼티 계약을 보존하면서 Spring Test에 값을 등록하는 작은 adapter가 필요합니다.

## 사용 시점 {#when-to-use}

Spring Test context가 `PropertyExportingServer`가 내보내는 프로퍼티를 사용할 때 `bluetape4k-testcontainers-spring`을 선택합니다. JVM system property가 명시적으로 필요한 테스트라면 core 모듈의 `registerSystemProperties()` API를 사용합니다. Spring이 아닌 테스트에는 이 bridge를 추가하지 말고, 이 모듈이 컨테이너를 시작·중지하거나 설정한다고 가정하지 않습니다.

## 의존성 좌표 {#coordinates}

```kotlin
dependencies {
    testImplementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    testImplementation("io.github.bluetape4k:bluetape4k-testcontainers-spring")
}
```

Gradle project path는 `:bluetape4k-testcontainers-spring`, source directory는 `testing/testcontainers-spring`입니다.

## 핵심 개념 {#concepts}

`PropertyExportingServer`는 `propertyNamespace`, `propertyKeys()`, 현재 값을 반환하는 `properties()`를 제공합니다. bridge는 각 키를 `testcontainers.{namespace}.{key}` 형식으로 바꾸고 Spring의 `DynamicPropertyRegistry`에 lazy supplier로 등록합니다. 등록만 bridge의 책임이며 server 생명주기는 테스트가 계속 소유합니다.

## 빠른 시작 {#quick-start}

static `@DynamicPropertySource` 메서드에서 server를 등록합니다.

```kotlin
import io.bluetape4k.testcontainers.spring.registerDynamicProperties
import io.bluetape4k.testcontainers.storage.RedisServer
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

companion object {
    @DynamicPropertySource
    @JvmStatic
    fun registerProperties(registry: DynamicPropertyRegistry) {
        RedisServer.Launcher.redis.registerDynamicProperties(registry)
    }
}
```

`RedisServer.Launcher.redis`의 시작과 종료는 기존 테스트 생명주기에서 처리합니다. bridge는 Spring이 등록된 supplier를 평가할 때만 값을 읽습니다.

## 작업별 API {#api-by-task}

| 작업 | 진입점 | 계약 |
| --- | --- | --- |
| server 프로퍼티를 Spring에 등록 | `PropertyExportingServer.registerDynamicProperties(registry)` | `propertyKeys()`가 반환한 각 키에 대해 lazy supplier 하나를 등록합니다. |
| core와 Spring 경계 유지 | `bluetape4k-testcontainers`의 `PropertyExportingServer` | core의 namespace와 프로퍼티 맵을 재사용하며 core 모듈에 Spring 의존성을 추가하지 않습니다. |

## 권장 패턴 {#patterns}

테스트의 `@DynamicPropertySource` 메서드에서 extension을 한 번 호출하고 컨테이너 시작은 기존 server launcher나 fixture에 맡깁니다. supplier는 lazy하고 값을 캐시하지 않으므로 Spring이 평가할 때마다 `properties()`에서 값을 읽습니다. 하나의 키에는 가능한 한 하나의 등록 경로만 사용하고, 중복 등록이 필요하면 이 bridge가 덮어쓰지 않고 Spring registry의 등록 순서 semantics에 위임한다는 점을 고려합니다.

## 통합 {#integrations}

이 모듈은 `PropertyExportingServer`를 위해 `bluetape4k-testcontainers`에 의존하고, `DynamicPropertyRegistry`를 위해 Spring Test에 의존합니다. Spring Boot auto-configuration이 아니라 선택형 adapter입니다. 호환되는 Spring Test 버전은 release catalog가 제공하므로 이 모듈을 사용하는 쪽에서 별도 버전을 고정하지 않습니다.

## 설정 {#configuration}

추가 설정 파일이나 system property를 만들지 않습니다. namespace가 `redis`이고 키가 `host`인 server는 `testcontainers.redis.host`로 노출됩니다. namespace와 키 집합은 기존 core server가 관리합니다.

## 실패 처리 {#failures}

`propertyKeys()`가 선언한 키가 `properties()`에 없으면 Spring이 값을 평가하는 시점에 `IllegalStateException`이 발생합니다. `properties()`에서 발생한 예외는 원래 타입과 메시지를 유지한 채 전달됩니다. bridge는 중복 키를 사전 검사하지 않고, 값을 캐시하거나 server 예외를 변환하지도 않습니다.

## 운영 {#operations}

bridge는 컨테이너를 시작·중지하지 않고 JVM system property도 변경하지 않습니다. resource ownership, readiness, shutdown, diagnostic은 기존 `PropertyExportingServer` launcher나 테스트 fixture에서 관리합니다. 이렇게 해야 Spring context 구성과 컨테이너 생명주기 결정을 분리할 수 있습니다.

## 테스트 {#testing}

집중 테스트는 다음 명령으로 실행합니다.

```bash
./gradlew :bluetape4k-testcontainers-spring:test --no-configuration-cache
```

[`PropertyExportingServerDynamicPropertyRegistryTest`](../../../../testing/testcontainers-spring/src/test/kotlin/io/bluetape4k/testcontainers/spring/PropertyExportingServerDynamicPropertyRegistryTest.kt)는 Docker 없이 key mapping, lazy 및 반복 supplier 평가, 누락 키 실패, 예외 전달, 중복 등록 위임, JVM system property 보존을 검증합니다.

## 워크숍 {#workshops}

이 adapter에 연결된 전용 workshop은 등록되어 있지 않습니다. 모듈 README와 집중 계약 테스트가 실행 가능한 사용법과 생명주기 근거를 제공합니다.

## 제한 사항 {#limitations}

이 모듈은 Spring Test의 dynamic property registry만 지원합니다. Spring Boot auto-configuration, 컨테이너 자동 시작, 프로퍼티 캐시, 충돌 해결, 기존 workshop helper의 migration은 제공하지 않습니다. core 모듈이 변경되면 server 프로퍼티 계약을 다시 확인해야 합니다.

## 근거 {#sources}

- [모듈 README](../../../../testing/testcontainers-spring/README.ko.md)
- [모듈 build](../../../../testing/testcontainers-spring/build.gradle.kts)
- [`PropertyExportingServerDynamicPropertyRegistry`](../../../../testing/testcontainers-spring/src/main/kotlin/io/bluetape4k/testcontainers/spring/PropertyExportingServerDynamicPropertyRegistry.kt)
- [`PropertyExportingServerDynamicPropertyRegistryTest`](../../../../testing/testcontainers-spring/src/test/kotlin/io/bluetape4k/testcontainers/spring/PropertyExportingServerDynamicPropertyRegistryTest.kt)
- [`PropertyExportingServer`](../../../../testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/PropertyExportingServer.kt)
