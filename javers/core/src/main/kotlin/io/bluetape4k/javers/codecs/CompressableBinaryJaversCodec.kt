package io.bluetape4k.javers.codecs

import com.google.gson.JsonObject
import io.bluetape4k.io.compressor.Compressor
import io.bluetape4k.io.compressor.Compressors


/**
 * [BinaryJaversCodec] 결과를 [Compressor]로 추가 압축하는 코덱.
 *
 * ## 동작/계약
 * - encode: innerCodec 직렬화 → 압축
 * - decode: 압축 해제 → innerCodec 역직렬화
 *
 * ```kotlin
 * val codec = CompressableBinaryJaversCodec(
 *     BinaryJaversCodec(BinarySerializers.Kryo),
 *     Compressors.LZ4
 * )
 * val compressed = codec.encode(jsonObject)
 * val decoded = codec.decode(compressed)
 * ```
 *
 * @property innerCodec 바이너리 직렬화를 수행하는 내부 코덱
 * @property compressor 압축/해제에 사용할 [Compressor] (기본값: GZip)
 */
class CompressableBinaryJaversCodec(
    private val innerCodec: BinaryJaversCodec,
    private val compressor: Compressor = Compressors.GZip,
): JaversCodec<ByteArray> {

    override fun encode(jsonElement: JsonObject): ByteArray {
        return compressor.compress(innerCodec.encode(jsonElement))
    }

    override fun decode(encodedData: ByteArray): JsonObject? {
        return innerCodec.decode(compressor.decompress(encodedData))
    }
}
