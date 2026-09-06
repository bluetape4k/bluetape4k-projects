# #1640 JPA·AsyncTaskExecutor 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** catalog 기반 Boot 4.1.1에서 실제 JPA와 Projects executor의 결합 및 종료 계약을 검증한다.

**Architecture:** Hibernate Lettuce 테스트에 core를 test-only로 연결한다. 좁은 JPA slice 8개로 기본 executor·caller back-off·Boot property를 분리하고, 제품 코드는 변경하지 않는다.

**Tech Stack:** 기존 catalog의 Spring Boot, Hibernate, H2, HikariCP, Kotlin, JUnit, bluetape4k-assertions.

---

## 기준과 실행 경계

### 2026-09-06 승인 범위 추가

사용자의 `추가해` 지시로 root `build.gradle.kts`의 Jakarta Persistence 관리만
기존 중앙 catalog의 `v31`에서 `v32`로 정렬한다. 아래 최초 범위와 원본 명세의
전역 변경 제외 조건에 대한 한정된 예외다. catalog 원본·ref·버전 상수·제품 API는 변경하지 않는다.

- [x] 선행 증거: JPA fixture 8개가 `FindOption` 누락으로 실패했고 production runtime도 3.2.0 → 3.1.0으로 낮아졌다. [실행 기록](../../review/2026-09-06-issue-1640-execution-checkpoint.md)을 보존한다.
- [ ] root alias 한 줄을 정렬하고 같은 fixture 8개를 재실행한다. 실패하면 원인을 진단하며 assertion을 약화하지 않는다.
- [ ] compile/runtime/test compile/test runtime의 실제 Jakarta 버전과 기존 Hibernate 계열 override를 확인한다. 기존 3.2.0 override 제거는 이번 변경에 포함하지 않는다.
- [ ] `examples/jpa-querydsl-demo`의 직접 v31 선언은 편집하지 않고 4개 classpath에서 실제 선택 버전을 확인한다. 3.1.0이 남으면 전역 정렬 검증은 실패이며 새로운 force로 숨기지 않는다.
- [ ] 대표 발행 모듈 Hibernate Lettuce의 `generatePomFileForBluetape4kPublication` 결과에서 Jakarta Persistence dependencyManagement가 3.2.0인지 확인한다.
- [ ] core → Hibernate Lettuce → demo 전체 테스트를 순차 실행하고 정적 검사를 수행한다.
- [ ] 전역 영향 검증으로 전체 모듈 build를 테스트 제외 상태로 먼저 실행한다. 성공은 전체 테스트 통과와 구분한다. 필요한 후속 전체 테스트 또는 exact-head Full Nightly 증거가 없으면 전역 검증은 PENDING이다. workflow dispatch는 별도 승인 없이 실행하지 않는다.
- [ ] 최종 리뷰·lesson·PR 조건은 기존 계획을 유지한다. 실패한 전역 검증은 로그와 모듈을 기록하며 무관한 제품 수정으로 범위를 넓히지 않는다.

위험: 공통 dependencyManagement는 모든 하위 모듈과 발행 metadata에 영향을 준다.
복구는 이 한 줄의 역방향 diff로 한정하며, 복구 시 JPA 실패가 재발함을 명시한다.
테스트 전용 force 재도입은 production 불일치를 남기므로 채택하지 않는다.
문서 검증은 승인 문구·실패 로그·현재 root 및 catalog와 대조하고, 한국어 기술 문체를 유지한다.

전역 build는 다음 명령을 단독 실행한다. 종료코드는 별도 파일에 저장하고
실패 시 그대로 반환한다. timeout process group 진단은 같은 로그에 보존한다.

```bash
python3 /tmp/issue-1640-bounded-run.py 1800 ./gradlew --no-daemon build -x test --no-parallel > /tmp/issue-1640-global-build.log 2>&1
result=$?
printf '%s\n' "$result" > /tmp/issue-1640-global-build.exit
test "$result" -eq 0 || exit "$result"
```

전체 build 실패 시 최초 실패 task·오류·dependency graph·종료코드를 실행 기록에 남기고
PR 진행을 중단한다. 관련 모듈의 추가 읽기 전용 진단만 수행한다.
직접 v31 소비자는 다음 순차 명령으로 검증한다.

