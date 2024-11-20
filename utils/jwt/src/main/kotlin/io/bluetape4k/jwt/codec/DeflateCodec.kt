package io.bluetape4k.jwt.codec

import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.unsafeLazy
import io.jsonwebtoken.impl.compression.AbstractCompressionCodec

class DeflateCodec: AbstractCompressionCodec() {

    companion object: KLogging() {
        const val ALGORITHM = "DEFLATE"
    }

    private val deflate by unsafeLazy { Compressors.Deflate }

    override fun getAlgorithmName(): String = ALGORITHM

    override fun doCompress(payload: ByteArray?): ByteArray {
        return deflate.compress(payload)
    }

    override fun doDecompress(compressed: ByteArray?): ByteArray {
        return deflate.decompress(compressed)
    }

}
