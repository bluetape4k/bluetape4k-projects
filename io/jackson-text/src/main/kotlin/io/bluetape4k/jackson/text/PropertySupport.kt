package io.bluetape4k.jackson.text

/**
 * 중첩된 [Map]에서 점(.) 구분자로 이루어진 경로를 따라 하위 Map 노드를 탐색합니다.
 *
 * @param path 탐색할 경로 (예: "parent.child.grandchild")
 * @param delimiter 경로 구분자 (기본값: ".")
 * @return 경로에 해당하는 하위 Map
 * @throws ClassCastException 경로의 중간 노드가 Map이 아닌 경우
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
