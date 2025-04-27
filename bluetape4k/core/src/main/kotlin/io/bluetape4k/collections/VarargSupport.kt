package io.bluetape4k.collections

/**
 * Generic 타입에 대해서 `toTypedArray()` 가 지원되지 않는데, 이를 해결하기 위한 함수입니다.
 */
@Suppress("UNCHECKED_CAST")
fun <T: Any> Collection<T>.toVarargArray(): Array<T> =
    (this as Collection<Any>).toTypedArray() as Array<T>
