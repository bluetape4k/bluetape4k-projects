# Spring reified 확장 함수 설계서

- 상태: 최초 27개 API 설계와 `autoConfigurationsOf<T>()` 추가 범위 승인 완료; 호출부 컴파일 예제와 SpringFactoriesLoader 계약을 보완한 28개 API 설계의 A03 필수 여섯 관점, 추가 Architecture 확인 및 통합 검토 완료; §11에 관점별 결과와 SPW/KO 상태를 기록
- 유형: Type A — Spring Framework와 Spring Boot에 걸친 공개 API 확장
- 대상: `spring-boot/core` (`bluetape4k-spring-boot-core`)
- 기준 브랜치: `feat/spring-reified-extensions` at `5fc36ff930d624674d852a364f7411c26522a56a`
- 승인: 최초 범위 2026-10-09; 추가 범위 2026-10-10
- 작성일: 2026-10-09 KST

## 1. 배경과 목표

bluetape4k 여러 모듈에서 Spring API의 `Class<T>` 토큰을 반복해서 전달한다. 호출부에서 타입을 한 번만 지정하도록 `spring-boot/core`에 Spring API 직접 위임형 reified 확장을 제공한다.

확장은 Spring 동작을 재구현하지 않는다. 필요한 경우 `T::class.java`를 `Bindable.of`에 감싸고, 그 외에는 Spring API에 타입 토큰을 넘긴다. 반환형, nullable 결과, 예외, 검색 의미, supplier 평가 시점, builder의 self 타입과 불변성은 원 API 계약을 따른다. reified 타입 매개변수는 `T : Any`로 제한하고, Spring 결과가 nullable인 API는 반환형에 `T?`를 표시한다.

## 2. 구현 범위와 정확한 계약

### 2.1 Spring Boot Binder

다음 확장을 `io.bluetape4k.spring.boot`에 추가한다.

| Kotlin API | 결과 | 위임 API |
|---|---|---|
| `inline fun <reified T : Any> Binder.bind(name: String): BindResult<T>` | `BindResult<T>` | `bind(name, T::class.java)` |
| `inline fun <reified T : Any> Binder.bind(name: String, bindHandler: BindHandler?): BindResult<T>` | `BindResult<T>` | `bind(name, Bindable.of(T::class.java), bindHandler)` |
| `inline fun <reified T : Any> Binder.bindOrCreate(name: String): T` | `T` | `bindOrCreate(name, T::class.java)` |
| `inline fun <reified T : Any> Binder.bindOrCreate(name: String, bindHandler: BindHandler): T` | `T` | `bindOrCreate(name, Bindable.of(T::class.java), bindHandler)` |

현재 모듈 의존성으로 확인한 Spring Boot 4.1.1의 `Binder`에는 `Class<T>`와 `BindHandler`를 함께 받는 overload가 없다. handler를 받는 확장은 `Bindable.of(T::class.java)`를 사용한다. `bind`의 `BindHandler?`는 Spring `String` overload의 nullable 계약을 따른다. Java member overload shadowing을 피하기 위해 nullable handler를 명시할 때는 `binder.bind<T>("server", bindHandler = null)`처럼 named argument를 사용한다. `bindOrCreate`의 handler overload는 해당 `String` overload와 같은 non-null handler 계약을 따른다. 반환 및 binding 실패 동작은 Spring `BindResult`와 Binder가 정한다.

### 2.2 Spring Boot BootstrapContext

다음 확장을 `io.bluetape4k.spring.boot`에 추가한다. `T`는 런타임 타입 토큰을 위해 `Any`로 제한하고, Spring이 허용하는 nullable 값을 반환형과 인자에 표현한다.

| Kotlin API | 위임 API |
|---|---|
| `inline fun <reified T : Any> BootstrapContext.get(): T?` | `get(T::class.java)` |
| `inline fun <reified T : Any> BootstrapContext.getOrElse(defaultValue: T?): T?` | `getOrElse(T::class.java, defaultValue)` |
| `inline fun <reified T : Any> BootstrapContext.getOrElseSupply(other: Supplier<T?>): T?` | `getOrElseSupply(T::class.java, other)` |
| `inline fun <reified T : Any> BootstrapContext.getOrElseThrow(exceptionSupplier: Supplier<out Throwable>): T?` | `getOrElseThrow(T::class.java, exceptionSupplier)` |
| `inline fun <reified T : Any> BootstrapContext.isRegistered(): Boolean` | `isRegistered(T::class.java)` |

`get`은 미등록 타입에 대한 Spring 예외를 보존한다. `getOrElse` 계열은 등록 여부를 Spring에 맡기며, 등록된 supplier가 `null`을 반환하면 기본값으로 대체하지 않는다. `getOrElseSupply`와 `getOrElseThrow`는 Spring의 지연 평가를 보존한다. 예외 supplier는 `Throwable` 하위 타입을 그대로 반환할 수 있다.

### 2.3 SpringFactoriesLoader

다음 단일 확장을 `io.bluetape4k.spring.core.io.support`에 추가한다.

```kotlin
inline fun <reified T : Any> SpringFactoriesLoader.load(
    argumentResolver: SpringFactoriesLoader.ArgumentResolver? = null,
    failureHandler: SpringFactoriesLoader.FailureHandler? = null,
): List<T>
```

이 확장은 Spring Framework 7.0.9의 3-인자 `load(T::class.java, argumentResolver, failureHandler)` overload에 직접 위임한다. 기본 인자 덕분에 `load<T>()`, `load<T>(argumentResolver = resolver)`, `load<T>(failureHandler = handler)`, 두 전략을 모두 지정하는 호출을 지원한다. 동일 이름의 Java member와 충돌할 수 있으므로 nullable 전략을 명시할 때는 `load<T>(argumentResolver = null)` 또는 `load<T>(failureHandler = null)`처럼 named argument를 사용한다. `ArgumentResolver`와 `FailureHandler`는 호출자가 제공하는 callback이다. Spring은 각 factory 생성자 parameter type에 resolver를 호출하고, resolver가 반환한 nullable 값을 constructor argument로 사용한다. resolver 예외는 factory 인스턴스화 실패로 `FailureHandler`에 전달된다. 기본 handler는 실패 원인을 포함한 `IllegalArgumentException`을 던지고, 사용자 정의 handler가 예외를 던지면 그 예외가 전파된다. 사용자 정의 handler가 정상 반환하면 Spring은 실패한 factory를 제외한 채 다음 항목을 계속 읽으므로 부분 `List<T>`가 반환될 수 있다. 모든 필수 factory가 필요하면 기본 handler를 쓰거나 예외를 던지는 handler를 지정한다.

