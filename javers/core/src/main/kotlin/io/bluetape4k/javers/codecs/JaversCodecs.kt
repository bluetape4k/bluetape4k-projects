package io.bluetape4k.javers.codecs

import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.javers.codecs.JaversCodecs.String
import io.bluetape4k.support.unsafeLazy


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
    val Default by unsafeLazy { String }

    // String Codecs

    /** JSON 문자열 코덱 (압축 없음) */
    val String by unsafeLazy { StringJaversCodec() }

    /** GZip 압축 문자열 코덱 */
    val GZipString by unsafeLazy { CompressableStringJaversCodec(String, Compressors.GZip) }

    /** Deflate 압축 문자열 코덱 */
    val DeflateString by unsafeLazy { CompressableStringJaversCodec(String, Compressors.Deflate) }

    /** LZ4 압축 문자열 코덱 */
    val LZ4String by unsafeLazy { CompressableStringJaversCodec(String, Compressors.LZ4) }

    /** Snappy 압축 문자열 코덱 */
    val SnappyString by unsafeLazy { CompressableStringJaversCodec(String, Compressors.Snappy) }

    /** Zstd 압축 문자열 코덱 */
    val ZstdString by unsafeLazy { CompressableStringJaversCodec(String, Compressors.Zstd) }

    // Binary Codecs - JDK Serialization

    /** JDK 직렬화 바이너리 코덱 */
    val Jdk by unsafeLazy { BinaryJaversCodec(BinarySerializers.Jdk) }

    val DeflateJdk by unsafeLazy { CompressableBinaryJaversCodec(Jdk, Compressors.Deflate) }
    val GZipJdk by unsafeLazy { CompressableBinaryJaversCodec(Jdk, Compressors.GZip) }
    val LZ4Jdk by unsafeLazy { CompressableBinaryJaversCodec(Jdk, Compressors.LZ4) }
    val SnappyJdk by unsafeLazy { CompressableBinaryJaversCodec(Jdk, Compressors.Snappy) }
    val ZstdJdk by unsafeLazy { CompressableBinaryJaversCodec(Jdk, Compressors.Zstd) }

    // Binary Codecs - Kryo Serialization

    /** Kryo 직렬화 바이너리 코덱 */
    val Kryo by unsafeLazy { BinaryJaversCodec(BinarySerializers.Kryo) }

    val DeflateKryo by unsafeLazy { CompressableBinaryJaversCodec(Kryo, Compressors.Deflate) }
    val GZipKryo by unsafeLazy { CompressableBinaryJaversCodec(Kryo, Compressors.GZip) }
    val LZ4Kryo by unsafeLazy { CompressableBinaryJaversCodec(Kryo, Compressors.LZ4) }
    val SnappyKryo by unsafeLazy { CompressableBinaryJaversCodec(Kryo, Compressors.Snappy) }
    val ZstdKryo by unsafeLazy { CompressableBinaryJaversCodec(Kryo, Compressors.Zstd) }

    // Binary Codecs - Fory Serialization

    /** Fory 직렬화 바이너리 코덱 */
    val Fory by unsafeLazy { BinaryJaversCodec(BinarySerializers.Fory) }

    val DeflateFory by unsafeLazy { CompressableBinaryJaversCodec(Fory, Compressors.Deflate) }
    val GZipFory by unsafeLazy { CompressableBinaryJaversCodec(Fory, Compressors.GZip) }
    val LZ4Fory by unsafeLazy { CompressableBinaryJaversCodec(Fory, Compressors.LZ4) }
    val SnappyFory by unsafeLazy { CompressableBinaryJaversCodec(Fory, Compressors.Snappy) }
    val ZstdFory by unsafeLazy { CompressableBinaryJaversCodec(Fory, Compressors.Zstd) }

    // Map Codec

    /** [JsonObject] ↔ `Map<String, Any?>` 변환 코덱 */
    val Map by unsafeLazy { MapJaversCodec() }
}
