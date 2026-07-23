package io.bluetape4k.redis.lettuce.codec

import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.io.serializer.JdkBinarySerializer

/**
 * 다양한 Serializer/Compressor 조합의 [LettuceBinaryCodec] 팩토리 모음.
 *
 * Protobuf 기반 Codec은 [io.bluetape4k.protobuf.serializers.LettuceProtobufCodecs]를 사용하세요.
 *
 * ```kotlin
 * // 기본 LZ4+Fory 코덱 사용
 * val codec = LettuceBinaryCodecs.default<MyData>()
 * val connection = redisClient.connect(codec)
 * connection.sync().set("key", myData)
 *
 * // Kryo + Snappy 조합 사용
 * val snappyKryoCodec = LettuceBinaryCodecs.snappyKryo<MyData>()
 * ```
 */
object LettuceBinaryCodecs {

    /**
     * 지정된 [BinarySerializer]를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     *
     * ```kotlin
     * val codec = LettuceBinaryCodecs.codec<MyData>(BinarySerializers.Kryo)
     * ```
     */
    fun <V: Any> codec(serializer: BinarySerializer): LettuceBinaryCodec<V> =
        LettuceBinaryCodec(serializer)

    /**
     * 기본 코덱(LZ4 + Fory)을 생성합니다.
     *
     * ```kotlin
     * val codec = LettuceBinaryCodecs.default<MyData>()
     * ```
     */
    fun <V: Any> default(): LettuceBinaryCodec<V> = lz4Fory()

    /**
     * Jdk Serializer를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> jdk(): LettuceBinaryCodec<V> = codec(JdkBinarySerializer())

    /**
     * Kryo Serializer를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> kryo(): LettuceBinaryCodec<V> = codec(BinarySerializers.Kryo)

    /**
     * Fory Serializer를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     *
     * 단일 인자 인코딩은 계속 [ByteArray]를 생성합니다. 호출자 소유 target 인코딩은 raw Fory가 direct
     * stream/ByteBuffer override를 제공할 때에만 bounded OutputStream 경로로 codec-level intermediary를
     * 제거하고 writer index를 마지막에 한 번만 커밋합니다. 이는 보편적인 zero-copy 보장이 아니며, 압축 Fory
     * 계열은 기존 ByteArray/압축 경로를 유지합니다.
     */
    fun <V: Any> fory(): LettuceBinaryCodec<V> = codec(BinarySerializers.Fory)


    /**
     * Jdk Serializer와 Gzip Compressor를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> gzipJdk(): LettuceBinaryCodec<V> = codec(BinarySerializers.GZipJdk)

    /**
     * Kryo Serializer와 Gzip Compressor를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> gzipKryo(): LettuceBinaryCodec<V> = codec(BinarySerializers.GZipKryo)

    /**
     * Fory Serializer와 Gzip Compressor를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> gzipFory(): LettuceBinaryCodec<V> = codec(BinarySerializers.GZipFory)


    /**
     * Jdk Serializer와 Deflate Compressor를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> deflateJdk(): LettuceBinaryCodec<V> = codec(BinarySerializers.DeflateJdk)

    /**
     * Kryo Serializer와 Deflate Compressor를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> deflateKryo(): LettuceBinaryCodec<V> = codec(BinarySerializers.DeflateKryo)

    /**
     * Fory Serializer와 Deflate Compressor를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> deflateFory(): LettuceBinaryCodec<V> = codec(BinarySerializers.DeflateFory)

    /**
     * Jdk Serializer와 LZ4 Compressor를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> lz4Jdk(): LettuceBinaryCodec<V> = codec(BinarySerializers.LZ4Jdk)

    /**
     * Kryo Serializer와 LZ4 Compressor를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> lz4Kryo(): LettuceBinaryCodec<V> = codec(BinarySerializers.LZ4Kryo)

    /**
     * Fory Serializer와 LZ4 Compressor를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> lz4Fory(): LettuceBinaryCodec<V> = codec(BinarySerializers.LZ4Fory)

    /**
     * Jdk Serializer와 Snappy Compressor를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> snappyJdk(): LettuceBinaryCodec<V> = codec(BinarySerializers.SnappyJdk)

    /**
     * Kryo Serializer와 Snappy Compressor를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> snappyKryo(): LettuceBinaryCodec<V> = codec(BinarySerializers.SnappyKryo)

    /**
     * Fory Serializer와 Snappy Compressor를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> snappyFory(): LettuceBinaryCodec<V> = codec(BinarySerializers.SnappyFory)


    /**
     * Jdk Serializer와 Zstd Compressor를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> zstdJdk(): LettuceBinaryCodec<V> = codec(BinarySerializers.ZstdJdk)

    /**
     * Kryo Serializer와 Zstd Compressor를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> zstdKryo(): LettuceBinaryCodec<V> = codec(BinarySerializers.ZstdKryo)

    /**
     * Fory Serializer와 Zstd Compressor를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> zstdFory(): LettuceBinaryCodec<V> = codec(BinarySerializers.ZstdFory)


    // -------------------------------------------------------------------------
    // FastFory 계열 코덱 (SCHEMA_CONSISTENT 모드, 고성능)
    // -------------------------------------------------------------------------

    /**
     * FastFory Serializer를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     *
     * FastFory는 `CompatibleMode.SCHEMA_CONSISTENT`를 사용하는 고성능 직렬화기입니다.
     * 단일 인자 인코딩은 계속 [ByteArray]를 생성합니다. 호출자 소유 target 인코딩은 raw FastFory가 direct
     * stream/ByteBuffer override를 제공할 때에만 bounded OutputStream 경로로 codec-level intermediary를
     * 제거하고 writer index를 마지막에 한 번만 커밋합니다. 이는 보편적인 zero-copy 보장이 아니며, 압축 FastFory
     * 계열은 기존 ByteArray/압축 경로를 유지합니다.
     *
     * ```kotlin
     * val codec = LettuceBinaryCodecs.fastFory<MyData>()
     * val connection = redisClient.connect(codec)
     * connection.sync().set("key", myData)
     * val result = connection.sync().get("key")
     * ```
     *
     * ⚠️ **와이어 포맷 경고**
     * - `CompatibleMode.SCHEMA_CONSISTENT`를 사용하며, 기본 Fory codec과 **와이어 포맷이 상호 비호환**합니다.
     * - fallback이 없으므로 기존 Fory 데이터를 FastFory로 읽으면 역직렬화 오류가 발생합니다.
     * - **휘발성 캐시(Redis, 메모리 캐시) 전용** — 영속 저장에 사용하지 마십시오.
     * - **순환 참조 객체 불가** (refTracking=false).
     * - **스키마 진화 불가** — 필드 추가/제거 시 기존 데이터 역직렬화 실패.
     */
    fun <V: Any> fastFory(): LettuceBinaryCodec<V> = codec(BinarySerializers.FastFory)

