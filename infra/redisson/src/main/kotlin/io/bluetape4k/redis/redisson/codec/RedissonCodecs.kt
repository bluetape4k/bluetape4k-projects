package io.bluetape4k.redis.redisson.codec

import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.redisson.codec.RedissonCodecs.Fory
import io.bluetape4k.redis.redisson.codec.RedissonCodecs.Jdk
import io.bluetape4k.redis.redisson.codec.RedissonCodecs.Kryo5
import io.bluetape4k.redis.redisson.codec.RedissonCodecs.LZ4Fory
import io.bluetape4k.redis.redisson.codec.RedissonCodecs.LZ4ForyComposite
import io.bluetape4k.redis.redisson.codec.RedissonCodecs.String
import io.bluetape4k.redis.redisson.codec.RedissonCodecs.ZstdFory
import io.bluetape4k.redis.redisson.codec.RedissonCodecs.ZstdForyComposite
import org.redisson.client.codec.Codec
import org.redisson.client.codec.DoubleCodec
import org.redisson.client.codec.IntegerCodec
import org.redisson.client.codec.LongCodec
import org.redisson.client.codec.StringCodec
import org.redisson.codec.CompositeCodec
import org.redisson.codec.Kryo5Codec
import org.redisson.codec.SerializationCodec
import org.redisson.codec.SnappyCodecV2

/**
 * Collects reusable [Codec] instances for Redisson.
 *
 * Provides custom codecs and serializer/compressor combinations in addition to Redisson's built-in codecs.
 *
 * ## Serialization
 * - [Kryo5]: Kryo5 serialization with a compact binary format
 * - [Fory]: Apache Fory serialization (default)
 * - [Jdk]: JDK serialization for compatibility-sensitive legacy integration
 * - [Jackson3]: Jackson 3 custom JSON envelope (`_type`/`_data`) for human-readable, non-JVM integration
 * - [Fastjson2]: Fastjson2 JSONB with `WriteClassName` for binary non-JVM integration
 *
 * ## Compression
 * - Gzip: higher compression ratio with lower throughput
 * - LZ4: lower compression ratio with high throughput, suitable for caches
 * - Snappy: characteristics similar to LZ4
 * - Zstd: a balance between Gzip-like compression and LZ4-like throughput
 *
 * ## Composite Codec
 * Composite codecs use [String] for map keys and a selected serializer/compressor codec for values.
 * They are useful for collection types such as maps, sets, and sorted sets.
 *
 * ## Recommended combinations
 * - General high throughput: [LZ4Fory] or [LZ4ForyComposite]
 * - Compression ratio first: [ZstdFory] or [ZstdForyComposite]
 * - Compatibility first: [Jdk]
 * - Human-readable JSON: [Jackson3] in trusted environments or the secured [jackson3] factory
 */
object RedissonCodecs: KLogging() {

    /**
     * 기본 Codec으로, [Fory] (Apache Fory 직렬화)를 사용합니다.
     */
    @JvmStatic
    val Default: Codec by lazy { Fory }

    /** Redis 정수 값 전용 Codec ([IntegerCodec]) */
    val Int: Codec by lazy { IntegerCodec() }

    /** Redis Long 값 전용 Codec ([LongCodec]) */
    val Long: Codec by lazy { LongCodec() }

    /** Redis Double 값 전용 Codec ([DoubleCodec]) */
    val Double: Codec by lazy { DoubleCodec() }

    /** Redis 문자열 값 전용 Codec ([StringCodec]) */
    val String: Codec by lazy { StringCodec() }

    /** Kryo5 직렬화 Codec. 빠르고 컴팩트한 바이너리 포맷을 제공합니다. */
    val Kryo5: Codec by lazy { Kryo5Codec() }

    /**
     * Apache Fory serialization codec. Falls back to Kryo5 after a supported serialization failure.
     */
    val Fory: Codec by lazy { ForyCodec() }

    /**
     * JDK 기본 직렬화 Codec.
     * 범용 호환성이 높으나 성능이 낮습니다. 레거시 시스템과의 연동에 사용하세요.
     *
     * ⚠️ **보안 경고**: JDK 직렬화는 역직렬화 가젯(deserialization gadget) 체인을 통한 원격 코드 실행(RCE) 취약점에
     * 노출될 수 있습니다. **신뢰된 내부 Redis 환경에서만 사용**하십시오.
     * 외부에 노출된 Redis에서는 [Kryo5] 또는 [Fory] 등 타입 안전 Codec을 사용하십시오.
     */
    val Jdk: Codec by lazy { SerializationCodec() }

