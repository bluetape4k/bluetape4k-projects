---
title: Spring Context와 설정 helper
description: merged annotation, BeanFactory, profile, property helper의 실제 계약과 자동 구성되지 않는 범위를 설명합니다.
manualId: bluetape4k-spring-boot-core
chapterId: spring-context-and-configuration
---

# Spring Context와 설정 helper

## Spring 의미를 Kotlin 문법으로 사용한다

이 모듈의 Context helper는 Spring 동작을 새로 정의하지 않습니다. `findMergedAnnotationOrNull<A>()`는 `AnnotatedElementUtils.findMergedAnnotation`, `BeanFactory.get<T>()`는 Spring `getBean<T>()`, `PropertyResolver.getAs<T>()`는 Spring conversion을 호출합니다. Kotlin에서 `Class<T>`를 반복해서 넘기지 않게 하는 얇은 계층입니다.

```kotlin
val requestMapping = method.findMergedAnnotationOrNull<RequestMapping>()
val timeout = environment.getAs<Duration>("client.timeout", Duration.ofSeconds(3))
```

Merged annotation은 meta-annotation과 `@AliasFor` 병합을 포함합니다. 단순한 `getAnnotation`과 탐색 범위가 다르므로 Spring의 find/get semantics를 알고 선택해야 합니다.

## bean 부재와 중복을 구분한다

`BeanFactory.findOrNull` 계열은 `NoSuchBeanDefinitionException`만 `null`로 바꿉니다. 같은 타입의 bean이 여러 개라면 `NoUniqueBeanDefinitionException`을 다시 던집니다. 선택이 모호한 상황을 “optional bean이 없다”로 숨기지 않는 계약입니다.

```kotlin
val registry = beanFactory.findOrNull<ObservationRegistry>()
```

정말 하나여야 하는 bean은 `get<T>()`로 조회해 startup 실패를 유지합니다. optional integration만 `findOrNull`로 읽고, 여러 후보가 가능하면 `@Qualifier`나 이름 기반 조회로 선택을 명시합니다.

## profile meta-annotation

`LocalProfile`, `DevelopProfile`, `FeatureProfile`, `TestProfile`, `QaProfile`, `StageProfile`, `ProductionProfile`은 Spring `@Profile`을 반복해서 쓰지 않게 만든 meta-annotation입니다.

```kotlin
@Configuration
@ProductionProfile
class ProductionClientConfiguration
```

`DevelopProfile`은 `dev`, `develop`, `development`, `ProductionProfile`은 `prod`, `product`, `production` 중 하나가 활성화되면 적용됩니다. 각 annotation의 `name` property는 metadata일 뿐, `@Profile` 표현식을 동적으로 바꾸지 않습니다.

## 필수 property는 startup에서 검증한다

```kotlin
val endpoint = environment.getRequiredPropertyAs<URI>("partner.endpoint")
val batchSize = environment.getAs("worker.batch-size", 100)
```

필수 값은 `getRequiredPropertyAs`로 읽어 누락이나 변환 실패를 startup에서 드러냅니다. 기본값이 domain에서 안전할 때만 default overload를 사용합니다. 이 모듈은 `@ConfigurationProperties` class, validation, property source를 만들지 않습니다.

## 자동 구성으로 오해하지 않는다

1.12.1 모듈에는 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`가 없습니다. `VirtualThreadAutoConfiguration`도 다음처럼 직접 가져와야 합니다.

```kotlin
@SpringBootApplication
@Import(VirtualThreadAutoConfiguration::class)
class Application
```

`HttpRequestCapturer`의 `@Component`와 `ApiExceptionHandler`의 `@RestControllerAdvice` 역시 application component scan 범위에 있을 때만 등록됩니다. artifact를 dependency에 추가했다는 사실만으로 모든 helper가 활성화되지는 않습니다.

## Source와 tests

- [`AnnotationExtensions.kt`](../../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/beans/AnnotationExtensions.kt)
- [`BeanFactoryExtensions.kt`](../../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/beans/BeanFactoryExtensions.kt)
- [`ProfileSupport.kt`](../../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/config/ProfileSupport.kt)
- [`PropertyResolverExtensions.kt`](../../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/core/PropertyResolverExtensions.kt)
- [`BeanFactoryExtensionsTest.kt`](../../../../../spring-boot/core/src/test/kotlin/io/bluetape4k/spring/beans/BeanFactoryExtensionsTest.kt)

## 다음 읽을 장

[RestClient와 coroutine 경계](./rest-client-and-coroutines.md)에서 blocking HTTP client를 suspend API로 사용할 때 유지해야 할 경계를 다룹니다.
