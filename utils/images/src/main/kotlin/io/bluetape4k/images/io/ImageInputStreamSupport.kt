package io.bluetape4k.images.io

import javax.imageio.stream.ImageInputStream

/**
 * Coroutines 환경에서 [ImageInputStream]을 안전하게 사용하고 자동으로 닫습니다.
 *
 * ```kotlin
 * val iis: ImageInputStream = ImageIO.createImageInputStream(File("photo.png"))
 * val image = iis.useSuspending { stream ->
 *     ImageIO.read(stream)
 * }
 * // image.width > 0
 * ```
 *
 * @param block 입력 스트림을 사용하는 블록
 */
suspend inline fun <T> ImageInputStream.useSuspending(
    crossinline block: suspend (ImageInputStream) -> T,
): T = use {
    block(this@useSuspending)
}

@Deprecated("use useSuspending instead.", replaceWith = ReplaceWith("useSuspending(block)"))
suspend inline fun <T> ImageInputStream.usingSuspend(
    crossinline block: suspend (ImageInputStream) -> T,
): T = use {
    block(this@usingSuspend)
}
