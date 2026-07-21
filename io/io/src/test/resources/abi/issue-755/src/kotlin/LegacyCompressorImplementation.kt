package io.bluetape4k.io.compressor.abi.issue755.kotlin

import io.bluetape4k.io.compressor.Compressor

class LegacyCompressorImplementation: Compressor {
    override fun compress(plain: ByteArray?): ByteArray = (plain ?: ByteArray(0)).reversedArray()

    override fun decompress(compressed: ByteArray?): ByteArray = (compressed ?: ByteArray(0)).reversedArray()
}
