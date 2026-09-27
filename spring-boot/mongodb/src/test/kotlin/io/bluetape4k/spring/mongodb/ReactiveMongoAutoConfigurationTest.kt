package io.bluetape4k.spring.mongodb

import com.mongodb.MongoClientSettings
import com.mongodb.reactivestreams.client.MongoClient
import io.bluetape4k.assertions.shouldBeEmpty
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import io.bluetape4k.spring.mongodb.config.ReactiveMongoAutoConfiguration
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.beans.factory.getBeansOfType
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.mongodb.autoconfigure.MongoProperties
import org.springframework.boot.test.context.FilteredClassLoader
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.core.convert.MongoConverter
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoReactiveAutoConfiguration as BootDataMongoReactiveAutoConfiguration
import org.springframework.boot.mongodb.autoconfigure.MongoReactiveAutoConfiguration as BootMongoReactiveAutoConfiguration

class ReactiveMongoAutoConfigurationTest {

    private companion object: KLogging() {
        const val LEGACY_URI_MESSAGE =
            "Unsupported legacy MongoDB property 'spring.data.mongodb.uri'; use 'spring.mongodb.uri' on Spring Boot 4.1+"
    }

    private val autoConfigurationRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ReactiveMongoAutoConfiguration::class.java))

    private val client = mockk<MongoClient>(relaxed = true)
    private val operations = mockk<ReactiveMongoOperations>(relaxed = true)
    private val databaseFactory = mockk<ReactiveMongoDatabaseFactory>(relaxed = true)
    private val converter = mockk<MappingMongoConverter>(relaxed = true)

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()
    }

    @Test
    fun `auto configuration은 public no-arg 생성자 ABI를 유지한다`() {
        ReactiveMongoAutoConfiguration::class.java.constructors
            .map { it.parameterCount }
            .shouldBeEqualTo(listOf(0))
    }

    @Test
    fun `auto configuration은 public static field를 노출하지 않는다`() {
        ReactiveMongoAutoConfiguration::class.java.fields
            .filter { java.lang.reflect.Modifier.isStatic(it.modifiers) }
            .shouldBeEmpty()
    }

    @Test
    fun `ReactiveMongoOperations class가 없으면 auto configuration이 비활성화된다`() {
        autoConfigurationRunner
            .withClassLoader(FilteredClassLoader(ReactiveMongoOperations::class.java))
            .run { context ->
                context.startupFailure.shouldBeNull()
                context.getBeansOfType<ReactiveMongoOperations>().shouldBeEmpty()
            }
    }

    @Test
    fun `spring mongodb uri가 Boot 41 MongoProperties에 bind된다`() {
        bootMongoRunner()
            .withBean(ReactiveMongoOperations::class.java, { operations })
            .withPropertyValues("spring.mongodb.uri=mongodb://127.0.0.1:27018/synthetic")
            .run { context ->
                context.getStartupFailure().shouldBeNull()
                context.getBean<MongoProperties>().uri shouldBeEqualTo
                        "mongodb://127.0.0.1:27018/synthetic"
            }
    }

    @Test
    fun `legacy URI만 있으면 정확한 migration exception으로 fail fast한다`() {
        autoConfigurationRunner
            .withPropertyValues("spring.data.mongodb.uri=mongodb://127.0.0.1:27018/legacy")
            .run { context ->
                val failure = context.startupFailure.shouldNotBeNull()
                val migrationFailure = generateSequence(failure) { it.cause }
                    .first { it is IllegalStateException && it.message == LEGACY_URI_MESSAGE }
                migrationFailure.message shouldBeEqualTo LEGACY_URI_MESSAGE
            }
    }

    @Test
    fun `사용자 ReactiveMongoOperations가 있으면 legacy URI 검사를 backoff한다`() {
        autoConfigurationRunner
            .withBean(ReactiveMongoOperations::class.java, { operations })
            .withPropertyValues("spring.data.mongodb.uri=mongodb://127.0.0.1:27018/legacy")
            .run { context ->
                context.startupFailure.shouldBeNull()
                context.getBeansOfType<ReactiveMongoOperations>().values.single() shouldBeSameInstanceAs operations
                context.getBeansOfType<ReactiveMongoTemplate>().shouldBeEmpty()
            }
    }

    @Test
    fun `Boot 제공 ReactiveMongoOperations가 있으면 legacy URI 검사를 backoff한다`() {
        val settings = MongoClientSettings.builder().build()

        bootMongoRunner(includeDataMongo = true, client = client, settings = settings)
            .withBean(ReactiveMongoDatabaseFactory::class.java, { databaseFactory })
            .withBean(MappingMongoConverter::class.java, { converter })
            .withPropertyValues("spring.data.mongodb.uri=mongodb://127.0.0.1:27018/legacy")
            .run { context ->
                context.getStartupFailure().shouldBeNull()
                context.getBeansOfType<ReactiveMongoOperations>() shouldHaveSize 1
                context.beanFactory
                    .getBeanDefinition("reactiveMongoTemplate")
                    .factoryBeanName
                    .shouldNotBeEmpty()
                    .shouldContain("DataMongoReactiveAutoConfiguration")
            }
    }

    @Test
    fun `새 URI와 legacy URI가 함께 있으면 새 namespace가 우선한다`() {
        bootMongoRunner()
            .withBean(ReactiveMongoOperations::class.java, { operations })
            .withPropertyValues(
                "spring.data.mongodb.uri=mongodb://127.0.0.1:27018/legacy",
                "spring.mongodb.uri=mongodb://127.0.0.1:27019/current",
            )
            .run { context ->
                context.startupFailure.shouldBeNull()
                context.getBean<MongoProperties>().uri shouldBeEqualTo "mongodb://127.0.0.1:27019/current"
            }
    }

    @Test
    fun `사용자 ReactiveMongoOperations가 fallback template보다 우선한다`() {
        autoConfigurationRunner
            .withBean(ReactiveMongoOperations::class.java, { operations })
            .withBean(ReactiveMongoDatabaseFactory::class.java, { databaseFactory })
            .withBean(MongoConverter::class.java, { converter })
            .run { context ->
                context.startupFailure.shouldBeNull()
                context.getBeansOfType<ReactiveMongoOperations>().values.single() shouldBeSameInstanceAs operations
                context.getBeansOfType<ReactiveMongoTemplate>().shouldBeEmpty()
            }
    }

    @Test
    fun `사용자 operations가 없으면 fallback ReactiveMongoTemplate이 생성된다`() {
        autoConfigurationRunner
            .withBean(ReactiveMongoDatabaseFactory::class.java, { databaseFactory })
            .withBean(MongoConverter::class.java, { converter })
            .run { context ->
                context.getStartupFailure().shouldBeNull()
                context.getBeansOfType<ReactiveMongoTemplate>() shouldHaveSize 1
            }
    }

    @Test
    fun `Boot Data Mongo reactive template이 먼저 등록되어 custom template과 중복되지 않는다`() {
        val settings = MongoClientSettings.builder().build()

        bootMongoRunner(includeDataMongo = true, client = client, settings = settings)
            .withBean(ReactiveMongoDatabaseFactory::class.java, { databaseFactory })
            .withBean(MappingMongoConverter::class.java, { converter })
            .withPropertyValues("spring.mongodb.uri=mongodb://127.0.0.1:27018/synthetic")
            .run { context ->
                context.getStartupFailure().shouldBeNull()
                context.getBeansOfType<MongoClient>() shouldHaveSize 1
                context.getBeansOfType<ReactiveMongoDatabaseFactory>() shouldHaveSize 1
                context.getBeansOfType<ReactiveMongoTemplate>() shouldHaveSize 1
                context.getBeansOfType<ReactiveMongoOperations>() shouldHaveSize 1

                context.beanFactory
                    .getBeanDefinition("reactiveMongoTemplate")
                    .factoryBeanName
                    .shouldNotBeEmpty()
                    .shouldContain("DataMongoReactiveAutoConfiguration")
            }
    }

    @Test
    fun `context close가 Spring 관리 reactive client를 정확히 한 번 닫는다`() {
        bootMongoRunner(client = client)
            .withBean(ReactiveMongoOperations::class.java, { operations })
            .run { context ->
                context.startupFailure.shouldBeNull()
            }

        verify(exactly = 1) { client.close() }
    }

    @Test
    fun `ReactiveMongoDatabaseFactory가 없으면 fallback configuration 원인이 context failure에 남는다`() {
        autoConfigurationRunner
            .withBean(MongoConverter::class.java, { mockk<MongoConverter>(relaxed = true) })
            .run { context ->
                val failure = context.startupFailure.shouldNotBeNull()
                failure.toString() shouldContain "ReactiveMongoDatabaseFactory"
            }
    }

    @Test
    fun `MongoConverter가 없으면 fallback configuration 원인이 context failure에 남는다`() {
        autoConfigurationRunner
            .withBean(
                ReactiveMongoDatabaseFactory::class.java,
                { mockk<ReactiveMongoDatabaseFactory>(relaxed = true) },
            )
            .run { context ->
                val failure = context.startupFailure.shouldNotBeNull()
                failure.toString() shouldContain "MongoConverter"
            }
    }

    private fun bootMongoRunner(
        includeDataMongo: Boolean = false,
        client: MongoClient = mockk(relaxed = true),
        settings: MongoClientSettings = MongoClientSettings.builder().build(),
    ): ApplicationContextRunner {
        val configurations = if (includeDataMongo) {
            AutoConfigurations.of(
                BootMongoReactiveAutoConfiguration::class.java,
                BootDataMongoReactiveAutoConfiguration::class.java,
                ReactiveMongoAutoConfiguration::class.java,
            )
        } else {
            AutoConfigurations.of(
                BootMongoReactiveAutoConfiguration::class.java,
                ReactiveMongoAutoConfiguration::class.java,
            )
        }

        return ApplicationContextRunner()
            .withConfiguration(configurations)
            .withBean(MongoClientSettings::class.java, { settings })
            .withBean(MongoClient::class.java, { client })
    }

}
