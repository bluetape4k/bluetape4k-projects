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
 * Redisson에서 사용할 수 있는 다양한 [Codec] 인스턴스를 모아놓은 객체입니다.
 *
 * Redisson 기본 제공 Codec보다 성능이 우수한 커스텀 Codec과 직렬화+압축 조합 Codec을 제공합니다.
 *
 * ## 직렬화 방식
 * - [Kryo5]: Kryo5 직렬화 (빠르고 컴팩트한 바이너리 포맷)
 * - [Fory]: Apache Fory 직렬화 (Kryo5 대비 2~10배 빠름, 기본값)
 * - [Jdk]: JDK 기본 직렬화 (호환성 높지만 속도 느림)
 * - [Jackson3]: Jackson 3 커스텀 JSON 엔벨로프 (`_type`/`_data`) — human-readable, non-JVM 클라이언트 연동
 * - [Fastjson2]: Fastjson2 JSONB WriteClassName — JSONB 바이너리, non-JVM 클라이언트 연동
 *
 * ## 압축 방식
 * - Gzip: 높은 압축률, 느린 속도
 * - LZ4: 낮은 압축률, 매우 빠른 속도 (캐시 환경에 권장)
 * - Snappy: LZ4와 유사한 특성, Google 제공
 * - Zstd: Gzip 수준의 압축률 + LZ4 수준의 속도
 *
 * ## Composite Codec
 * Map 키는 [String] Codec, 값은 지정한 직렬화+압축 Codec을 사용하는 [CompositeCodec] 조합입니다.
 * Map, Set, SortedSet 등 컬렉션 타입에 유용합니다.
 *
 * ## 권장 조합
 * - 범용 고성능: [LZ4Fory] 또는 [LZ4ForyComposite]
 * - 압축률 우선: [ZstdFory] 또는 [ZstdForyComposite]
 * - 호환성 우선: [Jdk]
 * - Human-readable JSON: [Jackson3] (신뢰된 환경) 또는 [jackson3] (보안 factory)
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
     * Apache Fory 직렬화 Codec.
     * Kryo5 대비 2~10배 빠른 직렬화 속도를 제공하며, 직렬화 실패 시 Kryo5로 자동 전환합니다.
     */
    val Fory: Codec by lazy { ForyCodec() }

    /**
     * JDK 기본 직렬화 Codec.
     * 범용 호환성이 높으나 성능이 낮습니다. 레거시 시스템과의 연동에 사용하세요.
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
     * `allowedPackagePrefixes`를 지정한 안전한 Jackson 3 JSON Codec을 생성합니다.
     *
     * 외부에 노출된 Redis 또는 보안 요구사항이 있는 환경에서 [Jackson3] 싱글턴 대신 사용하십시오.
     * 지정한 prefix에 속하지 않는 클래스 이름이 역직렬화 요청되면 [SecurityException]이 발생합니다.
     *
     * ```kotlin
     * val codec = RedissonCodecs.jackson3(setOf("com.mycompany.", "io.bluetape4k."))
     * config.codec = codec
     * ```
     *
     * @param allowedPackagePrefixes 허용할 패키지 prefix 집합 (예: `setOf("com.mycompany.", "io.bluetape4k.")`)
     */
    @JvmStatic
    fun jackson3(allowedPackagePrefixes: Set<String>): Codec =
        Jackson3Codec(allowedPackagePrefixes = allowedPackagePrefixes)

    /**
     * `allowedPackagePrefixes`를 지정한 안전한 Fastjson2 JSONB Codec을 생성합니다.
     *
     * 외부에 노출된 Redis 또는 보안 요구사항이 있는 환경에서 [Fastjson2] 싱글턴 대신 사용하십시오.
     * 지정한 prefix에 속하지 않는 AutoType 클래스 역직렬화 요청 시 [SecurityException]이 발생합니다.
     *
     * ```kotlin
     * val codec = RedissonCodecs.fastjson2(setOf("com.mycompany.", "io.bluetape4k."))
     * config.codec = codec
     * ```
     *
     * @param allowedPackagePrefixes 허용할 패키지 prefix 집합 (예: `setOf("com.mycompany.", "io.bluetape4k.")`)
     */
    @JvmStatic
    fun fastjson2(allowedPackagePrefixes: Set<String>): Codec =
        Fastjson2Codec(allowedPackagePrefixes = allowedPackagePrefixes)

}
