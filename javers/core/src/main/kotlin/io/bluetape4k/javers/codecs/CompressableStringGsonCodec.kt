package io.bluetape4k.javers.codecs

import com.google.gson.JsonObject
import io.bluetape4k.io.compressor.Compressor

class CompressableStringGsonCodec(
    private val innerCodec: StringGsonCodec,
    private val compressor: Compressor,
): GsonCodec<String> {

    override fun encode(jsonElement: JsonObject): String {
        return compressor.compress(innerCodec.encode(jsonElement))
    }

    override fun decode(encodedData: String): JsonObject? {
        return innerCodec.decode(compressor.decompress(encodedData))
    }
}
