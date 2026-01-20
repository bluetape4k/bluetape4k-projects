package io.bluetape4k.javers.codecs

import com.google.gson.JsonObject
import io.bluetape4k.io.compressor.Compressor
import io.bluetape4k.io.compressor.Compressors

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
