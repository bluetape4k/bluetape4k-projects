package io.bluetape4k.images.splitter

import io.bluetape4k.coroutines.flow.async
import io.bluetape4k.images.ImageFormat
import io.bluetape4k.images.coroutines.SuspendImageWriter
import io.bluetape4k.images.coroutines.SuspendJpegWriter
import io.bluetape4k.images.immutableImageOf
import io.bluetape4k.io.toByteArray
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requirePositiveNumber
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.imageio.ImageIO

/**
 * Height가 아주 큰 이미지 (예: 상품 이미지) 를 [defaultMaxHeight] 크기로 분할하는 ImageSplitter
 *
 * @property defaultMaxHeight 분할할 이미지의 Height
 */
class ImageSplitter private constructor(val defaultMaxHeight: Int) {

    companion object: KLogging() {
        const val DEFAULT_MIN_HEIGHT = 128
        const val DEFAULT_MAX_HEIGHT = 2048

        @JvmStatic
        operator fun invoke(maxHeight: Int = DEFAULT_MAX_HEIGHT): ImageSplitter {
            return ImageSplitter(maxHeight.coerceAtLeast(DEFAULT_MIN_HEIGHT))
        }
    }

    /**
     * [input] 이미지를 [format] 으로 변환하고 [splitHeight] 만큼 분할하여 [ByteArray] 로 반환합니다.
     *
     * ```
     * val splitter = ImageSplitter()
     * val input: InputStream = ...
     * val format = ImageFormat.JPG
     * val splitHeight = 1024
     * val images: Flow<ByteArray> = splitter.split(input, format, splitHeight)
     * ```
     *
     * @param input         원본 이미지 정보
     * @param format        변환할 이미지 포맷 (JPG, PNG ...) (기본: [ImageFormat.JPG])
     * @param splitHeight   분할할 이미지의 Height (기본: [defaultMaxHeight])
     * @return 분할된 이미지 정보의 Flow
     */
    fun split(
        input: InputStream,
        format: ImageFormat = ImageFormat.JPG,
        splitHeight: Int = this.defaultMaxHeight,
    ): Flow<ByteArray> {
        splitHeight.requirePositiveNumber("splitHeight")
        val height = splitHeight.coerceAtLeast(DEFAULT_MIN_HEIGHT)
        log.debug { "Split image. format=$format, split height=$height" }

        val source = ImageIO.read(input)
        val srcHeight = source.height
        val srcWidth = source.width

        if (srcHeight <= height) {
            return flowOf(input.toByteArray())
        }

        return channelFlow {
            getHeights(height, srcHeight)
                .async { h ->
                    ByteArrayOutputStream().use { bos ->
                        val splitImage = source.getSubimage(0, h, srcWidth, height.coerceAtMost(srcHeight - h))
                        ImageIO.write(splitImage, format.name, bos)
                        bos.toByteArray()
                    }
                }
                .collect {
                    send(it)
                }
        }
    }

    /**
     * [input] 이미지를 [format] 으로 변환하고 [splitHeight] 만큼 분할하여 [ByteArray] 로 반환합니다.
     *
     * ```
     * val splitter = ImageSplitter()
     * val input: InputStream = ...
     * val format = ImageFormat.JPG
     * val splitHeight = 1024
     * val images: Flow<ByteArray> = splitter.splitAndCompress(input, format, splitHeight)
     * ```
     *
     * @param input         원본 이미지 정보
     * @param format        변환할 이미지 포맷 (JPG, PNG ...) (기본: [ImageFormat.JPG])
     * @param splitHeight   분할할 이미지의 Height (기본: [defaultMaxHeight])
     * @param writer        이미지를 변환할 Writer (기본: [CoJpegWriter.Default])
     * @return 분할된 이미지 정보의 Flow
     */
    fun splitAndCompress(
        input: InputStream,
        format: ImageFormat = ImageFormat.JPG,
        splitHeight: Int = this.defaultMaxHeight,
        writer: SuspendImageWriter = SuspendJpegWriter.Default,
    ): Flow<ByteArray> {
        return channelFlow {
            split(input, format, splitHeight).buffer()
                .async { bytes ->
                    immutableImageOf(bytes).forWriter(writer).bytes()
                }
                .collect {
                    send(it)
                }
        }
    }

    private fun getHeightWithIndex(height: Int, sourceHeight: Int): Flow<Pair<Int, Int>> = flow {
        var index = 0
        var y = 0
        while (y < sourceHeight) {
            emit(index to y)
            index++
            y += height
        }
    }

    private fun getHeights(height: Int, maxHeight: Int): Flow<Int> = flow {
        var y = 0
        while (y < maxHeight) {
            emit(y)
            y += height
        }
    }
}
