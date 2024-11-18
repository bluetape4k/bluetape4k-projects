package io.bluetape4k.netty.util

import io.netty.util.ReferenceCounted

/**
 * [ReferenceCounted] 객체를 블록에서 사용하고 블록 실행 후에 릴리즈합니다.
 *
 * @receiver [ReferenceCounted]를 구현한 객체
 * @param decrement 참조 카운트를 내릴 수
 * @param block 실행할 블록
 */
inline fun <T: ReferenceCounted> T.use(decrement: Int = 1, block: (T) -> Unit) {
    try {
        block(this)
    } finally {
        release(decrement)
    }
}