확장은 receiver가 가진 resource location과 class loader를 바꾸지 않는다. 별도 class loader 구성, `Class.forName`, reflection fallback, 예외 억제를 추가하지 않는다. resolver가 `null`을 반환하면 helper가 값을 대체하거나 해석하지 않고 Spring의 생성자 호출에 그대로 맡긴다. callback은 loader 실행 중 호출자 코드를 실행할 수 있고, `FailureHandler`가 정상 반환하면 부분 목록이 유효한 결과처럼 소비될 수 있다. factory classpath를 신뢰할 수 있다는 것은 배포 환경이 보장해야 할 신뢰 가정이다. helper는 classpath나 factory class를 검증·격리·sandbox 처리하지 않으며, 로딩 과정에서 callback과 factory 클래스 초기화에 따른 사용자 코드 side effect가 발생할 수 있음을 KDoc에 밝힌다.

### 2.4 Spring Framework core, context, expression

다음 확장을 추가한다.

| 패키지 | Kotlin API | 위임 API와 계약 |
|---|---|---|
| `io.bluetape4k.spring.context.annotation` | `inline fun <reified T : Any> AnnotationConfigRegistry.register(): Unit` | `register(T::class.java)` 단일 컴포넌트 overload |
| `io.bluetape4k.spring.beans` 기존 `AnnotationExtensions.kt` | `inline fun <reified A : Annotation> AnnotatedElement.getMergedRepeatableAnnotations(): Set<A>` | `AnnotatedElementUtils.getMergedRepeatableAnnotations(this, A::class.java)`의 get semantics |
| `io.bluetape4k.spring.core` | `inline fun <reified T : Any> typeDescriptorOf(): TypeDescriptor` | `TypeDescriptor.valueOf(T::class.java)`; raw class만 표현 |
| `io.bluetape4k.spring.core` | `inline fun <reified T : Any> annotationMetadataOf(): AnnotationMetadata` | `AnnotationMetadata.introspect(T::class.java)` |
| `io.bluetape4k.spring.expression` | `inline fun <reified T : Any> Expression.getValueAs(): T?` | `getValue(T::class.java)` |
| 같은 패키지 | `inline fun <reified T : Any> Expression.getValueAs(rootObject: Any?): T?` | `getValue(rootObject, T::class.java)` |
| 같은 패키지 | `inline fun <reified T : Any> Expression.getValueAs(context: EvaluationContext): T?` | `getValue(context, T::class.java)` |
| 같은 패키지 | `inline fun <reified T : Any> Expression.getValueAs(context: EvaluationContext, rootObject: Any?): T?` | `getValue(context, rootObject, T::class.java)` |

반복 애너테이션 함수는 기존 `findAllMergedAnnotations<A>()`와 이름과 검색 의미를 구분한다. `typeDescriptorOf<T>()`는 Java `Class` 토큰만 쓰므로 generic type argument를 보존한다고 설명하지 않는다. `AnnotationConfigRegistry.register<T>()`에는 애플리케이션 또는 테스트의 신뢰된 설정 클래스만 전달한다. 이 helper는 클래스 검증, sandbox, 격리를 추가하지 않으며 실제 설정 클래스 처리는 Spring context lifecycle에 따른다. SpEL 확장은 주어진 `EvaluationContext`를 그대로 사용하고, context가 없는 overload는 Spring의 context 없는 overload를 그대로 호출한다. nullable root object를 명시할 때는 `expression.getValueAs<T>(rootObject = null)`처럼 named argument를 사용한다. context, resolver, parser를 암묵적으로 만들거나 접근 정책을 넓히지 않는다. 이 helper가 untrusted expression을 검증하거나 안전하게 만드는 기능이 아님을 KDoc에 명시한다.

### 2.5 Spring Boot 테스트 runner

다음 확장을 기존 `io.bluetape4k.spring.tests` 패키지에 추가한다. 호출자가 reified 타입 매개변수 하나만 지정하도록 세 concrete runner별 overload를 제공하고, 반환형은 각 receiver의 concrete 타입으로 고정한다.

| receiver | 설정 클래스 | 이름 없는 bean 등록 | 이름을 지정한 bean 등록 | 반환형 |
|---|---|---|---|---|
| `ApplicationContextRunner` | `withUserConfiguration<CONFIG>()` | `withBean<T>(vararg customizers, crossinline supplier)` | `withBean<T>(name: String?, vararg customizers, crossinline supplier)` | `ApplicationContextRunner` |
| `WebApplicationContextRunner` | `withUserConfiguration<CONFIG>()` | `withBean<T>(vararg customizers, crossinline supplier)` | `withBean<T>(name: String?, vararg customizers, crossinline supplier)` | `WebApplicationContextRunner` |
| `ReactiveWebApplicationContextRunner` | `withUserConfiguration<CONFIG>()` | `withBean<T>(vararg customizers, crossinline supplier)` | `withBean<T>(name: String?, vararg customizers, crossinline supplier)` | `ReactiveWebApplicationContextRunner` |

```kotlin
inline fun <reified CONFIG : Any> ApplicationContextRunner.withUserConfiguration(): ApplicationContextRunner
inline fun <reified CONFIG : Any> WebApplicationContextRunner.withUserConfiguration(): WebApplicationContextRunner
inline fun <reified CONFIG : Any> ReactiveWebApplicationContextRunner.withUserConfiguration(): ReactiveWebApplicationContextRunner

inline fun <reified T : Any> ApplicationContextRunner.withBean(
        vararg customizers: BeanDefinitionCustomizer,
        crossinline supplier: () -> T,
    ): ApplicationContextRunner

inline fun <reified T : Any> ApplicationContextRunner.withBean(
        name: String?,
        vararg customizers: BeanDefinitionCustomizer,
        crossinline supplier: () -> T,
    ): ApplicationContextRunner

```

