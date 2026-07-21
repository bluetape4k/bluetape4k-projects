package io.bluetape4k.io.compressor.abi.issue755.kotlin

import java.nio.ByteBuffer

object LegacyCompressorCaller {
    @JvmStatic
    fun main(args: Array<String>) {
        val compressor = LegacyCompressorImplementation()
        val payload = byteArrayOf(1, 2, 3, 4)
        val wire = compressor.compress(payload)
        check(compressor.decompress(wire).contentEquals(payload))
        val legacyWire = compressor.compress(ByteBuffer.wrap(payload))
        check(compressor.decompress(legacyWire).array().contentEquals(payload))
        println("legacy-kotlin=PASS")
    }
}
