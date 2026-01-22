package io.bluetape4k.jwt.codec

import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.logging.KLogging
import io.jsonwebtoken.impl.compression.AbstractCompressionCodec

class GzipCodec: AbstractCompressionCodec() {

    companion object: KLogging() {
        const val ALGORITHM = "GZIP"
    }

    private val gzip = Compressors.GZip

    override fun getAlgorithmName(): String = ALGORITHM

    override fun doCompress(payload: ByteArray?): ByteArray {
        return gzip.compress(payload)
    }

    override fun doDecompress(compressed: ByteArray?): ByteArray {
        return gzip.decompress(compressed)
    }

}