구현은 각 concrete runner의 Spring `withUserConfiguration(Class<?>...)` 및 두 supplier 기반 `withBean` overload에 직접 위임한다. 공개 확장은 `CONFIG` 또는 `T` 하나만 reified generic으로 받는다. Kotlin supplier는 `crossinline`이며 Java `Supplier<T>`로 감싸되 호출 즉시 실행하지 않는다. Spring은 실제 context를 구성할 때 bean registration과 customizer를 적용한다. supplier는 Spring이 해당 bean을 생성할 때 호출하며, eager bean이면 대개 `run`의 refresh 중 실행하고, lazy bean이면 첫 접근 때 실행한다. `prepare`는 refresh하지 않으며 supplier 호출 시점은 이후 bean 생성까지 늦어질 수 있다. 이름 인자는 Spring처럼 nullable이고 bean definition customizer vararg를 보존한다. Java `withBean` member와의 overload shadowing을 피하려고 nullable 이름은 `.withBean<Service>(name = null) { Service() }`처럼 named argument로 지정한다. positional `null` 호출은 지원 예시로 사용하지 않는다. 세 concrete runner 모두 이 named call이 확장을 선택하고 concrete 반환형을 유지하는지 compile 검증한다. 각 호출은 새 runner를 반환하며 원래 runner를 변경하지 않는다. 반복 실행은 별도 context를 만들며 context별 bean 생성 시 supplier를 호출할 수 있다. 세 runner 계열 각각에 대해 타입 인자 하나로 호출되는지, 반환 타입과 fluent chaining이 concrete 타입을 보존하는지 compile 검증한다.

### 2.6 Spring Boot AutoConfigurations test helper

다음 단일 설정 타입 helper를 기존 `io.bluetape4k.spring.tests` 패키지에 추가한다.

```kotlin
inline fun <reified T : Any> autoConfigurationsOf(): AutoConfigurations =
    AutoConfigurations.of(T::class.java)
```

Spring Boot 4.1.1의 `AutoConfigurations.of(Class<?>...)`는 Java static vararg method이며 Kotlin reified overload를 제공하지 않는다. 이 helper는 한 번에 설정 class 하나만 받는다. 여러 auto-configuration을 지정하는 호출은 기존 `AutoConfigurations.of(Class<?>...)`를 계속 사용해 vararg 의미와 정렬 동작을 보존한다. 호출 예는 `val runner = ApplicationContextRunner().withConfiguration(autoConfigurationsOf<MyAutoConfiguration>())`이다. `AutoConfigurations` 타입은 `org.springframework.boot:spring-boot-autoconfigure` artifact에 있다. 테스트에서는 기존 `ApplicationContextRunnerExtensionsTest` 안에 `@AutoConfiguration` fixture를 두고 등록 결과를 확인한다.

이 후보는 pinned workspace에서 확인한 실제 호출 근거에 따라 포함한다. 대상 checkout에는 4개 Kotlin file의 8개 호출이 있고, 전체 bluetape4k workspace 검색에는 107개 file이 있다. 범위에는 신규 reified API와 README 예제만 포함하며 기존 다른 module 호출부는 바꾸지 않는다.

`T`는 애플리케이션 또는 테스트의 신뢰된 auto-configuration class여야 한다. helper는 class를 검증하거나 격리하지 않으며, 조건 평가와 bean/factory method 실행 및 그 실패 처리는 Spring lifecycle 계약에 따른다. 여러 class를 지정할 때도 호출자는 Spring의 기존 vararg API에 신뢰된 class만 전달해야 한다.

## 3. 이미 reified를 제공하는 Spring API

Spring native 확장과 중복되는 API를 만들지 않는다.

- `PropertyResolver.getProperty<T>()`, `getProperty(key, defaultValue)`, `getRequiredProperty<T>()` 및 이를 상속하는 `Environment`
- `BeanFactory.getBean<T>()`, `getBeanProvider<T>()`
- `ListableBeanFactory.getBeansOfType<T>()` 및 annotation 기반 bean 검색
- `Cache.get<T>()`
- `GenericApplicationContext.registerBean<T>()`
- `SpringApplication.runApplication<T>()`
- Spring JDBC, web, test 응답 body reified 확장
- Spring Boot Kotlin BeanDefinition DSL의 reified bean 등록 API
- Spring AOT hint API의 reified type 등록 확장

현재 대상 모듈에도 자체 BeanFactory, annotation, property 확장이 있다. 새 함수 이름이나 검색 의미가 겹치지 않게 기존 코드를 확인한다. `findAllMergedAnnotations<A>()`는 Spring의 find-semantics이고 신규 함수는 get-semantics를 보존한다.

## 4. 대안과 결정

| 대안 | 장점 | 단점 | 결정 |
|---|---|---|---|
| Spring class-token 호출만 reified wrapper로 제공 | 호출부에서 타입 토큰 반복을 줄이고 Spring의 계약을 직접 보존 | 함수 수가 증가하고 API마다 nullability/overload를 확인해야 함 | 채택 |
| `KClass` 기반 범용 변환 계층을 새로 추가 | Kotlin 타입 인자를 받을 수 있음 | Spring Java API에 전달하기 전 변환 계층이 생기고 generic type 보존으로 오해할 수 있음 | 제외 |
| Spring의 모든 Class 인자 API를 일괄 포장 | 표면상 범위가 넓음 | native reified 확장 복제, 실사용 근거가 약한 API와 불필요한 공개 표면이 생김 | 제외 |

