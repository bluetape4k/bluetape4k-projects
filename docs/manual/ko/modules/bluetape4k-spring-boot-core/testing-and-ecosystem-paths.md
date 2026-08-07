---
title: 테스트와 생태계 학습 경로
description: Spring Boot Core helper를 작은 단위로 검증하고 데이터·WebFlux·관측 모듈로 확장하는 방법을 정리합니다.
manualId: bluetape4k-spring-boot-core
chapterId: testing-and-ecosystem-paths
---

# 테스트와 생태계 학습 경로

## 전체 context보다 작은 경계를 먼저 검증한다

Spring helper라고 해서 모든 테스트에 `@SpringBootTest`가 필요한 것은 아닙니다. Annotation, BeanFactory, property, 오류 body, Observation helper는 작은 unit test로 계약을 확인할 수 있습니다. Bean wiring과 component scan이 핵심일 때만 application context를 띄웁니다.

```bash
./gradlew :bluetape4k-spring-boot-core:test --no-configuration-cache
```

모듈 전체 테스트는 최종 확인에 사용하고, 문서나 호출 코드를 바꿀 때는 관련 test class부터 실행합니다.

## 기능별 test anchor

| 기능 | 먼저 읽을 테스트 | 확인하는 계약 |
| --- | --- | --- |
| annotation과 bean | `AnnotationExtensionsTest`, `BeanFactoryExtensionsTest` | merged lookup, bean 부재·중복 |
| property | `PropertyResolverExtensionsTest` | nullable, default, required conversion |
| RestClient | `RestClientExtensionsTest`, `RestClientCoroutinesDslTest` | method, headers, body, nullable, interrupt |
| WebFlux request | `HttpRequestFilterTest` | Reactor Context 전달과 context 부재 |
| DataBuffer | `DataBufferSupportTest` | read/write, byte limit, join, pooled release |
| 오류 응답 | `ApiExceptionHandlerTest` | 예외별 status와 body |
| 관측 | `SpringObservationSupportTest` | start/error/stop, cancellation, scope cleanup |
| WebClient resource | `CustomWebClientConfigTest` | 전용 resource와 bean wiring |

## 실제 외부 경계를 분리한다

HTTP client 테스트는 mock server나 test request factory를 사용해 method, body, converter와 cancellation을 검증합니다. 외부 인터넷 서비스에 의존하지 않습니다. WebClient resource test는 bean 생성만이 아니라 `isUseGlobalResources=false`, timeout과 connector wiring을 확인합니다.

DataBuffer 테스트에서는 default buffer와 Netty pooled buffer를 나눠 release 결과를 확인합니다. Observation 테스트는 exporter 없이 recording handler로 lifecycle과 key values를 검증합니다.

## Spring Boot가 제공하는 것

다음 기능은 이 모듈의 책임이 아닙니다.

- application bootstrap과 auto-configuration discovery
- embedded server와 WebFlux runtime
- Jackson converter와 직렬화 정책
- Actuator endpoint, Prometheus 또는 OpenTelemetry exporter
- authentication, authorization, transaction
- HTTP connection pool, retry, circuit breaker

테스트 fixture에서 Spring Boot가 이 기능을 제공한다고 해서 library가 제공하는 것으로 문서화하지 않습니다.

## 다음 모듈 선택

- Coroutine과 Flow 자체의 cancellation, dispatcher, context 규칙은 [`bluetape4k-coroutines`](../bluetape4k-coroutines.md)에서 익힙니다.
- JDBC helper와 imperative transaction은 [`bluetape4k-jdbc`](../bluetape4k-jdbc.md)로 이어갑니다.
- Spring Data R2DBC entity operation을 Flow와 suspend로 쓰려면 [`bluetape4k-spring-boot-r2dbc`](../bluetape4k-spring-boot-r2dbc.md)를 봅니다.
- Hibernate와 Exposed를 비교해야 한다면 JDBC 기반 persistence 요구와 transaction 모델을 먼저 정한 뒤 각 repository의 매뉴얼로 이동합니다.
- Micrometer backend 세부 helper는 `bluetape4k-micrometer`, resilience policy는 `bluetape4k-resilience4j`가 담당합니다.

## 실전 학습 순서

1. Context helper 하나를 unit test와 함께 사용합니다.
2. HTTP가 필요하면 blocking과 reactive client를 선택하고 timeout·converter 소유자를 정합니다.
3. WebFlux 요청 context나 DataBuffer를 사용하면 subscription과 buffer 수명주기를 테스트합니다.
4. 오류 응답에 외부 공개 message와 안정된 error code를 정의합니다.
5. application-owned `ObservationRegistry`로 service 경계를 감싸고 cardinality를 점검합니다.
6. 전용 executor나 event loop는 운영 격리가 실제로 필요할 때만 추가합니다.

## Source와 tests

- [모듈 build](../../../../../spring-boot/core/build.gradle.kts)
- [한국어 README](../../../../../spring-boot/core/README.ko.md)
- [`WebClientReadmeExamplesTest.kt`](../../../../../spring-boot/core/src/test/kotlin/io/bluetape4k/spring/tests/WebClientReadmeExamplesTest.kt)
- [`RestClientCoroutinesDslTest.kt`](../../../../../spring-boot/core/src/test/kotlin/io/bluetape4k/spring/http/RestClientCoroutinesDslTest.kt)
- [`HttpRequestFilterTest.kt`](../../../../../spring-boot/core/src/test/kotlin/io/bluetape4k/spring/webflux/filter/HttpRequestFilterTest.kt)
- [`SpringObservationSupportTest.kt`](../../../../../spring-boot/core/src/test/kotlin/io/bluetape4k/spring/observability/SpringObservationSupportTest.kt)

## 매뉴얼로 돌아가기

[Spring Boot 애플리케이션 공통 경계](../bluetape4k-spring-boot-core.md)에서 전체 API 지도와 1.12.1 제한 사항을 다시 확인합니다.
