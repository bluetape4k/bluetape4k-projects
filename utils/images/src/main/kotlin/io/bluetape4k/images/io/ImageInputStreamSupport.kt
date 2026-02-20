package io.bluetape4k.images.io

import javax.imageio.stream.ImageInputStream

/**
 * [ImageInputStream]을 안전하게 사용하고 자동으로 닫습니다.
 *
 * @param block 입력 스트림을 사용하는 블록
 */
@Deprecated("use `use` instead.", replaceWith = ReplaceWith("use"))
inline fun <T> ImageInputStream.using(block: (ImageInputStream) -> T): T =
    use { block(this) }

/**
 * Coroutines 환경에서 [ImageInputStream]을 안전하게 사용하고 자동으로 닫습니다.
 *
 * @param block 입력 스트림을 사용하는 블록
 */
suspend inline fun <T> ImageInputStream.useSuspending(
    @BuilderInference crossinline block: suspend (ImageInputStream) -> T,
): T = use {
    block(this@useSuspending)
}

@Deprecated("use useSuspending instead.", replaceWith = ReplaceWith("useSuspending(block)"))
suspend inline fun <T> ImageInputStream.usingSuspend(
    @BuilderInference crossinline block: suspend (ImageInputStream) -> T,
): T = use {
    block(this@usingSuspend)
}