`AutoConfigurations.of(Class<?>...)`는 Java static vararg API라 Spring native Kotlin reified 확장이 없다. 독립 caller review와 pinned source search에서 workspace 사용 근거가 확인되어 단일 타입용 `autoConfigurationsOf<T>()`를 포함한다. 복수 타입 호출은 기존 vararg API에 남긴다. 대상 checkout에서는 4개 file, 8개 호출이며 workspace 전체에서는 107개 Kotlin file에서 검색됐다. Boot 4.1.1 source JAR의 선언과 기존 `spring-boot-autoconfigure` compileOnly dependency를 확인했으며 Gradle dependency는 바꾸지 않는다.

`ResolvableType.forClass<T>()`, 정적 `SpringFactoriesLoader.loadFactories(Class, ClassLoader)`, 정적 `loadFactoryNames`는 현재 workspace 사용 근거가 없다. `loadFactoryNames`는 deprecated API이며 나머지는 이번 사용 근거 기반 범위 밖으로 둔다. 대상 API별 workspace 호출 근거를 새로 확인하면 후속 변경 후보로 기록한다.

## 5. 호출부 예시와 호환성

기존 Java 스타일 호출은 그대로 유지되며, reified 호출로 바꾸는 것은 선택 사항이다. 호출부를 이 저장소 외부나 AWS 저장소에서 변경하지 않는다.

| API 영역 | 기존 호출 | reified 호출 |
|---|---|---|
| Binder | `binder.bind("server", ServerProperties::class.java)` | `binder.bind<ServerProperties>("server")` |
| Binder handler | `binder.bind("server", Bindable.of(ServerProperties::class.java), handler)` | `binder.bind<ServerProperties>("server", handler)` |
| BootstrapContext | `context.get(SharedResource::class.java)` | `context.get<SharedResource>()` |
| SpringFactoriesLoader | `loader.load(Extension::class.java, resolver, failureHandler)` | `loader.load<Extension>(argumentResolver = resolver, failureHandler = failureHandler)` |
| annotation metadata | `AnnotationMetadata.introspect(AppConfiguration::class.java)` | `annotationMetadataOf<AppConfiguration>()` |
| SpEL | `expression.getValue(context, User::class.java)` | `expression.getValueAs<User>(context)` |
| 테스트 runner | `.withUserConfiguration(TestConfiguration::class.java)` | `.withUserConfiguration<TestConfiguration>()` |
| 테스트 bean | `.withBean(Service::class.java, Supplier { Service() })` | `.withBean<Service> { Service() }` |
| AutoConfigurations | `AutoConfigurations.of(MyAutoConfiguration::class.java)` | `autoConfigurationsOf<MyAutoConfiguration>()` |
| 이름·customizer가 있는 테스트 bean | `.withBean("service", Service::class.java, supplier, customizer)` | `.withBean<Service>("service", customizer) { Service() }` |
| nullable 이름을 쓰는 테스트 bean | Spring nullable-name member overload | `runner.withBean<Service>(name = null) { Service() }` |

복사해 쓸 수 있는 확장 import는 다음과 같다. README 예제도 실제 사용하는 함수의 전체 import를 각각 표시하고 wildcard import를 쓰지 않는다.

```kotlin
import io.bluetape4k.spring.boot.bind
import io.bluetape4k.spring.boot.bindOrCreate
import io.bluetape4k.spring.boot.get
import io.bluetape4k.spring.boot.getOrElse
import io.bluetape4k.spring.boot.getOrElseSupply
import io.bluetape4k.spring.boot.getOrElseThrow
import io.bluetape4k.spring.boot.isRegistered
import io.bluetape4k.spring.core.io.support.load
import io.bluetape4k.spring.context.annotation.register
import io.bluetape4k.spring.beans.getMergedRepeatableAnnotations
import io.bluetape4k.spring.core.typeDescriptorOf
import io.bluetape4k.spring.core.annotationMetadataOf
import io.bluetape4k.spring.expression.getValueAs
import io.bluetape4k.spring.tests.withUserConfiguration
import io.bluetape4k.spring.tests.withBean
import io.bluetape4k.spring.tests.autoConfigurationsOf
```

runner helper의 generic 반환형은 세 runner 계열에서 보존한다.

```kotlin
val applicationRunner: ApplicationContextRunner =
    ApplicationContextRunner().withUserConfiguration<ApplicationConfig>()
val webRunner: WebApplicationContextRunner =
    WebApplicationContextRunner().withUserConfiguration<WebConfig>()
val reactiveRunner: ReactiveWebApplicationContextRunner =
    ReactiveWebApplicationContextRunner().withUserConfiguration<ReactiveConfig>()

val applicationWithNullName: ApplicationContextRunner =
    ApplicationContextRunner().withBean<Service>(name = null) { Service() }
val webWithNullName: WebApplicationContextRunner =
    WebApplicationContextRunner().withBean<Service>(name = null) { Service() }
val reactiveWithNullName: ReactiveWebApplicationContextRunner =
    ReactiveWebApplicationContextRunner().withBean<Service>(name = null) { Service() }

val customizedRunner: ApplicationContextRunner =
    applicationRunner.withBean<Service>(BeanDefinitionCustomizer { it.setLazyInit(true) }) {
        Service()
    }
```

결과가 없을 때도 Spring 계약을 그대로 확인할 수 있다. 예를 들어 `binder.bind<ServerProperties>("missing").isBound`는 `false`일 수 있고, `loader.load<Extension>()`은 빈 목록을 반환할 수 있으며, `expression.getValueAs<User>()`는 `null`일 수 있다. 필수 bootstrap 항목이 없으면 `get<T>()`가 Spring 예외를 전달하고, 호출자는 `getOrElse<T>(defaultValue)`를 선택할 수 있다.

이 변경은 기존 API를 추가로 노출하는 source-compatible 변경이며, 호출부 마이그레이션은 선택적이고 수동이다. 대상 모듈은 Spring artifact를 compile-only로 선언하므로 소비자는 사용하는 공개 API에 해당하는 의존성을 직접 제공해야 한다.