```bash
for configuration in compileClasspath runtimeClasspath testCompileClasspath testRuntimeClasspath; do
  python3 /tmp/issue-1640-bounded-run.py 600 ./gradlew --no-daemon :bluetape4k-examples-jpa-querydsl-demo:dependencyInsight --configuration "$configuration" --dependency jakarta.persistence || exit $?
done
```

대표 POM은 같은 실행기로
`:bluetape4k-spring-boot-hibernate-lettuce:generatePomFileForBluetape4kPublication`을 실행하고
`spring-boot/hibernate-lettuce/build/publications/Bluetape4k/pom-default.xml`을 확인한다.
실제 task 또는 publication 이름이 다르면 task 목록에서 확인한 이름으로 기록한다.

- 승인 명세: [설계](../specs/2026-09-06-issue-1640-jpa-executor-contracts-design.md), SHA256 `57d0df0f6d1c6c4a21b38b1e73f7ea53ccb0a673c4508d81697cdc6de2ba1825`.
- 명세 커밋: `f8c87c4d58b62cf6142aff1275784a440045c8dc`; 사용자의 후속 `승인`으로 작성된 명세 확인 완료.
- 작업 위치: `/Users/debop/.config/superpowers/worktrees/bluetape4k-projects/test-issue-1640-jpa-executor-contracts`.
- PR: `bluetape4k/bluetape4k-projects`, base `develop`, head `test/issue-1640-jpa-executor-contracts`. 생성은 승인 범위, 병합은 별도 승인.
- 주 담당자가 순차 구현한다. 리뷰만 읽기 전용으로 위임하며 heavy command 동시 실행은 1개다.
- Kotlin·testing·Spring Boot 패턴을 적용한다. 비교·예외 assertion은 프로젝트 helper를 사용한다. Future 직접 제출은 검증 대상 API이므로 일반 동시성 stress helper로 바꾸지 않는다.
- 자동 구성 FQCN은 로컬 4.1.1 JAR에서 확인했다. repository 자동 구성은 `DataJpaRepositoriesAutoConfiguration`이며, bootstrap property의 정확한 키는 `spring.data.jpa.repositories.bootstrap-mode`다.
- GNO의 #1640과 현재 GitHub 이슈를 대조했다. docs 검색에는 관련 결과가 없었다.
- 테스트 전용 의존성 외 새 의존성, 전역 catalog, 제품 API, Ignite2, gRPC, cache 리팩터링은 제외한다.

## 파일별 책임

| 파일 | 작업 |
|---|---|
| `build.gradle.kts:791` | `dependency(rootBt4k.jakarta.persistence.v31.get().toString())`를 `dependency(rootBt4k.jakarta.persistence.v32.get().toString())`로 교체하고 diff가 이 한 줄인지 확인 |
| `spring-boot/hibernate-lettuce/build.gradle.kts` | 구버전 강제 제거, core testImplementation 1개 추가 |
| `spring-boot/hibernate-lettuce-demo/build.gradle.kts` | 같은 구버전 강제 제거 |
| `spring-boot/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/jpa/executor/JpaExecutorContractTest.kt` | 아래 전체 fixture와 3+2+3 parameterized 사례 |
| `docs/review/2026-09-06-issue-1640-plan-review.md` | 계획 관점별 검토·지적 처리 |
| `docs/review/2026-09-06-issue-1640-code-review.md` | 최종 diff·검증 증거 |
| `docs/lessons/2026-09-06-issue-1640-jpa-executor-contracts.md` | 버전 RED 구분, 시간 제한, 리뷰 수정의 재발 방지 |

기존 테스트의 구버전 우회가 정렬 후 실패한 경우에만 해당 fixture를 최소 수정한다. 현재는 수정 파일로 확정하지 않는다. 제품 변경이 필요하면 중단하고 설계 범위를 재검토한다.

## 작업 1 — 검증 실행과 버전 RED 고정

복잡도: 낮음. 선행: 명세 승인과 계획 리뷰. 산출물: 실행 로그, 실제 graph와 예상 실패.