    // -------------------------------------------------------------------------
    // JSON Codecs
    // -------------------------------------------------------------------------

    /**
     * Jackson 3 커스텀 JSON 엔벨로프 Codec. Human-readable JSON 포맷으로 Redis에 저장합니다.
     *
     * ⚠️ **보안 경고**: `allowedPackagePrefixes = null` (모든 타입 허용) 기본값입니다.
     * **신뢰된 내부 Redis 환경에서만 사용**하십시오.
     * 외부 노출 Redis에서는 [jackson3] factory 함수를 사용하여 `allowedPackagePrefixes`를 지정하십시오:
     * ```kotlin
     * val safeCodec = RedissonCodecs.jackson3(setOf("com.mycompany.", "io.bluetape4k."))
     * ```
     */
    val Jackson3: Codec by lazy { Jackson3Codec() }

    /**
     * Fastjson2 JSONB Codec. WriteClassName으로 타입 정보를 JSONB 바이너리에 임베딩합니다.
     *
     * ⚠️ **보안 경고**: `allowedPackagePrefixes = null` (모든 타입 허용) 기본값입니다.
     * **신뢰된 내부 Redis 환경에서만 사용**하십시오.
     * 외부 노출 Redis에서는 [fastjson2] factory 함수를 사용하여 `allowedPackagePrefixes`를 지정하십시오:
     * ```kotlin
     * val safeCodec = RedissonCodecs.fastjson2(setOf("com.mycompany.", "io.bluetape4k."))
     * ```
     */
    val Fastjson2: Codec by lazy { Fastjson2Codec() }

    /** Map 키: String, 값: Jackson3 JSON 엔벨로프를 사용하는 복합 Codec */
    val Jackson3Composite: Codec by lazy { CompositeCodec(String, Jackson3, Jackson3) }

    /** Map 키: String, 값: Fastjson2 JSONB를 사용하는 복합 Codec */
    val Fastjson2Composite: Codec by lazy { CompositeCodec(String, Fastjson2, Fastjson2) }

    /** Map 키: String, 값: Kryo5 직렬화를 사용하는 복합 Codec */
    val Kryo5Composite: Codec by lazy { CompositeCodec(String, Kryo5, Kryo5) }

    /** Map 키: String, 값: Fory 직렬화를 사용하는 복합 Codec */
    val ForyComposite: Codec by lazy { CompositeCodec(String, Fory, Fory) }

    /** Map 키: String, 값: JDK 직렬화를 사용하는 복합 Codec */
    val JdkComposite: Codec by lazy { CompositeCodec(String, Jdk, Jdk) }

    /** Kryo5 직렬화 + Gzip 압축 Codec */
    val GzipKryo5: Codec by lazy { GzipCodec(Kryo5) }

    /** Fory 직렬화 + Gzip 압축 Codec */
    val GzipFory: Codec by lazy { GzipCodec(Fory) }

    /** JDK 직렬화 + Gzip 압축 Codec */
    val GzipJdk: Codec by lazy { GzipCodec(Jdk) }

    /** Map 키: String, 값: Kryo5 직렬화 + Gzip 압축을 사용하는 복합 Codec */
    val GzipKryo5Composite: Codec by lazy { CompositeCodec(String, GzipKryo5, GzipKryo5) }

    /** Map 키: String, 값: Fory 직렬화 + Gzip 압축을 사용하는 복합 Codec */
    val GzipForyComposite: Codec by lazy { CompositeCodec(String, GzipFory, GzipFory) }

    /** Map 키: String, 값: JDK 직렬화 + Gzip 압축을 사용하는 복합 Codec */
    val GzipJdkComposite: Codec by lazy { CompositeCodec(String, GzipJdk, GzipJdk) }

    // -------------------------------------------------------------------------
    // FastFory (SCHEMA_CONSISTENT) Codecs
    //
    // ⚠️ 와이어 포맷 경고:
    // - `CompatibleMode.SCHEMA_CONSISTENT`를 사용하며, 기본 Fory codec과 **와이어 포맷이 상호 비호환**합니다.
    // - **비대칭 호환성**: FastFory codec은 구 Fory(COMPATIBLE) 데이터를 fallback으로 읽을 수 있습니다. 반대는 불가합니다.
    // - **휘발성 캐시(Redis, 메모리 캐시) 전용** — 영속 저장에 사용하지 마십시오.
    // - **순환 참조 객체 불가** (refTracking=false).
    // - **스키마 진화 불가** — 필드 추가/제거 시 기존 데이터 역직렬화 실패.
    // -------------------------------------------------------------------------

