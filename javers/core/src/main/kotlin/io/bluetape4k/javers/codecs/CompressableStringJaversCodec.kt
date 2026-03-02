package io.bluetape4k.javers.codecs

import com.google.gson.JsonObject
import io.bluetape4k.io.compressor.Compressor
import io.bluetape4k.io.compressor.Compressors

/**
 * [StringJaversCodec] 결과를 [Compressor]로 추가 압축하는 문자열 코덱.
 *
 * ## 동작/계약
 * - encode: innerCodec 문자열화 → 압축
 * - decode: 압축 해제 → innerCodec 파싱
 *
 * ```kotlin
 * val codec = CompressableStringJaversCodec(
 *     StringJaversCodec(),
 *     Compressors.LZ4
 * )
 * val compressed = codec.encode(jsonObject)
 * val decoded = codec.decode(compressed)
 * ```
 *
 * @property innerCodec 문자열 변환을 수행하는 내부 코덱
 * @property compressor 압축/해제에 사용할 [Compressor] (기본값: GZip)
 */
class CompressableStringJaversCodec(
    private val innerCodec: StringJaversCodec,
    private val compressor: Compressor = Compressors.GZip,
): JaversCodec<String> {

    override fun encode(jsonElement: JsonObject): String {
        return compressor.compress(innerCodec.encode(jsonElement))
    }

    override fun decode(encodedData: String): JsonObject? {
        return innerCodec.decode(compressor.decompress(encodedData))
    }
}
