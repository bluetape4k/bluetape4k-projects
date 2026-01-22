package io.bluetape4k.jwt.codec

import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.logging.KLogging
import io.jsonwebtoken.impl.compression.AbstractCompressionCodec

class ZstdCodec: AbstractCompressionCodec() {

    companion object: KLogging() {
        const val ALGORITHM = "ZSTD"
    }

    private val zstd = Compressors.Zstd

    override fun getAlgorithmName(): String = ALGORITHM

    override fun doCompress(payload: ByteArray?): ByteArray {
        return zstd.compress(payload)
    }

    override fun doDecompress(compressed: ByteArray?): ByteArray {
        return zstd.decompress(compressed)
    }
}
