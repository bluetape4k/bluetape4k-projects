package io.bluetape4k.hibernate.cache.lettuce

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertFailsWith

/**
 * [LettuceNearCacheProperties] 파싱 및 빌드 로직 단위 테스트.
 *
 * Redis 연결 없이 순수 설정 파싱 / 검증 동작을 확인한다.
 */
class LettuceNearCachePropertiesTest {

    @Test
    fun `기본값으로 생성 시 모든 필드가 합리적인 초기값을 가진다`() {
        val props = LettuceNearCacheProperties()

        props.redisUri shouldBeEqualTo "redis://localhost:6379"
        props.codec shouldBeEqualTo "lz4fory"
        props.localMaxSize shouldBeEqualTo 10_000L
        props.localExpireAfterWrite shouldBeEqualTo Duration.ofMinutes(30)
        props.redisTtlDefault shouldBeEqualTo Duration.ofSeconds(120)
        props.useResp3 shouldBeEqualTo true
        props.recordLocalStats shouldBeEqualTo false
        props.regionTtls.isEmpty().shouldBeTrue()
    }

    @Test
    fun `duration 단위 ms 파싱 - 밀리초`() {
        val props = LettuceNearCacheProperties.from(
            mapOf("hibernate.cache.lettuce.redis_ttl.default" to "500ms")
        )

        props.redisTtlDefault shouldBeEqualTo Duration.ofMillis(500)
    }

    @Test
    fun `duration 단위 s 파싱 - 초`() {
        val props = LettuceNearCacheProperties.from(
            mapOf("hibernate.cache.lettuce.redis_ttl.default" to "90s")
        )

        props.redisTtlDefault shouldBeEqualTo Duration.ofSeconds(90)
    }

    @Test
    fun `duration 단위 m 파싱 - 분`() {
        val props = LettuceNearCacheProperties.from(
            mapOf("hibernate.cache.lettuce.local.expire_after_write" to "15m")
        )

        props.localExpireAfterWrite shouldBeEqualTo Duration.ofMinutes(15)
    }

    @Test
    fun `duration 단위 h 파싱 - 시간`() {
        val props = LettuceNearCacheProperties.from(
            mapOf("hibernate.cache.lettuce.local.expire_after_write" to "2h")
        )

        props.localExpireAfterWrite shouldBeEqualTo Duration.ofHours(2)
    }

    @Test
    fun `duration 단위 미지정 시 초로 파싱된다`() {
        val props = LettuceNearCacheProperties.from(
            mapOf("hibernate.cache.lettuce.redis_ttl.default" to "300")
        )

        props.redisTtlDefault shouldBeEqualTo Duration.ofSeconds(300)
    }

    @Test
    fun `region별 TTL이 기본 TTL보다 우선 적용된다`() {
        val props = LettuceNearCacheProperties.from(
            mapOf(
                "hibernate.cache.lettuce.redis_ttl.default" to "60s",
                "hibernate.cache.lettuce.redis_ttl.mySpecialRegion" to "600s",
            )
        )

        val generalConfig = props.buildNearCacheConfig("someOtherRegion")
        val specialConfig = props.buildNearCacheConfig("mySpecialRegion")

        generalConfig.redisTtl shouldBeEqualTo Duration.ofSeconds(60)
        specialConfig.redisTtl shouldBeEqualTo Duration.ofSeconds(600)
    }

    @Test
    fun `timestamps region의 TTL은 항상 null이다`() {
        val props = LettuceNearCacheProperties.from(
            mapOf("hibernate.cache.lettuce.redis_ttl.default" to "120s")
        )

        val tsConfig = props.buildNearCacheConfig(
            org.hibernate.cache.spi.RegionFactory.DEFAULT_UPDATE_TIMESTAMPS_REGION_UNQUALIFIED_NAME
        )

        tsConfig.redisTtl.shouldBeNull()
    }

    @Test
    fun `buildNearCacheConfig에서 cacheName이 regionName과 일치한다`() {
        val props = LettuceNearCacheProperties()
        val regionName = "io.bluetape4k.example.MyEntity"

        val config = props.buildNearCacheConfig(regionName)

        config.cacheName shouldBeEqualTo regionName
    }

    @Test
    fun `redis_ttl_default가 null인 경우 TTL 없이 영속 저장된다`() {
        // from()에서 default가 Duration이므로, 직접 생성으로 테스트
        val props = LettuceNearCacheProperties(redisTtlDefault = null)
        val config = props.buildNearCacheConfig("anyRegion")

        config.redisTtl.shouldBeNull()
    }

    @Test
    fun `지원되는 모든 15가지 codec에 대해 createCodec이 성공한다`() {
        val codecs = listOf(
            "jdk", "kryo", "fory",
            "gzipjdk", "gzipkryo", "gzipfory",
            "lz4jdk", "lz4kryo", "lz4fory",
            "snappyjdk", "snappykryo", "snappyfory",
            "zstdjdk", "zstdkryo", "zstdfory",
        )

        codecs.forEach { codecName ->
            val props = LettuceNearCacheProperties(codec = codecName)
            props.createCodec().shouldNotBeNull()
        }
    }

    @Test
    fun `codec 이름이 대소문자를 구분하지 않는다`() {
        val propsUpper = LettuceNearCacheProperties.from(
            mapOf("hibernate.cache.lettuce.codec" to "LZ4FORY")
        )

        propsUpper.createCodec().shouldNotBeNull()
    }

    @Test
    fun `잘못된 boolean 설정은 즉시 실패한다`() {
        assertFailsWith<IllegalArgumentException> {
            LettuceNearCacheProperties.from(
                mapOf("hibernate.cache.lettuce.use_resp3" to "yes")
            )
        }
    }

    @Test
    fun `음수 local max size는 즉시 실패한다`() {
        assertFailsWith<IllegalArgumentException> {
            LettuceNearCacheProperties.from(
                mapOf("hibernate.cache.lettuce.local.max_size" to "-1")
            )
        }
    }

    @Test
    fun `음수 duration은 즉시 실패한다`() {
        assertFailsWith<IllegalArgumentException> {
            LettuceNearCacheProperties(
                redisTtlDefault = Duration.ofSeconds(-1)
            )
        }
    }

    @Test
    fun `빈 redisUri는 즉시 실패한다`() {
        assertFailsWith<IllegalArgumentException> {
            LettuceNearCacheProperties(redisUri = "")
        }
    }
}
