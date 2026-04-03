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

}