- [ ] `git status --porcelain=v1`, `git diff develop...HEAD --stat`로 격리를 확인한다. 세 모듈의 build 파일과 catalog ref는 현재 값을 로그에 남긴다.
- [ ] 아래 임시 실행기를 `/tmp/issue-1640-bounded-run.py`에 apply_patch로 준비한다. 제품이나 저장소 도구로 추가하지 않는다. 기존 저장소에는 적합한 일반 프로세스 제한 helper가 없고 macOS 환경에 timeout/gtimeout이 없음을 확인했다.
- [ ] 실행기 자체는 짧은 성공·실패·timeout 프로세스로 종료코드 0·7·124를 확인한다. timeout은 진단 후 해당 process group만 종료해야 한다.

```python
import os, signal, subprocess, sys
limit = int(sys.argv[1])
process = subprocess.Popen(sys.argv[2:], start_new_session=True)
try:
    sys.exit(process.wait(timeout=limit))
except subprocess.TimeoutExpired:
    print("TIMEOUT: owned process group", process.pid, flush=True)
    try:
        subprocess.run(["ps", "-g", str(process.pid), "-o", "pid,ppid,pgid,etime,comm"], timeout=5)
    except subprocess.TimeoutExpired:
        pass
    for sig in (signal.SIGQUIT, signal.SIGTERM, signal.SIGKILL):
        try:
            os.killpg(process.pid, sig)
        except ProcessLookupError:
            break
        try:
            process.wait(timeout=5)
        except subprocess.TimeoutExpired:
            pass
    process.wait(timeout=5)
    sys.exit(124)
```

- [ ] 다음 graph 명령을 실행하고 `spring-boot:4.1.1 -> 4.0.3`와 rule 선택 이유를 저장한다.

```bash
python3 /tmp/issue-1640-bounded-run.py 600 ./gradlew --no-daemon :bluetape4k-spring-boot-hibernate-lettuce:dependencyInsight --configuration testRuntimeClasspath --dependency org.springframework.boot:spring-boot
```

- [ ] 신규 테스트에 Boot 버전 assertion을 먼저 추가한다. `org.springframework.boot.SpringBootVersion.getVersion() shouldBeEqualTo "4.1.1"`이 현재 4.0.3에서 실패하는 것을 확인한다. 이는 의존성 계약 RED이며 순환 의존의 pre-fix 재현으로 부르지 않는다.

```kotlin
package io.bluetape4k.spring.boot.autoconfigure.jpa.executor

import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.springframework.boot.SpringBootVersion

class JpaExecutorContractTest {
    @Test
    fun `catalog의 Boot 버전으로 테스트한다`() {
        SpringBootVersion.getVersion() shouldBeEqualTo "4.1.1"
    }
}
```

- [ ] 그 후 아래 최종 테스트를 적용한다. 임시 버전 assertion은 graph 검증으로 대체하여 최종 테스트에 버전 상수를 남기지 않는다.

## 작업 2 — 최소 JPA fixture와 matrix

복잡도: 중간. 선행: 작업 1. 파일: 위 신규 Kotlin 파일. 제품 코드 변경 없음.

- [ ] test-only core 연결을 먼저 추가하여 fixture가 제품 executor를 직접 사용하게 한다.

```kotlin
testImplementation(project(":bluetape4k-spring-boot-core"))
```

- [ ] 다음 전체 파일을 작성한다. 추가 context는 만들지 않고 parameterized 사례 수는 8개로 유지한다.