    /**
     * FastFory Serializer와 LZ4 Compressor를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     *
     * LZ4 압축으로 네트워크 전송 크기를 줄이면서 고성능 직렬화를 제공합니다.
     *
     * ```kotlin
     * val codec = LettuceBinaryCodecs.lz4FastFory<MyData>()
     * val connection = redisClient.connect(codec)
     * connection.sync().set("key", myData)
     * ```
     *
     * ⚠️ **와이어 포맷 경고**
     * - `CompatibleMode.SCHEMA_CONSISTENT`를 사용하며, 기본 Fory codec과 **와이어 포맷이 상호 비호환**합니다.
     * - fallback이 없으므로 기존 Fory 데이터를 FastFory로 읽으면 역직렬화 오류가 발생합니다.
     * - **휘발성 캐시(Redis, 메모리 캐시) 전용** — 영속 저장에 사용하지 마십시오.
     * - **순환 참조 객체 불가** (refTracking=false).
     * - **스키마 진화 불가** — 필드 추가/제거 시 기존 데이터 역직렬화 실패.
     */
    fun <V: Any> lz4FastFory(): LettuceBinaryCodec<V> = codec(BinarySerializers.LZ4FastFory)

    /**
     * FastFory Serializer와 Zstd Compressor를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     *
     * Zstd 압축은 LZ4보다 높은 압축률을 제공하며, 큰 객체 캐시에 적합합니다.
     *
     * ```kotlin
     * val codec = LettuceBinaryCodecs.zstdFastFory<MyData>()
     * val connection = redisClient.connect(codec)
     * connection.sync().set("key", largeData)
     * ```
     *
     * ⚠️ **와이어 포맷 경고**
     * - `CompatibleMode.SCHEMA_CONSISTENT`를 사용하며, 기본 Fory codec과 **와이어 포맷이 상호 비호환**합니다.
     * - fallback이 없으므로 기존 Fory 데이터를 FastFory로 읽으면 역직렬화 오류가 발생합니다.
     * - **휘발성 캐시(Redis, 메모리 캐시) 전용** — 영속 저장에 사용하지 마십시오.
     * - **순환 참조 객체 불가** (refTracking=false).
     * - **스키마 진화 불가** — 필드 추가/제거 시 기존 데이터 역직렬화 실패.
     */
    fun <V: Any> zstdFastFory(): LettuceBinaryCodec<V> = codec(BinarySerializers.ZstdFastFory)

    /**
     * FastFory Serializer와 Snappy Compressor를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     *
     * ```kotlin
     * val codec = LettuceBinaryCodecs.snappyFastFory<MyData>()
     * val connection = redisClient.connect(codec)
     * connection.sync().set("key", myData)
     * ```
     *
     * ⚠️ **와이어 포맷 경고**
     * - `CompatibleMode.SCHEMA_CONSISTENT`를 사용하며, 기본 Fory codec과 **와이어 포맷이 상호 비호환**합니다.
     * - fallback이 없으므로 기존 Fory 데이터를 FastFory로 읽으면 역직렬화 오류가 발생합니다.
     * - **휘발성 캐시(Redis, 메모리 캐시) 전용** — 영속 저장에 사용하지 마십시오.
     * - **순환 참조 객체 불가** (refTracking=false).
     * - **스키마 진화 불가** — 필드 추가/제거 시 기존 데이터 역직렬화 실패.
     */
    fun <V: Any> snappyFastFory(): LettuceBinaryCodec<V> = codec(BinarySerializers.SnappyFastFory)

    /**
     * FastFory Serializer와 GZip Compressor를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     *
     * ```kotlin
     * val codec = LettuceBinaryCodecs.gzipFastFory<MyData>()
     * val connection = redisClient.connect(codec)
     * connection.sync().set("key", myData)
     * ```
     *
     * ⚠️ **와이어 포맷 경고**
     * - `CompatibleMode.SCHEMA_CONSISTENT`를 사용하며, 기본 Fory codec과 **와이어 포맷이 상호 비호환**합니다.
     * - fallback이 없으므로 기존 Fory 데이터를 FastFory로 읽으면 역직렬화 오류가 발생합니다.
     * - **휘발성 캐시(Redis, 메모리 캐시) 전용** — 영속 저장에 사용하지 마십시오.
     * - **순환 참조 객체 불가** (refTracking=false).
     * - **스키마 진화 불가** — 필드 추가/제거 시 기존 데이터 역직렬화 실패.
     */
    fun <V: Any> gzipFastFory(): LettuceBinaryCodec<V> = codec(BinarySerializers.GZipFastFory)
}
