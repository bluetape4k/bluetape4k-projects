package io.bluetape4k.images.tiles

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.coroutines.flow.extensions.mapParallel
import io.bluetape4k.images.batch.DEFAULT_MAX_TILE_COUNT
import io.bluetape4k.images.batch.defaultImageBatchParallelism
import io.bluetape4k.support.requirePositiveNumber
import kotlinx.coroutines.flow.Flow
import java.awt.image.BufferedImage

/**
 * 큰 이미지를 타일로 나누고 다시 병합하는 처리기입니다.
 */
class TileProcessor(
    private val maxTileCount: Int = DEFAULT_MAX_TILE_COUNT,
    private val parallelism: Int = defaultImageBatchParallelism(),
) {
    init {
        maxTileCount.requirePositiveNumber("maxTileCount")
        parallelism.requirePositiveNumber("parallelism")
    }

    /**
     * 이미지를 지정 타일 크기로 분할합니다.
     */
    fun split(image: ImmutableImage, tileSize: TileSize): List<ImageTile> {
        val tilesX = image.width.ceilDiv(tileSize.width)
        val tilesY = image.height.ceilDiv(tileSize.height)
        val tileCount = tilesX * tilesY
        require(tileCount <= maxTileCount) {
            "타일 수가 허용 한도를 초과했습니다. tileCount=$tileCount, maxTileCount=$maxTileCount"
        }

        return buildList(tileCount) {
            for (y in 0 until image.height step tileSize.height) {
                for (x in 0 until image.width step tileSize.width) {
                    val width = minOf(tileSize.width, image.width - x)
                    val height = minOf(tileSize.height, image.height - y)
                    add(ImageTile(x, y, width, height, image.subimage(x, y, width, height)))
                }
            }
        }
    }

    /**
     * 타일 목록을 하나의 이미지로 병합합니다.
     */
    fun merge(
        tiles: Collection<ImageTile>,
        width: Int,
        height: Int,
    ): ImmutableImage {
        width.requirePositiveNumber("width")
        height.requirePositiveNumber("height")
        require(tiles.isNotEmpty()) { "tiles는 비어 있을 수 없습니다." }
        require(tiles.size <= maxTileCount) {
            "타일 수가 허용 한도를 초과했습니다. tileCount=${tiles.size}, maxTileCount=$maxTileCount"
        }

        val output = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val graphics = output.createGraphics()
        try {
            tiles.sortedWith(compareBy<ImageTile> { it.y }.thenBy { it.x }).forEach { tile ->
                require(tile.x >= 0 && tile.y >= 0) { "타일 좌표는 0 이상이어야 합니다. tile=$tile" }
                require(tile.x + tile.width <= width && tile.y + tile.height <= height) {
                    "타일이 출력 이미지 영역을 벗어났습니다. tile=$tile, width=$width, height=$height"
                }
                graphics.drawImage(tile.image.awt(), tile.x, tile.y, null)
            }
        } finally {
            graphics.dispose()
        }

        return ImmutableImage.wrapAwt(output)
    }

    /**
     * 타일 스트림을 병렬 변환합니다.
     */
    fun <R> processTiles(
        tiles: Flow<ImageTile>,
        transform: suspend (ImageTile) -> R,
    ): Flow<R> =
        tiles.mapParallel(parallelism) { tile -> transform(tile) }

    private fun Int.ceilDiv(divisor: Int): Int =
        (this + divisor - CEIL_DIV_OFFSET) / divisor

    private companion object {
        private const val CEIL_DIV_OFFSET = 1
    }
}
