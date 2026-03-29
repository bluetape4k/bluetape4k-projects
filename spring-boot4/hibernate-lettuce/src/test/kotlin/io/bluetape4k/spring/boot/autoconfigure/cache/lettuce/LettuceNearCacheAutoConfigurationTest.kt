package io.bluetape4k.spring.boot.autoconfigure.cache.lettuce

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import jakarta.persistence.EntityManagerFactory
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.beans.factory.getBeansOfType
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.util.function.Supplier

class LettuceNearCacheAutoConfigurationTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(LettuceNearCacheHibernateAutoConfiguration::class.java)
        )

    private val metricsContextRunner = ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                LettuceNearCacheMetricsAutoConfiguration::class.java,
                LettuceNearCacheActuatorAutoConfiguration::class.java,
            )
        )
        .withBean(
            EntityManagerFactory::class.java,
            Supplier { org.mockito.Mockito.mock(EntityManagerFactory::class.java) }
        )

    @Test
    fun `HibernatePropertiesCustomizer가 기본 설정으로 등록된다`() {
        contextRunner.run { context ->
            context.getBeansOfType<HibernatePropertiesCustomizer>().shouldHaveSize(1)
        }
    }

    @Test
    fun `enabled=false이면 Bean이 등록되지 않는다`() {
        contextRunner
            .withPropertyValues("bluetape4k.cache.lettuce-near.enabled=false")
            .run { context ->
                context.getBeansOfType<HibernatePropertiesCustomizer>().shouldBeEmpty()
            }
    }

    @Test
    fun `custom redisUri가 Hibernate properties에 반영된다`() {
        contextRunner
            .withPropertyValues("bluetape4k.cache.lettuce-near.redis-uri=redis://myredis:6380")
            .run { context ->
                val customizer = context.getBean<HibernatePropertiesCustomizer>()
                val props = mutableMapOf<String, Any>()
                customizer.customize(props)

                props["hibernate.cache.lettuce.redis_uri"] shouldBeEqualTo "redis://myredis:6380"
                props["hibernate.cache.region.factory_class"] shouldBeEqualTo
                        "io.bluetape4k.hibernate.cache.lettuce.LettuceNearCacheRegionFactory"
                props["hibernate.cache.use_second_level_cache"] shouldBeEqualTo "true"
            }
    }

    @Test
    fun `metrics enabled이면 statistics 설정이 반영된다`() {
        contextRunner
            .withPropertyValues(
                "bluetape4k.cache.lettuce-near.metrics.enabled=true",
                "bluetape4k.cache.lettuce-near.metrics.enable-caffeine-stats=true",
            )
            .run { context ->
                val customizer = context.getBean<HibernatePropertiesCustomizer>()
                val props = mutableMapOf<String, Any>()
                customizer.customize(props)

                props["hibernate.generate_statistics"] shouldBeEqualTo "true"
                props["hibernate.cache.lettuce.local.record_stats"] shouldBeEqualTo "true"
            }
    }

    @Test
    fun `region별 TTL 설정이 Hibernate properties에 반영된다`() {
        contextRunner
            .withPropertyValues(
                "bluetape4k.cache.lettuce-near.redis-ttl.default=60s",
                // 점이 포함된 Map 키는 브라켓 표기법 사용
                "bluetape4k.cache.lettuce-near.redis-ttl.regions[product]=300s",
            )
            .run { context ->
                val customizer = context.getBean<HibernatePropertiesCustomizer>()
                val props = mutableMapOf<String, Any>()
                customizer.customize(props)

                props["hibernate.cache.lettuce.redis_ttl.default"] shouldBeEqualTo "60s"
                props["hibernate.cache.lettuce.redis_ttl.product"] shouldBeEqualTo "300s"
            }
    }

    @Test
    fun `LettuceNearCacheSpringProperties 기본값이 올바르게 설정된다`() {
        contextRunner.run { context ->
            val props = context.getBean<LettuceNearCacheSpringProperties>()
            props.enabled.shouldBeTrue()
            props.redisUri shouldBeEqualTo "redis://localhost:6379"
            props.codec shouldBeEqualTo "lz4fory"
            props.useResp3.shouldBeTrue()
            props.local.maxSize shouldBeEqualTo 10_000L
            props.metrics.enabled.shouldBeTrue()
        }
    }

    @Test
    fun `millisecond duration이 Hibernate properties에 보존된다`() {
        contextRunner
            .withPropertyValues(
                "bluetape4k.cache.lettuce-near.local.expire-after-write=500ms",
                "bluetape4k.cache.lettuce-near.redis-ttl.default=1500ms",
            )
            .run { context ->
                val customizer = context.getBean<HibernatePropertiesCustomizer>()
                val props = mutableMapOf<String, Any>()
                customizer.customize(props)

                props["hibernate.cache.lettuce.local.expire_after_write"] shouldBeEqualTo "500ms"
                props["hibernate.cache.lettuce.redis_ttl.default"] shouldBeEqualTo "1500ms"
            }
    }

    @Test
    fun `metrics auto configuration registers binder when registry exists`() {
        metricsContextRunner
            .withBean(SimpleMeterRegistry::class.java, Supplier { SimpleMeterRegistry() })
            .run { context ->
                context.getBeansOfType<LettuceNearCacheMetricsBinder>().shouldHaveSize(1)
            }
    }

    @Test
    fun `actuator auto configuration registers endpoint when entity manager exists`() {
        metricsContextRunner.run { context ->
            context.getBeansOfType<LettuceNearCacheActuatorEndpoint>().shouldHaveSize(1)
        }
    }

    @Test
    fun `metrics auto configuration backs off when disabled`() {
        metricsContextRunner
            .withBean(SimpleMeterRegistry::class.java, Supplier { SimpleMeterRegistry() })
            .withPropertyValues("bluetape4k.cache.lettuce-near.metrics.enabled=false")
            .run { context ->
                context.getBeansOfType<LettuceNearCacheMetricsBinder>().shouldBeEmpty()
            }
    }
}