```kotlin
package io.bluetape4k.spring.boot.autoconfigure.jpa.executor

import com.zaxxer.hikari.HikariDataSource
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeLessOrEqualTo
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.codec.Base58
import io.bluetape4k.spring.virtualthread.VirtualThreadAutoConfiguration
import jakarta.persistence.Entity
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.autoconfigure.AutoConfigurationPackage
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.core.task.TaskRejectedException
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

@Execution(ExecutionMode.SAME_THREAD)
@Timeout(value = 60, threadMode = Timeout.ThreadMode.SAME_THREAD)
class JpaExecutorContractTest {

    @ParameterizedTest
    @ValueSource(strings = ["unset", "false", "true"])
    fun `Projects 명시 등록은 Boot property와 무관하게 가상 스레드를 제공한다`(property: String) {
        verifyContext(
            runner(property).withUserConfiguration(VirtualThreadAutoConfiguration::class.java),
            expectedVirtual = true,
            expectedBeanName = "virtualThreadTaskExecutor",
            persist = property == "unset",
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["false", "true"])
    fun `EMF 의존 caller executor가 있으면 Projects 기본 bean은 생성되지 않는다`(property: String) {
        verifyContext(
            runner(property).withUserConfiguration(
                EmfDependentExecutorConfiguration::class.java,
                VirtualThreadAutoConfiguration::class.java,
            ),
            expectedVirtual = false,
            expectedBeanName = "callerExecutor",
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["unset", "false", "true"])
    fun `Boot 단독 경로는 property에 따라 실행 스레드를 선택한다`(property: String) {
        verifyContext(
            runner(property).withConfiguration(
                AutoConfigurations.of(TaskExecutionAutoConfiguration::class.java)
            ),
            expectedVirtual = property == "true",
            expectedBeanName = "applicationTaskExecutor",
        )
    }

    private fun runner(property: String): ApplicationContextRunner {
        val base = ApplicationContextRunner()
            .withUserConfiguration(JpaFixtureConfiguration::class.java)
            .withConfiguration(
                AutoConfigurations.of(
                    DataSourceAutoConfiguration::class.java,
                    HibernateJpaAutoConfiguration::class.java,
                    DataJpaRepositoriesAutoConfiguration::class.java,
                )
            )
            .withPropertyValues(
                "spring.datasource.url=jdbc:h2:mem:executor_" + Base58.randomString(12),
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.type=com.zaxxer.hikari.HikariDataSource",
                "spring.datasource.hikari.maximum-pool-size=1",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.open-in-view=false",
                "spring.data.jpa.repositories.bootstrap-mode=default",
            )
        return if (property == "unset") base else
            base.withPropertyValues("spring.threads.virtual.enabled=" + property)
    }

    /** context가 소유한 자원만 보관하고 runner 종료 후 실제 닫힘을 확인한다. */
    private fun verifyContext(
        runner: ApplicationContextRunner,
        expectedVirtual: Boolean,
        expectedBeanName: String,
        persist: Boolean = false,
    ) {
        var emf: EntityManagerFactory? = null
        var dataSource: HikariDataSource? = null
        var executor: AsyncTaskExecutor? = null
        var closeStarted: Long? = null
        var primaryFailure: Throwable? = null
        val started = System.nanoTime()
        try {
            runner.run { context ->
                context.startupFailure?.let { failure ->
                    throw AssertionError("JPA executor context 시작 실패: " + failure, failure)
                }
                try {
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) shouldBeLessOrEqualTo 30_000L
                    emf = context.getBean(EntityManagerFactory::class.java)
                    dataSource = context.getBean(HikariDataSource::class.java)
                    val executors = context.getBeansOfType(AsyncTaskExecutor::class.java)
                    executors shouldHaveSize 1
                    val actual = executors.getValue(expectedBeanName)
                    executor = actual
                    actual shouldBeSameInstanceAs context.getBean(expectedBeanName)
                    if (expectedBeanName == "callerExecutor") {
                        actual shouldBeSameInstanceAs context.getBean(EmfDependentExecutorConfiguration::class.java).ownedExecutor
                    }
                    if (expectedBeanName != "virtualThreadTaskExecutor") {
                        context.containsBean("virtualThreadTaskExecutor").shouldBeFalse()
                    }
                    if (expectedBeanName == "applicationTaskExecutor" && !expectedVirtual) {
                        actual.shouldBeInstanceOf<ThreadPoolTaskExecutor>()
                    } else {
                        actual.shouldBeInstanceOf<SimpleAsyncTaskExecutor>()
                    }
                    val repository = context.getBean(ExecutorProbeRepository::class.java)
                    if (persist) {
                        val saved = repository.saveAndFlush(ExecutorProbe(name = "executor-probe"))
                        val id = saved.id.shouldNotBeNull()
                        val loaded = repository.findById(id).orElseThrow()
                        loaded.id shouldBeEqualTo id
                        loaded.name shouldBeEqualTo "executor-probe"
                    }
                    val future = actual.submit(Callable { Thread.currentThread().isVirtual })
                    try {
                        future.get(5, TimeUnit.SECONDS) shouldBeEqualTo expectedVirtual
                    } finally {
                        if (!future.isDone) future.cancel(true)
                    }
                } finally {
                    closeStarted = System.nanoTime()
                }
            }
        } catch (failure: Throwable) {
            primaryFailure = failure
            throw failure
        } finally {
            try {
                closeStarted?.let {
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - it) shouldBeLessOrEqualTo 10_000L
                }
                emf?.isOpen?.shouldBeFalse()
                dataSource?.isClosed?.shouldBeTrue()
                executor?.let { closed ->
                    assertFailsWith<TaskRejectedException> { closed.submit(Callable { Unit }) }
                }
            } catch (cleanupFailure: Throwable) {
                val original = primaryFailure
                if (original == null) throw cleanupFailure
                original.addSuppressed(cleanupFailure)
            }
        }
    }

    @Configuration(proxyBeanMethods = false)
    @AutoConfigurationPackage(basePackageClasses = [ExecutorProbe::class])
    @EntityScan(basePackageClasses = [ExecutorProbe::class])
    class JpaFixtureConfiguration

    /** upstream #50801과 같이 executor 설정 자체가 실제 EMF에 의존한다. */
    @Configuration(proxyBeanMethods = false)
    class EmfDependentExecutorConfiguration(val entityManagerFactory: EntityManagerFactory) {
        val ownedExecutor = SimpleAsyncTaskExecutor()

        @Bean
        fun callerExecutor(): SimpleAsyncTaskExecutor = ownedExecutor
    }
}

@Entity
open class ExecutorProbe(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null,
    open var name: String = "",
)

interface ExecutorProbeRepository : JpaRepository<ExecutorProbe, Long>
```

