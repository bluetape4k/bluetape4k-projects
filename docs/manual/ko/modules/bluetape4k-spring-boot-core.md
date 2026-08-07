---
manualId: bluetape4k-spring-boot-core
title: "Spring Boot 애플리케이션 기반 기능"
description: "Spring Context, HTTP, WebFlux, 오류 응답, 관측과 실행 리소스를 Kotlin답게 연결하는 공통 라이브러리입니다."
kind: library
group: spring
learningOrder: 900
---

# Spring Boot 애플리케이션 기반 기능

## 제공하는 기능 {#problem}

`bluetape4k-spring-boot-core`는 Spring Framework와 Spring Boot 애플리케이션에서 반복해서 작성하는 작은 연결 코드를 모아 둔 라이브러리입니다. Bean과 merged annotation 조회, profile과 property 접근, `RestClient` coroutine 호출, WebFlux 요청 컨텍스트와 `DataBuffer`, 표준 오류 응답, Micrometer Observation, 전용 `WebClient` 리소스 구성을 제공합니다.

Spring Boot 자체를 대신하지는 않습니다. 애플리케이션 context, HTTP client 구현, JSON converter, Actuator exporter, 보안, transaction은 Spring Boot와 애플리케이션이 구성합니다. 이 모듈은 그 위에서 Kotlin 확장 함수와 명시적으로 가져다 쓰는 configuration을 제공합니다.

## 사용하기 전에 결정할 것 {#when-to-use}

- 필요한 기능이 Spring Context helper인지, HTTP/WebFlux helper인지 먼저 나눕니다. 전체 모듈을 애플리케이션 기반 프레임워크처럼 취급하지 않습니다.
- `VirtualThreadAutoConfiguration`은 classpath에 추가하는 것만으로 활성화되지 않습니다. `@Import`로 명시해야 합니다.
- `HttpRequestCapturer`와 `ApiExceptionHandler`도 애플리케이션 component scan 범위에 들어와야 합니다.
- `RestClient`는 blocking client입니다. suspend 확장은 blocking 호출을 `Dispatchers.IO`에서 interruptible하게 실행할 뿐, 요청을 reactive I/O로 바꾸지 않습니다.
- WebClient 전용 event loop와 coroutine controller scope를 만들면 종료 책임도 함께 생깁니다.

단순한 Spring Data R2DBC coroutine 확장이 필요하면 [`bluetape4k-spring-boot-r2dbc`](./bluetape4k-spring-boot-r2dbc.md), JDBC와 transaction helper가 필요하면 [`bluetape4k-jdbc`](./bluetape4k-jdbc.md)를 먼저 확인합니다.

## 의존성 추가 {#coordinates}

