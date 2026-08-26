package io.bluetape4k.spring.mongodb

import com.mongodb.MongoClientSettings
import com.mongodb.reactivestreams.client.MongoClient
import io.bluetape4k.assertions.shouldBeEmpty
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.spring.mongodb.config.ReactiveMongoAutoConfiguration
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.beans.factory.getBeansOfType
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.mongodb.autoconfigure.MongoProperties
import org.springframework.boot.mongodb.autoconfigure.MongoReactiveAutoConfiguration as BootMongoReactiveAutoConfiguration
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoReactiveAutoConfiguration as BootDataMongoReactiveAutoConfiguration
import org.springframework.boot.test.context.FilteredClassLoader
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.core.convert.MongoConverter
import java.util.function.Supplier

class ReactiveMongoAutoConfigurationTest {

    private val autoConfigurationRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ReactiveMongoAutoConfiguration::class.java))

    @Test
    fun `ReactiveMongoOperations class가 없으면 auto configuration이 비활성화된다`() {
        autoConfigurationRunner
            .withClassLoader(FilteredClassLoader(ReactiveMongoOperations::class.java))
            .run { context ->
                context.getStartupFailure() shouldBeEqualTo null
                context.getBeansOfType<ReactiveMongoOperations>().shouldBeEmpty()
            }
    }

    @Test
    fun `spring mongodb uri가 Boot 41 MongoProperties에 bind된다`() {
        val operations = mockk<ReactiveMongoOperations>(relaxed = true)
        bootMongoRunner()
            .withBean(ReactiveMongoOperations::class.java, Supplier { operations })
            .withPropertyValues("spring.mongodb.uri=mongodb://127.0.0.1:27018/synthetic")
            .run { context ->
                context.getStartupFailure() shouldBeEqualTo null
                context.getBean<MongoProperties>().uri shouldBeEqualTo
                        "mongodb://127.0.0.1:27018/synthetic"
            }
    }

    @Test
    fun `legacy URI만 있으면 정확한 migration exception으로 fail fast한다`() {
        autoConfigurationRunner
            .withPropertyValues("spring.data.mongodb.uri=mongodb://127.0.0.1:27018/legacy")
            .run { context ->
                val failure = context.getStartupFailure().shouldNotBeNull()
                val migrationFailure = generateSequence(failure) { it.cause }
                    .first { it is IllegalStateException && it.message == LEGACY_URI_MESSAGE }
                migrationFailure.message shouldBeEqualTo LEGACY_URI_MESSAGE
            }
    }

    @Test
    fun `새 URI와 legacy URI가 함께 있으면 새 namespace가 우선한다`() {
        val operations = mockk<ReactiveMongoOperations>(relaxed = true)
        bootMongoRunner()
            .withBean(ReactiveMongoOperations::class.java, Supplier { operations })
            .withPropertyValues(
                "spring.data.mongodb.uri=mongodb://127.0.0.1:27018/legacy",
                "spring.mongodb.uri=mongodb://127.0.0.1:27019/current",
            )
            .run { context ->
                context.getStartupFailure() shouldBeEqualTo null
                context.getBean<MongoProperties>().uri shouldBeEqualTo
                        "mongodb://127.0.0.1:27019/current"
            }
    }

    @Test
    fun `사용자 ReactiveMongoOperations가 fallback template보다 우선한다`() {
        val operations = mockk<ReactiveMongoOperations>(relaxed = true)
        val databaseFactory = mockk<ReactiveMongoDatabaseFactory>(relaxed = true)
        val converter = mockk<MongoConverter>(relaxed = true)

        autoConfigurationRunner
            .withBean(ReactiveMongoOperations::class.java, Supplier { operations })
            .withBean(ReactiveMongoDatabaseFactory::class.java, Supplier { databaseFactory })
            .withBean(MongoConverter::class.java, Supplier { converter })
            .run { context ->
                context.getStartupFailure() shouldBeEqualTo null
                context.getBeansOfType<ReactiveMongoOperations>().values.single() shouldBeSameInstanceAs operations
                context.getBeansOfType<ReactiveMongoTemplate>().shouldBeEmpty()
            }
    }

    @Test
    fun `사용자 operations가 없으면 fallback ReactiveMongoTemplate이 생성된다`() {
        val databaseFactory = mockk<ReactiveMongoDatabaseFactory>(relaxed = true)
        val converter = mockk<MongoConverter>(relaxed = true)

        autoConfigurationRunner
            .withBean(ReactiveMongoDatabaseFactory::class.java, Supplier { databaseFactory })
            .withBean(MongoConverter::class.java, Supplier { converter })
            .run { context ->
                context.getStartupFailure() shouldBeEqualTo null
                context.getBeansOfType<ReactiveMongoTemplate>().shouldHaveSize(1)
            }
    }

    @Test
    fun `Boot Data Mongo reactive template이 먼저 등록되어 custom template과 중복되지 않는다`() {
        val client = mockk<MongoClient>(relaxed = true)
        val settings = MongoClientSettings.builder().build()
        val databaseFactory = mockk<ReactiveMongoDatabaseFactory>(relaxed = true)
        val converter = mockk<MappingMongoConverter>(relaxed = true)

        bootMongoRunner(includeDataMongo = true, client = client, settings = settings)
            .withBean(ReactiveMongoDatabaseFactory::class.java, Supplier { databaseFactory })
            .withBean(MappingMongoConverter::class.java, Supplier { converter })
            .withPropertyValues("spring.mongodb.uri=mongodb://127.0.0.1:27018/synthetic")
            .run { context ->
                context.getStartupFailure() shouldBeEqualTo null
                context.getBeansOfType<MongoClient>().shouldHaveSize(1)
                context.getBeansOfType<ReactiveMongoDatabaseFactory>().shouldHaveSize(1)
                context.getBeansOfType<ReactiveMongoTemplate>().shouldHaveSize(1)
                context.getBeansOfType<ReactiveMongoOperations>().shouldHaveSize(1)

                context.beanFactory
                    .getBeanDefinition("reactiveMongoTemplate")
                    .factoryBeanName
                    .shouldNotBeNull()
                    .shouldContain("DataMongoReactiveAutoConfiguration")
            }
    }

    @Test
    fun `context close가 Spring 관리 reactive client를 닫고 공유 server lifecycle을 건드리지 않는다`() {
        val client = mockk<MongoClient>(relaxed = true)
        val operations = mockk<ReactiveMongoOperations>(relaxed = true)

        bootMongoRunner(client = client)
            .withBean(ReactiveMongoOperations::class.java, Supplier { operations })
            .run { context ->
                context.getStartupFailure() shouldBeEqualTo null
            }

        verify(exactly = 1) { client.close() }
    }

    @Test
    fun `ReactiveMongoDatabaseFactory가 없으면 fallback configuration 원인이 context failure에 남는다`() {
        autoConfigurationRunner
            .withBean(MongoConverter::class.java, Supplier { mockk<MongoConverter>(relaxed = true) })
            .run { context ->
                val failure = context.getStartupFailure().shouldNotBeNull()
                failure.toString() shouldContain "ReactiveMongoDatabaseFactory"
            }
    }

    @Test
    fun `MongoConverter가 없으면 fallback configuration 원인이 context failure에 남는다`() {
        autoConfigurationRunner
            .withBean(
                ReactiveMongoDatabaseFactory::class.java,
                Supplier { mockk<ReactiveMongoDatabaseFactory>(relaxed = true) },
            )
            .run { context ->
                val failure = context.getStartupFailure().shouldNotBeNull()
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
            .withBean(MongoClientSettings::class.java, Supplier { settings })
            .withBean(MongoClient::class.java, Supplier { client })
    }

    private companion object {
        const val LEGACY_URI_MESSAGE =
            "Unsupported legacy MongoDB property 'spring.data.mongodb.uri'; use 'spring.mongodb.uri' on Spring Boot 4.1+"
    }
}