    /**
     * Apache Fory SCHEMA_CONSISTENT 모드(FastFory) 직렬화 Codec.
     * 기본 [Fory](COMPATIBLE 모드) 대비 더 빠른 직렬화 속도를 제공하며, 직렬화 실패 시 [Fory]로 자동 전환합니다.
     *
     * ```kotlin
     * val config = Config()
     * config.codec = RedissonCodecs.FastFory
     * val redisson = Redisson.create(config)
     * val bucket = redisson.getBucket<MyData>("key")
     * bucket.set(myData)
     * val result = bucket.get()
     * ```
     *
     * ⚠️ [Fory] codec과 와이어 포맷이 상호 비호환입니다. **휘발성 캐시 전용**으로 사용하십시오.
     */
    val FastFory: Codec by lazy { FastForyCodec() }

    /**
     * Map 키: String, 값: FastFory 직렬화를 사용하는 복합 Codec.
     *
     * ⚠️ [ForyComposite] codec과 와이어 포맷이 상호 비호환입니다. **휘발성 캐시 전용**으로 사용하십시오.
     */
    val FastForyComposite: Codec by lazy { CompositeCodec(String, FastFory, FastFory) }

    /**
     * FastFory 직렬화 + LZ4 압축 Codec. 고성능 캐시 환경에서 권장하는 조합입니다.
     *
     * ⚠️ [LZ4Fory] codec과 와이어 포맷이 상호 비호환입니다. **휘발성 캐시 전용**으로 사용하십시오.
     */
    val LZ4FastFory: Codec by lazy { Lz4Codec(FastFory) }

    /**
     * Map 키: String, 값: FastFory 직렬화 + LZ4 압축을 사용하는 복합 Codec.
     *
     * ⚠️ [LZ4ForyComposite] codec과 와이어 포맷이 상호 비호환입니다. **휘발성 캐시 전용**으로 사용하십시오.
     */
    val LZ4FastForyComposite: Codec by lazy { CompositeCodec(String, LZ4FastFory, LZ4FastFory) }

    /**
     * FastFory 직렬화 + Zstd 압축 Codec. 압축률과 속도의 균형이 뛰어난 조합입니다.
     *
     * ⚠️ [ZstdFory] codec과 와이어 포맷이 상호 비호환입니다. **휘발성 캐시 전용**으로 사용하십시오.
     */
    val ZstdFastFory: Codec by lazy { ZstdCodec(FastFory) }

    /**
     * Map 키: String, 값: FastFory 직렬화 + Zstd 압축을 사용하는 복합 Codec.
     *
     * ⚠️ [ZstdForyComposite] codec과 와이어 포맷이 상호 비호환입니다. **휘발성 캐시 전용**으로 사용하십시오.
     */
    val ZstdFastForyComposite: Codec by lazy { CompositeCodec(String, ZstdFastFory, ZstdFastFory) }

    /**
     * FastFory 직렬화 + Snappy 압축 Codec.
     *
     * ⚠️ [SnappyFory] codec과 와이어 포맷이 상호 비호환입니다. **휘발성 캐시 전용**으로 사용하십시오.
     */
    val SnappyFastFory: Codec by lazy { SnappyCodecV2(FastFory) }

    /**
     * Map 키: String, 값: FastFory 직렬화 + Snappy 압축을 사용하는 복합 Codec.
     *
     * ⚠️ [SnappyForyComposite] codec과 와이어 포맷이 상호 비호환입니다. **휘발성 캐시 전용**으로 사용하십시오.
     */
    val SnappyFastForyComposite: Codec by lazy { CompositeCodec(String, SnappyFastFory, SnappyFastFory) }

    /**
     * FastFory 직렬화 + Gzip 압축 Codec. 높은 압축률이 필요할 때 사용합니다.
     *
     * ⚠️ [GzipFory] codec과 와이어 포맷이 상호 비호환입니다. **휘발성 캐시 전용**으로 사용하십시오.
     */
    val GzipFastFory: Codec by lazy { GzipCodec(FastFory) }

    /**
     * Map 키: String, 값: FastFory 직렬화 + Gzip 압축을 사용하는 복합 Codec.
     *
     * ⚠️ [GzipForyComposite] codec과 와이어 포맷이 상호 비호환입니다. **휘발성 캐시 전용**으로 사용하십시오.
     */
    val GzipFastForyComposite: Codec by lazy { CompositeCodec(String, GzipFastFory, GzipFastFory) }