| 공개 확장 | 소비자가 제공할 artifact |
|---|---|
| `Binder`, `BootstrapContext` | `org.springframework.boot:spring-boot` |
| `SpringFactoriesLoader`, repeatable annotation, `TypeDescriptor`, `AnnotationMetadata` | `org.springframework:spring-core` |
| `AnnotationConfigRegistry.register` | `org.springframework:spring-context` |
| SpEL `Expression.getValueAs` | `org.springframework:spring-expression` |
| ApplicationContext runner 확장 | `org.springframework.boot:spring-boot-test`를 테스트 구성에 추가 |
| `autoConfigurationsOf<T>()` | `org.springframework.boot:spring-boot-autoconfigure` |

새 외부 의존성이나 Spring 버전 변경은 없다.

## 6. 신뢰 경계와 실패 모드

| 상황 | 보존할 동작 |
|---|---|
| Binder 입력이 없거나 변환/검증이 실패함 | `BindResult`, 생성 동작, `BindHandler` callback/예외를 Spring에 맡긴다. |
| BootstrapContext 타입이 미등록되었거나 등록 supplier가 `null`을 반환함 | `get` 예외, nullable 결과, default/supplier/exception supplier 실행 시점을 Spring에 맡긴다. |
| SpringFactoriesLoader가 factory class를 찾거나 인스턴스화하지 못함 | 기본 throwing handler는 `IllegalArgumentException`과 원인을 보존한다. 사용자 정의 `FailureHandler`가 예외를 던지면 그대로 전파하고 정상 반환하면 실패 factory를 제외한 부분 `List<T>`가 반환될 수 있다. 필수 factory가 모두 로드되어야 하면 기본 또는 명시적 throwing handler를 사용한다. classpath trust는 배포 환경의 가정이며 helper는 classpath나 factory를 검증·격리하지 않는다. |
| `AnnotationConfigRegistry.register<T>()` 또는 `autoConfigurationsOf<T>()`로 설정 클래스를 전달함 | 애플리케이션 또는 테스트의 신뢰된 class만 입력한다. helper는 class를 검증·격리·sandbox 처리하지 않는다. 조건 평가, bean 등록, factory method 실행과 실패 전파는 Spring context lifecycle을 따른다. 외부 입력으로 선택한 class를 안전하게 만들지 않는다. |
| SpEL 표현식이 제한된 EvaluationContext에서 허용되지 않은 member를 호출함 | 전달한 context의 접근 정책과 Spring 평가 예외를 유지한다. |
| runner bean supplier가 실패함 | `withBean` 호출 때 실행하지 않는다. Spring은 bean을 실제 생성할 때 supplier를 호출한다. eager bean은 보통 `run`의 refresh 중 실행되고, lazy bean은 첫 접근 때 실행되며, `prepare`는 refresh하지 않아 supplier 실행을 뒤로 미룰 수 있다. 예외는 실제 bean 생성 단계에서 전파된다. |
| runner `BeanDefinitionCustomizer`가 실패함 | `withBean` 호출 때 실행하지 않는다. Spring이 context 구성 중 bean definition을 등록할 때 적용하며, 보통 `run` 또는 `prepare`에서 실제 context를 만들 때 예외를 전파한다. |

확장 자체는 예외를 삼키거나 로그·메트릭·health/readiness signal을 추가하지 않는다. SpringFactoriesLoader가 수행하는 자체 logging과 클래스 초기화는 Spring 동작으로 남는다. DB, 네트워크, executor, 애플리케이션 lifecycle을 추가하지 않으므로 runbook/health 변경은 없다.

## 7. 파일과 문서

새 소스 파일:

- `spring-boot/core/src/main/kotlin/io/bluetape4k/spring/boot/BinderExtensions.kt`
- `spring-boot/core/src/main/kotlin/io/bluetape4k/spring/boot/BootstrapContextExtensions.kt`
- `spring-boot/core/src/main/kotlin/io/bluetape4k/spring/core/io/support/SpringFactoriesLoaderExtensions.kt`
- `spring-boot/core/src/main/kotlin/io/bluetape4k/spring/context/annotation/AnnotationConfigRegistryExtensions.kt`
- `spring-boot/core/src/main/kotlin/io/bluetape4k/spring/core/TypeDescriptorExtensions.kt`
- `spring-boot/core/src/main/kotlin/io/bluetape4k/spring/core/AnnotationMetadataExtensions.kt`
- `spring-boot/core/src/main/kotlin/io/bluetape4k/spring/expression/ExpressionExtensions.kt`
- `spring-boot/core/src/main/kotlin/io/bluetape4k/spring/tests/ApplicationContextRunnerExtensions.kt`
- `spring-boot/core/src/main/kotlin/io/bluetape4k/spring/tests/AutoConfigurationsExtensions.kt`

기존 파일 수정:

- `spring-boot/core/src/main/kotlin/io/bluetape4k/spring/beans/AnnotationExtensions.kt`
- `spring-boot/core/README.md`
- `spring-boot/core/README.ko.md`

각 helper에 한국어 KDoc과 신뢰 경계, 안전·nullability·검색 의미를 설명한다. 설정/factory class는 신뢰된 애플리케이션 또는 테스트 코드 입력이며, helper가 classpath/class 입력을 검증하거나 격리하지 않는다는 점과 Spring lifecycle/failure semantics를 보존한다. 영어·한국어 README 모두 기능 목록과 동일한 사용 예를 갱신한다. Changelog/release note는 이번 feature 작업에 추가하지 않는다. 실제 릴리스가 승인되면 릴리스 작업의 version/changelog gate에서 관리한다.

## 8. 검증 기준과 DoD

