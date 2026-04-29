package io.bluetape4k.images.batch

import java.nio.file.Path
import javax.imageio.ImageIO

/**
 * 이미지를 완전히 디코딩하지 않고 첫 프레임의 픽셀 수를 읽습니다.
 */
fun probeImagePixelCount(path: Path): Long? {
    val input = ImageIO.createImageInputStream(path.toFile()) ?: return null
    input.use { stream ->
        val readers = ImageIO.getImageReaders(stream)
        if (!readers.hasNext()) {
            return null
        }

        val reader = readers.next()
        try {
            reader.input = stream
            return reader.getWidth(0).toLong() * reader.getHeight(0).toLong()
        } finally {
            reader.dispose()
        }
    }
}
