package io.bluetape4k.javers.codecs

import com.google.gson.JsonObject
import io.bluetape4k.io.compressor.Compressor


class CompressableBinaryJaversCodec(
    private val innerCodec: BinaryJaversCodec,
    private val compressor: Compressor,
): JaversCodec<ByteArray> {

    override fun encode(jsonElement: JsonObject): ByteArray {
        return compressor.compress(innerCodec.encode(jsonElement))
    }

    override fun decode(encodedData: ByteArray): JsonObject? {
        return innerCodec.decode(compressor.decompress(encodedData))
    }
}
