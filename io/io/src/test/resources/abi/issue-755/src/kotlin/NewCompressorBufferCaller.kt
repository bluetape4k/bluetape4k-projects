package io.bluetape4k.io.compressor.abi.issue755.kotlin

import java.nio.ByteBuffer

object NewCompressorBufferCaller {
    @JvmStatic
    fun main(args: Array<String>) {
        val compressor = LegacyCompressorImplementation()
        val payload = byteArrayOf(1, 2, 3, 4)
        val wire = ByteBuffer.allocate(payload.size)
        check(compressor.compress(ByteBuffer.wrap(payload), wire) == payload.size)
        wire.flip()
        val plain = ByteBuffer.allocate(payload.size)
        check(compressor.decompress(wire, plain) == payload.size)
        check(plain.array().contentEquals(payload))
        println("new-kotlin=PASS")
    }
}
