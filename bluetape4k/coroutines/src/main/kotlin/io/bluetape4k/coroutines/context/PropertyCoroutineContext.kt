package io.bluetape4k.coroutines.context

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Coroutine Context 에 다양한 정보를 담아서 전파하기 위해 사용합니다.
 *
 * ```
 * val context = PropertyCoroutineContext(mapOf("name" to "bluetape4k", "id" to 1234))
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

    /**
     * 현재 속성 스냅샷을 반환합니다.
     * 내부 mutable map을 직접 노출하지 않기 위해 복사본을 제공합니다.
     */
    val properties: Map<String, Any?> get() = _props.toMap()

    /** [name]에 해당하는 속성 값을 조회합니다. */
    operator fun get(name: String): Any? = _props[name]

    /** [name]에 해당하는 속성 값을 설정합니다. */
    operator fun set(name: String, value: Any?) {
        _props[name] = value
    }

    /** 전달받은 key/value 쌍을 모두 설정합니다. */
    fun putAll(vararg props: Pair<String, Any?>) {
        _props.putAll(props)
    }

    /** 전달받은 map의 항목을 모두 설정합니다. */
    fun putAll(props: Map<String, Any?>) {
        _props.putAll(props)
    }

    override fun toString(): String = "PropertyCoroutineContext(props=$_props)"
}
