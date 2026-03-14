package io.bluetape4k.netty.util

import io.netty.util.ReferenceCounted

/**
 * [ReferenceCounted] 객체를 블록에서 사용하고 블록 실행 후에 릴리즈합니다.
 *
 * ## 동작/계약
 * - 블록 실행 후 항상 [ReferenceCounted.release]가 호출됩니다.
 * - `release()` 호출 시 예외가 발생해도 무시하며 원래 블록 예외가 전파됩니다.
 *
 * ```kotlin
 * ByteBufAllocator.DEFAULT.buffer().use { buf ->
 *     buf.writeInt(42)
 * }
 * ```
 *
 * @receiver [ReferenceCounted]를 구현한 객체
 * @param decrement 참조 카운트를 내릴 수 (기본값: 1)
 * @param block 실행할 블록
 */
inline fun <T : ReferenceCounted> T.use(
    decrement: Int = 1,
    block: (T) -> Unit,
) {
    try {
        block(this)
    } finally {
        runCatching { release(decrement) }
    }
}
