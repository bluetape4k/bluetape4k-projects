package io.bluetape4k.images

import io.bluetape4k.io.toInputStream
import io.bluetape4k.support.requirePositiveNumber
import java.awt.Graphics2D
import java.awt.GraphicsEnvironment
import java.awt.Image
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.awt.image.ImageObserver
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.URL
import javax.imageio.ImageIO
import javax.imageio.stream.ImageInputStream
import javax.imageio.stream.ImageOutputStream

/**
 * [BufferedImage]를 [format] 형식으로 [path]에 저장합니다.
 */
fun BufferedImage.write(format: ImageFormat, path: String): Boolean {
    return ImageIO.write(this, format.name, File(path))
}

/**
 * [BufferedImage]를 [format] 형식으로 [file]에 저장합니다.
 */
fun BufferedImage.write(format: ImageFormat, file: File): Boolean {
    return ImageIO.write(this, format.name, file)
}

/**
 * [BufferedImage]를 [format] 형식으로 [outputStream]에 저장합니다.
 */
fun BufferedImage.write(format: ImageFormat, outputStream: OutputStream): Boolean {
    return ImageIO.write(this, format.name, outputStream)
}

/**
 * [BufferedImage]를 [format] 형식으로 [outputStream]에 저장합니다.
 */
fun BufferedImage.write(format: ImageFormat, outputStream: ImageOutputStream): Boolean {
    return ImageIO.write(this, format.name, outputStream)
}

/**
 * [BufferedImage]에 [source] 이미지를 [transform] 변환 정보를 적용하여 그립니다.
 */
fun BufferedImage.drawRenderedImage(source: BufferedImage, transform: AffineTransform) {
    useGraphics { graphics ->
        graphics.drawRenderedImage(source, transform)
    }
}

/**
 * [BufferedImage]에 [image]를 그립니다.
 *
 * @param image 그릴 이미지
 * @param transform 변환 정보
 * @param observer 이미지 변환 관찰자 (null 이면 무시)
 */
fun BufferedImage.drawImage(
    image: Image,
    transform: AffineTransform,
    observer: ImageObserver? = null,
) {
    useGraphics { graphics ->
        graphics.drawImage(image, transform, observer)
    }
}

/**
 * [BufferedImage]에 (x,y) 좌표에 [image]를 그립니다.
 *
 * @param image 그릴 이미지
 * @param x x 좌표
 * @param y y 좌표
 * @param observer 이미지 변환 관찰자 (null 이면 무시)
 */
fun BufferedImage.drawImage(
    image: Image,
    x: Int = 0,
    y: Int = 0,
    observer: ImageObserver? = null,
) {
    useGraphics { graphics ->
        graphics.drawImage(image, x, y, observer)
    }
}

/**
 * [BufferedImage]에 주어진 영역에 [image]를 그립니다.
 *
 * @param image 그릴 이미지
 * @param x x 좌표
 * @param y y 좌표
 * @param width 너비
 * @param height 높이
 * @param observer 이미지 변환 관찰자 (null 이면 무시)
 */
fun BufferedImage.drawImage(
    image: Image,
    x: Int = 0,
    y: Int = 0,
    width: Int = this@drawImage.width,
    height: Int = this@drawImage.height,
    observer: ImageObserver? = null,
) {
    useGraphics { graphics ->
        graphics.drawImage(image, x, y, width, height, observer)
    }
}

/**
 * [BufferedImage]에 [action]을 수행합니다.
 *
 * ```
 * val image = bufferedImageOf(100, 100)
 *
 * image.useGraphics { graphics ->
 *    graphics.color = Color.RED
 *    graphics.fillRect(0, 0, 100, 100)
 *    graphics.color = Color.BLACK
 *    graphics.drawString("Hello, World!", 10, 10)
 * }
 *
 * @param action 그래픽 작업
 */
inline fun BufferedImage.useGraphics(action: (graphics: Graphics2D) -> Unit) {
    val graphics = this.createGraphics()
    try {
        action(graphics)
    } finally {
        graphics.dispose()
    }
}

/**
 * 새로운 [BufferedImage]를 생성합니다.
 *
 * ```
 * val image = bufferedImageOf(200, 100)
 * ```
 *
 * @param w width   이미지 넓이
 * @param h height  이미지 높이
 * @return [BufferedImage] instance
 */
fun bufferedImageOf(w: Int, h: Int): BufferedImage {
    w.requirePositiveNumber("w")
    h.requirePositiveNumber("h")

    val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
    val gd = ge.defaultScreenDevice
    val gc = gd.defaultConfiguration
    return gc.createCompatibleImage(w, h)
}

/**
 * [InputStream] 정보를 읽어 [BufferedImage]를 생성합니다.
 */
fun bufferedImageOf(inputStream: InputStream): BufferedImage = ImageIO.read(inputStream)

/**
 * [ImageInputStream] 정보를 읽어 [BufferedImage]를 생성합니다.
 */
fun bufferedImageOf(inputStream: ImageInputStream): BufferedImage = ImageIO.read(inputStream)

/**
 * [File] 정보를 읽어 [BufferedImage]를 생성합니다.
 */
fun bufferedImageOf(file: File): BufferedImage = ImageIO.read(file)

/**
 * [URL] 정보를 읽어 [BufferedImage]를 생성합니다.
 */
fun bufferedImageOf(url: URL): BufferedImage = ImageIO.read(url)

/**
 * 이미지 정보를 담은 [bytes]를 읽어 [BufferedImage] 를 생성합니다.
 */
fun bufferedImageOf(bytes: ByteArray): BufferedImage = ImageIO.read(bytes.toInputStream())

/**
 * [BufferedImage] 정보를 ByteArray 로 변환합니다.
 *
 * @param formatName 이미지 포맷 ([ImageFormat])
 * @return 이미지 정보를 담은 ByteArray
 */
fun BufferedImage.toByteArray(formatName: String): ByteArray {
    return ByteArrayOutputStream().use { bos ->
        ImageIO.write(this, formatName, bos)
        bos.toByteArray()
    }
}