1. 표에 나열한 28개 API가 Kotlin compile usage에서 타입 토큰 없이 호출되고 Spring의 정확한 overload를 선택한다. `autoConfigurationsOf<T>()`는 단일 class를 Spring의 Java vararg API에 전달하며 여러 class 호출은 기존 vararg API로 유지한다.
2. reified class-token 타입은 `T : Any`, 애너테이션 타입은 `A : Annotation`으로 제한한다. unchecked cast, `Class<*> as Class<T>`, `Any as T`는 사용하지 않는다.
3. Binder handler overload는 `Bindable.of(T::class.java)`로 실제 Spring handler overload를 부른다.
4. BootstrapContext nullable 반환/default/supplier 동작과 `Throwable` 허용 범위를 테스트한다.
5. loader resolver가 constructor parameter type을 받고 non-null 값과 `null`을 Spring에 그대로 넘기는지 확인한다. resolver 예외가 `FailureHandler`에 전달되는지, 기본 throwing handler가 `IllegalArgumentException`과 원인을 보존하는지, 정상 반환하는 custom handler가 성공 factory와 실패 factory가 섞인 SPI에서 부분 목록을 반환하는지 receiver의 classloader/resource 기준으로 검증한다.
6. repeatable annotation get semantics와 기존 find semantics 차이, `TypeDescriptor` raw class 한계를 테스트와 KDoc에 드러낸다.
7. SpEL 네 overload, named nullable root, nullable 결과, 제한 EvaluationContext 정책 보존을 검증한다.
8. 세 runner subtype에서 concrete fluent chaining을 컴파일한다. 각 subtype에 `.withBean<T>(name = null) { ... }`를 호출해 named nullable argument가 확장을 선택하고 concrete 반환형을 보존하는지 확인한다. positional `null` 호출은 지원 예시로 제시하지 않는다. `withBean` 호출 전 supplier/customizer 미실행, context 구성 중 customizer 적용과 실패 전파, eager/lazy bean 및 `prepare` 이후 실제 접근에 따른 supplier 실행 시점과 실패 전파, 반복 실행마다 별도 context에 맞는 supplier 호출, customizer 전달, 원 runner 불변성을 확인한다.
9. 변경 파일에 대응하는 단위 테스트, 대상 모듈 test/build 및 lint/static analysis를 통과시킨다. Testcontainers 검증은 모듈 내 대상이 있을 때 순차 실행한다.
10. 영어·한국어 README를 같은 28개 API 범위와 정확한 함수별 전체 import로 갱신한다. Binder, BootstrapContext, SpringFactoriesLoader 및 annotation/type/metadata/expression 확장의 import는 복사 가능한 `import ...functionName` 문장으로 표시하고 wildcard를 쓰지 않는다. 세 runner 모두 `name = null` named call을 보여 주며 positional `null` 호출은 넣지 않는다. `autoConfigurationsOf<T>()`와 기존 다중 class vararg 호출을 함께 보여 주고 `git diff --check`와 문서 예제 대조를 통과시킨다.
11. dependency graph 변경이 없고, 공개 API별 `spring-boot`, `spring-boot-autoconfigure`, `spring-core`, `spring-context`, `spring-expression`, `spring-boot-test` 소비자 compile/runtime 범위를 README에서 안내한다.
12. 릴리스, PR, merge는 이번 작업 승인 범위에 포함하지 않는다. 회귀 시 변경 commit을 revert하거나 이전 공개 artifact를 사용해 되돌릴 수 있다.
13. `SpringFactoriesLoader`, `AnnotationConfigRegistry.register<T>()`, `autoConfigurationsOf<T>()`의 KDoc은 factory classpath 및 configuration class가 신뢰된 배포·애플리케이션 입력이라는 경계를 설명한다. wrapper가 class를 검증·격리·sandbox 처리하지 않고 Spring lifecycle, 사용자 코드 실행, 예외 전파를 그대로 따른다는 점을 문서와 review에서 확인한다.

## 9. 구현 전 검토에서 반영한 사항

| 우선도 | 관점 | 발견 및 반영 |
|---|---|---|
| P1 | Stability, Developer/API | Binder handler `Class<T>` overload가 없어 `Bindable.of` 위임으로 수정했다. |
| P1 | Developer/API | runner 공개 함수를 concrete runner 세 종류별 overload로 정리해 타입 인자를 하나만 받도록 했다. supplier를 캡처하는 inline 인자는 `crossinline`으로 제한했다. |
| P1 | Developer/API | `BootstrapContext.getOrElseSupply` 인자는 Spring의 invariant parameter와 일치하도록 `Supplier<T?>`로 바꿨다. |
| P2 | Developer/API | public signature가 노출하는 Spring artifact별 소비자 compile/runtime 의존성을 명시했다. |
| P2 | User/Caller | 독립 호출부 검토에서 발견한 `AutoConfigurations.of` 단일 타입 사용을 28번째 API로 포함하고, single-type helper와 기존 vararg 경계를 명시했다. 대상 checkout 4개 file/8개 호출, workspace 107개 file 검색, Boot 4.1.1 source JAR 및 consumer artifact를 추적한다. |
| P2 | User/Caller | §2.6의 예제가 `ApplicationContextRunner` 타입 이름에서 인스턴스 메서드를 호출하던 오류를 `ApplicationContextRunner()` 인스턴스 기반 예제로 수정했다. 같은 예를 참조하던 plan Task 7도 runner 인스턴스 호출을 사용한다. |
| P2 | User/Caller | runner Java member overload와의 shadowing을 피하도록 nullable 이름은 `name = null` named argument로 호출하고 세 concrete runner에서 compile-check하도록 명시했다. |
| P3 | User/Caller | annotation/type/metadata/expression 및 Binder/BootstrapContext README import 안내를 package 이름이나 wildcard가 아닌 복사 가능한 전체 function import로 구체화했다. |
| P1 | Stability | BootstrapContext의 nullable 결과, nullable default/supplier 값, `Throwable` exception supplier 계약을 명시했다. |
| P1 | Stability, Developer/API, User/Caller | 세 concrete runner별 public overload가 reified 타입 인자 하나만 받고 concrete 반환형을 보존하도록 수정했다. customizer, nullable name, 지연 supplier 계약도 명시했다. |
| P1 | User/Caller | SpEL `getValue`의 네 typed overload와 nullable root/result를 명시했다. |
| P2 | Security | loader classpath trust boundary와 SpEL EvaluationContext 보존 정책을 추가했다. |
| P2 | Stability, Developer/API | SpringFactoriesLoader의 strategy 인자를 하나의 defaulted Kotlin helper로 모아 조합별 호출을 지원하고, nullable strategy는 named argument로 전달하도록 했다. |
| P2 | Stability | Binder와 SpringFactoriesLoader의 member shadowing을 고려해 nullable 전략 인자는 named argument로 지정하도록 명시했다. |
| P2 | Security | A03 review에서 확인한 `FailureHandler`의 반환 정책을 반영했다. callback이 정상 반환하면 실패 factory를 건너뛰어 부분 목록이 될 수 있음을 명시하고, 모든 필수 factory가 필요한 경우 기본 또는 throwing handler를 사용하도록 안내하며 성공·실패 factory 혼합 fixture 검증을 추가했다. |
| P2 | Stability | A03 review에서 확인한 resolver 계약 공백을 보완했다. Spring이 constructor parameter type별 resolver를 호출하고 nullable 결과를 constructor argument로 사용하며, resolver 예외를 `FailureHandler`에 전달하는 흐름을 명시했다. non-null/nullable 반환과 resolver 예외 전달을 focused acceptance test에 추가한다. |
| P2 | Stability | runner customizer의 context 준비 시 적용·실패 시점과 검증을 명시했다. |
| P2 | Operator/Ops, User/Caller | 양쪽 README 업데이트, additive 호환성, 수동 선택 마이그레이션, rollback 및 운영 신호 N/A를 명시했다. |
| P3 | Security, User/Caller | annotation generic bound와 unchecked cast 금지를 추가하고, 설계 상태를 승인 완료로 고쳤다. |

