package io.bluetape4k.images.similarity

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.JpegWriter
import io.bluetape4k.images.AbstractImageTest
import io.bluetape4k.images.immutableImageOf
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.utils.Resourcex
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.amshove.kluent.shouldThrow
import org.junit.jupiter.api.Test

class HashSimilarityTest: AbstractImageTest() {

    companion object: KLoggingChannel() {
        private const val HOMER_JPG = "images/homer.jpg"
        private const val LANDSCAPE_JPG = "images/landscape.jpg"
    }

    private fun loadImage(path: String): ImmutableImage =
        immutableImageOf(Resourcex.getInputStream(path)!!)

    /** scrimage [JpegWriter] 로 90% 품질 JPEG 재인코딩. */
    private fun ImmutableImage.toJpeg90(): ImmutableImage =
        immutableImageOf(bytes(JpegWriter(90, false)))

    @Test
    fun `identical images have all hash distances zero`() {
        val a = loadImage(HOMER_JPG)
        val b = loadImage(HOMER_JPG)

        HashDistance.hamming(a.ahashOf(), b.ahashOf()) shouldBeEqualTo 0
        HashDistance.hamming(a.dhashOf(), b.dhashOf()) shouldBeEqualTo 0
        HashDistance.hamming(a.whashOf(), b.whashOf()) shouldBeEqualTo 0
        HashDistance.hamming(a.phashOf(), b.phashOf()) shouldBeEqualTo 0

        HashDistance.hamming(a.ahash(), b.ahash()) shouldBeEqualTo 0
        HashDistance.hamming(a.dhash(), b.dhash()) shouldBeEqualTo 0
        HashDistance.hamming(a.whash(), b.whash()) shouldBeEqualTo 0
    }

    @Test
    fun `jpeg 90 percent re-encoding keeps hash distances small`() {
        val original = loadImage(HOMER_JPG)
        val recompressed = original.toJpeg90()

        val aDist = HashDistance.hamming(original.ahash(), recompressed.ahash())
        val dDist = HashDistance.hamming(original.dhash(), recompressed.dhash())
        val wDist = HashDistance.hamming(original.whash(), recompressed.whash())
        val pDist = HashDistance.hamming(original.phash(), recompressed.phash())

        log.debug("jpeg-90 hash distances: aHash=$aDist, dHash=$dDist, wHash=$wDist, pHash=$pDist")

        aDist shouldBeLessOrEqualTo 4
        dDist shouldBeLessOrEqualTo 4
        wDist shouldBeLessOrEqualTo 6
        pDist shouldBeLessOrEqualTo 4
    }

    @Test
    fun `different images produce large hash distances`() {
        val homer = loadImage(HOMER_JPG)
        val landscape = loadImage(LANDSCAPE_JPG)

        val aDist = HashDistance.hamming(homer.ahash(), landscape.ahash())
        val dDist = HashDistance.hamming(homer.dhash(), landscape.dhash())
        val wDist = HashDistance.hamming(homer.whash(), landscape.whash())
        val pDist = HashDistance.hamming(homer.phash(), landscape.phash())

        log.debug("different images hash distances: aHash=$aDist, dHash=$dDist, wHash=$wDist, pHash=$pDist")

        aDist shouldBeGreaterThan 15
        dDist shouldBeGreaterThan 15
        wDist shouldBeGreaterThan 15
        pDist shouldBeGreaterThan 15
    }

    @Test
    fun `phashOf BITS_64 matches legacy phash`() {
        val image = loadImage(HOMER_JPG)

        val legacy = image.phash()
        val new64 = image.phashOf(PHashSize.BITS_64)[0]

        new64 shouldBeEqualTo legacy
    }

    @Test
    fun `phashOfDistanceTo matches HashDistance hamming on phashOf`() {
        val a = loadImage(HOMER_JPG)
        val b = loadImage(LANDSCAPE_JPG)

        val direct = a.phashOfDistanceTo(b, PHashSize.BITS_256)
        val viaHashes = HashDistance.hamming(a.phashOf(PHashSize.BITS_256), b.phashOf(PHashSize.BITS_256))

        direct shouldBeEqualTo viaHashes
    }

    @Test
    fun `HashDistance hamming throws when array lengths differ`() {
        val invocation = {
            HashDistance.hamming(LongArray(1), LongArray(4))
        }
        invocation shouldThrow IllegalArgumentException::class
    }

    @Test
    fun `ahashOf returns LongArray of expected size for each HashSize`() {
        val image = loadImage(HOMER_JPG)

        image.ahashOf(HashSize.BITS_64).size shouldBeEqualTo 1
        image.ahashOf(HashSize.BITS_256).size shouldBeEqualTo 4
        image.ahashOf(HashSize.BITS_1024).size shouldBeEqualTo 16
    }

    @Test
    fun `dhashOf returns LongArray of expected size for each HashSize`() {
        val image = loadImage(HOMER_JPG)

        image.dhashOf(HashSize.BITS_64).size shouldBeEqualTo 1
        image.dhashOf(HashSize.BITS_256).size shouldBeEqualTo 4
        image.dhashOf(HashSize.BITS_1024).size shouldBeEqualTo 16
    }

    @Test
    fun `whashOf returns LongArray of expected size for each PHashSize`() {
        val image = loadImage(HOMER_JPG)

        image.whashOf(PHashSize.BITS_64).size shouldBeEqualTo 1
        image.whashOf(PHashSize.BITS_256).size shouldBeEqualTo 4
        image.whashOf(PHashSize.BITS_1024).size shouldBeEqualTo 16
    }

    @Test
    fun `phashOf returns LongArray of expected size for each PHashSize`() {
        val image = loadImage(HOMER_JPG)

        image.phashOf(PHashSize.BITS_64).size shouldBeEqualTo 1
        image.phashOf(PHashSize.BITS_256).size shouldBeEqualTo 4
        image.phashOf(PHashSize.BITS_1024).size shouldBeEqualTo 16
    }

    @Test
    fun `convenience shorthand functions match BITS_64 LongArray first slot`() {
        val image = loadImage(HOMER_JPG)

        image.ahash() shouldBeEqualTo image.ahashOf(HashSize.BITS_64)[0]
        image.dhash() shouldBeEqualTo image.dhashOf(HashSize.BITS_64)[0]
        image.whash() shouldBeEqualTo image.whashOf(PHashSize.BITS_64)[0]
    }

    @Test
    fun `HashDistance hamming Long overload counts bit differences correctly`() {
        HashDistance.hamming(0L, 0L) shouldBeEqualTo 0
        HashDistance.hamming(0L, 1L) shouldBeEqualTo 1
        HashDistance.hamming(0L, 0xFFL) shouldBeEqualTo 8
        HashDistance.hamming(0b1010L, 0b0101L) shouldBeEqualTo 4
        HashDistance.hamming(-1L, 0L) shouldBeEqualTo 64
    }

    @Test
    fun `HashDistance hamming LongArray overload sums per-slot popcount`() {
        val a = longArrayOf(0L, 0L, 0L, 0L)
        val b = longArrayOf(-1L, 0xFFL, 0b1010L, 0L)

        // 64 + 8 + 2 + 0 = 74
        HashDistance.hamming(a, b) shouldBeEqualTo 74
    }
}