- [ ] IDE 진단이 사용 가능하면 실행한다. 대상 worktree에 연결된 IDE가 없으면 소스 import 검토와 `compileTestKotlin`로 대체하고 도구 부재를 기록한다.
- [ ] 단독 fixture를 10분 상한으로 실행한다. 클래스 누락·entity scan·JDBC 오류는 fixture 실패로 분류하고 원인을 해결한다. `BeanCurrentlyInCreationException`이 발생하면 원인 사슬과 bean 경로를 보존한다.
- [ ] 명령별 stdout/stderr는 `/tmp/issue-1640-<단계>.log`에 저장한다. 시작 실패의 bean 경로는 아래 XML과 로그에서 추출한다. timeout 시 실행기가 출력한 process group ID로 `ps -g <해당 ID> -o pid,ppid,pgid,etime,comm`를 다시 실행해 자식 JVM 부재를 확인한다. 남아 있으면 종료 검증은 실패이며 소유 관계를 확인한 해당 그룹만 정리한다.

```bash
rg -n 'BeanCurrentlyInCreationException|Caused by|Error creating bean' spring-boot/hibernate-lettuce/build/test-results/test /tmp/issue-1640-fixture.log
```
- [ ] 개발 중 음성 대조를 각각 하나씩 적용하고 동일 단독 명령을 실행한다: 기본 bean 이름을 잘못된 이름으로 변경, expectedVirtual 반전, 닫힌 EMF에 isOpen=true assertion. 각 실패를 관찰한 즉시 원복한다. 최종 버전에서 다시 8개 통과를 확인한다. 이 검증은 assertion 감지 능력이며 upstream 4.1.0 재현이 아니다.

```bash
python3 /tmp/issue-1640-bounded-run.py 600 ./gradlew --no-daemon :bluetape4k-spring-boot-hibernate-lettuce:cleanTest :bluetape4k-spring-boot-hibernate-lettuce:test --tests '*JpaExecutorContractTest' --no-build-cache
```

## 작업 3 — 두 모듈의 catalog 정렬

복잡도: 중간. 선행: 버전 RED 기록. 작업 2의 fixture는 이후 정렬 graph에서 다시 검증한다.

- [ ] 두 build 파일에서 `configurations.matching { it.name.startsWith("test") }.configureEach` 전체 블록과 바로 앞의 오래된 강제 설명 주석을 삭제한다. 다른 configuration·BOM·의존성은 보존한다.
- [ ] 최종 블록은 다음과 같이 이어져야 한다. Hibernate Lettuce의 testImplementation 추가 외에는 기존 dependencies 본문을 유지한다.

