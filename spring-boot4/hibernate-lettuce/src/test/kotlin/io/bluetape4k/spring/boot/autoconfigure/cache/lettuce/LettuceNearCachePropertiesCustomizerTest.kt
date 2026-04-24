package io.bluetape4k.spring.boot.autoconfigure.cache.lettuce

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.junit.jupiter.api.Test
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer
import java.time.Duration

/**
 * [LettuceNearCacheHibernateAutoConfiguration]이 생성하는 [HibernatePropertiesCustomizer] 람다를
 * Spring 컨텍스트 없이 직접 호출하여 갭 케이스를 검증한다.
 */
class LettuceNearCachePropertiesCustomizerTest {

    private fun createCustomizer(props: LettuceNearCacheSpringProperties): HibernatePropertiesCustomizer {
        val config = LettuceNearCacheHibernateAutoConfiguration()
        return config.lettuceNearCacheHibernatePropertiesCustomizer(props)
    }

    @Test
    fun `redisTtl regions 다건 매핑이 모두 properties에 추가된다`() {
        val props = LettuceNearCacheSpringProperties(
            redisTtl = LettuceNearCacheSpringProperties.RedisTtlProperties(
                regions = mapOf(
                    "A" to Duration.ofSeconds(60),
                    "B" to Duration.ofSeconds(300),
                    "C" to Duration.ofSeconds(900),
                )
            )
        )
        val customizer = createCustomizer(props)
        val hibernateProperties = mutableMapOf<String, Any>()
        customizer.customize(hibernateProperties)

        hibernateProperties["hibernate.cache.lettuce.redis_ttl.A"] shouldBeEqualTo "60s"
        hibernateProperties["hibernate.cache.lettuce.redis_ttl.B"] shouldBeEqualTo "300s"
        hibernateProperties["hibernate.cache.lettuce.redis_ttl.C"] shouldBeEqualTo "900s"
    }

    @Test
    fun `metrics enabled=false이면 generate_statistics 키가 없다`() {
        val props = LettuceNearCacheSpringProperties(
            metrics = LettuceNearCacheSpringProperties.MetricsProperties(enabled = false)
        )
        val customizer = createCustomizer(props)
        val hibernateProperties = mutableMapOf<String, Any>()
        customizer.customize(hibernateProperties)

        hibernateProperties.containsKey("hibernate.generate_statistics").shouldBeFalse()
    }

    @Test
    fun `metrics enableCaffeineStats=false이면 local_record_stats 키가 없다`() {
        val props = LettuceNearCacheSpringProperties(
            metrics = LettuceNearCacheSpringProperties.MetricsProperties(
                enabled = true,
                enableCaffeineStats = false,
            )
        )
        val customizer = createCustomizer(props)
        val hibernateProperties = mutableMapOf<String, Any>()
        customizer.customize(hibernateProperties)

        hibernateProperties.containsKey("hibernate.cache.lettuce.local.record_stats").shouldBeFalse()
    }
}
