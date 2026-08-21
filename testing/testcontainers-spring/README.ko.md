# 모듈 bluetape4k-testcontainers-spring

[English](./README.md) | 한국어

`bluetape4k-testcontainers`의 `PropertyExportingServer`를 Spring Test의
`DynamicPropertyRegistry`에 연결하는 선택 모듈입니다. SDK-neutral core에는 Spring
의존성을 추가하지 않고, 기존 `testcontainers.{namespace}.{key}` 프로퍼티 계약을
그대로 재사용합니다.

<!-- issue-1321-spring-bridge:start -->
## 의존성

```kotlin
testImplementation("io.bluetape4k:bluetape4k-testcontainers-spring:<version>")
```

이 모듈은 core의 `PropertyExportingServer` API와 Spring Test의
`DynamicPropertyRegistry`를 노출합니다. 버전은 Bluetape4k release catalog가
관리하므로 별도의 Spring Test 버전을 고정하지 않습니다.

## 사용법

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

namespace가 `redis`이고 키가 `host`인 서버는
`testcontainers.redis.host` Spring 키로 등록됩니다. bridge는
`propertyKeys()`가 반환한 모든 키를 등록하고, Spring이 값을 해석할 때
`properties()`를 lazy하게 평가합니다. 컨테이너를 시작·중지하지 않으며 JVM system
property도 변경하지 않으므로, 서버 생명주기는 테스트가 소유합니다.

`propertyKeys()`에는 있지만 `properties()`에 없는 키는 값을 해석하는 시점에
`IllegalStateException`으로 실패합니다. 중복 키를 사전 검사하거나 덮어쓰지 않으므로,
등록 경로를 하나로 선택하고 Spring registry의 등록 순서 semantics를 따릅니다.

기존 `registerSystemProperties()` API는 별도 계약으로 유지됩니다. Spring 동적
프로퍼티 대신 JVM system property가 필요한 테스트에서만 사용하세요.

supplier 생명주기와 등록 모델은 Spring의
[DynamicPropertyRegistry Javadoc](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/test/context/DynamicPropertyRegistry.html)와
[DynamicPropertySource reference](https://docs.spring.io/spring-framework/reference/testing/annotations/integration-spring/annotation-dynamicpropertysource.html)를
참고하세요.
<!-- issue-1321-spring-bridge:end -->

## 범위

이 모듈은 작은 adapter입니다. Spring Boot auto-configuration, 컨테이너 자동 시작,
프로퍼티 캐시, 충돌 해결, 기존 Workshop helper의 일괄 migration은 제공하지 않습니다.
