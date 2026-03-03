package io.bluetape4k.javers.codecs

import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.javers.codecs.JaversCodecs.String


/**
 * 미리 구성된 [JaversCodec] 인스턴스들을 제공하는 팩토리 객체.
 *
 * ## 동작/계약
 * - 모든 코덱은 lazy 초기화되어 최초 접근 시 생성된다
 * - String 계열: JSON 문자열 기반 인코딩 (선택적 압축)
 * - Binary 계열: [BinarySerializer] + 선택적 압축 (Jdk/Kryo/Fory)
 * - Map 계열: [JsonObject] ↔ `Map<String, Any?>` 변환
 *
 * ```kotlin
 * val codec = JaversCodecs.LZ4String
 * val encoded = codec.encode(jsonObject)
 * val decoded = codec.decode(encoded)
 * // decoded.toString() == jsonObject.toString()
 * ```
 */
object JaversCodecs {

    /** 기본 코덱 ([String]) */
    val Default by lazy { String }

    // String Codecs

    /** JSON 문자열 코덱 (압축 없음) */
    val String by lazy { StringJaversCodec() }

    /** GZip 압축 문자열 코덱 */
    val GZipString by lazy { CompressableStringJaversCodec(String, Compressors.GZip) }

    /** Deflate 압축 문자열 코덱 */
    val DeflateString by lazy { CompressableStringJaversCodec(String, Compressors.Deflate) }

    /** LZ4 압축 문자열 코덱 */
    val LZ4String by lazy { CompressableStringJaversCodec(String, Compressors.LZ4) }

    /** Snappy 압축 문자열 코덱 */
    val SnappyString by lazy { CompressableStringJaversCodec(String, Compressors.Snappy) }

    /** Zstd 압축 문자열 코덱 */
    val ZstdString by lazy { CompressableStringJaversCodec(String, Compressors.Zstd) }

    // Binary Codecs - JDK Serialization

    /** JDK 직렬화 바이너리 코덱 */
    val Jdk by lazy { BinaryJaversCodec(BinarySerializers.Jdk) }

    val DeflateJdk by lazy { CompressableBinaryJaversCodec(Jdk, Compressors.Deflate) }
    val GZipJdk by lazy { CompressableBinaryJaversCodec(Jdk, Compressors.GZip) }
    val LZ4Jdk by lazy { CompressableBinaryJaversCodec(Jdk, Compressors.LZ4) }
    val SnappyJdk by lazy { CompressableBinaryJaversCodec(Jdk, Compressors.Snappy) }
    val ZstdJdk by lazy { CompressableBinaryJaversCodec(Jdk, Compressors.Zstd) }

    // Binary Codecs - Kryo Serialization

    /** Kryo 직렬화 바이너리 코덱 */
    val Kryo by lazy { BinaryJaversCodec(BinarySerializers.Kryo) }

    val DeflateKryo by lazy { CompressableBinaryJaversCodec(Kryo, Compressors.Deflate) }
    val GZipKryo by lazy { CompressableBinaryJaversCodec(Kryo, Compressors.GZip) }
    val LZ4Kryo by lazy { CompressableBinaryJaversCodec(Kryo, Compressors.LZ4) }
    val SnappyKryo by lazy { CompressableBinaryJaversCodec(Kryo, Compressors.Snappy) }
    val ZstdKryo by lazy { CompressableBinaryJaversCodec(Kryo, Compressors.Zstd) }

    // Binary Codecs - Fory Serialization

    /** Fory 직렬화 바이너리 코덱 */
    val Fory by lazy { BinaryJaversCodec(BinarySerializers.Fory) }

    val DeflateFory by lazy { CompressableBinaryJaversCodec(Fory, Compressors.Deflate) }
    val GZipFory by lazy { CompressableBinaryJaversCodec(Fory, Compressors.GZip) }
    val LZ4Fory by lazy { CompressableBinaryJaversCodec(Fory, Compressors.LZ4) }
    val SnappyFory by lazy { CompressableBinaryJaversCodec(Fory, Compressors.Snappy) }
    val ZstdFory by lazy { CompressableBinaryJaversCodec(Fory, Compressors.Zstd) }

    // Map Codec

    /** [JsonObject] ↔ `Map<String, Any?>` 변환 코덱 */
    val Map by lazy { MapJaversCodec() }
}