사용자는 개별 Spring 또는 bluetape4k 라이브러리 버전을 맞추지 않고 `bluetape4k-dependencies` BOM 버전만 관리합니다. 실제로 사용할 Spring starter는 애플리케이션이 추가해야 합니다.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-spring-boot-core")

    implementation("org.springframework.boot:spring-boot-starter-webflux") // 사용하는 기능에 맞게 선택
}
```

이 모듈의 Web, WebFlux, Spring Data, Micrometer, Netty 의존성 대부분은 `compileOnly`입니다. API를 호출하려면 해당 기능을 제공하는 starter나 라이브러리가 runtime classpath에 있어야 합니다.

## 첫 사용 예제 {#quick-start}

Spring의 merged annotation 탐색을 reified 확장으로 간단히 사용할 수 있습니다.

```kotlin
val mapping = handlerMethod.findMergedAnnotationOrNull<RequestMapping>()
val isMapped = handlerMethod.hasMergedAnnotation<RequestMapping>()
```

Blocking `RestClient`를 coroutine service에서 호출할 때는 다음처럼 사용합니다.

```kotlin
class UserClient(
    private val client: RestClient,
) {
    suspend fun find(id: Long): User =
        client.suspendGet("/users/$id", MediaType.APPLICATION_JSON)
}
```

`suspendGet`은 `runInterruptible(Dispatchers.IO)`를 사용합니다. coroutine을 취소하면 client thread에 interrupt를 전달할 수 있지만, 실제 요청 중단 여부는 `ClientHttpRequestFactory`가 interrupt를 어떻게 처리하는지에 달려 있습니다.

## 작업별 API {#api-by-task}

| 필요한 작업 | API | 중요한 경계 |
| --- | --- | --- |
| merged annotation 조회 | `findMergedAnnotationOrNull`, `hasMergedAnnotation` | Spring의 meta-annotation과 `@AliasFor` 규칙을 그대로 따릅니다. |
| bean 조회 | `BeanFactory.get`, `findOrNull` 계열 | bean 부재는 `null`로 바꿀 수 있지만 중복 bean은 오류로 남깁니다. |
| profile 선언 | `LocalProfile`, `DevelopProfile`, `ProductionProfile` 등 | Spring `@Profile`의 별칭이며 property source를 만들지 않습니다. |
| property 조회 | `PropertyResolver.getAs`, `getRequiredPropertyAs` | Spring conversion을 사용하며 필수 값 부재는 실패합니다. |
| blocking HTTP 호출 | `restClientOf`, `httpGet`, `suspendGet` 계열 | request factory, converter, timeout은 애플리케이션 소유입니다. |
| 요청 컨텍스트 조회 | `HttpRequestCapturer`, `HttpRequestHolder` | Reactor Context 안에서만 현재 요청을 읽을 수 있습니다. |
| buffer streaming | `readAsDataBuffer`, `write`, `join`, `release` | cold `Flow`, buffer 크기, pooled buffer 해제 책임을 구분해야 합니다. |
| 오류 응답 | `ApiExceptionHandler`, `ApiErrorBody` | 정해진 예외만 처리하고 예외 메시지를 응답에 포함합니다. |
| 관측 | `observeSpring`, `observeSpringSuspending` | registry와 exporter는 애플리케이션이 제공합니다. |
| 전용 WebClient 구성 | `AbstractWebClientConfig` | event loop, SSL, timeout, codec memory와 종료 lifecycle을 함께 소유합니다. |
| 가상 스레드 executor | `VirtualThreadAutoConfiguration` | `@Import`가 필요하며 기존 `AsyncTaskExecutor`가 있으면 등록하지 않습니다. |

## 학습 경로 {#concepts}

각 장은 1.12.1 소스와 테스트를 기준으로 구성했습니다. 단순 API 목록이 아니라 애플리케이션이 소유해야 할 설정과 lifecycle, 실패가 어디까지 전파되는지 함께 설명합니다.

1. [Spring Context와 설정 helper](./bluetape4k-spring-boot-core/spring-context-and-configuration.md) — annotation, bean, profile, property helper와 auto-configuration이 아닌 범위를 구분합니다.
2. [RestClient와 coroutine 경계](./bluetape4k-spring-boot-core/rest-client-and-coroutines.md) — blocking HTTP 호출, nullable 응답, cancellation과 converter 책임을 다룹니다.
3. [WebFlux 요청 컨텍스트와 DataBuffer](./bluetape4k-spring-boot-core/webflux-request-and-buffers.md) — Reactor Context, 내부 경로 치환, cold `Flow`, pooled buffer 수명주기를 설명합니다.
4. [오류 응답과 관측](./bluetape4k-spring-boot-core/error-responses-and-observability.md) — HTTP 상태 매핑, 메시지 노출, Observation scope와 cardinality를 연결합니다.
5. [실행 리소스와 lifecycle](./bluetape4k-spring-boot-core/execution-resources-and-lifecycle.md) — 전용 WebClient loop, coroutine scope, virtual thread executor의 생성과 종료를 다룹니다.
6. [테스트와 생태계 학습 경로](./bluetape4k-spring-boot-core/testing-and-ecosystem-paths.md) — 가장 작은 검증 방법과 Spring Data·WebFlux·관측 모듈로 이어지는 길을 정리합니다.

처음 사용한다면 1장을 읽고 필요한 기능의 장으로 이동합니다. HTTP 호출을 추가한다면 2→5, WebFlux server 기능을 추가한다면 3→4→5 순서가 좋습니다.

## 권장 패턴 {#patterns}

애플리케이션의 service와 configuration이 Spring 객체의 소유권을 유지하고, 이 모듈에서는 필요한 확장만 골라 씁니다. `BeanFactory.findOrNull`로 bean 중복까지 숨기지 않고, 필수 property는 `getRequiredPropertyAs`로 startup에 실패하게 둡니다.

HTTP client는 blocking `RestClient`와 reactive `WebClient` 중 하나를 호출 경로에 맞게 선택합니다. `RestClient`를 suspend 함수로 감쌌다는 이유로 event loop에서 안전한 non-blocking client가 되는 것은 아닙니다. 반대로 WebClient 전용 loop를 만들면 thread 수, timeout, SSL trust와 shutdown을 한 configuration에서 관리합니다.

## 연동 {#integrations}

```text
Spring Boot application configuration
        ├── Spring Context / PropertyResolver
        ├── RestClient + request factory + message converters
        ├── WebFlux + Reactor Context + DataBuffer
        ├── ObservationRegistry + application exporter
        └── application-owned executors and lifecycle
                         ↓
             bluetape4k Spring helper APIs