```kotlin
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    // 기존 platform과 의존성 선언은 그대로 유지한다.
```

- [ ] 두 모듈 × 4개 classpath의 전체 dependencies report를 아래 8회 순차 명령으로 수집한다. Boot·Framework·Hibernate·Spring Data·Jakarta Persistence 선택 버전을 해당 ref catalog/BOM과 대조한다. 선택 이유가 불명확한 충돌에만 작업 1의 dependencyInsight 명령에서 해당 configuration과 dependency를 지정한다. global force나 새 version 상수로 실패를 숨기지 않는다.

```bash
for module in hibernate-lettuce hibernate-lettuce-demo; do
  for configuration in compileClasspath runtimeClasspath testCompileClasspath testRuntimeClasspath; do
    python3 /tmp/issue-1640-bounded-run.py 600 ./gradlew --no-daemon ":bluetape4k-spring-boot-$module:dependencies" --configuration "$configuration" || exit $?
  done
done
```

- [ ] 단독 fixture 8개와 controller lifecycle 3개를 순차 재실행한다. 작업 2의 fixture 명령과 작업 4의 controller 명령을 각각 600초 bounded-run으로 그대로 사용한다. scope 밖 classpath 결함이면 멈추고 실제 원인과 필요한 범위를 보고한다.
- [ ] `git diff --check` 후 이 두 build 파일과 신규 테스트만 Korean Lore 커밋한다. 실패 상태는 커밋 완료로 보고하지 않는다.

## 작업 4 — 전체 검증과 위험 판정

복잡도: 중간. 선행: 작업 2·3의 최종 GREEN. 컨테이너 테스트는 타 작업과 병렬 실행하지 않는다.

- [ ] 기존 controller를 먼저 재검증한다.

```bash
python3 /tmp/issue-1640-bounded-run.py 600 ./gradlew --no-daemon :bluetape4k-spring-boot-core:cleanTest :bluetape4k-spring-boot-core:test --tests '*AbstractVirtualThreadControllerTest' --no-build-cache
```

- [ ] core → Hibernate Lettuce → demo 순서로 각 전체 테스트를 30분 상한에서 실행한다.

```bash
for module in core hibernate-lettuce hibernate-lettuce-demo; do
  python3 /tmp/issue-1640-bounded-run.py 1800 ./gradlew --no-daemon ":bluetape4k-spring-boot-$module:cleanTest" ":bluetape4k-spring-boot-$module:test" --no-build-cache || exit $?
done
```

- [ ] Colima socket 문제가 있으면 `colima status`, `docker context show`, `docker info`를 먼저 확인한다. 비대화형 export 누락일 때만 해당 명령에 기존 TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE를 적용한다. 건강한 VM을 재시작하지 않는다.
- [ ] JUnit XML에서 실제 tests/failures/errors/skipped를 합산하고 expected fixture=8, controller=3과 대조한다. 제외·실패를 통과로 집계하지 않는다.
- [ ] compile와 detekt를 순차 실행한다.

```bash
python3 /tmp/issue-1640-bounded-run.py 1800 ./gradlew --no-daemon :bluetape4k-spring-boot-core:compileTestKotlin :bluetape4k-spring-boot-hibernate-lettuce:compileTestKotlin :bluetape4k-spring-boot-hibernate-lettuce-demo:compileTestKotlin :bluetape4k-spring-boot-core:detekt :bluetape4k-spring-boot-hibernate-lettuce:detekt :bluetape4k-spring-boot-hibernate-lettuce-demo:detekt || exit $?
git diff --check
```

- [ ] lifecycle·리소스·blocking·시간 경계 관점으로 현재 diff를 검토한다. stress·benchmark는 제품 성능을 변경하지 않는 8개 고정 fixture이므로 추가하지 않는다.
- [ ] 위 실행이 실패하면 로그와 XML을 먼저 분석한다. assertion 약화·테스트 제외·재시도만으로 통과시키지 않는다.

### 위험·진단·복구

