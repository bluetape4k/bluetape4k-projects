package io.bluetape4k.jackson.text

/**
 * 점 구분 경로를 따라 중첩 [Map]의 하위 노드를 조회합니다.
 *
 * ## 동작/계약
 * - [path]를 [delimiter]로 분리해 순서대로 하위 값을 탐색합니다.
 * - 중간 노드가 `Map<Any, Any?>`가 아니면 [ClassCastException]이 발생합니다.
 * - 경로 키가 없으면 `null as Map` 캐스팅으로 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val root = mapOf("a" to mapOf("b" to mapOf("c" to 1))) as Map<Any, Any?>
 * val node = root.getNode("a.b")
 * // node == mapOf("c" to 1)
 * ```
 *
 * @param path 조회할 점 구분 경로입니다. 예: `"parent.child"`.
 * @param delimiter 경로 분리자입니다.
 */
@Suppress("UNCHECKED_CAST")
fun Map<Any, Any?>.getNode(path: String, delimiter: String = "."): Map<Any, Any?> {
    val nodes = path.split(delimiter)
    var map = this

    nodes.forEach {
        map = map[it] as Map<Any, Any?>
    }
    return map
}

/**
 * 점 구분 경로를 따라 중첩 [Map]의 하위 노드를 안전하게 조회합니다.
 *
 * ## 동작/계약
 * - [path]를 [delimiter]로 분리해 순서대로 하위 값을 탐색합니다.
 * - 중간 노드가 `Map<Any, Any?>`가 아니거나 경로 키가 없으면 `null`을 반환합니다.
 * - 예외를 발생시키지 않는 [getNode]의 안전한 대안입니다.
 *
 * ```kotlin
 * val root = mapOf("a" to mapOf("b" to mapOf("c" to 1))) as Map<Any, Any?>
 * val node = root.getNodeOrNull("a.b")   // mapOf("c" to 1)
 * val none = root.getNodeOrNull("a.x")   // null
 * ```
 *
 * @param path 조회할 점 구분 경로입니다. 예: `"parent.child"`.
 * @param delimiter 경로 분리자입니다.
 */
@Suppress("UNCHECKED_CAST")
fun Map<Any, Any?>.getNodeOrNull(path: String, delimiter: String = "."): Map<Any, Any?>? {
    var map: Map<Any, Any?> = this
    for (key in path.split(delimiter)) {
        map = (map[key] as? Map<Any, Any?>) ?: return null
    }
    return map
}
