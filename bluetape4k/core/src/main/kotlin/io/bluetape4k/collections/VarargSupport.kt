package io.bluetape4k.collections

/**
 * Generic 타입에 대해서 reified 가 없을 때에는 `toTypedArray()` 가 지원되지 않는데, 이를 해결하기 위한 함수입니다.
 *
 * 첫 번째 원소의 런타임 타입을 기반으로 올바른 타입의 배열을 생성합니다.
 * 빈 컬렉션의 경우 `Array<Any>`로 생성됩니다.
 */
@Suppress("UNCHECKED_CAST")
fun <T: Any> Collection<T>.toVarargArray(): Array<T> {
    val componentType = firstOrNull()?.javaClass ?: Any::class.java
    val result = java.lang.reflect.Array.newInstance(componentType, size) as Array<T>
    forEachIndexed { i, e -> result[i] = e }
    return result
}