| 위험 | 신호 | 대응과 재실행 |
|---|---|---|
| 정상 자동 구성 순서에서 caller back-off 실패 | 중복 executor 또는 시작 실패 | bean 정의 순서·타입 확인, 제품 조건을 바꾸지 않고 해당 fixture만 수정·재실행 |
| 테스트 구버전 force 제거로 기존 cache fixture 실패 | 기존 모듈 XML 실패 | 실제 graph 확인 후 불필요한 fixture 우회만 제거; 제품 변경 필요 시 설계 재검토 |
| 비협력적인 startup/close 정지 | 10분/30분 timeout | 소유 process group 진단·종료, 원인 수정 후 처음부터 재실행 |
| 실제 저장 없이 repository 생성만 확인 | 값 재조회 검증 누락 | 기본 미설정 사례에서 saveAndFlush와 새 repository 조회를 유지 |
| 4.0.3 실패를 upstream RED로 오인 | 보고서 버전·오류 불일치 | graph RED와 동작 음성 대조를 분리; 4.1.0 비교 미실행은 그대로 명시 |

코드 복구는 이 작업의 diff 또는 커밋만 되돌리는 별도 변경으로 수행한다. 사용자 변경을 reset하지 않는다. 전역 catalog와 생산 API가 그대로이므로 제품 마이그레이션·데이터 복구 작업은 없다.

## 작업 5 — 문서·리뷰·전달

복잡도: 낮음. 선행: 전체 검증.

- [ ] AC 대응표와 exact diff로 verifier 검토 및 6개 관점 pre-PR 리뷰를 수행한다. 독립 lane이 실행되지 않으면 실패 근거를 보존하고 inline fallback으로 정확히 표시한다.
- [ ] 위 lesson 파일에 spec의 시간상한 누락, RED 구분, plan·TDD·code-review에서 실제로 발견한 수정과 재발 방지 검증을 통합한다.
- [ ] 제품 API·문서화된 동작·모듈 등록을 변경하지 않으므로 README locale·CHANGELOG·AGENTS·BOM·Nightly 편집은 N/A다. CI는 기존 spring-boot job에 75분 timeout과 세 모듈 테스트가 있으므로 새 workflow를 추가하지 않는다. 검증 중 실제 변경이 발생하면 이 N/A 판정을 다시 확인한다.
- [ ] 조사 자료 wiki 보존·색인을 별도 해당 저장소 workflow에 따라 완료한다. 다른 작업의 dirty 변경을 포함하지 않는다.
- [ ] 문서 용어 감사, 재독, diff 검사 후 lesson·최종 리뷰를 커밋한다.
- [ ] live 이슈 metadata를 다시 읽고 승인된 repo/base/head로 PR을 생성한다. debop·milestone 2.1.0·test/dependencies를 반영하고 본문 마지막은 `## DoD Status`로 둔다.
- [ ] exact-head CI 성공, 리뷰·thread read-back, 최종 지적 P0=0/P1=0을 확인한다. PR과 SHA를 명시한 병합 준비 DoD를 보고하고 새 병합 승인을 기다린다. 자동 병합·workflow dispatch·tag·publication·삭제는 수행하지 않는다.

## 수락 기준 대응

| 명세 | 구현·검증 작업 |
|---|---|
| 승인 범위 추가 | root v31 → v32 한 줄, 직접 v31 소비자 4개 classpath, 대표 POM, 전체 build 및 후속 전역 테스트 증거 |
| AC-01 | 작업 1 version RED, 작업 3 실제 graph |
| AC-02 | 작업 2 기본 executor와 실제 저장·조회 |
| AC-03 | 작업 2 EMF 생성자 의존 caller 2개 |
| AC-04 | 작업 2 Projects 3개·Boot 3개·caller 2개 |
| AC-05 | 작업 1 실행 제한, 작업 2 자원 종료·Future timeout, 작업 4 전체 실행 |
| AC-06 | 작업 4 세 모듈 전체 테스트와 XML |
| AC-07 | 작업 4 진단·compile·detekt, 작업 5 exact-head 리뷰·CI |

## 계획 DoD

계획 문서·리뷰의 SPW-01–05 및 KO-01–07 검증, 6개 관점 통합 P0/P1 해소 후 계획을 커밋한다. 구현 실행 기록과 결과는 위 작업 checkbox와 최종 리뷰에 남긴다. 현재 코드·테스트·CI 검증은 계획이며 실행 결과가 아니다.
