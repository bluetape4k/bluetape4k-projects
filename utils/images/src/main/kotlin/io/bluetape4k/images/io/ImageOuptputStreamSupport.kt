package io.bluetape4k.images.io

import javax.imageio.stream.ImageOutputStream

/**
 * [ImageOutputStream]을 안전하게 사용하고 자동으로 닫습니다.
 *
 * @param block 출력 스트림을 사용하는 블록
 */
@Deprecated("use `use` instead", replaceWith = ReplaceWith("use"))
inline fun <T> ImageOutputStream.using(block: (ImageOutputStream) -> T): T =
    use { block(this) }

/**
 * Coroutines 환경에서 [ImageOutputStream]을 안전하게 사용하고 자동으로 닫습니다.
 *
 * @param block 출력 스트림을 사용하는 블록
 */
suspend inline fun <T> ImageOutputStream.useSuspending(
    @BuilderInference crossinline block: suspend (ImageOutputStream) -> T,
): T = use {
    block(this@useSuspending)
}

@Deprecated("use useSuspending instead.", replaceWith = ReplaceWith("useSuspending(block)"))
suspend inline fun <T> ImageOutputStream.usingSuspend(
    @BuilderInference crossinline block: suspend (ImageOutputStream) -> T,
): T = use {
    block(this@usingSuspend)
}
