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
                        val caller = context.getBean(EmfDependentExecutorConfiguration::class.java)
                        actual shouldBeSameInstanceAs caller.ownedExecutor
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
                    if (persist) verifyPersistence(repository)
                    verifyExecution(actual, expectedVirtual)
                } finally {
                    closeStarted = System.nanoTime()
                }
            }
        } catch (failure: Throwable) {
            primaryFailure = failure
        }
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
            if (original == null) primaryFailure = cleanupFailure
            else original.addSuppressed(cleanupFailure)
        }
        primaryFailure?.let { throw it }
    }

    private fun verifyPersistence(repository: ExecutorProbeRepository) {
        val saved = repository.saveAndFlush(ExecutorProbe(name = "executor-probe"))
        val id = saved.id.shouldNotBeNull()
        val loaded = repository.findById(id).orElseThrow()
        loaded.id shouldBeEqualTo id
        loaded.name shouldBeEqualTo "executor-probe"
    }

    private fun verifyExecution(executor: AsyncTaskExecutor, expectedVirtual: Boolean) {
        val future = executor.submit(Callable { Thread.currentThread().isVirtual })
        try {
            future.get(5, TimeUnit.SECONDS) shouldBeEqualTo expectedVirtual
        } finally {
            if (!future.isDone) future.cancel(true)
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