    /** Kryo5 직렬화 + LZ4 압축 Codec. 빠른 속도가 필요한 캐시 환경에 적합합니다. */
    val LZ4Kryo5: Codec by lazy { Lz4Codec(Kryo5) }

    /** Fory 직렬화 + LZ4 압축 Codec. 고성능 캐시 환경에서 기본으로 권장하는 조합입니다. */
    val LZ4Fory: Codec by lazy { Lz4Codec(Fory) }

    /** JDK 직렬화 + LZ4 압축 Codec */
    val LZ4Jdk: Codec by lazy { Lz4Codec(Jdk) }

    /** Map 키: String, 값: Kryo5 직렬화 + LZ4 압축을 사용하는 복합 Codec */
    val LZ4Kryo5Composite: Codec by lazy { CompositeCodec(String, LZ4Kryo5, LZ4Kryo5) }

    /** Map 키: String, 값: Fory 직렬화 + LZ4 압축을 사용하는 복합 Codec. Map 캐시에 권장하는 조합입니다. */
    val LZ4ForyComposite: Codec by lazy { CompositeCodec(String, LZ4Fory, LZ4Fory) }

    /** Map 키: String, 값: JDK 직렬화 + LZ4 압축을 사용하는 복합 Codec */
    val LZ4JdkComposite: Codec by lazy { CompositeCodec(String, LZ4Jdk, LZ4Jdk) }

    /** Kryo5 직렬화 + Snappy 압축 Codec */
    val SnappyKryo5: Codec by lazy { SnappyCodecV2(Kryo5) }

    /** Fory 직렬화 + Snappy 압축 Codec */
    val SnappyFory: Codec by lazy { SnappyCodecV2(Fory) }

    /** JDK 직렬화 + Snappy 압축 Codec */
    val SnappyJdk: Codec by lazy { SnappyCodecV2(Jdk) }

    /** Map 키: String, 값: Kryo5 직렬화 + Snappy 압축을 사용하는 복합 Codec */
    val SnappyKryo5Composite: Codec by lazy { CompositeCodec(String, SnappyKryo5, SnappyKryo5) }

    /** Map 키: String, 값: Fory 직렬화 + Snappy 압축을 사용하는 복합 Codec */
    val SnappyForyComposite: Codec by lazy { CompositeCodec(String, SnappyFory, SnappyFory) }

    /** Map 키: String, 값: JDK 직렬화 + Snappy 압축을 사용하는 복합 Codec */
    val SnappyJdkComposite: Codec by lazy { CompositeCodec(String, SnappyJdk, SnappyJdk) }

    /** Kryo5 직렬화 + Zstd 압축 Codec. 높은 압축률과 빠른 속도를 동시에 원할 때 사용합니다. */
    val ZstdKryo5: Codec by lazy { ZstdCodec(Kryo5) }

    /** Fory 직렬화 + Zstd 압축 Codec. 압축률과 속도의 균형이 뛰어난 조합입니다. */
    val ZstdFory: Codec by lazy { ZstdCodec(Fory) }

    /** JDK 직렬화 + Zstd 압축 Codec */
    val ZstdJdk: Codec by lazy { ZstdCodec(Jdk) }

    /** Map 키: String, 값: Kryo5 직렬화 + Zstd 압축을 사용하는 복합 Codec */
    val ZstdKryo5Composite: Codec by lazy { CompositeCodec(String, ZstdKryo5, ZstdKryo5) }

    /** Map 키: String, 값: Fory 직렬화 + Zstd 압축을 사용하는 복합 Codec */
    val ZstdForyComposite: Codec by lazy { CompositeCodec(String, ZstdFory, ZstdFory) }

    /** Map 키: String, 값: JDK 직렬화 + Zstd 압축을 사용하는 복합 Codec */
    val ZstdJdkComposite: Codec by lazy { CompositeCodec(String, ZstdJdk, ZstdJdk) }

    // -------------------------------------------------------------------------
    // Use-case oriented factory functions (H4 - Iteration 2)
    //
    // 이 팩토리 함수들은 기존 val 프로퍼티를 대체하지 않고, 사용 목적(use-case)에
    // 따라 적절한 Codec 을 쉽게 선택할 수 있도록 제공됩니다.
    // Java 호출자는 @JvmStatic 덕분에 `RedissonCodecs.forCache()` 형식으로 호출합니다.
    // -------------------------------------------------------------------------

    /**
     * 처리량 중심의 값(value) 캐시용 Codec.
     *
     * 1KB 이상의 객체를 자주 읽는 RBucket/RList 등 범용 value 캐시에 적합합니다.
     * LZ4 압축 + Fory 직렬화 조합으로 속도와 크기 균형을 제공합니다.
     */
    @JvmStatic
    fun forCache(): Codec = LZ4Fory

