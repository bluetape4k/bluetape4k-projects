package io.bluetape4k.coroutines.context

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Coroutine Context 에 다양한 정보를 담아서 전파하기 위해 사용합니다.
 *
 * ```
 * val context = PropertyCoroutineContext(mapOf("name" to "kommons", "id" to 1234))
 * val scope = CoroutineScope(context) + Dispatchers.IO
 *
 * scope.launch {
 *   println("name: ${coroutineContext[PropertyCoroutineContext]?.get("name")}")
 * }
 *
 * withContext(scope.coroutineContext) {
 *   println("id: ${coroutineContext[PropertyCoroutineContext]?.get("id")}")
 * }
 * ```
 *
 * @param props 전파할 정보를 담은 Map
 */
class PropertyCoroutineContext(
    props: Map<String, Any?> = emptyMap(),
): AbstractCoroutineContextElement(Key) {

    companion object Key: CoroutineContext.Key<PropertyCoroutineContext>

    private val _props: MutableMap<String, Any?> = props.toMutableMap()

    val properties: Map<String, Any?> get() = _props

    operator fun get(name: String): Any? = _props[name]

    operator fun set(name: String, value: Any?) {
        _props[name] = value
    }

    fun putAll(vararg props: Pair<String, Any?>) {
        _props.putAll(props)
    }

    fun putAll(props: Map<String, Any?>) {
        _props.putAll(props)
    }

    override fun toString(): String = "PropertyCoroutineContext(props=$_props)"
}