`TypeDescriptor`와 `AnnotationMetadata`, SpEL, repeatable annotation 호출 근거는 workspace source 검색을 사용했다. `ApplicationContextRunner.withUserConfiguration` 호출은 약 234건/141 파일, `withBean(Class, Supplier)` 후보는 약 406건이었다. Boot 4.1.1 및 Spring Framework 7.0.9 source JAR에서 overload/nullability/runner lifecycle을 대조했다. 대상 모듈 compile classpath의 `dependencyInsight` 결과는 Boot 4.1.1을 선택한다. 열린 AWS PR #656 diff 전체는 GitHub 파일 수 제한 때문에 일괄 확인하지 못했다. AWS 저장소는 변경하지 않는다.

## 10. 최초 27개 API 설계서의 Step 2-R 검토 이력

아래 여섯 관점 결과와 writer DoD는 2026-10-09에 승인된 최초 27개 API 설계서에 대한 이력이다. 2026-10-10에 `autoConfigurationsOf<T>()` 범위가 추가 승인되어 기술 의미와 DoD가 바뀌었으므로, 이 결과는 수정본의 검토 증거로 재사용하지 않는다.

| 관점 | 최종 P0 | 최종 P1 | 최종 P2 | 최종 P3 | 결과 |
|---|---:|---:|---:|---:|---|
| 성능 | 0 | 0 | 0 | 0 | 완료; wrapper 외 추가 hot path 없음 |
| 안정성 | 0 | 0 | 0 | 0 | 완료; nullable overload와 runner lifecycle 재검토 |
| 보안 | 0 | 0 | 0 | 0 | 완료; classloader, SpEL 정책, cast 경계 검토 |
| 운영 | 0 | 0 | 0 | 0 | 완료; 의존성, rollback, runbook/lifecycle 검토 |
| 개발자/API | 0 | 0 | 0 | 0 | 완료; Boot 4.1.1 source 근거 재검토 |
| 사용자/caller | 0 | 0 | 0 | 0 | 완료; 호출 예시, nullable 호출, 소비자 의존성 검토 |

### Writer DoD

| 항목 | 상태 | 증거 |
|---|---|---|
| SPW-01 — 대상 독자·목적·근거 | PASS | 구현자와 모듈 사용자용 설계서; 저장소 검색, source JAR, Spring 공식 API 링크를 §9와 출처에 기록 |
| SPW-02 — 설계서 구조 | PASS | 목표, 정확한 API, 제외 범위, 대안, 실패 모드, 호환성, acceptance criteria 포함 |
| SPW-03 — 한국어 기술 문체 | PASS | 용어와 문장 흐름을 최종 read-back했고, contextual terminology audit에서 findings 0 |
| SPW-04 — 기술 근거와 추적성 | PASS | 여섯 리뷰 finding을 §9의 disposition과 §2/§5/§6/§8 수정에 연결; 최신 P0/P1 0 |
| SPW-05 — 최종 read-back | PASS | 제목·표·목록·코드 블록·링크를 포함한 전체 설계서를 재확인; `git diff --check` 진단 없음 |

최초 27개 API 설계서 Step 2-R verdict: **PASS (historical)**. 추가 범위를 반영한 28개 API 설계서는 설계 SHA-256 `4244125aedeff7f6d7ffcdba302e39da718dd807037ab2f280a6ea16c3840302`에서 여섯 관점과 통합 검토를 마쳤다. 아래 writer/KO gate 상태를 포함한 최종 문서는 다시 read-back한 뒤 Step 2-R을 닫는다.

## 11. 승인된 추가 범위 A03 재검토 결과

필수 여섯 관점 검토는 위 SHA-256의 28개 API 설계서를 대상으로 했다. 성능과 안정성은 독립 리뷰어가 검토했고, API와 Caller 리뷰어는 플랫폼 사용량 한도로 verdict를 내지 못했다. 이 두 실패 기록은 보존하고, workflow의 inline fallback 규칙에 따라 main session이 검토했다. 추가 Architecture 확인도 수행했으며, inline 결과는 독립 리뷰어의 검토로 표시하지 않는다.

