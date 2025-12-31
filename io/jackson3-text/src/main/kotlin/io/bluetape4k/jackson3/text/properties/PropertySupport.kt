package io.bluetape4k.jackson3.text.properties

/**
 *
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
