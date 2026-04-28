package io.bluetape4k.images.tiles

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.coroutines.flow.extensions.mapParallel
import io.bluetape4k.images.batch.DEFAULT_MAX_TILE_COUNT
import io.bluetape4k.images.batch.defaultImageBatchParallelism
import io.bluetape4k.support.requirePositiveNumber
import kotlinx.coroutines.flow.Flow
import java.awt.image.BufferedImage

/**
 * 큰 이미지를 타일로 분할하고, 각 타일을 병렬 처리한 후 다시 하나의 이미지로 병합하는 처리기입니다.
 *
 * [split]으로 이미지를 격자 타일로 잘라내고, [processTiles]로 각 타일에 독립적인 변환을 적용한 다음,
 * [merge]로 원래 크기의 이미지를 재조립합니다.
 * 타일 수가 [maxTileCount]를 초과하면 예외가 발생합니다.
 *
 * ```kotlin
 * import com.sksamuel.scrimage.ImmutableImage
 * import io.bluetape4k.images.tiles.*
 * import kotlinx.coroutines.flow.asFlow
 * import kotlinx.coroutines.flow.toList
 *
 * val image: ImmutableImage = ImmutableImage.loader().fromFile(File("large-photo.jpg"))
 * val processor = TileProcessor(maxTileCount = 256, parallelism = 4)
 *
 * // 1. 512×512 타일로 분할
 * val tiles: List<ImageTile> = processor.split(image, TileSize(width = 512, height = 512))
 *
 * // 2. 각 타일에 병렬 변환 적용 (예: 그레이스케일 변환)
 * val processedTiles: List<ImageTile> = processor
 *     .processTiles(tiles.asFlow()) { tile ->
 *         tile.copy(image = tile.image.filter(GrayscaleFilter()))
 *     }
 *     .toList()
 *
 * // 3. 처리된 타일을 원래 크기로 병합
 * val result: ImmutableImage = processor.merge(processedTiles, image.width, image.height)
 * ```
 *
 * @param maxTileCount 허용되는 최대 타일 수 (기본값: [DEFAULT_MAX_TILE_COUNT])
 * @param parallelism [processTiles] 병렬 처리 동시성 수준 (기본값: [defaultImageBatchParallelism])
 * @see split
 * @see merge
 * @see processTiles
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
     * 이미지를 [tileSize] 크기의 격자 타일로 분할합니다.
     *
     * 이미지 크기가 타일 크기의 배수가 아닌 경우 가장자리 타일은 더 작을 수 있습니다.
     * 총 타일 수가 [maxTileCount]를 초과하면 [IllegalArgumentException]이 발생합니다.
     *
     * ```kotlin
     * import com.sksamuel.scrimage.ImmutableImage
     * import io.bluetape4k.images.tiles.*
     *
     * val image: ImmutableImage = ImmutableImage.loader().fromFile(File("large.jpg"))
     * val processor = TileProcessor()
     *
     * // 256×256 타일로 분할 — 1920×1080 이미지 → 8×5 = 40 타일
     * val tiles: List<ImageTile> = processor.split(image, TileSize(width = 256, height = 256))
     *
     * println("타일 수: ${tiles.size}")               // 40
     * tiles.forEach { tile ->
     *     println("  (${tile.x}, ${tile.y}) ${tile.width}×${tile.height}")
     * }
     * ```
     *
     * @param image 분할할 원본 이미지
     * @param tileSize 각 타일의 목표 크기
     * @return 좌상단(0,0)부터 행 우선(row-major) 순서로 정렬된 [ImageTile] 목록
     * @throws IllegalArgumentException 타일 수가 [maxTileCount]를 초과할 경우
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
     * 타일 컬렉션을 하나의 이미지로 병합합니다.
     *
     * 각 타일을 [ImageTile.y] → [ImageTile.x] 순서로 정렬한 뒤 `width × height` 크기의
     * 새 이미지에 순서대로 합성합니다. 타일이 출력 이미지 영역을 벗어나거나 좌표가 음수이면
     * [IllegalArgumentException]이 발생합니다.
     *
     * ```kotlin
     * import com.sksamuel.scrimage.ImmutableImage
     * import io.bluetape4k.images.tiles.*
     *
     * val image: ImmutableImage = ImmutableImage.loader().fromFile(File("large.jpg"))
     * val processor = TileProcessor()
     *
     * // 분할 → 변환 → 병합
     * val tiles = processor.split(image, TileSize(width = 512, height = 512))
     *
     * // 각 타일에 밝기 조정 후 원래 크기로 합성
     * val brightened = tiles.map { tile ->
     *     tile.copy(image = tile.image.filter(BrightnessFilter(1.2f)))
     * }
     * val merged: ImmutableImage = processor.merge(brightened, image.width, image.height)
     *
     * merged.output(JpegWriter.Default, File("output.jpg"))
     * ```
     *
     * @param tiles 병합할 [ImageTile] 컬렉션 (순서 무관, 좌표로 재정렬)
     * @param width 출력 이미지의 너비 (픽셀)
     * @param height 출력 이미지의 높이 (픽셀)
     * @return 타일이 합성된 [ImmutableImage]
     * @throws IllegalArgumentException tiles가 비어있거나, [maxTileCount]를 초과하거나,
     *   타일 좌표가 출력 이미지 영역을 벗어난 경우
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
            tiles.sortedWith(TILE_ORDER).forEach { tile ->
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
     * 타일 Flow를 병렬로 변환하여 결과 Flow를 반환합니다.
     *
     * 각 타일에 대해 [transform]을 [parallelism] 수준의 동시성으로 실행합니다.
     * [split]의 결과를 `List.asFlow()`로 변환하여 넘기면 됩니다.
     *
     * ```kotlin
     * import com.sksamuel.scrimage.ImmutableImage
     * import com.sksamuel.scrimage.filter.GrayscaleFilter
     * import io.bluetape4k.images.tiles.*
     * import kotlinx.coroutines.flow.asFlow
     * import kotlinx.coroutines.flow.toList
     *
     * val image: ImmutableImage = ImmutableImage.loader().fromFile(File("large.jpg"))
     * val processor = TileProcessor(parallelism = 4)
     * val tiles = processor.split(image, TileSize(256, 256))
     *
     * // 각 타일을 병렬로 그레이스케일 변환 → 원본 타일 좌표 유지
     * val grayTiles: List<ImageTile> = processor
     *     .processTiles(tiles.asFlow()) { tile ->
     *         tile.copy(image = tile.image.filter(GrayscaleFilter()))
     *     }
     *     .toList()
     *
     * // 처리된 타일을 다시 병합
     * val result = processor.merge(grayTiles, image.width, image.height)
     * ```
     *
     * @param tiles 처리할 [ImageTile]의 Flow
     * @param transform 각 타일에 적용할 suspend 변환 함수
     * @return 변환 결과 [R]의 Flow
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
        private val TILE_ORDER: Comparator<ImageTile> = compareBy<ImageTile> { it.y }.thenBy { it.x }
    }
}
