package io.bluetape4k.collections

/**
 * Generic 타입에 대해서 reified 가 없을 때에는 `toTypedArray()` 가 지원되지 않는데, 이를 해결하기 위한 함수입니다.
 *
 * 첫 번째 원소의 런타임 타입을 기반으로 올바른 타입의 배열을 생성합니다.
 * 빈 컬렉션의 경우 `Array<Any>`로 생성됩니다.
 *
 * ```kotlin
 * // reified 없이 제네릭 컨텍스트에서 vararg 함수 호출 시 활용
 * fun <T: Any> printAll(vararg items: T) {
 *     items.forEach { println(it) }
 * }
 *
 * val strings: Collection<String> = listOf("alpha", "beta", "gamma")
 * val arr: Array<String> = strings.toVarargArray()
 * printAll(*arr) // "alpha", "beta", "gamma" 출력
 *
 * // 빈 컬렉션은 Array<Any>로 생성됨
 * val empty: Collection<String> = emptyList()
 * val emptyArr = empty.toVarargArray() // Array<Any> 타입으로 생성
 * ```
 *
 * @return 컬렉션 원소를 담은 런타임 타입 배열
 */
@Suppress("UNCHECKED_CAST")
fun <T: Any> Collection<T>.toVarargArray(): Array<T> {
    val componentType = firstOrNull()?.javaClass ?: Any::class.java
    val result = java.lang.reflect.Array.newInstance(componentType, size) as Array<T>
    forEachIndexed { i, e -> result[i] = e }
    return result
}
