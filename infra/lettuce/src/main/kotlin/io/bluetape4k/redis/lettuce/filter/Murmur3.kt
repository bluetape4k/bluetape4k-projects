package io.bluetape4k.redis.lettuce.filter

/**
 * MurmurHash3 x64 128-bit 순수 Kotlin 구현.
 *
 * 외부 의존성 없이 Guava/Apache Commons 없이 동작합니다.
 * BloomFilter/CuckooFilter에서 비트 위치 계산에 사용됩니다.
 */
internal object Murmur3 {
    private const val C1 = -0x783c846eeebdac2bL  // 0x87c37b91114253d5
    private const val C2 = 0x4cf5ad432745937fL

    /**
     * 바이트 배열에 대한 128-bit MurmurHash3 (x64) 해시를 반환합니다.
     * @return LongArray(2) — [h1, h2]
     */
    fun hash128x64(data: ByteArray, seed: Long = 0L): LongArray {
        var h1 = seed
        var h2 = seed
        val len = data.size
        val nblocks = len ushr 4  // len / 16

        // body: 16바이트 블록 처리
        var i = 0
        while (i < nblocks * 16) {
            var k1 = data.getLeL(i)
            var k2 = data.getLeL(i + 8)

            k1 = k1 * C1; k1 = k1.rotateLeft(31); k1 = k1 * C2; h1 = h1 xor k1
            h1 = h1.rotateLeft(27); h1 += h2; h1 = h1 * 5 + 0x52dce729L

            k2 = k2 * C2; k2 = k2.rotateLeft(33); k2 = k2 * C1; h2 = h2 xor k2
            h2 = h2.rotateLeft(31); h2 += h1; h2 = h2 * 5 + 0x38495ab5L

            i += 16
        }

        // tail: 나머지 바이트 처리
        val tailStart = nblocks * 16
        var k1 = 0L
        var k2 = 0L
        val remaining = len and 15

        if (remaining >= 15) k2 = k2 xor ((data[tailStart + 14].toLong() and 0xff) shl 48)
        if (remaining >= 14) k2 = k2 xor ((data[tailStart + 13].toLong() and 0xff) shl 40)
        if (remaining >= 13) k2 = k2 xor ((data[tailStart + 12].toLong() and 0xff) shl 32)
        if (remaining >= 12) k2 = k2 xor ((data[tailStart + 11].toLong() and 0xff) shl 24)
        if (remaining >= 11) k2 = k2 xor ((data[tailStart + 10].toLong() and 0xff) shl 16)
        if (remaining >= 10) k2 = k2 xor ((data[tailStart + 9].toLong() and 0xff) shl 8)
        if (remaining >= 9)  k2 = k2 xor (data[tailStart + 8].toLong() and 0xff)
        if (remaining > 8) { k2 = k2 * C2; k2 = k2.rotateLeft(33); k2 = k2 * C1; h2 = h2 xor k2 }

        if (remaining >= 8) k1 = k1 xor ((data[tailStart + 7].toLong() and 0xff) shl 56)
        if (remaining >= 7) k1 = k1 xor ((data[tailStart + 6].toLong() and 0xff) shl 48)
        if (remaining >= 6) k1 = k1 xor ((data[tailStart + 5].toLong() and 0xff) shl 40)
        if (remaining >= 5) k1 = k1 xor ((data[tailStart + 4].toLong() and 0xff) shl 32)
        if (remaining >= 4) k1 = k1 xor ((data[tailStart + 3].toLong() and 0xff) shl 24)
        if (remaining >= 3) k1 = k1 xor ((data[tailStart + 2].toLong() and 0xff) shl 16)
        if (remaining >= 2) k1 = k1 xor ((data[tailStart + 1].toLong() and 0xff) shl 8)
        if (remaining >= 1) k1 = k1 xor (data[tailStart].toLong() and 0xff)
        if (remaining > 0) { k1 = k1 * C1; k1 = k1.rotateLeft(31); k1 = k1 * C2; h1 = h1 xor k1 }

        // finalization
        h1 = h1 xor len.toLong()
        h2 = h2 xor len.toLong()
        h1 += h2; h2 += h1
        h1 = fmix64(h1); h2 = fmix64(h2)
        h1 += h2; h2 += h1

        return longArrayOf(h1, h2)
    }

    private fun fmix64(k: Long): Long {
        // 두의 보수 표현: 0xff51afd7ed558ccd = -0x00ae502812aa7333
        //                 0xc4ceb9fe1a85ec53 = -0x3b314601e57a13ad
        var h = k
        h = h xor (h ushr 33)
        h *= -0x00ae502812aa7333L
        h = h xor (h ushr 33)
        h *= -0x3b314601e57a13adL
        h = h xor (h ushr 33)
        return h
    }

    /** 리틀 엔디언 방식으로 8바이트 → Long 변환. */
    private fun ByteArray.getLeL(index: Int): Long =
        (this[index].toLong() and 0xff) or
        ((this[index + 1].toLong() and 0xff) shl 8) or
        ((this[index + 2].toLong() and 0xff) shl 16) or
        ((this[index + 3].toLong() and 0xff) shl 24) or
        ((this[index + 4].toLong() and 0xff) shl 32) or
        ((this[index + 5].toLong() and 0xff) shl 40) or
        ((this[index + 6].toLong() and 0xff) shl 48) or
        ((this[index + 7].toLong() and 0xff) shl 56)
}
