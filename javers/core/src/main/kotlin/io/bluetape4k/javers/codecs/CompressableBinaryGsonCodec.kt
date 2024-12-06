package io.bluetape4k.javers.codecs

import com.google.gson.JsonObject
import io.bluetape4k.io.compressor.Compressor


class CompressableBinaryGsonCodec(
    private val innerCodec: BinaryGsonCodec,
    private val compressor: Compressor,
): GsonCodec<ByteArray> {

    override fun encode(jsonElement: JsonObject): ByteArray {
        return compressor.compress(innerCodec.encode(jsonElement))
    }

    override fun decode(encodedData: ByteArray): JsonObject? {
        return innerCodec.decode(compressor.decompress(encodedData))
    }
}
