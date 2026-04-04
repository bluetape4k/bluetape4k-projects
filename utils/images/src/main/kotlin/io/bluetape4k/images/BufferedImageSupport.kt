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
 *
 * ## 동작/계약
 * - 내부적으로 `ImageIO.write`를 호출합니다.
 * - 반환값은 해당 포맷 writer 존재 여부/쓰기 성공 여부를 따릅니다.
 *
 * ```kotlin
 * val written = image.write(ImageFormat.PNG, "out.png")
 * // written == true
 * ```
 */
fun BufferedImage.write(format: ImageFormat, path: String): Boolean {
    return ImageIO.write(this, format.name, File(path))
}

/**
 * [BufferedImage]를 [format] 형식으로 [file]에 저장합니다.
 *
 * ```kotlin
 * val written = image.write(ImageFormat.PNG, File("out.png"))
 * // written == true
 * ```
 */
fun BufferedImage.write(format: ImageFormat, file: File): Boolean {
    return ImageIO.write(this, format.name, file)
}

/**
 * [BufferedImage]를 [format] 형식으로 [outputStream]에 저장합니다.
 *
 * ## 동작/계약
 * - 스트림을 닫지 않으며 호출자가 생명주기를 관리해야 합니다.
 *
 * ```kotlin
 * val bos = ByteArrayOutputStream()
 * val written = image.write(ImageFormat.JPG, bos)
 * // written == true
 * // bos.toByteArray().isNotEmpty() == true
 * ```
 */
fun BufferedImage.write(format: ImageFormat, outputStream: OutputStream): Boolean {
    return ImageIO.write(this, format.name, outputStream)
}

/**
 * [BufferedImage]를 [format] 형식으로 [outputStream]에 저장합니다.
 *
 * ```kotlin
 * val ios: ImageOutputStream = ImageIO.createImageOutputStream(File("out.png"))
 * val written = image.write(ImageFormat.PNG, ios)
 * // written == true
 * ```
 */
fun BufferedImage.write(format: ImageFormat, outputStream: ImageOutputStream): Boolean {
    return ImageIO.write(this, format.name, outputStream)
}

/**
 * [BufferedImage]에 [source] 이미지를 [transform] 변환 정보를 적용하여 그립니다.
 *
 * ```kotlin
 * val base = bufferedImageOf(200, 200)
 * val overlay = bufferedImageOf(100, 100)
 * val transform = AffineTransform.getTranslateInstance(50.0, 50.0)
 * base.drawRenderedImage(overlay, transform)
 * // base에 overlay가 (50, 50) 위치에 그려짐
 * ```
 */
fun BufferedImage.drawRenderedImage(source: BufferedImage, transform: AffineTransform) {
    useGraphics { graphics ->
        graphics.drawRenderedImage(source, transform)
    }
}

/**
 * [BufferedImage]에 [image]를 그립니다.
 *
 * ```kotlin
 * val base = bufferedImageOf(200, 200)
 * val overlay = bufferedImageOf(50, 50)
 * val transform = AffineTransform.getScaleInstance(0.5, 0.5)
 * base.drawImage(overlay, transform)
 * // base에 overlay가 0.5배 축소 후 그려짐
 * ```
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
 * ```kotlin
 * val base = bufferedImageOf(200, 200)
 * val overlay = bufferedImageOf(50, 50)
 * base.drawImage(overlay, x = 10, y = 20)
 * // base의 (10, 20) 위치에 overlay가 그려짐
 * ```
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
 * ```kotlin
 * val base = bufferedImageOf(200, 200)
 * val overlay = bufferedImageOf(100, 100)
 * base.drawImage(overlay, x = 0, y = 0, width = 80, height = 80)
 * // base의 (0, 0)에서 80x80 영역에 overlay가 축소되어 그려짐
 * ```
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
 * ## 동작/계약
 * - `createGraphics()`로 얻은 Graphics2D를 `finally`에서 항상 `dispose()`합니다.
 * - 수신 이미지는 `action` 내용에 따라 mutate 됩니다.
 *
 * ```kotlin
 * val image = bufferedImageOf(100, 100)
 *
 * image.useGraphics { graphics ->
 *    graphics.color = Color.RED
 *    graphics.fillRect(0, 0, 100, 100)
 *    graphics.color = Color.BLACK
 *    graphics.drawString("Hello, World!", 10, 10)
 * }
 * ```
 *
 * @param action 그래픽 작업
 */
inline fun BufferedImage.useGraphics(
    action: (graphics: Graphics2D) -> Unit,
) {
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
 * ## 동작/계약
 * - `w`, `h`는 양수여야 하며 위반 시 검증 예외가 발생합니다.
 * - headless 환경이면 `TYPE_INT_ARGB`, GUI 환경이면 디바이스 호환 이미지를 생성합니다.
 *
 * ```kotlin
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

    if (GraphicsEnvironment.isHeadless()) {
        return BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
    }

    val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
    val gd = ge.defaultScreenDevice
    val gc = gd.defaultConfiguration
    return gc.createCompatibleImage(w, h)
}

/**
 * [InputStream] 정보를 읽어 [BufferedImage]를 생성합니다.
 *
 * ## 동작/계약
 * - 읽기 실패 시 `ImageIO.read`가 `null` 또는 예외를 반환할 수 있습니다.
 *
 * ```kotlin
 * val image = bufferedImageOf(File("photo.jpg").inputStream())
 * // image.width > 0
 * // image.height > 0
 * ```
 */
fun bufferedImageOf(inputStream: InputStream): BufferedImage = ImageIO.read(inputStream)

/**
 * [ImageInputStream] 정보를 읽어 [BufferedImage]를 생성합니다.
 *
 * ```kotlin
 * val iis: ImageInputStream = ImageIO.createImageInputStream(File("photo.png"))
 * val image = bufferedImageOf(iis)
 * // image.width > 0
 * ```
 */
fun bufferedImageOf(inputStream: ImageInputStream): BufferedImage = ImageIO.read(inputStream)

/**
 * [File] 정보를 읽어 [BufferedImage]를 생성합니다.
 *
 * ```kotlin
 * val image = bufferedImageOf(File("photo.jpg"))
 * // image.width > 0
 * // image.height > 0
 * ```
 */
fun bufferedImageOf(file: File): BufferedImage = ImageIO.read(file)

/**
 * [URL] 정보를 읽어 [BufferedImage]를 생성합니다.
 *
 * ```kotlin
 * val url = URL("https://example.com/photo.png")
 * val image = bufferedImageOf(url)
 * // image.width > 0
 * ```
 */
fun bufferedImageOf(url: URL): BufferedImage = ImageIO.read(url)

/**
 * 이미지 정보를 담은 [bytes]를 읽어 [BufferedImage] 를 생성합니다.
 *
 * ```kotlin
 * val bytes: ByteArray = File("photo.jpg").readBytes()
 * val image = bufferedImageOf(bytes)
 * // image.width > 0
 * // image.height > 0
 * ```
 */
fun bufferedImageOf(bytes: ByteArray): BufferedImage = ImageIO.read(bytes.toInputStream())

/**
 * [BufferedImage] 정보를 ByteArray 로 변환합니다.
 *
 * ## 동작/계약
 * - 메모리 버퍼([ByteArrayOutputStream])를 새로 할당해 인코딩 결과를 반환합니다.
 * - 지원되지 않는 `formatName`이면 빈 결과 또는 실패가 발생할 수 있습니다.
 *
 * ```kotlin
 * val bytes = image.toByteArray("png")
 * // bytes.isNotEmpty() == true
 * ```
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