    /**
     * 최고 처리량(high-throughput) 캐시용 Codec.
     *
     * [forCache]보다 ~27% 높은 직렬화 처리량이 필요할 때 사용합니다.
     * FastFory(`SCHEMA_CONSISTENT` + `refTracking=false`) + LZ4 압축 조합입니다.
     * 직렬화 실패 시 [Fory]로 자동 fallback하여 마이그레이션 기간에도 안전합니다.
     *
     * ```kotlin
     * val config = Config()
     * config.codec = RedissonCodecs.forHighThroughput()
     * val redisson = Redisson.create(config)
     * ```
     *
     * ⚠️ [forCache] 및 [LZ4Fory] codec과 와이어 포맷이 상호 비호환입니다. **휘발성 캐시 전용**으로 사용하십시오.
     */
    @JvmStatic
    fun forHighThroughput(): Codec = LZ4FastFory

    /**
     * Map 형태의 캐시용 Codec.
     *
     * RMap / RLocalCachedMap 등 Map 컬렉션 캐시에 적합합니다.
     * 키는 [String], 값은 LZ4 + Fory 로 직렬화되는 [CompositeCodec] 조합입니다.
     */
    @JvmStatic
    fun forCacheMap(): Codec = LZ4ForyComposite

    /**
     * 범용 기본 Codec.
     *
     * 혼합된 읽기/쓰기 워크로드의 기본 선택지. Apache Fory 직렬화를 사용합니다.
     */
    @JvmStatic
    fun forGeneral(): Codec = Fory

    /**
     * 작은 값(<1KB) 전용 Codec.
     *
     * 작은 객체는 압축 오버헤드가 이익을 넘기 쉬우므로 압축을 생략한 Kryo5 를 권장합니다.
     */
    @JvmStatic
    fun forSmallValue(): Codec = Kryo5

    /**
     * 아카이브/콜드 스토리지용 Codec.
     *
     * 큰 객체를 드물게 읽고 쓰는 상황에서 최고의 압축률을 제공합니다.
     * Zstd 압축 + Fory 직렬화로 저장 공간 효율을 극대화합니다.
     */
    @JvmStatic
    fun forArchival(): Codec = ZstdFory

    /**
     * 호환성 우선 Codec.
     *
     * bluetape4k 를 사용하지 않는 외부 시스템과의 상호 운용이 필요할 때 사용합니다.
     * JDK 기본 직렬화를 사용하므로 성능은 낮지만 범용성이 높습니다.
     */
    @JvmStatic
    fun forCompatibility(): Codec = Jdk

    // -------------------------------------------------------------------------
    // JSON Codec safe factory functions
    // -------------------------------------------------------------------------

    /**
     * Creates an allow-listed Jackson 3 JSON codec.
     *
     * Use this factory instead of [Jackson3] for exposed Redis boundaries or multi-tenant data.
     * Decode rejects class names outside [allowedPackagePrefixes], and fallback binary decode is
     * disabled so non-JSON payloads cannot bypass the allow-list.
     *
     * ```kotlin
     * val codec = RedissonCodecs.jackson3(setOf("com.mycompany.", "io.bluetape4k."))
     * config.codec = codec
     * ```
     *
     * @param allowedPackagePrefixes allowed package prefixes, for example
     * `setOf("com.mycompany.", "io.bluetape4k.")`.
     */
    @JvmStatic
    fun jackson3(allowedPackagePrefixes: Set<String>): Codec =
        Jackson3Codec(allowedPackagePrefixes = allowedPackagePrefixes)

    /**
     * Creates an allow-listed Fastjson2 JSONB codec.
     *
     * Use this factory instead of [Fastjson2] for exposed Redis boundaries or multi-tenant data.
     * Decode rejects class names outside [allowedPackagePrefixes], and fallback binary decode is
     * disabled so non-JSONB payloads cannot bypass the allow-list.
     *
     * ```kotlin
     * val codec = RedissonCodecs.fastjson2(setOf("com.mycompany.", "io.bluetape4k."))
     * config.codec = codec
     * ```
     *
     * @param allowedPackagePrefixes allowed package prefixes, for example
     * `setOf("com.mycompany.", "io.bluetape4k.")`.
     */
    @JvmStatic
    fun fastjson2(allowedPackagePrefixes: Set<String>): Codec =
        Fastjson2Codec(allowedPackagePrefixes = allowedPackagePrefixes)

}
