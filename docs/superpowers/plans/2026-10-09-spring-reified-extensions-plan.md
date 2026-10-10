# Spring reified 확장 구현 계획

> **실행 방식:** 사용자가 이 계획과 `$executing-plans` inline 실행을 승인했습니다. 아래 순서와 파일 범위, 검증 명령을 그대로 적용합니다.

**목표:** 승인된 Spring Boot·Spring Framework용 Kotlin reified 확장 함수 28개를 테스트와 영문·한국어 README 예제와 함께 bluetape4k-spring-boot-core에 추가합니다.

**구현 구조:** 패키지별 `inline` 확장 함수가 `T::class.java`를 지정한 Spring 오버로드에 직접 전달해 null 허용 계약, 예외, 생명주기, 검색 의미와 runner의 구체 반환형을 보존합니다. API 영역별 집중 테스트 클래스를 추가하고 기존 애너테이션 테스트를 수정합니다. Gradle 의존성은 바꾸지 않고 소비자가 제공해야 하는 Spring artifact를 문서화합니다.

**기술 구성:** Kotlin 2.4, Spring Boot 4.1.1, Spring Framework 7.0.9, JUnit 5, Gradle 9.7.0, Kover, Detekt.

---

## 권한, 기준선, 중단 조건

- 승인된 설계: docs/superpowers/specs/2026-10-09-spring-reified-extensions-design.md.
- 저장소: `bluetape4k-projects`.
- 작업 트리: `.worktrees/feat-spring-reified-extensions`.
- 브랜치: `feat/spring-reified-extensions`.
- 기준 commit: `5fc36ff930d624674d852a364f7411c26522a56a`.
- 승인: 사용자는 최초 설계와 계획, `$executing-plans` inline 실행을 승인했습니다. 2026-10-10에는 호출부 검토에서 발견한 `AutoConfigurations.of` 사용을 reified 확장 함수 범위에 추가하는 제안을 명시적으로 승인했습니다. 이 추가는 아래 파일·테스트·README 변경에 반영하며 소스 구현은 A-03/A-04 최신 검토 게이트 뒤에 시작합니다.
- GitHub 이슈: [#1823](https://github.com/bluetape4k/bluetape4k-projects/issues/1823) — 현재 GitHub에서 확인한 값은 상태 `OPEN`, label `enhancement`, milestone `2.1.0` (#18), 담당자 `debop`입니다. PR 등 원격 전달 단계에 들어가기 전에 다시 확인합니다.
- 실행을 시작할 때 `git status --short --branch`와 `git rev-parse HEAD`를 기록합니다. 이 작업 트리에서는 승인된 설계서와 계획서만 미추적 파일로 존재해야 합니다. 그 밖의 변경이나 미추적 경로가 있으면 중단하고 보존합니다. 해당 경로에 `git clean`, `reset`, `stash`, `checkout`을 실행하거나 덮어쓰지 않습니다.
- AWS 저장소, 호출부, 버전 catalog, 의존성 선언, CI, `CHANGELOG`나 release metadata, 다른 작업 트리는 수정하지 않습니다. PR, merge, tag, workflow dispatch, publication, release 권한은 포함되지 않습니다.
- 계획을 승인받은 뒤 승인된 설계서와 계획서를 Lore 형식의 한국어 커밋 하나로 기록합니다. 구현 작업도 각각 별도의 Lore 형식 한국어 커밋으로 유지합니다. feature를 되돌릴 때 승인된 설계서·계획서 커밋을 amend하거나 revert하지 않습니다.
- 구현에 앞서 `$test-driven-development`, `$bluetape-kotlin-patterns`, `$ecc-kotlin-testing`을 단계 4에 적용할 지침으로 다시 읽습니다. 이번 실행은 승인된 `$executing-plans` inline 실행 경로를 따릅니다. Type A의 독립 검토는 필요한 단계마다 여섯 관점으로 진행합니다. 해당 skill을 읽기 전에는 구현하지 않습니다.

## 파일 구성

다음 Kotlin 소스 파일을 만듭니다.

- spring-boot/core/src/main/kotlin/io/bluetape4k/spring/boot/BinderExtensions.kt — Binder의 `bind`와 `bindOrCreate` 확장 함수.
- spring-boot/core/src/main/kotlin/io/bluetape4k/spring/boot/BootstrapContextExtensions.kt — BootstrapContext 조회 확장 함수.
- spring-boot/core/src/main/kotlin/io/bluetape4k/spring/core/io/support/SpringFactoriesLoaderExtensions.kt — SpringFactoriesLoader factory 로딩 확장 함수.
- spring-boot/core/src/main/kotlin/io/bluetape4k/spring/context/annotation/AnnotationConfigRegistryExtensions.kt — registry에 class를 등록하는 확장 함수.
- spring-boot/core/src/main/kotlin/io/bluetape4k/spring/core/TypeDescriptorExtensions.kt — 원시 클래스 기반 TypeDescriptor 보조 함수.
- spring-boot/core/src/main/kotlin/io/bluetape4k/spring/core/AnnotationMetadataExtensions.kt — 애너테이션 메타데이터 보조 함수.
- spring-boot/core/src/main/kotlin/io/bluetape4k/spring/expression/ExpressionExtensions.kt — SpEL 결과를 지정한 타입으로 받는 네 확장 함수.
- spring-boot/core/src/main/kotlin/io/bluetape4k/spring/tests/ApplicationContextRunnerExtensions.kt — 구체 runner의 설정 및 bean 확장 함수.
- spring-boot/core/src/main/kotlin/io/bluetape4k/spring/tests/AutoConfigurationsExtensions.kt — auto-configuration class 하나를 받는 reified 보조 함수.

다음 Kotlin 파일을 수정합니다.

- spring-boot/core/src/main/kotlin/io/bluetape4k/spring/beans/AnnotationExtensions.kt — 반복 애너테이션 get 의미를 추가하고 `findAllMergedAnnotations`를 유지합니다.
- spring-boot/core/src/test/kotlin/io/bluetape4k/spring/beans/AnnotationExtensionsTest.kt — get 의미와 기존 find 의미의 차이를 검증합니다.

다음 테스트 파일을 만듭니다.

- spring-boot/core/src/test/kotlin/io/bluetape4k/spring/boot/BinderExtensionsTest.kt.
- spring-boot/core/src/test/kotlin/io/bluetape4k/spring/boot/BootstrapContextExtensionsTest.kt.
- spring-boot/core/src/test/kotlin/io/bluetape4k/spring/core/io/support/SpringFactoriesLoaderExtensionsTest.kt.
- spring-boot/core/src/test/kotlin/io/bluetape4k/spring/context/annotation/AnnotationConfigRegistryExtensionsTest.kt.
- spring-boot/core/src/test/kotlin/io/bluetape4k/spring/core/TypeDescriptorExtensionsTest.kt.
- spring-boot/core/src/test/kotlin/io/bluetape4k/spring/core/AnnotationMetadataExtensionsTest.kt.
- spring-boot/core/src/test/kotlin/io/bluetape4k/spring/expression/ExpressionExtensionsTest.kt.
- spring-boot/core/src/test/kotlin/io/bluetape4k/spring/tests/ApplicationContextRunnerExtensionsTest.kt.

다음 테스트 리소스를 만듭니다. 현재 이 파일은 없습니다.

- spring-boot/core/src/test/resources/META-INF/bluetape4k-spring-reified-extension-test.factories — `SpringFactoriesLoaderExtensionsTest`에서만 사용하는 전용 factory fixture.

다음 사용자 문서를 수정합니다.

- spring-boot/core/README.md — 영어 예제와 dependency 표.
- spring-boot/core/README.ko.md — 이에 대응하는 한국어 예제와 dependency 표.

다음 workflow 증거 문서를 만듭니다.

- docs/superpowers/reviews/2026-10-10-spring-reified-extensions-final-review.md — A-08의 여섯 관점 구현 검토와 통합 결과.
- docs/lessons/2026-10-10-spring-reified-workflow.md — A-09의 재발 방지 교훈.

Gradle이나 CI 파일은 변경하지 않습니다. 새 테스트 클래스에 사용할 정확한 Gradle 필터는 다음과 같습니다.

1. io.bluetape4k.spring.boot.BinderExtensionsTest
2. io.bluetape4k.spring.boot.BootstrapContextExtensionsTest
3. io.bluetape4k.spring.core.io.support.SpringFactoriesLoaderExtensionsTest
4. io.bluetape4k.spring.context.annotation.AnnotationConfigRegistryExtensionsTest
5. io.bluetape4k.spring.core.TypeDescriptorExtensionsTest
6. io.bluetape4k.spring.core.AnnotationMetadataExtensionsTest
7. io.bluetape4k.spring.expression.ExpressionExtensionsTest
8. io.bluetape4k.spring.tests.ApplicationContextRunnerExtensionsTest
9. io.bluetape4k.spring.beans.AnnotationExtensionsTest

이 공개 API 기능은 아키텍처 다이어그램, `CHANGELOG.md`, `AGENTS.md`, release note를 변경하지 않습니다. 단계 9에서 변경 범위를 확인하고 적용하지 않는 이유와 함께 `N/A`로 기록합니다.

## 공개 API 목록

구현은 다음 시그니처와 Spring 오버로드를 그대로 따라야 합니다. 각 확장 함수는 승인 설계에서 정한 패키지에 둡니다. 모든 `Class` 토큰 타입은 `T : Any`로 제한하고 애너테이션 타입에는 `A : Annotation`을 사용합니다.

~~~kotlin
// io.bluetape4k.spring.boot
inline fun <reified T : Any> Binder.bind(name: String): BindResult<T>
inline fun <reified T : Any> Binder.bind(name: String, bindHandler: BindHandler?): BindResult<T>
inline fun <reified T : Any> Binder.bindOrCreate(name: String): T
inline fun <reified T : Any> Binder.bindOrCreate(name: String, bindHandler: BindHandler): T

inline fun <reified T : Any> BootstrapContext.get(): T?
inline fun <reified T : Any> BootstrapContext.getOrElse(defaultValue: T?): T?
inline fun <reified T : Any> BootstrapContext.getOrElseSupply(other: Supplier<T?>): T?
inline fun <reified T : Any> BootstrapContext.getOrElseThrow(exceptionSupplier: Supplier<out Throwable>): T?
inline fun <reified T : Any> BootstrapContext.isRegistered(): Boolean

// io.bluetape4k.spring.core.io.support
inline fun <reified T : Any> SpringFactoriesLoader.load(
    argumentResolver: SpringFactoriesLoader.ArgumentResolver? = null,
    failureHandler: SpringFactoriesLoader.FailureHandler? = null,
): List<T>

// io.bluetape4k.spring.context.annotation
inline fun <reified T : Any> AnnotationConfigRegistry.register()

// io.bluetape4k.spring.beans
inline fun <reified A : Annotation> AnnotatedElement.getMergedRepeatableAnnotations(): Set<A>

// io.bluetape4k.spring.core
inline fun <reified T : Any> typeDescriptorOf(): TypeDescriptor
inline fun <reified T : Any> annotationMetadataOf(): AnnotationMetadata

// io.bluetape4k.spring.expression
inline fun <reified T : Any> Expression.getValueAs(): T?
inline fun <reified T : Any> Expression.getValueAs(rootObject: Any?): T?
inline fun <reified T : Any> Expression.getValueAs(context: EvaluationContext): T?
inline fun <reified T : Any> Expression.getValueAs(context: EvaluationContext, rootObject: Any?): T?

// io.bluetape4k.spring.tests
inline fun <reified T : Any> autoConfigurationsOf(): AutoConfigurations

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
inline fun <reified T : Any> WebApplicationContextRunner.withBean(
    vararg customizers: BeanDefinitionCustomizer,
    crossinline supplier: () -> T,
): WebApplicationContextRunner
inline fun <reified T : Any> WebApplicationContextRunner.withBean(
    name: String?,
    vararg customizers: BeanDefinitionCustomizer,
    crossinline supplier: () -> T,
): WebApplicationContextRunner
inline fun <reified T : Any> ReactiveWebApplicationContextRunner.withBean(
    vararg customizers: BeanDefinitionCustomizer,
    crossinline supplier: () -> T,
): ReactiveWebApplicationContextRunner
inline fun <reified T : Any> ReactiveWebApplicationContextRunner.withBean(
    name: String?,
    vararg customizers: BeanDefinitionCustomizer,
    crossinline supplier: () -> T,
): ReactiveWebApplicationContextRunner
~~~

Binder의 handler 오버로드는 `bind(name, Bindable.of(T::class.java), bindHandler)`와 `bindOrCreate(name, Bindable.of(T::class.java), bindHandler)`에 위임합니다. Spring Boot 4.1.1에는 `Class<T>`와 handler를 함께 받는 오버로드가 없습니다. 따라서 null을 허용하는 Binder handler 인자는 문자열 오버로드가 아니라 `Bindable` 오버로드에 전달합니다.

SpringFactoriesLoader 확장은 세 인자를 받는 `load(T::class.java, argumentResolver, failureHandler)`에 위임합니다. `ArgumentResolver`에는 factory 생성자가 요구하는 인자 타입을 전달하고, resolver의 반환값은 생성자 인자로 사용합니다. resolver가 예외를 던지면 Spring은 factory 인스턴스화 실패를 `FailureHandler`에 전달합니다. 이 handler에는 factory class, 구현체 이름, 원인을 포함한 `Throwable`이 전달되지만 resolver에는 factory 이름이 전달되지 않습니다. 기본 handler는 원인을 보존한 `IllegalArgumentException`을 던집니다. 호출자가 전달한 handler가 정상 반환하면 실패 factory를 건너뛰고 부분 `List<T>`를 반환할 수 있습니다. 모든 factory가 필요하면 기본 handler를 사용하거나 예외를 던지는 handler를 지정합니다. receiver가 가진 resource location과 classloader는 바꾸지 않습니다.

runner 확장은 null을 허용하는 이름을 Spring의 이름 지정 `withBean` 오버로드에 전달하고 `BeanDefinitionCustomizer` vararg를 그대로 전달합니다. 공급자는 Java `Supplier`로 감싸며 확장 함수를 구성하는 시점에는 호출하지 않습니다. Spring이 bean을 생성할 때 공급자를 실행합니다.

## 실행 작업

### 작업 0: 승인된 계획 기록

**Action:** A-04 검토와 문서 작성자 게이트를 마친 뒤 이슈 #1823을 갱신하고 승인된 설계서·계획서를 한 커밋으로 기록합니다.
**Expected DoD:** 최신 28개 API 범위를 설명하는 live issue와 검토한 정확한 hash의 문서 두 개가 커밋되고, 커밋 내용과 기준 branch가 다시 확인됩니다.

**파일:**
- 이미 승인된 설계서와 이 구현 계획만 커밋합니다.

- [ ] **단계 1: worktree 허용 경로 확인**

실행: `git status --short --branch` 및 `git rev-parse HEAD`.

예상 결과: branch가 `5fc36ff930d624674d852a364f7411c26522a56a`에서 갈라진 `feat/spring-reified-extensions`이고 승인된 설계서·계획서 두 개만 untracked 상태입니다. 예상 밖 변경 경로가 있으면 중단하고 보존합니다.

- [ ] **단계 2: 승인 전 checklist 상태 고정**

최신 설계서와 계획을 기준으로 Type A 및 common-gates checklist를 다시 작성하고 A-03/A-04, SPW-01–05, KO-01–07, CG-01–CG-10의 Required/Conditional/N/A 상태와 근거를 기록합니다. 문서 커밋 전에는 CG-01–CG-04, A-03 여섯 관점 통합 검토, 작성자·KO gate와 A-04 여섯 관점 통합 검토, 작성자·KO gate를 모두 통과해야 합니다. 승인된 설계·계획 커밋과 정확한 문서 내용을 다시 확인한 뒤에만 A-04를 PASS로 닫습니다. 구현에 종속된 CG-05–CG-10은 선행 근거를 요구하지 않으며 해당 작업 전까지 PENDING으로 둡니다. 단독 개발자 범위에서는 CG-14의 대면 검토 하위 gate만 `N/A (single-developer lane)`로 기록할 수 있습니다. exact-head CI, review/thread 재확인, 독립 최종 코드 검토, P0/P1 처리와 적용 가능한 artifact는 각각 근거가 필요하며, 전체를 N/A나 SKIPPED 처리하지 않습니다.

예상 결과: checklist 항목별 상태와 근거가 기록되고 현재 문서 커밋 전 필수 gate는 모두 PASS이며, 구현 단계 항목은 PENDING으로 남습니다.

- [ ] **단계 3: 이슈 #1823 범위 갱신**

A-03/A-04의 여섯 관점 검토와 통합 판단, 작성자·KO gate를 통과한 뒤 GitHub issue #1823의 한국어 본문을 28개 helper 범위로 갱신합니다. A-04는 설계·계획 문서의 커밋과 내용을 다시 확인했다는 근거가 생길 때까지 PENDING으로 둡니다. `autoConfigurationsOf<T>()`, 단일 타입과 Spring vararg의 경계, 소비자가 제공할 `spring-boot-autoconfigure` artifact, 테스트·README 완료 기준을 반영하고 PR을 만들지 않는 범위를 유지합니다. `gh issue view 1823 --json number,title,state,body,milestone,assignees,url`로 바뀐 내용을 다시 읽습니다.

예상 결과: GitHub 이슈가 OPEN, milestone 2.1.0, 담당자 `debop`을 유지하고 28개 API와 계획의 완료 기준을 정확히 설명합니다. 수정이 거부되거나 다시 읽은 내용이 다르면 이슈 갱신을 PENDING으로 두고 문서 커밋 전에 원인을 보고합니다.

- [ ] **단계 4: 승인된 설계서·계획서 커밋**

`docs/superpowers/specs/2026-10-09-spring-reified-extensions-design.md`와 `docs/superpowers/plans/2026-10-09-spring-reified-extensions-plan.md` 두 문서만 stage합니다. Constraint, Rejected, Confidence, Scope-risk, Directive, Tested, Not-tested trailer를 포함한 한국어 의도 우선 Lore 메시지를 사용합니다. 여섯 관점 통합 검토, 수정본의 작성자·KO gate, 이슈 본문 재확인을 통과한 문서만 커밋합니다. 해당 커밋의 경로와 내용을 다시 읽은 뒤 A-04를 PASS로 기록합니다. 아직 구현 코드나 모듈 테스트를 시작하지 않았고 PR·release 작업은 승인 범위 밖이라는 점도 기록합니다.

예상 결과: 커밋 하나에 정확히 두 문서만 들어가고 `git show --stat --oneline HEAD`에 그 경로만 표시됩니다. 커밋 후 A-04를 PASS로 기록할 때 대상 hash와 두 문서 내용이 검토본과 일치해야 합니다.

- [ ] **단계 5: 구현 기준선 확인**

실행: git status --short --branch 및 git rev-parse HEAD.

예상 결과: 새 문서 커밋을 가리키는 feature branch의 worktree가 clean 상태입니다.

**Step DoD: PENDING** — A-04 검토, issue 갱신, 문서 커밋 및 clean baseline 확인 전입니다.

### 작업 1: Binder 확장 함수

**Action:** 네 Binder reified 확장 함수를 실패 테스트부터 구현해 Spring의 binding 및 handler 계약을 보존합니다.
**Expected DoD:** `BinderExtensionsTest`의 focused filter가 통과하고 Binder source·test만 Lore 형식의 한국어 커밋으로 기록됩니다.

**파일:**
- 생성: spring-boot/core/src/test/kotlin/io/bluetape4k/spring/boot/BinderExtensionsTest.kt.
- 생성: spring-boot/core/src/main/kotlin/io/bluetape4k/spring/boot/BinderExtensions.kt.

- [ ] **단계 1: 실패하는 Binder test 추가**

기본 포트가 8080인 변경 가능한 no-argument 테스트 bean을 만듭니다. `server.port=9090`을 가진 `MapConfigurationPropertySource`로 `Binder`를 구성합니다. typed bind 결과와 `.get()`, 바인딩되지 않은 prefix의 `isBound=false`, prefix가 있을 때 9090을 반환하고 없을 때 8080을 반환하는 `bindOrCreate`, 두 handler 오버로드에서 호출 횟수를 세는 `AbstractBindHandler.onSuccess` 콜백을 검증합니다. `bindHandler = null`을 명시한 호출도 컴파일 확인합니다.

null call은 정확히 다음과 같아야 합니다.

~~~kotlin
val result = binder.bind<ServerProperties>("server", bindHandler = null)
(result.isBound).shouldBeTrue()
~~~

handler 테스트에서는 `AbstractBindHandler`를 상속해 `onSuccess(ConfigurationPropertyName, Bindable<*>, BindContext, Any): Any?`를 재정의합니다. 호출 횟수를 증가시키고 결과를 반환한 뒤, 값이 바인딩되고 콜백이 한 번 이상 실행됐는지 확인합니다. Spring은 aggregate binding과 property binding 모두에서 handler를 호출할 수 있으므로 정확한 횟수는 고정하지 않습니다. `bind`와 `bindOrCreate` 검증 사이에서 카운터를 초기화합니다.

다음 완전한 test member를 BinderExtensionsTest.kt에 추가합니다.

~~~kotlin
package io.bluetape4k.spring.boot

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource
import org.springframework.boot.context.properties.bind.AbstractBindHandler
import org.springframework.boot.context.properties.bind.BindContext
import org.springframework.boot.context.properties.bind.BindHandler
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.context.properties.source.ConfigurationPropertyName
import java.util.concurrent.atomic.AtomicInteger

class BinderExtensionsTest {
    class ServerProperties {
        var port: Int = 8080
    }

    private fun binder() = Binder(listOf(MapConfigurationPropertySource(mapOf("server.port" to "9090"))))

    private fun countingHandler(calls: AtomicInteger): BindHandler = object : AbstractBindHandler() {
        override fun onSuccess(
            name: ConfigurationPropertyName,
            target: Bindable<*>,
            context: BindContext,
            result: Any,
        ): Any? {
            calls.incrementAndGet()
            return result
        }
    }

    @Test
    fun bindsAndReportsMissingPrefix() {
        binder().bind<ServerProperties>("server").get().port shouldBeEqualTo 9090
        (binder().bind<ServerProperties>("missing").isBound).shouldBeFalse()
    }

    @Test
    fun bindsOrCreatesUsingMutableBeanDefaults() {
        binder().bindOrCreate<ServerProperties>("server").port shouldBeEqualTo 9090
        binder().bindOrCreate<ServerProperties>("missing").port shouldBeEqualTo 8080
    }

    @Test
    fun handlerOverloadsInvokeTheProvidedHandler() {
        val calls = AtomicInteger()
        val handler = countingHandler(calls)

        binder().bind<ServerProperties>("server", handler).get().port shouldBeEqualTo 9090
        (calls.get() > 0).shouldBeTrue()
        calls.set(0)
        binder().bindOrCreate<ServerProperties>("server", handler).port shouldBeEqualTo 9090
        (calls.get() > 0).shouldBeTrue()
    }

    @Test
    fun nullableBindHandlerIsPassedByNamedArgument() {
        val result = binder().bind<ServerProperties>("server", bindHandler = null)
        (result.isBound).shouldBeTrue()
        result.get().port shouldBeEqualTo 9090
    }
}
~~~

- [ ] **단계 2: 구현 전에 Binder filter 실행**

실행: ./gradlew :bluetape4k-spring-boot-core:test --tests 'io.bluetape4k.spring.boot.BinderExtensionsTest'

예상 결과: 네 reified 확장 함수가 아직 없어서 컴파일에 실패합니다. 빌드 또는 환경의 다른 실패를 예상한 RED 결과로 처리하지 말고 원인을 조사합니다.

- [ ] **단계 3: 네 개의 direct Binder wrapper 추가**

BinderExtensions.kt에 다음 코드를 구현합니다.

~~~kotlin
package io.bluetape4k.spring.boot

import org.springframework.boot.context.properties.bind.BindHandler
import org.springframework.boot.context.properties.bind.BindResult
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder

inline fun <reified T : Any> Binder.bind(name: String): BindResult<T> =
    bind(name, T::class.java)

inline fun <reified T : Any> Binder.bind(name: String, bindHandler: BindHandler?): BindResult<T> =
    bind(name, Bindable.of(T::class.java), bindHandler)

inline fun <reified T : Any> Binder.bindOrCreate(name: String): T =
    bindOrCreate(name, T::class.java)

inline fun <reified T : Any> Binder.bindOrCreate(name: String, bindHandler: BindHandler): T =
    bindOrCreate(name, Bindable.of(T::class.java), bindHandler)
~~~

Spring 오버로드와 `bindHandler = null` 이름 지정 인자 호출을 설명하는 한국어 KDoc을 추가합니다.

- [ ] **단계 4: Binder filter 실행**

실행: ./gradlew :bluetape4k-spring-boot-core:test --tests 'io.bluetape4k.spring.boot.BinderExtensionsTest'

예상 결과: BUILD SUCCESSFUL; 값의 바인딩 여부, 기본 객체 생성, handler 콜백 횟수와 null 허용 handler 호출 검증이 모두 통과합니다.

- [ ] **단계 5: Binder commit**

Binder 소스와 테스트만 Lore 형식의 한국어 커밋으로 기록합니다. Tested trailer에는 Binder 필터 명령과 통과 결과를 적고, Not-tested에는 모듈 전체 검증이 남아 있음을 적습니다.

**Step DoD: PENDING** — Binder의 RED/GREEN 결과와 해당 commit을 확인하기 전입니다.

### 작업 2: BootstrapContext 확장 함수

**Action:** 다섯 BootstrapContext reified 확장 함수를 테스트 우선으로 구현하고 Spring의 null 허용, 기본값 평가, 예외 계약을 보존합니다.
**Expected DoD:** `BootstrapContextExtensionsTest`의 focused filter가 통과하고 해당 source·test commit에 정확한 검증 명령이 기록됩니다.

**파일:**
- 생성: spring-boot/core/src/test/kotlin/io/bluetape4k/spring/boot/BootstrapContextExtensionsTest.kt.
- 생성: spring-boot/core/src/main/kotlin/io/bluetape4k/spring/boot/BootstrapContextExtensions.kt.

- [ ] **단계 1: 실패하는 BootstrapContext test 추가**

`DefaultBootstrapContext`를 사용하고 `ConfigurableBootstrapContext.register(SharedResource::class.java, InstanceSupplier { SharedResource() })`로 `SharedResource`를 등록합니다. 이어지는 테스트 클래스 전체에서 등록·미등록 조회, null 결과, 즉시·지연 기본값, 공급자 호출 횟수와 미등록 타입 예외를 확인합니다. `shared`, `fallback`, `lazyFallback`, `missingException` 필드는 `registeredContext()`가 만든 context에서 사용합니다. 공급자와 호출 횟수 카운터는 해당 테스트 메서드 안에 둡니다. 참조 타입인 `NullableResource`에는 null을 반환하는 `InstanceSupplier`를 등록하고 typed 조회와 `getOrElse`가 fallback으로 대체하지 않고 null을 반환하는지 확인합니다. `Supplier<T?>`와 `Supplier<out Throwable>` 시그니처도 컴파일 확인합니다.

다음 테스트 클래스 전체를 `BootstrapContextExtensionsTest` 본문으로 사용합니다.

~~~kotlin
package io.bluetape4k.spring.boot

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.bootstrap.BootstrapRegistry
import org.springframework.boot.bootstrap.DefaultBootstrapContext
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Supplier

class BootstrapContextExtensionsTest {
    class SharedResource
    class MissingResource
    class NullableResource
    class MissingException : RuntimeException()

    private val shared = SharedResource()
    private val fallback = MissingResource()
    private val lazyFallback = MissingResource()
    private val missingException = MissingException()

    private fun registeredContext(): DefaultBootstrapContext = DefaultBootstrapContext().apply {
        register(SharedResource::class.java, BootstrapRegistry.InstanceSupplier { shared })
        register(NullableResource::class.java, BootstrapRegistry.InstanceSupplier { null })
    }

    @Test
    fun registeredAndMissingLookupsPreserveSpringResults() {
        val context = registeredContext()
        context.get<SharedResource>() shouldBeSameInstanceAs shared
        (context.isRegistered<SharedResource>()).shouldBeTrue()
        (context.isRegistered<MissingResource>()).shouldBeFalse()
        assertFailsWith<IllegalStateException> { context.get<MissingResource>() }
        context.get<NullableResource>().shouldBeNull()
        context.getOrElse<NullableResource>(NullableResource()).shouldBeNull()
    }

    @Test
    fun defaultsAndSuppliersAreUsedOnlyForMissingTypes() {
        val context = registeredContext()
        val supplierCalls = AtomicInteger()
        val registeredSupplierCalls = AtomicInteger()
        val supplier = Supplier<MissingResource?> {
            supplierCalls.incrementAndGet()
            lazyFallback
        }
        val registeredSupplier = Supplier<SharedResource?> {
            registeredSupplierCalls.incrementAndGet()
            null
        }

        context.getOrElse<SharedResource>(null) shouldBeSameInstanceAs shared
        context.getOrElseSupply<SharedResource>(registeredSupplier) shouldBeSameInstanceAs shared
        registeredSupplierCalls.get() shouldBeEqualTo 0
        context.getOrElse<MissingResource>(fallback) shouldBeSameInstanceAs fallback
        context.getOrElseSupply<MissingResource>(supplier) shouldBeSameInstanceAs lazyFallback
        supplierCalls.get() shouldBeEqualTo 1
    }

    @Test
    fun exceptionSupplierRunsOnlyForMissingTypes() {
        val context = registeredContext()
        val supplierCalls = AtomicInteger()
        val supplier = Supplier<Throwable> {
            supplierCalls.incrementAndGet()
            missingException
        }

        context.getOrElseThrow<SharedResource>(supplier) shouldBeSameInstanceAs shared
        supplierCalls.get() shouldBeEqualTo 0
        val actual = assertFailsWith<MissingException> {
            context.getOrElseThrow<MissingResource>(supplier)
        }
        actual shouldBeSameInstanceAs missingException
        supplierCalls.get() shouldBeEqualTo 1
    }
}
~~~

- [ ] **단계 2: 구현 전에 BootstrapContext filter 실행**

실행: ./gradlew :bluetape4k-spring-boot-core:test --tests 'io.bluetape4k.spring.boot.BootstrapContextExtensionsTest'

예상 결과: 확장 함수가 아직 정의되지 않아 컴파일에 실패합니다.

- [ ] **단계 3: BootstrapContext wrapper 구현**

BootstrapContextExtensions.kt에 다음 코드를 구현합니다. 각 함수의 한국어 KDoc에는 Spring 등록 계약, null 허용성, 기본값 평가 시점과 예외 동작을 설명합니다.

~~~kotlin
package io.bluetape4k.spring.boot

import org.springframework.boot.bootstrap.BootstrapContext
import java.util.function.Supplier

inline fun <reified T : Any> BootstrapContext.get(): T? =
    get(T::class.java)

inline fun <reified T : Any> BootstrapContext.getOrElse(defaultValue: T?): T? =
    getOrElse(T::class.java, defaultValue)

inline fun <reified T : Any> BootstrapContext.getOrElseSupply(other: Supplier<T?>): T? =
    getOrElseSupply(T::class.java, other)

inline fun <reified T : Any> BootstrapContext.getOrElseThrow(exceptionSupplier: Supplier<out Throwable>): T? =
    getOrElseThrow(T::class.java, exceptionSupplier)

inline fun <reified T : Any> BootstrapContext.isRegistered(): Boolean =
    isRegistered(T::class.java)
~~~

try/catch를 추가하거나 Spring default를 대체하지 않습니다.

- [ ] **단계 4: BootstrapContext filter 실행**

실행: ./gradlew :bluetape4k-spring-boot-core:test --tests 'io.bluetape4k.spring.boot.BootstrapContextExtensionsTest'

예상 결과: BUILD SUCCESSFUL; 등록·미등록 조회, null 처리, 기본값, 지연 평가와 예외 검증이 모두 통과합니다.

- [ ] **단계 5: BootstrapContext commit**

해당 소스와 테스트만 Lore 형식의 한국어 커밋으로 기록하고, 실행한 정확한 테스트 필터를 Tested에 적습니다.

**Step DoD: PENDING** — BootstrapContext의 RED/GREEN 결과와 해당 commit을 확인하기 전입니다.

### 작업 3: SpringFactoriesLoader 확장 함수

**Action:** factory 로더 reified 확장 함수를 고유 테스트 resource와 실패·callback fixture로 검증한 뒤 구현합니다.
**Expected DoD:** `SpringFactoriesLoaderExtensionsTest`가 성공·부분·실패 경로와 callback 계약을 모두 통과하고 source·test·resource만 함께 커밋됩니다.

**파일:**
- 생성: spring-boot/core/src/test/kotlin/io/bluetape4k/spring/core/io/support/SpringFactoriesLoaderExtensionsTest.kt.
- 생성: spring-boot/core/src/test/resources/META-INF/bluetape4k-spring-reified-extension-test.factories.
- 생성: spring-boot/core/src/main/kotlin/io/bluetape4k/spring/core/io/support/SpringFactoriesLoaderExtensions.kt.

- [ ] **단계 1: 격리된 factory fixture와 실패하는 테스트 추가**

테스트 package `io.bluetape4k.spring.core.io.support`에 다음 공개 최상위 fixture 타입을 둡니다. `name` 속성을 가진 `TestFactory`, public no-argument constructor로 `TestFactory`를 구현하는 `DefaultFactory`, `requiredArgument` 속성을 가진 `ResolverFactory`, `RequiredArgument`를 생성자에서 받는 `RequiredArgumentFactory`, marker SPI `FailingFactory`, 생성자에서 `IllegalStateException("factory-construction-failed")`를 던지는 `FailingImplementation`입니다. `RequiredArgument`는 public data class로 둡니다.

fixture의 선언과 완전 수식 이름이 일치하도록 다음 리소스 내용을 사용합니다.

~~~properties
io.bluetape4k.spring.core.io.support.TestFactory=io.bluetape4k.spring.core.io.support.DefaultFactory
io.bluetape4k.spring.core.io.support.ResolverFactory=io.bluetape4k.spring.core.io.support.RequiredArgumentFactory
io.bluetape4k.spring.core.io.support.NullableResolverFactory=io.bluetape4k.spring.core.io.support.NullableArgumentFactory
io.bluetape4k.spring.core.io.support.FailingFactory=io.bluetape4k.spring.core.io.support.FailingImplementation
io.bluetape4k.spring.core.io.support.PartialFactory=io.bluetape4k.spring.core.io.support.AvailablePartialFactory,io.bluetape4k.spring.core.io.support.FailingPartialFactory
~~~

각 loader는 `SpringFactoriesLoader.forResourceLocation(TEST_RESOURCE_LOCATION, javaClass.classLoader)`로 만듭니다. 이 기능 전용 리소스가 테스트 classloader에서 하나만 발견되는지 `Collections.list(javaClass.classLoader.getResources(TEST_RESOURCE_LOCATION)).size == 1`로 확인합니다. `load<TestFactory>()`는 `DefaultFactory`를 반환해야 합니다. `argumentResolver`와 `failureHandler`를 함께 넘기는 `load<ResolverFactory>`에서는 resolver가 `RequiredArgument::class.java`를 정확히 한 번 받고, 해당 값을 가진 factory를 반환하며, 성공 시 failure handler를 호출하지 않는지 검증합니다. `load<NullableResolverFactory>`에서는 resolver의 null 반환값이 null 허용 생성자 인자로 전달되는지 확인합니다. resolver가 예외를 던지는 경우 정상 반환하는 `FailureHandler`가 그 예외를 받고 해당 factory가 결과에서 제외되는지 검증합니다. `load<FailingFactory>(failureHandler = handler)`는 handler에 factory 타입, `FailingImplementation`의 완전 수식 이름, 원래 `IllegalStateException`을 cause에 담은 `Throwable`을 전달한 뒤 빈 목록을 반환해야 합니다. 별도의 `PartialFactory` SPI에는 성공 구현체와 생성 중 실패하는 구현체를 함께 등록합니다. 정상 반환하는 사용자 정의 handler를 지정한 `load<PartialFactory>`에서는 실패 콜백을 확인하면서 성공 구현체만 담은 부분 목록을 반환하는지 검증합니다. handler를 생략한 `load<FailingFactory>()`는 Spring 기본 예외 동작을 유지해 `IllegalArgumentException`을 던지고, 원래 `IllegalStateException("factory-construction-failed")`을 cause에 보존해야 합니다. `load<TestFactory>(argumentResolver = null, failureHandler = null)`과 nullable 전략 하나만 이름 지정 인자로 전달하는 호출도 컴파일하고 실행합니다.

다음 공개 fixture 선언과 테스트 메서드를 `SpringFactoriesLoaderExtensionsTest.kt`에 추가합니다.

~~~kotlin
package io.bluetape4k.spring.core.io.support

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import java.util.Collections
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.core.io.support.SpringFactoriesLoader

interface TestFactory {
    val name: String
}

class DefaultFactory : TestFactory {
    override val name = "default"
}

data class RequiredArgument(val value: String)

interface ResolverFactory {
    val requiredArgument: RequiredArgument
}

class RequiredArgumentFactory(override val requiredArgument: RequiredArgument) : ResolverFactory

interface NullableResolverFactory {
    val value: String?
}

class NullableArgumentFactory(override val value: String?) : NullableResolverFactory

interface FailingFactory

class FailingImplementation : FailingFactory {
    init {
        throw IllegalStateException("factory-construction-failed")
    }
}

interface PartialFactory {
    val name: String
}

class AvailablePartialFactory : PartialFactory {
    override val name = "available"
}

class FailingPartialFactory : PartialFactory {
    init {
        throw IllegalStateException("partial-factory-failed")
    }
}

private const val TEST_RESOURCE_LOCATION = "META-INF/bluetape4k-spring-reified-extension-test.factories"

class SpringFactoriesLoaderExtensionsTest {
    private fun loader() = SpringFactoriesLoader.forResourceLocation(
        TEST_RESOURCE_LOCATION,
        javaClass.classLoader,
    )

    @Test
    fun loadsNoArgumentFactoryAndAcceptsExplicitNullStrategies() {
        Collections.list(javaClass.classLoader.getResources(TEST_RESOURCE_LOCATION)).size shouldBeEqualTo 1
        loader().load<TestFactory>().single().name shouldBeEqualTo "default"
        loader().load<TestFactory>(argumentResolver = null, failureHandler = null).single().name shouldBeEqualTo "default"
        loader().load<TestFactory>(argumentResolver = null).single().name shouldBeEqualTo "default"
        loader().load<TestFactory>(failureHandler = null).single().name shouldBeEqualTo "default"
    }

    @Test
    fun resolvesConstructorArgumentTypes() {
        val resolvedTypes = mutableListOf<Class<*>>()
        val resolver = SpringFactoriesLoader.ArgumentResolver.from { type ->
            resolvedTypes += type
            if (type == RequiredArgument::class.java) RequiredArgument("resolved") else null
        }
        var failureHandlerCalls = 0
        val failureHandler = SpringFactoriesLoader.FailureHandler { _, _, _ -> failureHandlerCalls++ }

        val factory = loader().load<ResolverFactory>(
            argumentResolver = resolver,
            failureHandler = failureHandler,
        ).single()

        resolvedTypes shouldBeEqualTo listOf(RequiredArgument::class.java)
        factory.requiredArgument.value shouldBeEqualTo "resolved"
        failureHandlerCalls shouldBeEqualTo 0
    }

    @Test
    fun passesNullableResolverResultToSpringConstructor() {
        val resolver = SpringFactoriesLoader.ArgumentResolver.from { type ->
            if (type == String::class.java) null else null
        }

        loader().load<NullableResolverFactory>(argumentResolver = resolver).single().value shouldBeEqualTo null
    }

    @Test
    fun resolverFailureIsPassedToFailureHandler() {
        val resolverFailure = IllegalStateException("resolver-failed")
        var receivedFailure: Throwable? = null
        val resolver = SpringFactoriesLoader.ArgumentResolver.from { throw resolverFailure }
        val handler = SpringFactoriesLoader.FailureHandler { _, _, failure -> receivedFailure = failure }

        loader().load<ResolverFactory>(argumentResolver = resolver, failureHandler = handler).isEmpty().shouldBeTrue()
        generateSequence(receivedFailure) { it.cause }.any { it === resolverFailure }.shouldBeTrue()
    }

    @Test
    fun failureHandlerReceivesTheSpringFailureContext() {
        var receivedType: Class<*>? = null
        var receivedName: String? = null
        var receivedFailure: Throwable? = null
        val handler = SpringFactoriesLoader.FailureHandler { type, name, failure ->
            receivedType = type
            receivedName = name
            receivedFailure = failure
        }

        (loader().load<FailingFactory>(failureHandler = handler).isEmpty()).shouldBeTrue()
        receivedType shouldBeEqualTo FailingFactory::class.java
        receivedName shouldBeEqualTo FailingImplementation::class.java.name
        (generateSequence(receivedFailure) { it.cause }.any {
            it is IllegalStateException && it.message == "factory-construction-failed"
        }).shouldBeTrue()
    }

    @Test
    fun nonThrowingFailureHandlerReturnsOnlySuccessfullyLoadedFactories() {
        var receivedType: Class<*>? = null
        var receivedName: String? = null
        var receivedFailure: Throwable? = null
        val handler = SpringFactoriesLoader.FailureHandler { type, name, failure ->
            receivedType = type
            receivedName = name
            receivedFailure = failure
        }

        val factories = loader().load<PartialFactory>(failureHandler = handler)

        factories.map { it.name } shouldBeEqualTo listOf("available")
        receivedType shouldBeEqualTo PartialFactory::class.java
        receivedName shouldBeEqualTo FailingPartialFactory::class.java.name
        (generateSequence(receivedFailure) { it.cause }.any {
            it is IllegalStateException && it.message == "partial-factory-failed"
        }).shouldBeTrue()
    }

    @Test
    fun defaultFailureHandlerThrowsAndPreservesFactoryCause() {
        val failure = assertThrows<IllegalArgumentException> {
            loader().load<FailingFactory>()
        }

        generateSequence(failure) { it.cause }.any {
            it is IllegalStateException && it.message == "factory-construction-failed"
        }.shouldBeTrue()
    }
}
~~~

- [ ] **단계 2: 구현 전에 SpringFactoriesLoader 테스트 필터 실행**

실행: ./gradlew :bluetape4k-spring-boot-core:test --tests 'io.bluetape4k.spring.core.io.support.SpringFactoriesLoaderExtensionsTest'

예상 결과: 확장 함수가 아직 정의되지 않아 컴파일에 실패합니다.

- [ ] **단계 3: loader 확장 함수 추가**

SpringFactoriesLoaderExtensions.kt를 다음과 같이 구현합니다.

~~~kotlin
package io.bluetape4k.spring.core.io.support

import org.springframework.core.io.support.SpringFactoriesLoader

inline fun <reified T : Any> SpringFactoriesLoader.load(
    argumentResolver: SpringFactoriesLoader.ArgumentResolver? = null,
    failureHandler: SpringFactoriesLoader.FailureHandler? = null,
): List<T> = load(T::class.java, argumentResolver, failureHandler)
~~~

resource 위치와 classloader를 보존하고 `ArgumentResolver`와 `FailureHandler`가 호출자가 제공하는 콜백임을 설명하는 한국어 KDoc을 추가합니다. resolver의 null 반환값은 생성자 인자로, resolver 예외는 failure handler로 전달됩니다. 기본 handler는 예외를 던집니다. 사용자 정의 handler가 정상 반환하면 실패한 factory가 결과에서 빠져 부분 목록이 될 수 있다는 점도 적습니다. 모든 factory가 필요하면 기본 handler나 명시적으로 예외를 던지는 handler를 사용하도록 안내합니다. 배포 환경이 classpath를 신뢰한다는 전제와 factory 초기화·콜백의 부수 효과 가능성을 밝히고, 확장 함수가 classpath나 factory를 검증·격리하지 않는다고 명시합니다. 예외를 삼키거나 loader를 새로 만들지 않습니다.

- [ ] **단계 4: SpringFactoriesLoader filter 실행**

실행: ./gradlew :bluetape4k-spring-boot-core:test --tests 'io.bluetape4k.spring.core.io.support.SpringFactoriesLoaderExtensionsTest'

예상 결과: BUILD SUCCESSFUL이며 전용 리소스만 발견되는지, 기본 factory 로딩, 생성자 인자 해석, resolver의 null 반환과 예외 전달, 콜백 인자, 성공·실패 factory가 섞인 경우의 부분 목록, handler 생략 시 `IllegalArgumentException`과 원래 cause 보존, 이름 지정 null 호출 검증이 모두 통과합니다.

- [ ] **단계 5: loader 확장 함수와 fixture 커밋**

loader 소스·테스트·새 리소스만 한국어 Lore 형식으로 커밋하고, 실행한 테스트 필터를 Tested에 적습니다.

**Step DoD: PENDING** — factory callback 경계의 실패·통과 테스트 결과와 해당 커밋을 확인하기 전입니다.

### 작업 4: AnnotationConfigRegistry 및 반복 애너테이션 확장 함수

**Action:** registry 등록 확장 함수와 반복 애너테이션 get 확장 함수를 기존 find 확장 함수와 구분해 테스트 우선으로 추가합니다.
**Expected DoD:** 두 테스트 필터가 Spring 직접 호출 결과와 일치하고 변경한 소스·테스트 네 파일만 한 커밋에 기록됩니다.

**파일:**
- 생성: spring-boot/core/src/test/kotlin/io/bluetape4k/spring/context/annotation/AnnotationConfigRegistryExtensionsTest.kt.
- 생성: spring-boot/core/src/main/kotlin/io/bluetape4k/spring/context/annotation/AnnotationConfigRegistryExtensions.kt.
- 수정: spring-boot/core/src/main/kotlin/io/bluetape4k/spring/beans/AnnotationExtensions.kt.
- 수정: spring-boot/core/src/test/kotlin/io/bluetape4k/spring/beans/AnnotationExtensionsTest.kt.

- [ ] **단계 1: 실패하는 registry·반복 애너테이션 테스트 추가**

`AnnotationConfigRegistryExtensionsTest`에 `SampleBean`을 bean으로 등록하는 `SampleConfiguration`을 정의합니다. `AnnotationConfigApplicationContext`를 `AnnotationConfigRegistry`로 다루고 `register<SampleConfiguration>()`을 호출합니다. context를 refresh한 뒤 `getBean(SampleBean::class.java)`가 성공하는지 확인합니다.

`AnnotationExtensionsTest`에 런타임까지 유지되는 `Marker` 애너테이션을 정의하고 Kotlin `@Repeatable`을 붙입니다. Kotlin 컴파일러가 repeatable 애너테이션 컨테이너를 만들도록 둡니다. 한 메서드에 `Marker` 값 `first`와 `second`를 각각 붙입니다. `getMergedRepeatableAnnotations<Marker>()`가 애너테이션 두 개를 반환하고 `AnnotatedElementUtils.getMergedRepeatableAnnotations(element, Marker::class.java)`와 같은지 확인합니다. 기존 `findAllMergedAnnotations<Marker>()`도 Spring 직접 호출과 결과가 같은지 별도 테스트에서 확인합니다.

다음 완전한 registry test body를 AnnotationConfigRegistryExtensionsTest.kt에 추가합니다.

~~~kotlin
package io.bluetape4k.spring.context.annotation

import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.AnnotationConfigRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

class SampleBean

@Configuration
class SampleConfiguration {
    @Bean
    fun sampleBean() = SampleBean()
}

class AnnotationConfigRegistryExtensionsTest {
    @Test
    fun registerReifiedConfiguration() {
        val context = AnnotationConfigApplicationContext()
        try {
            val registry: AnnotationConfigRegistry = context
            registry.register<SampleConfiguration>()
            context.refresh()
            context.getBean(SampleBean::class.java).shouldNotBeNull()
        } finally {
            context.close()
        }
    }
}
~~~

기존 `AnnotationExtensionsTest` 클래스에 파일의 package와 import를 그대로 사용해 다음 테스트 메서드를 추가합니다.

~~~kotlin
    @Target(AnnotationTarget.FUNCTION)
    @Retention(AnnotationRetention.RUNTIME)
    @Repeatable
    annotation class Marker(val value: String)

    @Marker("first")
    @Marker("second")
    fun repeatedMarkerMethod() {}

    @Test
    fun getMergedRepeatableAnnotationsMatchesSpringGetSemantics() {
        val element = this::class.java.getDeclaredMethod("repeatedMarkerMethod")
        val actual = element.getMergedRepeatableAnnotations<Marker>()
        val expected = AnnotatedElementUtils.getMergedRepeatableAnnotations(element, Marker::class.java)
        val found = element.findAllMergedAnnotations<Marker>()
        val expectedFound = AnnotatedElementUtils.findAllMergedAnnotations(element, Marker::class.java)

        actual.map { it.value }.toSet().shouldBeEqualTo(setOf("first", "second"))
        actual.shouldBeEqualTo(expected)
        found.shouldBeEqualTo(expectedFound)
    }
~~~

기존 테스트 import에 `org.springframework.core.annotation.AnnotatedElementUtils`를 추가합니다. `shouldBeEqualTo`는 이미 import되어 있으므로 기존 검증은 유지합니다.

- [ ] **단계 2: 구현 전에 두 테스트 필터 실행**

registry와 annotation filter를 순서대로 실행합니다.

~~~bash
./gradlew :bluetape4k-spring-boot-core:test --tests 'io.bluetape4k.spring.context.annotation.AnnotationConfigRegistryExtensionsTest'
./gradlew :bluetape4k-spring-boot-core:test --tests 'io.bluetape4k.spring.beans.AnnotationExtensionsTest'
~~~

예상 결과: registry reified 호출과 새 repeatable 보조 함수는 컴파일에 실패하고 기존 annotation 테스트는 계속 컴파일됩니다.

- [ ] **단계 3: registry 확장 함수 추가**

다음 구현으로 `AnnotationConfigRegistryExtensions.kt`를 생성하고 한국어 KDoc을 작성합니다. 입력 클래스는 애플리케이션 또는 테스트에서 신뢰하는 설정 코드입니다. helper는 클래스를 검증하거나 격리하지 않습니다. 등록 뒤 클래스 분석, 조건 평가, bean/factory method 실행과 실패 전파는 Spring context 생명주기에 맡긴다고 설명합니다.

~~~kotlin
package io.bluetape4k.spring.context.annotation

import org.springframework.context.annotation.AnnotationConfigRegistry

inline fun <reified T : Any> AnnotationConfigRegistry.register() {
    register(T::class.java)
}
~~~

다른 class를 추가 등록하거나 context를 refresh하지 않습니다.

- [ ] **단계 4: 반복 애너테이션 get 확장 함수 추가**

기존 `AnnotationExtensions.kt`에 다음 함수를 추가하고 get 조회 의미와 find 검색 의미의 차이를 설명하는 한국어 KDoc을 작성합니다.

~~~kotlin
inline fun <reified A : Annotation> AnnotatedElement.getMergedRepeatableAnnotations(): Set<A> =
    AnnotatedElementUtils.getMergedRepeatableAnnotations(this, A::class.java)
~~~

findAllMergedAnnotations는 변경하지 않습니다.

- [ ] **단계 5: 두 테스트 필터 실행**

Step 2와 같이 두 filter를 순서대로 실행합니다.

예상 결과: BUILD SUCCESSFUL이며 registry bean 등록, 반복 애너테이션 결과와 Spring 직접 호출의 일치, 기존 find 결과의 일치 검증이 통과합니다.

- [ ] **단계 6: registry 및 애너테이션 변경 커밋**

registry와 애너테이션의 소스·테스트만 한국어 Lore 형식의 커밋 하나로 기록합니다. 통과한 두 필터를 Tested에 적습니다.

**Step DoD: PENDING** — registry 및 애너테이션의 두 RED/GREEN 결과와 해당 commit을 확인하기 전입니다.

### 작업 5: TypeDescriptor 및 AnnotationMetadata 확장 함수

**Action:** 원시 클래스 의미를 유지하는 TypeDescriptor와 AnnotationMetadata reified 확장 함수를 각각 테스트해 구현합니다.
**Expected DoD:** 두 테스트 필터가 Spring 직접 호출 결과와 일치하고 소스·테스트 두 쌍만 한 커밋에 기록됩니다.

**파일:**
- 생성: spring-boot/core/src/test/kotlin/io/bluetape4k/spring/core/TypeDescriptorExtensionsTest.kt.
- 생성: spring-boot/core/src/test/kotlin/io/bluetape4k/spring/core/AnnotationMetadataExtensionsTest.kt.
- 생성: spring-boot/core/src/main/kotlin/io/bluetape4k/spring/core/TypeDescriptorExtensions.kt.
- 생성: spring-boot/core/src/main/kotlin/io/bluetape4k/spring/core/AnnotationMetadataExtensions.kt.

- [ ] **단계 1: 실패하는 타입·메타데이터 테스트 추가**

TypeDescriptor 테스트는 `typeDescriptorOf<String>().type`이 `String::class.java`와 같고, `typeDescriptorOf<List<*>>().type`이 `List::class.java`와 같은지 확인합니다. 매개변수화된 타입의 원소 메타데이터는 보존 대상으로 두지 않습니다.

AnnotationMetadata 테스트는 런타임까지 유지되는 `SampleMarker`를 붙인 `SampleConfiguration`을 정의합니다. `annotationMetadataOf<SampleConfiguration>().className`이 `SampleConfiguration::class.java.name`과 같고 `hasAnnotation(SampleMarker::class.java.name)`이 true인지 확인합니다. `AnnotationMetadata.introspect(SampleConfiguration::class.java)`의 결과와도 비교합니다.

위에 나열한 파일 경로와 일치하는 package를 사용해 다음 테스트 클래스를 추가합니다.

~~~kotlin
package io.bluetape4k.spring.core

import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test

class TypeDescriptorExtensionsTest {
    @Test
    fun typeDescriptorContainsOnlyTheRawClass() {
        typeDescriptorOf<String>().type shouldBeEqualTo String::class.java
        typeDescriptorOf<List<*>>().type shouldBeEqualTo List::class.java
    }
}
~~~

~~~kotlin
package io.bluetape4k.spring.core

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import org.springframework.core.type.AnnotationMetadata

class AnnotationMetadataExtensionsTest {
    @Target(AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class SampleMarker

    @SampleMarker
    class SampleConfiguration

    @Test
    fun introspectsTheReifiedClass() {
        val actual = annotationMetadataOf<SampleConfiguration>()
        val expected = AnnotationMetadata.introspect(SampleConfiguration::class.java)

        actual.className shouldBeEqualTo SampleConfiguration::class.java.name
        (actual.hasAnnotation(SampleMarker::class.java.name)).shouldBeTrue()
        actual.className shouldBeEqualTo expected.className
        actual.annotationTypes shouldBeEqualTo expected.annotationTypes
    }
}
~~~

- [ ] **단계 2: 구현 전에 두 테스트 필터 실행**

다음 filter를 순서대로 실행합니다.

~~~bash
./gradlew :bluetape4k-spring-boot-core:test --tests 'io.bluetape4k.spring.core.TypeDescriptorExtensionsTest'
./gradlew :bluetape4k-spring-boot-core:test --tests 'io.bluetape4k.spring.core.AnnotationMetadataExtensionsTest'
~~~

예상 결과: 두 보조 함수가 아직 정의되지 않아 컴파일에 실패합니다.

- [ ] **단계 3: TypeDescriptor wrapper 구현**

다음 코드를 사용해 TypeDescriptorExtensions.kt를 생성합니다. 한국어 KDoc에는 Java `Class` 토큰이 원시 클래스만 나타내며 타입 인자는 보존하지 않는다는 점을 설명합니다.

~~~kotlin
package io.bluetape4k.spring.core

import org.springframework.core.convert.TypeDescriptor

inline fun <reified T : Any> typeDescriptorOf(): TypeDescriptor =
    TypeDescriptor.valueOf(T::class.java)
~~~

- [ ] **단계 4: AnnotationMetadata wrapper 구현**

다음 코드로 `AnnotationMetadataExtensions.kt`를 생성합니다. 한국어 KDoc에는 Spring의 introspection 과정과 해당 예외 동작을 설명합니다.

~~~kotlin
package io.bluetape4k.spring.core

import org.springframework.core.type.AnnotationMetadata

inline fun <reified T : Any> annotationMetadataOf(): AnnotationMetadata =
    AnnotationMetadata.introspect(T::class.java)
~~~

- [ ] **단계 5: 두 테스트 필터 실행**

다음 filter를 순서대로 실행합니다.

~~~bash
./gradlew :bluetape4k-spring-boot-core:test --tests 'io.bluetape4k.spring.core.TypeDescriptorExtensionsTest'
./gradlew :bluetape4k-spring-boot-core:test --tests 'io.bluetape4k.spring.core.AnnotationMetadataExtensionsTest'
~~~

예상 결과: 두 명령이 모두 BUILD SUCCESSFUL로 끝나고 각 검증 결과가 Spring 직접 호출 결과와 일치합니다.

- [ ] **단계 6: 타입·메타데이터 변경 커밋**

새 소스 파일 두 개와 대응하는 테스트만 Lore 형식의 한국어 커밋으로 기록하고, 실행한 두 테스트 필터를 적습니다.

**Step DoD: PENDING** — 두 확장 함수의 실패·통과 테스트 결과와 해당 커밋을 확인하기 전입니다.

### 작업 6: SpEL 확장 함수

**Action:** 호출자가 제공하는 `EvaluationContext`를 보존하는 Expression의 타입 지정 reified overload 네 개를 테스트 우선으로 구현합니다.
**Expected DoD:** `ExpressionExtensionsTest`가 네 호출형, null 허용 결과와 Spring의 접근 정책에 따른 실패를 확인하고 소스·테스트가 한 커밋에 기록됩니다.

**파일:**
- 생성: spring-boot/core/src/test/kotlin/io/bluetape4k/spring/expression/ExpressionExtensionsTest.kt.
- 생성: spring-boot/core/src/main/kotlin/io/bluetape4k/spring/expression/ExpressionExtensions.kt.

- [ ] **단계 1: 실패하는 Expression 테스트 추가**

`SpelExpressionParser`로 표현식을 만듭니다. context 없이 호출하는 타입 지정 overload, root object overload, `EvaluationContext` overload, context와 root를 함께 받는 overload를 모두 다룹니다. root가 null인 `getValueAs<String>(rootObject = null)` 호출을 컴파일하고 실행해 null 결과를 확인합니다. 접근 정책이 유지되는지 확인하도록 data binding 전용 읽기 전용 `SimpleEvaluationContext`를 제공하고, 해당 context가 금지하는 메서드 호출을 평가해 Spring의 평가·접근 예외가 전파되는지 확인합니다.

다음 호출을 컴파일하고 실행합니다.

~~~kotlin
parser.parseExpression("'value'").getValueAs<String>() shouldBeEqualTo "value"
parser.parseExpression("name").getValueAs<String>(root) shouldBeEqualTo "value"
parser.parseExpression("'value'").getValueAs<String>(evaluationContext) shouldBeEqualTo "value"
parser.parseExpression("name").getValueAs<String>(evaluationContext, root) shouldBeEqualTo "value"
parser.parseExpression("null").getValueAs<String>(rootObject = null).shouldBeNull()
~~~

`ExpressionExtensionsTest.kt`에는 다음 테스트 클래스를 사용합니다.

~~~kotlin
package io.bluetape4k.spring.expression

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import org.junit.jupiter.api.Test
import org.springframework.expression.EvaluationException
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.SimpleEvaluationContext
import org.springframework.expression.spel.support.StandardEvaluationContext

class ExpressionExtensionsTest {
    data class Root(val name: String)

    private val parser = SpelExpressionParser()
    private val root = Root("value")

    @Test
    fun evaluatesAllFourTypedOverloads() {
        parser.parseExpression("'value'").getValueAs<String>() shouldBeEqualTo "value"
        parser.parseExpression("name").getValueAs<String>(root) shouldBeEqualTo "value"
        parser.parseExpression("'value'").getValueAs<String>(StandardEvaluationContext()) shouldBeEqualTo "value"
        parser.parseExpression("name").getValueAs<String>(StandardEvaluationContext(), root) shouldBeEqualTo "value"
        parser.parseExpression("null").getValueAs<String>(rootObject = null).shouldBeNull()
    }

    @Test
    fun preservesProvidedEvaluationContextPolicy() {
        val context = SimpleEvaluationContext.forReadOnlyDataBinding().build()
        assertFailsWith<EvaluationException> {
            parser.parseExpression("name.substring(1)").getValueAs<String>(context, root)
        }
    }
}
~~~

- [ ] **단계 2: 구현 전에 Expression 테스트 필터 실행**

실행: ./gradlew :bluetape4k-spring-boot-core:test --tests 'io.bluetape4k.spring.expression.ExpressionExtensionsTest'

예상 결과: 아직 `getValueAs`가 정의되지 않아 컴파일에 실패합니다.

- [ ] **단계 3: Spring Expression 메서드에 직접 위임하는 함수 네 개 추가**

다음 구현으로 `ExpressionExtensions.kt`를 생성합니다. 한국어 KDoc에는 호출자가 제공한 `EvaluationContext`의 정책을 보존하고 이 편의 확장 함수가 신뢰할 수 없는 expression을 검증하지 않는다는 점을 명시합니다.

~~~kotlin
package io.bluetape4k.spring.expression

import org.springframework.expression.EvaluationContext
import org.springframework.expression.Expression

inline fun <reified T : Any> Expression.getValueAs(): T? =
    getValue(T::class.java)

inline fun <reified T : Any> Expression.getValueAs(rootObject: Any?): T? =
    getValue(rootObject, T::class.java)

inline fun <reified T : Any> Expression.getValueAs(context: EvaluationContext): T? =
    getValue(context, T::class.java)

inline fun <reified T : Any> Expression.getValueAs(context: EvaluationContext, rootObject: Any?): T? =
    getValue(context, rootObject, T::class.java)
~~~

새 EvaluationContext를 생성하거나 expression error를 catch하지 않습니다.

- [ ] **단계 4: Expression filter 실행**

실행: ./gradlew :bluetape4k-spring-boot-core:test --tests 'io.bluetape4k.spring.expression.ExpressionExtensionsTest'

예상 결과: BUILD SUCCESSFUL; 네 오버로드, 이름 지정 null root 호출, null 결과, context 접근 정책에 따른 실패 검증이 모두 통과합니다.

- [ ] **단계 5: Expression 변경 commit**

소스와 테스트만 Lore 형식의 한국어 커밋으로 기록하고, 정확한 테스트 필터와 결과를 적습니다.

**Step DoD: PENDING** — SpEL의 실패·통과 테스트 결과와 해당 커밋을 확인하기 전입니다.

### 작업 7: ApplicationContext runner와 AutoConfigurations 확장 함수

**Action:** 구체 runner 세 종류의 설정·bean 함수와 단일 class를 받는 `autoConfigurationsOf<T>()`를 테스트 우선으로 구현합니다.
**Expected DoD:** `ApplicationContextRunnerExtensionsTest`가 반환 타입, 이름이 null인 인자 호출, 공급자 생명주기와 실패 전파를 확인하고 소스·테스트가 커밋됩니다.

**파일:**
- 생성: spring-boot/core/src/test/kotlin/io/bluetape4k/spring/tests/ApplicationContextRunnerExtensionsTest.kt.
- 생성: spring-boot/core/src/main/kotlin/io/bluetape4k/spring/tests/ApplicationContextRunnerExtensions.kt.
- 생성: spring-boot/core/src/main/kotlin/io/bluetape4k/spring/tests/AutoConfigurationsExtensions.kt.

- [ ] **단계 1: 실패하는 runner 타입·생명주기 테스트 추가**

구체 runner별 테스트를 작성합니다. `withUserConfiguration<SampleConfiguration>()`의 결과를 각 runner의 변수에 할당한 다음 runner별 fluent method를 호출해 구체 반환 타입 세 가지를 컴파일 단계에서 확인합니다. 설정 class는 애플리케이션 또는 테스트의 신뢰된 코드 입력으로만 사용하고 함수가 이를 검증하거나 격리하지 않는다고 KDoc에 명시합니다. Spring의 설정 분석, 조건 평가, bean·factory method 실행과 실패 전파는 Spring context 생명주기를 따릅니다. 추가로 `val runner = ApplicationContextRunner().withConfiguration(autoConfigurationsOf<SampleAutoConfiguration>())` 호출로 `@AutoConfiguration` fixture의 bean 등록을 확인합니다. 여러 class를 지정할 때는 신뢰된 class만 전달하는 Spring의 `AutoConfigurations.of(Class<?>...)` vararg 함수를 그대로 사용합니다.

각 runner에서 `withBean<Service> { Service() }`, `withBean<Service>("service", customizer) { Service() }`, `withBean<Service>(name = null) { Service() }` 호출이 컴파일되는지 확인합니다. 이름이 null인 호출은 구체 runner 세 종류 모두 이름 지정 인자로 작성해 Kotlin 확장 함수가 선택되고, 각 호출이 해당 runner의 구체 타입을 반환하는지 확인합니다. 위치 인자로 null을 전달하는 호출은 문서나 사용 예제에 넣지 않습니다.

세 runner의 구현 계약이 같으므로 생명주기 동작은 `ApplicationContextRunner`로 확인합니다. runner를 구성할 때 함수가 공급자나 사용자 지정 함수를 호출하지 않는지 검사합니다. 일반 bean 공급자는 `run`의 context refresh 중 한 번 호출되고 callback 안에서 bean을 읽을 수 있어야 합니다. 지연 초기화 사용자 지정 함수가 `BeanDefinition.setLazyInit(true)`를 설정한 경우, 공급자를 호출하지 않은 채 `run`이 성공해야 하며 최초 `getBean` 호출에서 횟수가 1이 되어야 합니다.

`prepare()`에서는 callback이 아직 refresh하지 않은 context를 받고 공급자 호출 횟수가 0으로 유지되는지 확인합니다. refresh 전에는 `context.isActive`가 false인지 확인하고 bean을 요청하지 않습니다. 이후 `context.getSourceApplicationContext().refresh()`를 명시적으로 호출해 context가 활성화되고 즉시 초기화 공급자가 한 번 실행되며 bean을 읽을 수 있는지 확인합니다. runner의 `prepare` 자체가 refresh하거나 공급자를 실행한다고 단정하지 않습니다.

즉시 초기화 중 공급자가 실패하면 `context.getStartupFailure()`가 null이 아니며 `BeanCreationException`이고 원래 `IllegalStateException`을 원인 체인에 포함하는지 확인합니다. 지연 초기화 공급자는 `run`이 성공한 뒤 `getBean`이 원래 예외를 원인 체인에 포함한 `BeanCreationException`을 던지는지 확인합니다. 사용자 지정 함수가 실패하면 `context.getStartupFailure()`가 해당 `IllegalStateException`이고 공급자 호출 횟수는 0인지 확인합니다. 시작 실패는 `getStartupFailure()`로 확인합니다. `AssertableApplicationContext`에는 `hasFailed()` 메서드가 없습니다. 같은 runner를 두 번 실행해 각 실행에서 서로 다른 context와 한 번의 공급자 호출을 확인하고, 파생 runner가 원본 runner의 등록 정보를 바꾸지 않는지도 확인합니다.

`ApplicationContextRunnerExtensionsTest.kt`에는 아래 구체 타입·실행 테스트와 `autoConfigurationsHelperUsesOneReifiedConfigurationClass` 테스트를 사용합니다. 필요한 runner, auto-configuration 및 JUnit import와 아래 fixture를 추가합니다.

~~~kotlin
package io.bluetape4k.spring.tests

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.assertions.shouldNotBeSameInstanceAs
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.BeanCreationException
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.config.BeanDefinitionCustomizer
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.atomic.AtomicInteger

class ServiceBean

@AutoConfiguration
class SampleAutoConfiguration {
    @Bean
    fun autoConfiguredService() = ServiceBean()
}

@Configuration(proxyBeanMethods = false)
class SampleRunnerConfiguration {
    @Bean
    fun configuredService() = ServiceBean()
}

class ApplicationContextRunnerExtensionsTest {
    @Test
    fun autoConfigurationsHelperUsesOneReifiedConfigurationClass() {
        ApplicationContextRunner()
            .withConfiguration(autoConfigurationsOf<SampleAutoConfiguration>())
            .run { context ->
                context.getBean(ServiceBean::class.java).shouldNotBeNull()
            }
    }

    @Test
    fun allRunnerWrappersKeepTheirConcreteReturnTypes() {
        val application: ApplicationContextRunner = ApplicationContextRunner()
            .withUserConfiguration<SampleRunnerConfiguration>()
            .withBean<ServiceBean> { ServiceBean() }
            .withBean<ServiceBean>("service", BeanDefinitionCustomizer { it.setLazyInit(true) }) { ServiceBean() }
        val applicationWithNullName: ApplicationContextRunner = ApplicationContextRunner()
            .withBean<ServiceBean>(name = null) { ServiceBean() }

        val web: WebApplicationContextRunner = WebApplicationContextRunner()
            .withUserConfiguration<SampleRunnerConfiguration>()
            .withBean<ServiceBean> { ServiceBean() }
            .withBean<ServiceBean>("service", BeanDefinitionCustomizer { it.setLazyInit(true) }) { ServiceBean() }
        val webWithNullName: WebApplicationContextRunner = WebApplicationContextRunner()
            .withBean<ServiceBean>(name = null) { ServiceBean() }

        val reactive: ReactiveWebApplicationContextRunner = ReactiveWebApplicationContextRunner()
            .withUserConfiguration<SampleRunnerConfiguration>()
            .withBean<ServiceBean> { ServiceBean() }
            .withBean<ServiceBean>("service", BeanDefinitionCustomizer { it.setLazyInit(true) }) { ServiceBean() }
        val reactiveWithNullName: ReactiveWebApplicationContextRunner = ReactiveWebApplicationContextRunner()
            .withBean<ServiceBean>(name = null) { ServiceBean() }

        application.shouldNotBeNull()
        applicationWithNullName.shouldNotBeNull()
        web.shouldNotBeNull()
        webWithNullName.shouldNotBeNull()
        reactive.shouldNotBeNull()
        reactiveWithNullName.shouldNotBeNull()
    }

    @Test
    fun supplierAndCustomizerRunAtSpringLifecycleBoundaries() {
        val eagerCalls = AtomicInteger()
        val eagerRunner = ApplicationContextRunner()
            .withBean<ServiceBean> { eagerCalls.incrementAndGet(); ServiceBean() }
        eagerCalls.get() shouldBeEqualTo 0
        eagerRunner
            .run { context ->
                eagerCalls.get() shouldBeEqualTo 1
                context.getBean(ServiceBean::class.java).shouldNotBeNull()
            }

        val lazyCalls = AtomicInteger()
        val customizerCalls = AtomicInteger()
        val lazyCustomizer = BeanDefinitionCustomizer {
            customizerCalls.incrementAndGet()
            it.setLazyInit(true)
        }
        val lazyRunner = ApplicationContextRunner().withBean<ServiceBean>(lazyCustomizer) {
            lazyCalls.incrementAndGet()
            ServiceBean()
        }
        lazyCalls.get() shouldBeEqualTo 0
        customizerCalls.get() shouldBeEqualTo 0
        lazyRunner.run { context ->
            customizerCalls.get() shouldBeEqualTo 1
            lazyCalls.get() shouldBeEqualTo 0
            context.getBean(ServiceBean::class.java).shouldNotBeNull()
            lazyCalls.get() shouldBeEqualTo 1
        }

        val preparedCalls = AtomicInteger()
        ApplicationContextRunner().withBean<ServiceBean> {
            preparedCalls.incrementAndGet()
            ServiceBean()
        }.prepare { context ->
            preparedCalls.get() shouldBeEqualTo 0
            (context.isActive).shouldBeFalse()
            context.sourceApplicationContext.refresh()
            (context.isActive).shouldBeTrue()
            preparedCalls.get() shouldBeEqualTo 1
            context.getBean(ServiceBean::class.java).shouldNotBeNull()
        }
    }

    @Test
    fun failuresRemainObservableAtSpringCreationBoundaries() {
        val supplierFailure = IllegalStateException("supplier-failed")
        ApplicationContextRunner().withBean<ServiceBean> { throw supplierFailure }.run { context ->
            val startupFailure = context.getStartupFailure()
            (startupFailure is BeanCreationException).shouldBeTrue()
            (generateSequence(startupFailure) { it.cause }.any { it === supplierFailure }).shouldBeTrue()
        }

        val lazyFailure = IllegalStateException("lazy-supplier-failed")
        val lazyCustomizer = BeanDefinitionCustomizer { it.setLazyInit(true) }
        ApplicationContextRunner().withBean<ServiceBean>(lazyCustomizer) { throw lazyFailure }.run { context ->
            context.getStartupFailure().shouldBeNull()
            val actual = assertFailsWith<BeanCreationException> { context.getBean(ServiceBean::class.java) }
            (generateSequence(actual) { it.cause }.any { it === lazyFailure }).shouldBeTrue()
        }

        val customizerFailure = IllegalStateException("customizer-failed")
        val supplierCalls = AtomicInteger()
        ApplicationContextRunner()
            .withBean<ServiceBean>(BeanDefinitionCustomizer { throw customizerFailure }) {
                supplierCalls.incrementAndGet()
                ServiceBean()
            }
            .run { context ->
                context.getStartupFailure().shouldNotBeNull()
                context.getStartupFailure() shouldBeSameInstanceAs customizerFailure
                supplierCalls.get() shouldBeEqualTo 0
            }
    }

    @Test
    fun repeatedRunsUseIndependentContextsAndDerivedRunnersDoNotMutateSource() {
        val calls = AtomicInteger()
        val runner = ApplicationContextRunner().withBean<ServiceBean> {
            calls.incrementAndGet()
            ServiceBean()
        }
        val contexts = mutableListOf<Any>()
        repeat(2) {
            runner.run { context ->
                contexts += context.sourceApplicationContext
                context.getBean(ServiceBean::class.java).shouldNotBeNull()
            }
        }
        contexts[1] shouldNotBeSameInstanceAs contexts[0]
        calls.get() shouldBeEqualTo 2

        val original = ApplicationContextRunner()
        val derived = original.withBean<ServiceBean> { ServiceBean() }
        original.run { context ->
            assertFailsWith<NoSuchBeanDefinitionException> { context.getBean(ServiceBean::class.java) }
        }
        derived.run { context -> context.getBean(ServiceBean::class.java).shouldNotBeNull() }
    }
}
~~~

- [ ] **단계 2: 구현 전에 runner 테스트 필터 실행**

실행: ./gradlew :bluetape4k-spring-boot-core:test --tests 'io.bluetape4k.spring.tests.ApplicationContextRunnerExtensionsTest'

예상 결과: 확장 함수가 없어 컴파일에 실패합니다. 함수 미정의 외에 생명주기 예상 결과가 실패하면 다음 단계로 진행하기 전에 원인을 조사합니다.

- [ ] **단계 3: 설정 함수 세 개 구현**

ApplicationContextRunnerExtensions.kt에 다음 세 concrete wrapper를 추가하고 각각에 Korean KDoc을 작성합니다.

~~~kotlin
inline fun <reified CONFIG : Any> ApplicationContextRunner.withUserConfiguration(): ApplicationContextRunner =
    withUserConfiguration(CONFIG::class.java)

inline fun <reified CONFIG : Any> WebApplicationContextRunner.withUserConfiguration(): WebApplicationContextRunner =
    withUserConfiguration(CONFIG::class.java)

inline fun <reified CONFIG : Any> ReactiveWebApplicationContextRunner.withUserConfiguration(): ReactiveWebApplicationContextRunner =
    withUserConfiguration(CONFIG::class.java)
~~~

- [ ] **단계 4: bean 등록 함수 여섯 개 구현**

`ApplicationContextRunnerExtensions.kt`의 package 및 import 다음에 아래 구현을 추가합니다. 각 overload의 bean 등록, 구체적인 fluent 반환 타입, 공급자 실행 시점을 설명하는 한국어 KDoc을 작성합니다.

~~~kotlin
inline fun <reified T : Any> ApplicationContextRunner.withBean(
    vararg customizers: BeanDefinitionCustomizer,
    crossinline supplier: () -> T,
): ApplicationContextRunner =
    withBean(T::class.java, Supplier { supplier() }, *customizers)

inline fun <reified T : Any> ApplicationContextRunner.withBean(
    name: String?,
    vararg customizers: BeanDefinitionCustomizer,
    crossinline supplier: () -> T,
): ApplicationContextRunner =
    withBean(name, T::class.java, Supplier { supplier() }, *customizers)

inline fun <reified T : Any> WebApplicationContextRunner.withBean(
    vararg customizers: BeanDefinitionCustomizer,
    crossinline supplier: () -> T,
): WebApplicationContextRunner =
    withBean(T::class.java, Supplier { supplier() }, *customizers)

inline fun <reified T : Any> WebApplicationContextRunner.withBean(
    name: String?,
    vararg customizers: BeanDefinitionCustomizer,
    crossinline supplier: () -> T,
): WebApplicationContextRunner =
    withBean(name, T::class.java, Supplier { supplier() }, *customizers)

inline fun <reified T : Any> ReactiveWebApplicationContextRunner.withBean(
    vararg customizers: BeanDefinitionCustomizer,
    crossinline supplier: () -> T,
): ReactiveWebApplicationContextRunner =
    withBean(T::class.java, Supplier { supplier() }, *customizers)

inline fun <reified T : Any> ReactiveWebApplicationContextRunner.withBean(
    name: String?,
    vararg customizers: BeanDefinitionCustomizer,
    crossinline supplier: () -> T,
): ReactiveWebApplicationContextRunner =
    withBean(name, T::class.java, Supplier { supplier() }, *customizers)
~~~

`BeanDefinitionCustomizer`, runner 세 종류와 `java.util.function.Supplier`를 import합니다. null일 수 있는 이름과 사용자 지정 함수는 그대로 전달하고 Spring이 bean을 요청하기 전에는 공급자를 실행하지 않습니다. `withUserConfiguration` 세 overload와 `autoConfigurationsOf<T>()`의 한국어 KDoc에는 설정 class가 신뢰된 코드 입력이며 확장 함수가 class 검증이나 격리를 추가하지 않는다고 설명합니다.

- [ ] **단계 5: runner filter 실행**

실행: ./gradlew :bluetape4k-spring-boot-core:test --tests 'io.bluetape4k.spring.tests.ApplicationContextRunnerExtensionsTest'

예상 결과: BUILD SUCCESSFUL이며 구체 반환 타입, 이름이 null인 bean 등록, 불변 파생, 즉시·지연 초기화 및 `prepare` 시점, 시작·지연 실패의 원인 체인, 사용자 지정 함수 실패와 반복 실행 격리를 확인합니다.

- [ ] **단계 6: runner 변경 commit**

`ApplicationContextRunnerExtensions.kt`, `AutoConfigurationsExtensions.kt` 및 runner·auto-configuration 테스트만 한국어 Lore 형식으로 커밋합니다. 실행한 테스트 필터와 결과를 기록합니다.

**Step DoD: PENDING** — 세 runner와 AutoConfigurations의 RED/GREEN 결과 및 해당 commit 확인 전입니다.

### 작업 8: 영어 및 한국어 README 예제

**Action:** 두 README에 28개 함수의 전체 import와 호출 예제, 소비자 의존성 표를 같은 범위로 기록합니다.
**Expected DoD:** 양쪽 문서의 API·의존성 범위가 일치하고 모든 예제가 대상 테스트에서 컴파일되며 두 README만 커밋됩니다.

**파일:**
- 수정: spring-boot/core/README.md.
- 수정: spring-boot/core/README.ko.md.

- [ ] **단계 1: 대응하는 전체 API 예제 추가**

각 README에 목록에 있는 공개 함수 28개 모두의 전체 import와 복사해 쓸 수 있는 호출 예제를 추가합니다. 와일드카드 import나 패키지만 적은 안내는 사용하지 않습니다. Binder와 BootstrapContext는 실제 호출하는 함수의 전체 import를 각각 표시하고 SpringFactoriesLoader, 애너테이션·타입·메타데이터·표현식 및 runner 확장도 실제 패키지와 함수명을 갖춘 import문을 제시합니다. Binder overload 네 개, BootstrapContext 함수 다섯 개, resolver가 없거나 한쪽 또는 양쪽 모두 지정된 경우와 이름이 null인 경우를 포함한 SpringFactoriesLoader.load, AnnotationConfigRegistry.register, getMergedRepeatableAnnotations, typeDescriptorOf, annotationMetadataOf, SpEL 호출 네 가지, runner별 withUserConfiguration, runner별 이름 지정·미지정 withBean, 단일 auto-configuration에 사용하는 autoConfigurationsOf<T>()를 모두 다룹니다. 각 API 묶음의 소비자 artifact도 안내합니다. 여러 시그니처를 “그 밖의 형태”와 같은 문구로 생략하지 않습니다. 확장 함수 import는 설계서 §5와 대조하고 README 호출에 필요한 전체 import를 적습니다.

~~~kotlin
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
~~~

reified `register<T>()`, `withUserConfiguration<CONFIG>()`, `autoConfigurationsOf<T>()`는 호출마다 class 하나를 받습니다. 여러 auto-configuration을 지정할 때는 Spring의 `AutoConfigurations.of(Class<?>...)` 가변 인자 함수를 그대로 사용하고, 단일 class용 확장 함수를 반복 호출하는 방법이 이를 대체한다고 설명하지 않습니다. 여러 설정 class가 필요한 README 예제에도 기존 Spring 가변 인자 함수를 사용합니다. `getMergedRepeatableAnnotations<A>()`는 기존 `findAllMergedAnnotations<A>()`를 대체하지 않으므로 양쪽을 함께 보여 주고, 선언 위치 탐색과 계층 검색 중 필요한 의미에 따라 선택하는 전환 안내를 추가합니다.

Java 멤버 오버로드 선택이 모호해질 수 있으므로 다음처럼 null을 이름 지정 인자로 전달하는 예제를 포함합니다. runner의 null 이름은 `name = null`로 지정하며 위치 인자 `null` 호출은 제시하지 않습니다. 각 README 예제에는 실제 호출 함수의 정확한 import도 둡니다.

~~~kotlin
binder.bind<ServerProperties>("server", bindHandler = null)
loader.load<Factory>(argumentResolver = null)
loader.load<Factory>(failureHandler = null)
expression.getValueAs<String>(rootObject = null)
ApplicationContextRunner().withBean<Service>(name = null) { Service() }
WebApplicationContextRunner().withBean<Service>(name = null) { Service() }
ReactiveWebApplicationContextRunner().withBean<Service>(name = null) { Service() }
~~~

두 언어 문서 모두 이 확장 함수가 선택적으로 쓸 수 있는 호출 방식이며 기존 Java 형식 호출도 계속 쓸 수 있다고 설명합니다. 소비자는 사용하는 API를 제공하는 artifact를 직접 선언해야 합니다. 의존성 표는 API가 제공되는 artifact와 기존 Spring Boot starter의 관계를 설명하고, 이미 사용하는 starter와 Spring Boot BOM을 통해 Spring 버전을 맞추도록 안내합니다. artifact 목록은 확장 함수가 참조하는 API의 최소 안내이며, 예제를 위해 새 의존성이 필요하지 않다는 점을 분명히 합니다. `spring-boot-test`는 runner 예제용 테스트 범위 의존성입니다. 의존성 표에는 다음 항목을 포함합니다.
- org.springframework.boot:spring-boot — Binder 및 BootstrapContext.
- org.springframework:spring-core — SpringFactoriesLoader, repeatable annotation, TypeDescriptor, AnnotationMetadata.
- org.springframework:spring-context — AnnotationConfigRegistry.
- org.springframework:spring-expression — Expression.
- org.springframework.boot:spring-boot-test (test scope) — runner helper.
- org.springframework.boot:spring-boot-autoconfigure — `autoConfigurationsOf<T>()`.

검증 기준인 Spring Boot 4.1.1 및 Spring Framework 7.0.9를 기록하고 의존성이나 버전 변경은 이 기능 범위에 포함하지 않습니다. 기존 Spring Boot starter를 사용하는 소비자는 artifact를 중복 추가하지 않고 해당 starter와 Spring Boot BOM이 정렬해 주는 버전을 사용하도록 안내합니다. README.md는 자연스러운 영어로, README.ko.md는 한국어로 작성합니다.

- [ ] **단계 2: 두 README의 내용 대응 검토**

두 README의 해당 절을 비교합니다. 모든 API 묶음, 함수별 전체 import 문장, null을 이름 지정 인자로 전달하는 호출, 의존성 표의 각 행이 대응하는지 확인합니다. 특히 Binder와 BootstrapContext에 와일드카드 import가 없고 runner 세 종류 모두 `name = null`로 호출되는지 확인합니다. 코드 식별자는 위 공개 API 목록과 대조합니다.

예상 결과: 두 언어 문서의 API와 의존성 범위가 같고 설명 문장만 언어별로 다릅니다. 각 코드 예제는 대응 테스트에서 실제로 컴파일한 호출을 사용합니다: `BinderExtensionsTest`, `BootstrapContextExtensionsTest`, `SpringFactoriesLoaderExtensionsTest`, `AnnotationConfigRegistryExtensionsTest`, `AnnotationExtensionsTest`, `TypeDescriptorExtensionsTest`, `AnnotationMetadataExtensionsTest`, `ExpressionExtensionsTest`, `ApplicationContextRunnerExtensionsTest`. `autoConfigurationsOf<T>()` 예제는 마지막 테스트 클래스의 전용 테스트 메서드와 대응해야 합니다. 이 테스트에서 컴파일하지 않은 호출은 문서에 넣지 않습니다. README를 확정한 뒤 작업 9에서 아홉 테스트 클래스를 포함하는 모듈 전체 테스트를 실행합니다.

- [ ] **단계 3: README 변경 commit**

두 README만 한국어 Lore 형식으로 커밋하고, 문서 대응 검토와 의존성 변경이 없다는 사실을 Tested에 기록합니다.

**Step DoD: PENDING** — README 대응 검토와 compile-checked 예제, 해당 commit 확인 전입니다.

### 작업 9: 통합, 의존성, 커버리지 및 CI 경로 검증

**Action:** 모듈 전체 테스트, 정적 분석, 커버리지, 의존성, CI 경로, JVM 시그니처와 최종 구현 검토를 순서대로 확인합니다.
**Expected DoD:** 각 검증의 최신 결과, 구현 변경에 대한 통합 검토의 P0=0/P1=0, 알려진 공백과 위험이 기록됩니다.

**파일:**
- 구현 파일은 추가하지 않습니다. 빌드 파일과 workflow 파일은 읽기 전용으로 확인합니다. 최종 구현 검토 기록은 `docs/superpowers/reviews/2026-10-10-spring-reified-extensions-final-review.md`입니다.

#### 검증 시간 제한과 시간 초과 대응

각 Gradle 명령은 별도 실행 세션에서 순서대로 실행하고 시작·종료 UTC 시각, 마지막 출력과 종료 상태를 기록합니다. 최대 실행 시간은 테스트 필터마다 15분, 모듈 전체 테스트 30분, Detekt 15분, Kover 20분, 의존성 보고서 각각 15분, 모듈 빌드 30분입니다. 제한 시간에 도달하면 해당 Gradle 실행을 중단하고 프로세스 목록, Gradle daemon log, 마지막 출력과 NUL-safe git status를 보존합니다. 원인을 진단하기 전까지 검증은 PENDING으로 두고 재시도하지 않습니다. 정상 동작 중인 Gradle daemon은 임의로 종료하지 않습니다.

- [ ] **단계 1: 모듈 전체 테스트 실행**

실행: ./gradlew :bluetape4k-spring-boot-core:test

예상 결과: 기존 테스트와 집중 테스트 클래스 아홉 개를 포함해 BUILD SUCCESSFUL입니다. 작업 1–7에서 실행한 각 테스트 필터의 정확한 결과는 해당 작업 커밋에 기록합니다. 실패하면 로그를 조사해 제품 결함과 실행 환경 문제를 구분하고, 승인 범위 안의 제품 결함을 수정한 뒤 모듈 전체 테스트를 다시 실행합니다. 테스트가 Testcontainers를 시작하면 다른 모듈이나 worktree의 컨테이너 검증과 겹치지 않도록 순서대로 실행합니다.

- [ ] **단계 2: 모듈 정적 분석 실행**

실행: ./gradlew :bluetape4k-spring-boot-core:detekt

예상 결과: 변경한 소스나 테스트에 새 지적 사항 없이 BUILD SUCCESSFUL입니다.

- [ ] **단계 3: Kover XML coverage 생성 및 확인**

실행: ./gradlew :bluetape4k-spring-boot-core:koverXmlReport -x test --no-configuration-cache

예상 결과: BUILD SUCCESSFUL이며 `spring-boot/core/build/reports/kover/` 아래에 XML 보고서가 생성됩니다. 보고서에서 경로와 전체 line/branch counter를 읽어 완료 보고에 기록합니다. production line coverage를 저장소의 80% 목표와 비교합니다. 모듈 커버리지가 80%보다 낮으면 정확한 비율과 이 기능에서 추가한 helper line의 포함 여부를 기록하고, 목표를 통과했다고 주장하지 않은 채 모듈별 공백을 설명합니다. 새 threshold를 도입하거나 Kover 설정을 수정하지 않습니다.

- [ ] **단계 4: 의존성 구성 확인**

다음 명령을 순서대로 실행하고 출력을 작업 기록에 저장합니다.

~~~bash
./gradlew :bluetape4k-spring-boot-core:dependencies --configuration compileClasspath
./gradlew :bluetape4k-spring-boot-core:dependencies --configuration testCompileClasspath
./gradlew :bluetape4k-spring-boot-core:dependencies --configuration testRuntimeClasspath
~~~

예상 결과: 프로젝트의 의존성 선언은 바뀌지 않았고 Spring API가 기존 compile-only/runtime/test 의존성 구성에서 resolve됩니다. resolve된 coordinate와 README의 artifact 안내를 대조합니다. README에 맞추려고 Gradle 파일을 수정하지 않습니다. 실제 불일치는 승인 설계 범위에서 해결할 blocker로 보고합니다.

- [ ] **단계 5: 기존 CI 및 Nightly 적용 범위 확인**

`.github/workflows/ci.yml`과 `.github/workflows/nightly-tests.yml`에서 `spring-boot/core` 경로 필터, 테스트 작업, `koverXmlReport` matrix entry를 확인합니다. 정확한 task name을 `rg`로 찾고 주변 YAML도 읽습니다. `spring-boot/**` 경로가 해당 job으로 연결되고 모듈 테스트와 Kover task가 있는지 확인합니다. workflow는 수정하지 않습니다.

예상 결과: 기존 CI/Nightly의 경로와 task entry를 간결하게 기록합니다. 현재 checkout에서 필요한 entry가 없으면 그 공백을 보고하고 이 계획에 따라 CI를 수정하지 않습니다.

- [ ] **단계 6: 최종 빌드 및 소스 diff 확인**

다음을 순서대로 실행합니다.

~~~bash
./gradlew :bluetape4k-spring-boot-core:build
git diff --check 5fc36ff930d624674d852a364f7411c26522a56a...HEAD
git status --short --branch
git diff --stat 5fc36ff930d624674d852a364f7411c26522a56a...HEAD
git diff --name-only 5fc36ff930d624674d852a364f7411c26522a56a...HEAD
~~~

예상 결과: 모듈 빌드가 성공하고 기준 branch와 비교한 diff 확인에서 진단이 나오지 않으며, 작업 0–8 커밋 뒤 worktree가 깨끗합니다. 이 Step 6에서 branch diff에는 승인된 설계·계획, Kotlin 소스 경로 10개(신규 9개, 수정 1개), 집중 테스트 파일 9개(신규 8개, 수정 1개), 새 테스트 리소스 하나와 README 두 개만 포함됩니다. A-08 구현 검토 기록과 A-09 lesson은 각각 작업 9의 Step 9와 작업 10에서 만들므로 이 중간 목록에는 아직 없습니다. Gradle, workflow, 모듈 등록, CI, 의존성, 다이어그램, CHANGELOG.md, AGENTS.md와 release note는 변경하지 않아야 합니다.

- [ ] **단계 7: 컴파일된 공개 JVM 시그니처 확인**

실행: `javap -classpath spring-boot/core/build/classes/kotlin/main -public -s`에 아래 생성된 owner class를 전달합니다.

~~~text
io.bluetape4k.spring.boot.BinderExtensionsKt
io.bluetape4k.spring.boot.BootstrapContextExtensionsKt
io.bluetape4k.spring.core.io.support.SpringFactoriesLoaderExtensionsKt
io.bluetape4k.spring.context.annotation.AnnotationConfigRegistryExtensionsKt
io.bluetape4k.spring.beans.AnnotationExtensionsKt
io.bluetape4k.spring.core.TypeDescriptorExtensionsKt
io.bluetape4k.spring.core.AnnotationMetadataExtensionsKt
io.bluetape4k.spring.expression.ExpressionExtensionsKt
io.bluetape4k.spring.tests.ApplicationContextRunnerExtensionsKt
io.bluetape4k.spring.tests.AutoConfigurationsExtensionsKt
~~~

예상 결과: 컴파일된 public static JVM method name, erased descriptor, generic signature를 계획의 API 목록과 대조해 누락·중복·의도하지 않은 overload를 찾습니다. 저장소에서 별도의 binary-compatibility baseline task/plugin은 발견하지 못했습니다. 따라서 이 단계는 해당 시그니처를 직접 읽어 확인하며 전체 ABI 기준선과 비교하지는 않습니다.

- [ ] **단계 8: 최종 API 및 문서 점검**

`git diff 5fc36ff930d624674d852a364f7411c26522a56a...HEAD`를 검토하고 다음을 확인합니다.
- 각 공개 API가 승인된 시그니처와 Spring 직접 위임을 사용하는지 확인합니다.
- unchecked cast, reflection fallback, 예외를 무시하는 경로, 숨겨진 context 생성, 공급자의 조기 실행이 없는지 확인합니다.
- Binder handler overload가 `Bindable.of(T::class.java)`를 사용하는지 확인합니다.
- null을 받을 수 있는 member overload에 이름 지정 인자 예제가 있는지 확인합니다.
- runner의 구체 self type이 보존되는지 확인합니다.
- Annotation get 함수와 find 함수의 의미가 분리되어 있는지 확인합니다.
- TypeDescriptor가 generic erasure를 문서화하는지 확인합니다.
- SpEL이 호출자가 제공한 `EvaluationContext`를 보존하는지 확인합니다.
- SpringFactoriesLoader의 classpath 신뢰 가정이 배포 환경을 전제로 적혀 있는지, `register<T>()`, `withUserConfiguration<T>()`, `autoConfigurationsOf<T>()`의 class 입력이 신뢰된 애플리케이션·테스트 코드인지, KDoc에서 확장 함수가 class 검증·격리·sandbox를 추가하지 않는다고 설명하는지 확인합니다. Spring의 configuration parsing, condition evaluation, bean/factory method 실행과 failure propagation을 감추거나 바꾸지 않는지도 확인합니다.
- 테스트 fixture resource가 새로 만들어졌고 격리되었는지 확인합니다.
- README의 artifact 안내가 resolve된 dependency graph와 일치하는지 확인합니다.
- dependency, version, CI, module-registration, AWS, release, publication 파일이 바뀌지 않았는지 확인합니다.
- 이번 변경은 local helper API와 대응 README만 추가하며 system architecture, repository guidance, release metadata는 바꾸지 않습니다. 따라서 architecture diagram, CHANGELOG.md, AGENTS.md, release note를 추가할 필요가 없습니다. 전체 feature diff에 해당 경로가 없는지 확인합니다.

예상 결과: 모든 checklist item을 source 또는 command output으로 검증하고, gap이 있으면 명시적으로 기록합니다.

- [ ] **단계 9: PR 전 최종 구현 검토 (A-08)**

최종 branch diff를 여섯 독립 관점(performance, stability, security, operations, developer/API, user/caller)과 주 세션 통합 검토로 살핍니다. 각 관점은 고정한 정확한 head와 source/test/doc diff를 기준으로 P0/P1/P2/P3 finding을 기록합니다. P0/P1은 수정하고 영향을 받은 검증을 다시 실행합니다. 통합 결과가 P0=0, P1=0일 때만 이 gate를 닫습니다. 통합 검토문과 다음 한국어 review artifact에 SPW-01–05를 모두 적용합니다.

`docs/superpowers/reviews/2026-10-10-spring-reified-extensions-final-review.md`에는 관점별 finding 수, 수정 또는 근거를 갖춘 disposition, 재검증 증거, 남은 P2/P3 위험과 exact head를 기록합니다. 검토 범위를 넘어 API나 dependency 범위를 바꿔야 하면 계획을 다시 승인받기 전까지 멈춥니다.

예상 결과: 최신 통합 검토의 P0=0, P1=0이며 review artifact의 SPW-01–05가 모두 PASS입니다. 모든 finding의 disposition을 정리한 뒤 review artifact만 한국어 Lore 형식으로 커밋하고 commit SHA를 보존합니다. blocker가 남으면 구현 완료 처리는 PENDING입니다.

- [ ] **단계 10: 최종 구현 증거 기록**

각 작업의 commit, 변경 경로, 테스트 명령과 정확한 결과, 모듈 전체 테스트, Detekt, Kover report/counter, build, dependency 확인, CI/Nightly 확인, `git diff --check`, 알려진 실패와 미실행 작업을 기록합니다. PR, merge, release, publication, tag 및 외부 저장소 작업은 대기 상태로 둡니다.

예상 결과: 구현 증거에는 코드 작업별 계획 상태, 검증 출력, 변경 산출물, 위험·공백과 미실행 검증이 포함됩니다. 현재 branch에는 PR CI 실행이 없으므로 원격 PR check는 N/A로 분리하고 local test/build/static analysis를 원격 CI 결과로 계산하지 않습니다. 최종 checklist 수와 workflow DoD는 작업 11에서 lesson 및 no-PR gate 증거를 포함해 계산합니다. 검증을 실행하지 않았으면 N/A로 바꾸지 말고 PENDING 또는 BLOCKED 사유를 설명합니다.

**Step DoD: PENDING** — 모듈 검증, coverage·dependency·CI read-back, A-08 review와 증거 기록 전입니다.

### 작업 10: A-09 재발 방지 교훈 작성 및 커밋

**Action:** 이 실행에서 확인한 workflow 누락과 검증 가능한 guard를 한국어 교훈 문서로 정리하고 커밋합니다.
**Expected DoD:** 교훈 문서에 원인·결정·결과·검증·재발 방지 기준이 담기고 writer gate를 통과한 단일 커밋이 있습니다.

**파일:**
- 생성: docs/lessons/2026-10-10-spring-reified-workflow.md.

이번 작업에서 확인한 workflow 누락과 사용자 수정을 재발 방지 규칙으로 정리합니다. 계획 승인과 `$executing-plans` 선택이 A-04 계획 검토, 여섯 관점 통합, SPW/KO gate를 건너뛸 권한을 주지 않는다는 점, issue milestone을 live GitHub와 대조해야 한다는 점, workflow helper의 mutation input이 receipt checksum 및 bounded JSON file contract를 따라야 한다는 점을 원인·근거·결정·결과·누락·향후 guard와 연결합니다. 시간순 기록이나 raw review table은 lesson에 복사하지 않습니다.

교훈 문서에 SPW-01–05를 모두 적용하고 한국어 용어 점검, 공백·자리표시자·코드 울타리 검사를 통과시킨 뒤 해당 파일만 한국어 Lore 형식으로 커밋합니다. A-09 evidence에는 교훈 문서 경로와 commit, writer gate 결과를 기록합니다.

예상 결과: 교훈 문서가 커밋되고 계획 승인 전 누락, 사용자 수정, helper 호출 실수를 다음 workflow 시작 때 막는 구체 guard가 남습니다.

**Step DoD: PENDING** — 근거 기반 lesson, 작성자 게이트와 lesson commit 확인 전입니다.

### 작업 11: PR 없는 최종 DoD 및 전달 상태 기록

**Action:** PR 없는 범위에서 workflow gate, issue 상태, branch 보존과 완료·미완료 항목을 마지막으로 계산해 보고합니다.
**Expected DoD:** `Required checks: X/Y; N/A: N; Blocked: B`가 checklist와 일치하고 미완료 ID, local branch/worktree, 위험과 최종 PENDING 사유가 드러납니다.

PR, push, merge, tag, release, publication, workflow dispatch는 이 승인 범위에 없습니다. live GitHub에서 #1823 상태와 milestone을 다시 확인하고, CG-11–CG-13은 PR 권한 부재와 PR 없는 범위를 근거로 N/A 처리합니다. CG-14는 적용 가능한 gate로 유지합니다. `debop` 단독 유지보수 범위를 근거로 기록하고 대면 human-review 하위 gate만 `N/A (single-developer lane)`로 둡니다. exact-head CI, review/thread read-back, 독립 final code review, P0/P1 disposition 및 적용 가능한 artifact 검토는 계속 요구합니다. PR이 없어 증거를 얻지 못한 항목은 N/A나 SKIPPED로 바꾸지 않고 PENDING으로 보고합니다. CG-15에는 merge 준비 완료 대신 이번 전달 범위의 DoD를 보고합니다. merge-specific CG-16/CG-17은 이번 PR 없는 범위에서 N/A 처리하되, 이후 merge에는 최신 head에 대한 별도 승인과 검증이 필요합니다. 원격 PR CI/release/publication/tag 검증은 항목별 적용 여부와 근거를 기록하고 local CI-equivalent 검증을 원격 결과로 계산하지 않습니다.

교훈 문서가 격리된 feature branch에만 커밋된 경우 CG-17A 통합 checkout 반영과 CG-18 GNO 색인은 통합 권한을 얻을 때까지 PENDING입니다. CG-17B cleanup도 그보다 먼저 실행하지 않고 worktree와 branch를 보존합니다. 이 세 항목을 완료나 N/A로 표시하지 않습니다. 마지막 보고에는 checklist에서 계산한 `Required checks: X/Y; N/A: N; Blocked: B`, 미완료 ID, worktree/branch 상태와 workflow 상태를 포함합니다. 교훈 문서 커밋 뒤 기준 branch 대비 `git diff --check`와 `git status --short --branch`를 다시 실행해 최종 diff와 보존 상태를 기록합니다. 이 승인 범위의 최종 상태는 CG-17A/B/18의 근거가 충족되지 않는 한 PENDING입니다.

**Step DoD: PENDING** — 최종 체크리스트 계산과 사용자용 DoD 보고 전입니다.

수정 전 계획의 호출부/API 검토 이력은 아래에 적은 최초 결과이며 현재 계획의 검토 완료를 뜻하지 않습니다. `plan-api-r3`는 사용량 한도로 완료하지 못했고 `plan-caller-r3`는 위 P2를 보고했습니다. 사용자가 추가 범위를 승인해 이 초안에 반영했지만, 현재 계획본을 대상으로 하는 여섯 관점의 새 검토와 주 세션 통합은 모두 PENDING입니다.

## 단계 3-P: 위험 예측

| 영역 | 위험 | 발생 가능성 | 영향 | 완화책과 근거 |
|---|---|---:|---:|---|
| Kotlin overload 선택 | Reified 확장 함수가 Java member에 가려지거나 잘못된 nullable overload를 선택할 수 있습니다. | 보통 | 보통 | 테스트와 README에 이름 지정 nullable 호출 예제를 넣고, Binder handler 경로에서 `Bindable`을 명시적으로 사용합니다. |
| Spring API 변경 | 반환 nullability나 overload가 가정한 version과 다를 수 있습니다. | 낮음 | 높음 | 계획에서 확인한 Boot 4.1.1/Spring Framework 7.0.9 artifact를 기준으로 모듈에서 컴파일하고 Spring 직접 호출 결과와 비교합니다. |
| runner 생명주기 | `prepare`는 refresh 전 상태이며 즉시·지연 초기화와 실패가 서로 다른 시점에 나타납니다. | 보통 | 높음 | `prepare` callback에서 `isActive == false`를 확인하고 원본 context를 직접 refresh한 뒤 활성 상태와 공급자 실행을 확인합니다. `run`의 `startupFailure`, 지연 `getBean`의 `BeanCreationException`, 사용자 지정 함수의 `startupFailure`도 각각 검사합니다. |
| AutoConfigurations 호출 형태 | Java vararg API를 단일 helper로 잘못 일반화하거나 여러 설정 class 호출의 의미를 바꿀 수 있습니다. | 낮음 | 보통 | reified helper는 단일 타입 계약을 유지하고 Spring vararg 호출은 회귀 예제에 둡니다. `@AutoConfiguration` fixture를 `ApplicationContextRunner`에서 컴파일·실행해 확인합니다. |
| factory 로딩 | 테스트 resource가 기본 factory 목록을 오염시키거나 예상하지 않은 class를 초기화할 수 있습니다. 정상 반환한 사용자 정의 handler로 인해 일부 구현체만 담긴 목록이 반환될 수도 있습니다. | 보통 | 높음 | 고유 resource location과 test classloader를 사용하고 callback 인자, 성공·실패 factory가 섞인 SPI의 부분 결과를 확인합니다. 모든 provider가 필요하면 기본 handler나 예외를 던지는 handler를 사용하도록 KDoc과 테스트를 맞춥니다. classpath를 신뢰한다는 가정과 helper가 class를 검증·격리하지 않는 경계도 설명합니다. |
| configuration class 신뢰 경계 | `register<T>()`, `withUserConfiguration<T>()`, `autoConfigurationsOf<T>()`가 context refresh나 configuration 처리 중 사용자 코드를 실행할 수 있습니다. | 낮음 | 높음 | helper가 class 검증·sandbox·격리를 하지 않는다고 문서화하고 신뢰된 애플리케이션·테스트 class만 전달합니다. Spring 생명주기와 실패 계약을 보존합니다. |
| 공개 API 호환성 | public inline 확장 함수가 모듈 API를 늘리고 signature에 Spring type을 노출합니다. | 보통 | 보통 | 타입 bound와 signature를 승인 설계로 제한하고 compile-only 의존성을 유지합니다. 소비자 artifact를 문서화하고 모든 API 테스트가 컴파일되는지 확인합니다. |
| 워크스페이스 상태 보존 | 구현 시작 시 기존 사용자 파일이 있을 수 있습니다. | 낮음 | 높음 | 기준 상태를 기록하고 승인된 문서 두 개만 허용합니다. 예상 밖 경로가 있으면 중단하며 clean/reset/stash나 덮어쓰기를 실행하지 않습니다. |
| 커버리지 및 CI 범위 주장 | Kover나 workflow path-filter 범위를 잘못 해석할 수 있습니다. | 낮음 | 보통 | XML을 생성해 확인하고 현재 CI/Nightly YAML과 dependency report를 직접 조사합니다. 실행되지 않은 범위는 명시합니다. |

## 설계-계획 추적성

| 승인된 설계 요구사항 | 작업 | 증거 |
|---|---:|---|
| Binder의 `bind`·`bindOrCreate` overload와 `Bindable` handler 경로 | 1 | `BinderExtensionsTest` 대상 테스트 filter |
| BootstrapContext의 null 허용 반환·기본값·지연 평가·`Throwable` 계약 | 2 | `BootstrapContextExtensionsTest` 대상 테스트 filter |
| SpringFactoriesLoader resolver의 null/non-null 반환, 예외 전달, failure handler, resource 및 classloader 보존 | 3 | 격리 resource와 `SpringFactoriesLoaderExtensionsTest` |
| `AnnotationConfigRegistry.register` 확장 함수 | 4 | registry 테스트 |
| 기존 find 의미와 구분되는 반복 애너테이션 get 의미 | 4 | 수정한 `AnnotationExtensionsTest` |
| TypeDescriptor의 raw class 의미와 AnnotationMetadata introspection | 5 | 두 대상 테스트 filter |
| SpEL typed overload 네 개, nullable root/result, 접근 정책 보존 | 6 | `ExpressionExtensionsTest` |
| 구체 runner 세 종류, 확장 아홉 개, named nullable `name = null` overload 선택, supplier/customizer 생명주기와 실패 | 7 | `ApplicationContextRunnerExtensionsTest` |
| 호출자가 복사할 수 있는 전체 확장 import와 runner 세 종류의 nullable 예제 | 8 | 양쪽 README 대응 및 import 검토 |
| `AutoConfigurations.of(Class<?>...)` 단일 class helper와 기존 vararg 보존 | 7 | `autoConfigurationsHelperUsesOneReifiedConfigurationClass`와 `ApplicationContextRunnerExtensionsTest` |
| factory/configuration class의 신뢰 경계와 Spring 실행·실패 계약 | 3, 4, 7, 9 | 관련 KDoc와 Security 검토 read-back |
| 영어·한국어 문서와 소비자 artifact 안내 | 8 | README 대응 검토와 resolve된 dependency report |
| 의존성·버전·모듈·CI·AWS 변경 없음 | 전체 | git diff 및 dependency/CI read-back |
| 테스트, Detekt, Kover, build와 최종 diff 근거 | 9 | exact command output 및 report path |

## 되돌리기 및 복구

- 승인된 design-and-plan commit은 모든 implementation commit과 분리해 유지합니다.
- 작업에서 regression이 생기면 `git status`와 implementation commit hash를 기록한 다음 `git revert --no-edit <implementation-commit>`으로 문제를 일으킨 구현 commit만 되돌립니다. 계획 commit은 되돌리지 않고 기존 사용자 상태도 건드리지 않습니다.
- 되돌린 뒤 영향을 받은 테스트 필터와 `git diff --check`를 실행하고, 계속하기 전 `git status`와 `git show --stat`을 확인합니다.
- regression이 여러 구현 commit에 걸쳐 있으면 최신 commit부터 하나씩 되돌리고 각 단계 후 상태와 영향을 받은 테스트를 확인합니다. reset, clean, broad checkout, force push는 사용하지 않습니다.
- 되돌린 뒤 알려진 기준선이 복원되지 않거나 관련 없는 파일에 영향을 줄 수 있으면 rollback 범위를 넓히지 말고 근거를 구체적으로 보고합니다.

## 계획 품질 및 검토 기록

이 절의 첫 표는 `autoConfigurationsOf<T>()` 추가 전 계획에 대한 최초 Step 3-R 이력입니다. 그 뒤의 PENDING 표시는 이번 계획을 A-04 검토에 넘기기 전의 상태입니다. 실제 진행 결과는 workflow receipt와 통합 보고에 기록합니다. 계획 hash를 고정한 뒤 내용을 바꾸면 영향받은 검토와 작성자 게이트를 다시 수행합니다.

| 관점 | 수정 전 결과 | 이번 revision에 반영한 조치 | 수정본 재검토 |
|---|---|---|---|
| Performance | P0=0, P1=0, P2=0, P3=0 — PASS | 직접 위임 구조와 시그니처 검사는 유지합니다. 작업 9에 검증별 실행 시간 상한과 중단 절차를 추가합니다. | 수정 전 이력; 최신 계획 검토 전 |
| Stability | P0=0, P1=0, P2=1, P3=1 — PARTIAL | 작업 3에 handler 미지정 시 Spring 기본 예외와 원인을 보존하는 회귀 검사를 추가하고, 작업 9에 시간 초과와 상태 보존 절차를 추가합니다. | 수정 전 이력; 최신 계획 검토 전 |
| Security | P0=0, P1=0, P2=1, P3=1 — REQUEST CHANGES | 안정성 지적과 중복되는 기본 failure-handler 검사를 한 번만 추가합니다. 작업 3의 fixture 경로를 고유하게 만들고 classloader에서 resource가 하나만 보이는지 확인합니다. | 수정 전 이력; 최신 계획 검토 전 |
| Operations | P0=0, P1=4, P2=0, P3=0 — FAIL | #1823의 실제 milestone을 2.1.0으로 바로잡고, 작업 10–11에 A-09 lesson과 PR 없는 closeout을 추가합니다. 최종 검사와 N/A/PENDING 계산도 명시합니다. 이 이력은 최신 검토 결과로 취급하지 않습니다. | 수정 전 이력; 최신 계획 검토 전 |
| Developer/API | P0=0, P1=0, P2=0, P3=0 — PASS | 승인된 API 시그니처와 범위를 유지합니다. | 수정 전 이력; 최신 계획 검토 전 |
| User/caller | P0=0, P1=0, P2=3, P3=0 — REQUEST CHANGES | 작업 8에 단일 클래스와 vararg의 경계, starter와 Boot BOM 의존성 안내, get/find 애너테이션 조회 차이를 추가합니다. | 수정 전 이력; 최신 계획 검토 전 |
| User/caller 추가 발견 | P0=0, P1=0, P2=1, P3=0 — REQUEST CHANGES | `AutoConfigurations.of` 단일 타입 사용을 추가하고 집중 테스트, 두 README, `spring-boot-autoconfigure` 의존성 표를 보완합니다. 여러 클래스는 기존 Spring vararg API를 유지합니다. | 이전 지적을 계획에 반영; 최신 호출부 관점 재검토 필요 |
| User/caller runner/import 재발견 | P0=0, P1=0, P2=1, P3=1 — REQUEST CHANGES | 세 concrete type 모두 `name = null` named argument로 컴파일 확인하고, §5와 양쪽 README에 해당 함수의 전체 import를 적도록 작업 7–8과 추적성 표를 구체화합니다. | 이전 지적을 계획에 반영; A-03 호출부 검토 PASS; A-04 대기 |
| User/caller compile 오류 | P0=0, P1=0, P2=1, P3=0 — REQUEST CHANGES | 설계 §2.6과 작업 7에서 `ApplicationContextRunner` 타입 이름에 instance method를 호출하던 예제를 `ApplicationContextRunner()` 인스턴스 호출로 고칩니다. | 이전 지적을 계획에 반영; A-03 호출부 검토 PASS; A-04 대기 |
| Security callback partial result | P0=0, P1=0, P2=1, P3=0 — REQUEST CHANGES | Spring의 caller-supplied `FailureHandler`가 정상 반환하면 실패한 factory를 제외한 부분 `List<T>`가 반환될 수 있음을 설계 §2.3/§6/§8, 작업 3의 fixture·테스트·KDoc, 위험·수용 기준에 반영합니다. 필수 factory 전체가 필요한 호출에는 기본 handler 또는 예외를 던지는 handler를 요구합니다. | 이전 지적을 설계와 계획에 반영; A-03 보안·개발/API 검토 PASS; A-04 대기 |
| Stability resolver callback semantics | P0=0, P1=0, P2=1, P3=0 — REQUEST CHANGES | Spring 7.0.9 계약을 근거로 resolver 예외의 failure handler 전달과 nullable 반환값의 constructor 전달을 설계 §2.3/§8, 작업 3의 fixture·테스트·KDoc에 반영합니다. | 이전 지적을 설계와 계획에 반영; A-03 안정성 검토 PASS; A-04 대기 |

최초 리뷰의 blocker와 문서 누락은 이 revision에서 계획 단계로 반영했습니다. 이전 version의 PASS나 terminology audit 결과를 이번 revision의 증거로 재사용하지 않습니다. 새 여섯 관점 검토, main-session 통합, writer gate, KO 검토를 모두 마치고 latest integrated finding이 P0=0/P1=0일 때까지 A-04는 닫히지 않습니다. source implementation과 작업 0 commit은 그 전까지 시작하지 않습니다.

현재 28-API 설계서의 A-03 Security 검토에서는 사용자 정의 `FailureHandler`가 정상 반환할 때 부분 factory 목록이 나올 수 있다는 P2를, Stability 검토에서는 `ArgumentResolver` 예외와 nullable 반환 계약의 테스트 공백이라는 P2를 확인했습니다. 두 사항의 설계·수용 기준·작업 3 수정은 문서에 반영했습니다. A-03 검토는 통과했고, A-04는 정확한 최신 계획서 기준의 검토가 끝날 때까지 PENDING입니다.

### SPW-01–05: 실행 계획 문서 게이트

| 게이트 | 결과 | 근거 |
|---|---|---|
| SPW-01 — 독자, 목적, 근거 고정 | PASS | 구현자 대상 한국어 계획으로 승인 설계·기준 commit·Spring Boot 4.1.1/Spring Framework 7.0.9와 live #1823을 대조했습니다. GNO 조사 노트 `gno://bluetape4k-wiki/research/2026-10-09-spring-reified-extension-candidates.md`를 확인했습니다. 최초 조사에서 `AutoConfigurations.of`는 근거가 없어 제외됐으나, 호출부 검토(대상 4개 파일/8개 호출, workspace 107개 Kotlin 파일 검색) 뒤 사용자가 2026-10-10 단일 타입 helper를 승인해 설계·계획에 28번째 API로 반영했습니다. 현재 GNO docs/GitHub 검색은 결과가 없고, `gh`로 #364 CLOSED, #1823 OPEN/enhancement/2.1.0/`debop` 및 기존 27개 범위 본문을 확인했습니다. |
| SPW-02 — 계획 문서 계약 | PASS | 공개 API 28개가 신규 소스 9개와 수정 소스 1개, 신규 테스트 8개와 수정 테스트 1개, focused test filter 9개 및 양쪽 README 변경에 연결됩니다. 작업 0–11 각각에 Action·Expected DoD·Step DoD가 있으며, 미착수 단계는 PENDING입니다. |
| SPW-03 — 한국어 기술 문체 | PASS | 한국어 자연스러움 점검표로 기술적 사실을 고정한 뒤 문장별 번역투·명사화·반복 전환어를 검토했습니다. API·식별자·명령은 그대로 보존하고 문맥 용어 감사는 결과가 없습니다. |
| SPW-04 — 기술 의미와 추적성 | PASS | 승인 설계의 28개 API, 파일·테스트·README 대응표와 이전 리뷰 발견을 대조했습니다. Binder 경계는 작업 1, factory 경계는 작업 3, runner와 auto-configuration은 작업 7, README는 작업 8, 통합 검증·최종 리뷰·lesson은 작업 9–11에 연결됩니다. |
| SPW-05 — 최종 읽기와 작성자 DoD | PASS | `glow -w 150` 전체 출력 1,914줄을 렌더링해 표·목록·코드 울타리와 결말을 확인했습니다. 코드 울타리 표시 62개가 짝을 이루고, 자리표시자·끝 공백·범위 물결표가 없습니다. 미추적 문서에는 `git diff --check`를 완료 증거로 사용하지 않으며 stage 후 `git diff --cached --check`를 실행합니다. |

### KO-01–07: 실행 계획 한국어 검토

| 게이트 | 결과 | 근거 |
|---|---|---|
| KO-01 — 의미를 바꾸는 근거 고정 | PASS | 설계 SHA `4f649610fcc7db3f88a1e690d62419e807f79448d133798a6d2751e17d5df809`, 기준 commit, 28개 시그니처, 대상 경로와 현재 issue 메타데이터를 대조했습니다. 계획 경로와 조사 근거 문구만 바로잡았고 API 계약은 유지했습니다. |
| KO-02 — 근거 없는 강조 제거 | PASS | 효과를 과장하는 표현 없이 각 단계의 실행 명령, 기대 결과, 실패 조건과 중단 지점을 구체적으로 적었습니다. |
| KO-03 — 번역투와 정형 문장 제거 | PASS | 제목·표·본문을 자연스러움 점검표 기준으로 읽고 불필요한 영어식 어순, 명사화와 반복 전환어를 고쳤습니다. 기술 용어와 코드 토큰은 유지했습니다. |
| KO-04 — 문체와 용어 검증 | PASS | 같은 개념에 `확장 함수`, `공급자`, `생명주기`, `테스트 필터`를 일관되게 사용하고, Spring API 이름·식별자·명령은 원문 표기를 유지했습니다. |
| KO-05 — 비유와 어조 제한 | PASS | 홍보성 수식이나 구현 조건을 흐리는 비유 없이 작업과 검증을 엔지니어 대상의 직접적인 기술 문체로 정리했습니다. |
| KO-06 — 독자에게 보이는 표면 검증 | PASS | 렌더링된 계획의 제목, 표, 경로, API 예제와 양쪽 README 대응 범위를 읽었습니다. 파일 목록, 작업 순서와 실행 예제 사이에 누락된 연결은 없습니다. |
| KO-07 — 문맥 기반 용어 감사 | PASS | `node /Users/debop/.codex/skills/bluetape-writer/scripts/audit-korean-terms.mjs --json docs/superpowers/plans/2026-10-09-spring-reified-extensions-plan.md` 결과는 `findings: []`입니다. |

### A-04 통합 결과 기록 위치

A-04 여섯 관점의 통합 finding, disposition, P0/P1 결과와 남은 위험은 실행 중 workflow receipt와 통합 보고에 기록합니다. 통합 보고에는 별도의 SPW-01–05 및 KO-01–07 검토를 적용합니다. A-04 검토가 시작된 뒤에는 이 계획 파일에 결과를 덧붙여 검토 hash를 바꾸지 않습니다.

### 자체 검토 항목

- 승인 설계의 모든 API가 정확한 작업, 소스·테스트 파일, focused test filter에 연결되는지 확인합니다.
- 작업 0–11의 의존 순서, test-first 절차, 명령, 기대 증거와 커밋 경계가 실행 가능한지 확인합니다.
- 구현 검토, lesson, PR 없는 closeout과 미완료 통합 게이트가 계획에 기록되는지 확인합니다.
- Spring Boot 4.1.1 runner lifecycle, factory 오류 처리, classloader resource 의미가 공식 source와 일치하는지 확인합니다.
- README 범위 제한, annotation get/find 의미, starter/BOM 안내가 양쪽 언어에 대응하는지 확인합니다.
- test, Detekt, Kover, 의존성, CI/Nightly, 시간 제한, 최종 build 증거와 공백 보고가 계획되어 있는지 확인합니다.
- rollback, 기존 변경 보존, PR/publish 권한 경계가 명확한지 확인합니다.
- 수정본의 한국어 용어 감사, 자리표시자·코드 울타리·공백 검증을 실행했는지 확인합니다.

## 실행 상태

이 계획을 고정할 때 A-03 설계 검토는 PASS였고 A-04 계획 검토는 PENDING이었습니다. 사용자는 최초 설계·계획과 `$executing-plans` inline 실행을 승인했고 2026-10-10에 `autoConfigurationsOf<T>()` 추가 범위도 승인했습니다. live issue 확인, 호출부·보안·안정성·운영 지적, 28개 API 추적성, A-08–A-11 closeout을 반영했습니다. 이후 A-04 진행 결과는 workflow receipt에 기록하며 exact-hash 검토가 시작된 뒤에는 이 파일을 수정하지 않습니다. A-04와 작성자 게이트를 통과하기 전에는 소스 구현이나 작업 0 커밋을 시작하지 않습니다.
