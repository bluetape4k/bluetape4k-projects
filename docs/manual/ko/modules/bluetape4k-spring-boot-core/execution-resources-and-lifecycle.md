---
title: 실행 리소스와 lifecycle
description: 전용 WebClient event loop, controller coroutine scope와 virtual thread executor의 생성·설정·종료 책임을 설명합니다.
manualId: bluetape4k-spring-boot-core
chapterId: execution-resources-and-lifecycle
---

# 실행 리소스와 lifecycle

## WebClient 리소스를 서버와 분리한다

`AbstractWebClientConfig`는 전용 `LoopResources`, `ReactorResourceFactory`, `ReactorClientHttpConnector`, `ExchangeStrategies`, `WebClient` bean을 만듭니다. `isUseGlobalResources = false`이므로 server나 다른 client의 전역 Reactor resource와 분리됩니다.

```kotlin
@Configuration
class PartnerWebClientConfig : AbstractWebClientConfig() {
    override val threadCount: Int = 8
    override val connectTimeoutMillis: Int = 2_000
    override val shutdownTimeout: Duration = Duration.ofSeconds(10)
    override val maxInMemorySize: Int = 4 * 1024 * 1024
}
```

격리는 장애 범위를 줄이지만 thread와 direct memory를 추가로 사용합니다. client마다 무조건 별도 loop를 만들기보다 traffic과 격리 목적이 분명한 경계에서 사용합니다.

## timeout과 SSL

Response timeout 기본값은 `PT30S`이며 `bluetape4k.webclient.response-timeout` property로 바꿀 수 있습니다. Connect timeout은 subclass property를 override합니다.

기본 `sslContext()`는 JDK trust store로 서버 인증서를 검증합니다. `insecureSslContext()`는 모든 인증서를 신뢰하므로 개발·테스트에서만 사용합니다. 운영의 사설 CA는 trust store를 구성한 custom `SslContext`로 연결합니다.

## codec memory limit

`exchangeStrategies()`는 default codec의 `maxInMemorySize`만 변경하며 기본값은 16 MiB입니다. 이 값은 모든 streaming payload의 전체 크기 제한이 아니라, codec이 메모리에 모으는 단위의 경계입니다. 큰 JSON이나 multipart를 무작정 늘리기 전에 streaming 또는 payload 설계를 검토합니다.

## controller coroutine scope

`AbstractCoroutineDefaultController`, `AbstractCoroutineIOController`, `AbstractCoroutineVTController`는 각각 별도 `CoroutineScope`에 `SupervisorJob`을 붙입니다. Spring bean이 파괴될 때 `@PreDestroy`가 job을 취소합니다.

이 scope는 request coroutine의 child가 아닙니다. 요청 취소와 Reactor/Spring Security context가 자동으로 따라오지 않습니다. 요청 수명 작업은 controller suspend 함수의 현재 context에서 실행하고, 요청보다 오래 살아야 하는 작업만 별도 application service scope로 명시적으로 넘깁니다.

## virtual thread 구성

`VirtualThreadAutoConfiguration`은 `AsyncTaskExecutor` bean이 없을 때 virtual thread를 활성화한 `SimpleAsyncTaskExecutor`를 등록합니다.

```kotlin
@Import(VirtualThreadAutoConfiguration::class)
class AsyncConfiguration
```

Auto-configuration imports metadata가 없으므로 명시적 `@Import`가 필요합니다. 기존 executor가 있다면 `@ConditionalOnMissingBean` 때문에 등록되지 않습니다.

`AbstractVirtualThreadController.virtualThreadExecutor`는 `newVirtualThreadPerTaskExecutor()`로 만든 process-wide companion object입니다. 이 클래스에는 close hook이 없습니다. 애플리케이션 종료 제어가 필요하다면 직접 소유하는 executor bean을 권장합니다.

## Source와 tests

- [`AbstractWebClientConfig.kt`](../../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/webflux/config/AbstractWebClientConfig.kt)
- [`AbstractCoroutineDefaultController.kt`](../../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/webflux/controller/AbstractCoroutineDefaultController.kt)
- [`AbstractCoroutineIOController.kt`](../../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/webflux/controller/AbstractCoroutineIOController.kt)
- [`AbstractCoroutineVTController.kt`](../../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/webflux/controller/AbstractCoroutineVTController.kt)
- [`VirtualThreadAutoConfiguration.kt`](../../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/virtualthread/VirtualThreadAutoConfiguration.kt)
- [`AbstractVirtualThreadController.kt`](../../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/virtualthread/AbstractVirtualThreadController.kt)
- [`CustomWebClientConfigTest.kt`](../../../../../spring-boot/core/src/test/kotlin/io/bluetape4k/spring/webflux/config/CustomWebClientConfigTest.kt)

## 다음 읽을 장

[테스트와 생태계 학습 경로](./testing-and-ecosystem-paths.md)에서 기능별 검증 anchor와 다음 모듈 선택 기준을 정리합니다.