| 검토 영역 | 결과 | 근거와 disposition |
|---|---|---|
| Performance | PASS; P0/P1/P2/P3 = 0 | 독립 리뷰어가 정확한 설계 SHA를 확인했다. 각 wrapper는 Spring API에 직접 위임하며 별도 hot path나 평가를 추가하지 않는다. |
| Stability | PASS; P0/P1/P2/P3 = 0 | 독립 리뷰어가 정확한 설계 SHA를 확인했다. 최초 draft의 resolver nullable/예외 계약 P2를 §2.3, §8, 계획 Task 3에 반영했다. |
| Security | PASS; P0/P1/P2/P3 = 0 | Inline fallback review. factory/configuration class의 신뢰 경계와 SpEL `EvaluationContext` 정책을 유지하고 검증·격리·sandbox·예외 억제를 추가하지 않는다. |
| Operator/Ops | PASS; P0/P1/P2/P3 = 0 | Inline fallback review. 소비자 artifact, README, 되돌리기 방법을 기록했고 배포·release·PR 작업을 범위 밖으로 둔다. |
| Developer/API | PASS; P0/P1/P2/P3 = 0 | Inline fallback review. Boot 4.1.1 및 Spring Framework 7.0.9 overload, nullability, consumer artifact와 28개 signature가 일치한다. 플랫폼 사용량 한도로 실패한 native lane은 별도 fallback lane으로 해결했다. |
| User/Caller | PASS; P0/P1/P2/P3 = 0 | Inline fallback review. 전체 import, nullable named argument, concrete runner 반환형과 supplier 시점, `autoConfigurationsOf<T>()` 단일 타입/기존 vararg 경계를 확인했다. 실패한 native lane은 별도 fallback lane으로 해결했다. |
| Architecture (추가 확인) | PASS; P0/P1/P2/P3 = 0 | Inline review. 기존 core 모듈의 얇은 wrapper로 한정하고 새 dependency나 공통 변환 계층을 추가하지 않는다. |
| Main-session integration | PASS; P0/P1/P2/P3 = 0 | 발견 사항과 출처를 대조했다. FailureHandler 부분 결과와 resolver 예외·nullable 반환 계약은 수용 기준과 테스트 계획에 반영했고 미결 P2/P3는 없다. |
| SPW-01 — 독자·목적·근거 | PASS | 구현자와 모듈 사용자를 위한 설계서다. 저장소 사용 근거와 공식 Spring API 문서를 출처에 연결하고, 버전·호출 수·검토 범위를 구분했다. |
| SPW-02 — 설계서 구조 | PASS | 목표, API 계약, 제외 범위, 대안, 신뢰 경계, 실패 모드, 호환성, acceptance criteria, rollback과 DoD를 포함한다. |
| SPW-03 — 한국어 기술 문체 | PASS | 호출부·반환·위임·평가 시점 용어를 문맥에 맞게 다듬고, 모호한 실패 표현을 구체적인 handler 동작으로 고쳤다. |
| SPW-04 — 기술 근거와 추적성 | PASS | 28개 API와 Boot 4.1.1/Spring Framework 7.0.9 계약을 공식 문서 및 source 근거에 대조했다. A03 finding은 §9와 acceptance criteria에 연결했다. |
| SPW-05 — 최종 read-back | PASS | 최종 rendered Markdown의 제목, 표, 목록, 코드 블록, 링크를 읽었다. `glow -w 150` 렌더링, trailing-whitespace 검사, terminology audit에서 오류가 없었다. |
| KO-01 — 사실과 식별자 보존 | PASS | API 수, 대상 버전, 호출 근거, review SHA와 판정을 대조했으며 기술 사실·식별자·불확실성을 변경하지 않았다. |
| KO-02 — 근거 없는 강조 제거 | PASS | 중요성·효율성 같은 일반 강조 대신 위임 동작, nullable 계약, 호출 근거를 썼다. |
| KO-03 — 번역투와 형식적 문장 제거 | PASS | `호출부`, `반환`, `위임`처럼 구체적인 표현을 사용했고, 모호한 실패 표현을 바로잡았다. |
| KO-04 — 문체와 용어 확인 | PASS | 독자 대상 설계서 문체를 유지하고, API 이름·코드 토큰 및 review 관점 이름을 그대로 보존했다. |
| KO-05 — 유머와 저자 음성 경계 | PASS | 농담이나 새 비유를 넣지 않고 기술 동작을 직접 서술했다. |
| KO-06 — 독자에게 보이는 모든 면 확인 | PASS | 본문, 메타데이터, 표, 링크, 코드 예제를 확인했다. 단일 언어 설계서이며 그림과 locale 짝 문서는 범위에 없다. |
| KO-07 — 문맥 용어 감사 | PASS | `audit-korean-terms.mjs --json` 결과는 `files: 1`, `findings: []`다. |

## 출처

- [Spring Boot Binder](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/context/properties/bind/Binder.html)
- [Spring Boot BootstrapContext](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/bootstrap/BootstrapContext.html)
- [Spring Boot AbstractApplicationContextRunner](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/test/context/runner/AbstractApplicationContextRunner.html)
- [Spring Boot AutoConfigurations](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/autoconfigure/AutoConfigurations.html)
- [Spring Framework SpringFactoriesLoader](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/io/support/SpringFactoriesLoader.html)
- [Spring Framework 7.0.9 SpringFactoriesLoader.ArgumentResolver](https://docs.spring.io/spring-framework/docs/7.0.9/javadoc-api/org/springframework/core/io/support/SpringFactoriesLoader.ArgumentResolver.html)
- [Spring Framework 7.0.9 SpringFactoriesLoader source](https://github.com/spring-projects/spring-framework/blob/v7.0.9/spring-core/src/main/java/org/springframework/core/io/support/SpringFactoriesLoader.java)
- [Spring Framework AnnotationConfigRegistry](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/context/annotation/AnnotationConfigRegistry.html)
- [Spring Framework AnnotatedElementUtils](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/annotation/AnnotatedElementUtils.html)
- [Spring Framework TypeDescriptor](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/convert/TypeDescriptor.html)
- [Spring Framework AnnotationMetadata](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/type/AnnotationMetadata.html)
- [Spring Framework Expression](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/expression/Expression.html)
- [Spring PropertyResolver Kotlin API](https://docs.spring.io/spring-framework/docs/current/kdoc-api/spring-core/org.springframework.core.env/index.html)
