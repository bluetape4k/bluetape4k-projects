package io.bluetape4k.images.tiles

import io.bluetape4k.images.AbstractImageTest
import io.bluetape4k.images.immutableImageOf
import io.bluetape4k.utils.Resourcex
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.junit.jupiter.api.Test

class TileProcessorTest: AbstractImageTest() {

    @Test
    fun `split and merge preserve image size`() {
        val image = immutableImageOf(Resourcex.getInputStream(CAFE_JPG)!!)
        val processor = TileProcessor(maxTileCount = TEST_MAX_TILE_COUNT, parallelism = TEST_PARALLELISM)

        val tiles = processor.split(image, TileSize(TEST_TILE_WIDTH, TEST_TILE_HEIGHT))
        val merged = processor.merge(tiles, image.width, image.height)

        tiles.size shouldBeGreaterThan 1
        merged.width shouldBeEqualTo image.width
        merged.height shouldBeEqualTo image.height
    }

    private companion object {
        private const val TEST_PARALLELISM = 1
        private const val TEST_MAX_TILE_COUNT = 4096
        private const val TEST_TILE_WIDTH = 64
        private const val TEST_TILE_HEIGHT = 64
    }
}
