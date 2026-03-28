package io.bluetape4k.hibernate.cache.lettuce

import io.bluetape4k.cache.nearcache.LettuceNearCacheConfig
import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecs
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requirePositiveNumber
import org.hibernate.cache.spi.RegionFactory
import java.time.Duration

/**
 * Hibernate properties에서 Near Cache 설정을 파싱하는 data class.
 *
 * ```
 * # Hibernate properties 예시
 * hibernate.lettuce.redis_uri=redis://localhost:6379
 * hibernate.lettuce.codec=lz4fory               # lz4fory(기본), fory, kryo, lz4kryo 등
 * hibernate.lettuce.local.max_size=10000
 * hibernate.lettuce.local.expire_after_write=30m
 * hibernate.lettuce.redis_ttl.default=120s
 * hibernate.lettuce.redis_ttl.{regionName}=300s  # region별 TTL 오버라이드
 * hibernate.lettuce.use_resp3=true
 * ```
 */
data class LettuceNearCacheProperties(
    val redisUri: String = "redis://localhost:6379",
    val codec: String = "lz4fory",
    val localMaxSize: Long = 10_000L,
    val localExpireAfterWrite: Duration = Duration.ofMinutes(30),
    val redisTtlDefault: Duration? = Duration.ofSeconds(120),
    val regionTtls: Map<String, Duration> = emptyMap(),
    val useResp3: Boolean = true,
    val recordLocalStats: Boolean = false,
) {
    init {
        redisUri.requireNotBlank("redisUri")
        localMaxSize.requirePositiveNumber("localMaxSize")
        validatePositiveDuration("localExpireAfterWrite", localExpireAfterWrite)
        validatePositiveDuration("redisTtlDefault", redisTtlDefault)
        regionTtls.forEach { (regionName, ttl) ->
            regionName.requireNotBlank("regionTtls.key")
            validatePositiveDuration("regionTtls[$regionName]", ttl)
        }
    }

    companion object {
        private const val PREFIX = "hibernate.cache.lettuce."

        fun from(configValues: Map<String, Any>): LettuceNearCacheProperties {
            fun str(key: String, default: String): String =
                configValues["$PREFIX$key"]?.toString() ?: default

            fun long(key: String, default: Long): Long {
                val propertyName = "$PREFIX$key"
                val raw = configValues[propertyName]?.toString() ?: return default
                return raw.toLongOrNull()?.requirePositiveNumber(propertyName)
                    ?: throw IllegalArgumentException("Invalid positive number for '$propertyName': $raw")
            }

            fun bool(key: String, default: Boolean): Boolean {
                val propertyName = "$PREFIX$key"
                val raw = configValues[propertyName]?.toString() ?: return default
                return raw.toBooleanStrictOrNull()
                    ?: throw IllegalArgumentException("Invalid boolean for '$propertyName': $raw")
            }

            fun duration(key: String, default: Duration?): Duration? {
                val propertyName = "$PREFIX$key"
                val raw = configValues[propertyName]?.toString() ?: return default
                return parseDuration(raw)
                    ?: throw IllegalArgumentException("Invalid duration for '$propertyName': $raw")
            }

            val regionTtls = configValues.entries
                .filter { it.key.startsWith("${PREFIX}redis_ttl.") && !it.key.endsWith(".default") }
                .associate { (k, v) ->
                    val regionName = k.removePrefix("${PREFIX}redis_ttl.")
                    regionName to (
                            parseDuration(v.toString())
                                ?: throw IllegalArgumentException("Invalid duration for '$k': ${v.toString()}")
                            )
                }

            return LettuceNearCacheProperties(
                redisUri = str("redis_uri", "redis://localhost:6379"),
                codec = str("codec", "lz4fory"),
                localMaxSize = long("local.max_size", 10_000L),
                localExpireAfterWrite = duration("local.expire_after_write", Duration.ofMinutes(30))
                    ?: Duration.ofMinutes(30),
                redisTtlDefault = duration("redis_ttl.default", Duration.ofSeconds(120)),
                regionTtls = regionTtls,
                useResp3 = bool("use_resp3", true),
                recordLocalStats = bool("local.record_stats", false),
            )
        }

        private fun parseDuration(raw: String): Duration? = when {
            raw.endsWith("ms") -> Duration.ofMillis(raw.dropLast(2).toLongOrNull() ?: return null)
            raw.endsWith("s")  -> Duration.ofSeconds(raw.dropLast(1).toLongOrNull() ?: return null)
            raw.endsWith("m")  -> Duration.ofMinutes(raw.dropLast(1).toLongOrNull() ?: return null)
            raw.endsWith("h")  -> Duration.ofHours(raw.dropLast(1).toLongOrNull() ?: return null)
            else               -> Duration.ofSeconds(raw.toLongOrNull() ?: return null)
        }
    }

    /**
     * [BinarySerializers]를 사용해 [LettuceBinaryCodec]을 직접 생성.
     * [io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecs]는 protobuf 의존성 문제로 사용하지 않음.
     */
    fun createCodec(): LettuceBinaryCodec<Any> = when (codec.lowercase()) {
        "jdk"        -> LettuceBinaryCodecs.jdk()
        "kryo"       -> LettuceBinaryCodecs.kryo()
        "fory"       -> LettuceBinaryCodecs.fory()
        "gzipjdk"    -> LettuceBinaryCodecs.gzipJdk()
        "gzipkryo"   -> LettuceBinaryCodecs.gzipKryo()
        "gzipfory"   -> LettuceBinaryCodecs.gzipFory()
        "lz4jdk"     -> LettuceBinaryCodecs.lz4Jdk()
        "lz4kryo"    -> LettuceBinaryCodecs.lz4Kryo()
        "lz4fory"    -> LettuceBinaryCodecs.lz4Fory()
        "snappyjdk"  -> LettuceBinaryCodecs.snappyJdk()
        "snappykryo" -> LettuceBinaryCodecs.snappyKryo()
        "snappyfory" -> LettuceBinaryCodecs.snappyFory()
        "zstdjdk"    -> LettuceBinaryCodecs.zstdJdk()
        "zstdkryo"   -> LettuceBinaryCodecs.zstdKryo()
        "zstdfory"   -> LettuceBinaryCodecs.zstdFory()
        else -> LettuceBinaryCodecs.default()
    }

    fun buildNearCacheConfig(regionName: String): LettuceNearCacheConfig<String, Any> {
        val ttl = resolveRedisTtl(regionName)
        return LettuceNearCacheConfig(
            cacheName = regionName,
            maxLocalSize = localMaxSize,
            frontExpireAfterWrite = localExpireAfterWrite,
            redisTtl = ttl,
            useRespProtocol3 = useResp3,
            recordStats = recordLocalStats,
        )
    }

    private fun resolveRedisTtl(regionName: String): Duration? =
        if (regionName == RegionFactory.DEFAULT_UPDATE_TIMESTAMPS_REGION_UNQUALIFIED_NAME) {
            null
        } else {
            regionTtls[regionName] ?: redisTtlDefault
        }

    private fun validatePositiveDuration(name: String, duration: Duration?) {
        if (duration == null) return
        require(!duration.isZero && !duration.isNegative) {
            "$name must be a positive duration, but was: $duration"
        }
    }
}