```

`bluetape4k-spring-boot-core`는 이 객체들을 생성하는 범용 starter가 아닙니다. 예외적으로 `AbstractWebClientConfig`의 bean factory methods와, 명시적으로 import한 `VirtualThreadAutoConfiguration`은 객체를 생성합니다.

## 설정 {#configuration}

모듈이 직접 읽는 property는 `AbstractWebClientConfig.responseTimeout`의 `bluetape4k.webclient.response-timeout`입니다. 기본값은 ISO-8601 duration `PT30S`이며 Spring conversion으로 `30s` 같은 값도 binding할 수 있습니다.

```yaml
bluetape4k:
  webclient:
    response-timeout: 5s
```

전용 configuration properties class나 `META-INF/spring/...AutoConfiguration.imports`는 없습니다. `threadCount`, connect timeout, shutdown timeout, codec memory, SSL context는 `AbstractWebClientConfig`를 상속해 override합니다. Profile annotation은 활성 profile을 고르기 위한 meta-annotation이지 별도 설정 저장소가 아닙니다.

## 실패 동작 {#failures}

`getRequiredPropertyAs`는 값이 없거나 변환할 수 없으면 Spring의 property 예외를 전파합니다. `BeanFactory.findOrNull`은 bean 부재만 `null`로 바꾸고 `NoUniqueBeanDefinitionException`은 다시 던집니다.

`RestClient.retrieve()`의 HTTP status·decoding 오류도 그대로 전파됩니다. `suspendGet<T>`처럼 non-null을 약속하는 함수는 응답 body가 없으면 `!!`에서 실패하며, 빈 body가 정상이라면 `suspendGetOrNull<T>`을 사용합니다.

`ApiExceptionHandler`는 선언된 API 예외와 `HttpMessageNotReadableException`만 처리합니다. 오류 본문에는 stack trace가 없지만 `exception.message`가 들어가므로 외부에 공개해도 되는 메시지만 domain 예외에 넣어야 합니다.

## 운영 {#operations}

`AbstractWebClientConfig`를 사용하면 전용 Netty loop의 thread 수, connect/response timeout, codec 최대 메모리와 shutdown 시간을 관찰합니다. 기본 SSL context는 JDK trust store로 인증서를 검증합니다. `insecureSslContext()`는 개발·테스트 외에는 사용하지 않습니다.

Observation helper는 observation을 시작하고 scope를 열고 반드시 정지합니다. cancellation은 error로 기록하지 않고 다시 던집니다. metric label에는 bounded low-cardinality 값을 사용하고 request ID 같은 값은 high-cardinality trace field로 제한합니다.

## 테스트 {#testing}

```bash
./gradlew :bluetape4k-spring-boot-core:test --no-configuration-cache
```

`CustomWebClientConfigTest`는 전용 Reactor resource와 WebClient bean을, `RestClientCoroutinesDslTest`는 성공·nullable body·cancellation interrupt를 검증합니다. `HttpRequestFilterTest`는 Reactor Context의 요청 전달을, `SpringObservationSupportTest`는 start/error/stop과 cancellation 정리를 확인합니다.

오류 응답과 DataBuffer는 각각 `ApiExceptionHandlerTest`, `DataBufferSupportTest`에서 상태 코드, body, byte-count, read/write, pooled buffer release를 검증합니다.

## 워크숍과 예제 {#workshops}

전용 workshop은 manual manifest에 등록되어 있지 않습니다. 대신 모듈 README의 HTTP·관측 예제와 테스트 fixture가 실행 가능한 학습 자료입니다. 특히 `CustomWebClientConfig`, `WebClientReadmeExamplesTest`, `SpringObservationSupportTest`는 configuration에서 호출 코드까지 이어서 읽기 좋습니다.

Data access로 확장하려면 Spring Data JDBC/R2DBC 매뉴얼을, 더 낮은 수준의 coroutine과 Flow 규칙을 익히려면 [`bluetape4k-coroutines`](./bluetape4k-coroutines.md)를 함께 봅니다.

## 1.12.1 범위 {#limitations}

이 매뉴얼은 `bluetape4k-projects` 1.12.1 배포 소스를 기준으로 합니다. artifact 이름에 `spring-boot-core`가 들어가지만 starter metadata, configuration properties binding, 범용 application bootstrap은 제공하지 않습니다.

`VirtualThreadAutoConfiguration`은 자동 발견되지 않으며, `AbstractVirtualThreadController`의 companion executor는 process 수명 동안 공유되고 이 클래스 자체에서 닫지 않습니다. WebFlux coroutine controller base class의 별도 scope에는 Reactor/Spring Security context가 자동 전파되지 않습니다. 새 코드에서는 요청 자체의 suspend context로 충분한지 먼저 확인합니다.

## Source와 tests {#sources}

- [모듈 README](../../../../spring-boot/core/README.ko.md)
- [모듈 build](../../../../spring-boot/core/build.gradle.kts)
- [`AnnotationExtensions.kt`](../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/beans/AnnotationExtensions.kt)
- [`PropertyResolverExtensions.kt`](../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/core/PropertyResolverExtensions.kt)
- [`RestClientCoroutinesDsl.kt`](../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/http/RestClientCoroutinesDsl.kt)
- [`AbstractWebClientConfig.kt`](../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/webflux/config/AbstractWebClientConfig.kt)
- [`HttpRequestCapturer.kt`](../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/webflux/filter/HttpRequestCapturer.kt)
- [`DataBufferSupport.kt`](../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/core/io/buffer/DataBufferSupport.kt)
- [`ApiExceptionHandler.kt`](../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/rest/exceptions/ApiExceptionHandler.kt)
- [`SpringObservationSupport.kt`](../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/observability/SpringObservationSupport.kt)
- [`VirtualThreadAutoConfiguration.kt`](../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/virtualthread/VirtualThreadAutoConfiguration.kt)
- [`CustomWebClientConfigTest.kt`](../../../../spring-boot/core/src/test/kotlin/io/bluetape4k/spring/webflux/config/CustomWebClientConfigTest.kt)
- [`SpringObservationSupportTest.kt`](../../../../spring-boot/core/src/test/kotlin/io/bluetape4k/spring/observability/SpringObservationSupportTest.kt)

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### Spring Boot Core 기능 맵 다이어그램

[![Spring Boot Core 기능 맵 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-core-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-core-diagram-01.svg)

_배포본 README: [`spring-boot/core/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/spring-boot/core/README.ko.md)_

### Spring WebFlux + Coroutines 다이어그램

[![Spring WebFlux + Coroutines 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-core-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-core-diagram-02.svg)

_배포본 README: [`spring-boot/core/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/spring-boot/core/README.ko.md)_

### RestClient Coroutines DSL 다이어그램

[![RestClient Coroutines DSL 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-core-diagram-03.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-core-diagram-03.svg)

_배포본 README: [`spring-boot/core/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/spring-boot/core/README.ko.md)_

### WebClient 전용 리소스 구성 다이어그램

[![WebClient 전용 리소스 구성 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-core-diagram-04.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/spring-boot-core-diagram-04.svg)

_배포본 README: [`spring-boot/core/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/spring-boot/core/README.ko.md)_

<!-- release-readme-diagrams:end -->
