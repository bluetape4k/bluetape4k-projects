package io.bluetape4k.jwt.codec

import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.io.compressor.LZ4Compressor
import io.bluetape4k.logging.KLogging
import io.jsonwebtoken.impl.compression.AbstractCompressionCodec

class Lz4Codec: AbstractCompressionCodec() {

    companion object: KLogging() {
        const val ALGORITHM = "LZ4"
    }

    private val lz4: LZ4Compressor = Compressors.LZ4

    override fun getAlgorithmName(): String {
        return ALGORITHM
    }

    override fun doCompress(payload: ByteArray?): ByteArray {
        return lz4.compress(payload)
    }

    override fun doDecompress(compressed: ByteArray?): ByteArray {
        return lz4.decompress(compressed)
    }
}
