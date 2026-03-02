package io.bluetape4k.coroutines.context

import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * 코루틴 컨텍스트에 문자열 키 기반 속성 맵을 저장합니다.
 *
 * ## 동작/계약
 * - 내부 저장소는 [ConcurrentHashMap]이라 동시 접근 시 기본적인 스레드 안전성을 제공합니다.
 * - 생성 시 전달한 [props]를 복사해 보관하며, 이후 외부 맵 변경은 내부 상태에 영향을 주지 않습니다.
 * - `get`/`set`/`putAll`로 내부 상태를 변경할 수 있습니다.
 * - 별도 입력 검증은 없고, `null` 값 저장을 허용합니다.
 *
 * ```kotlin
 * val ctx = PropertyCoroutineContext(mapOf("traceId" to "t-1"))
 * ctx["userId"] = 10L
 * // ctx["traceId"] == "t-1"
 * ```
 * @param props 초기 속성 집합입니다.
 */
class PropertyCoroutineContext(
    props: Map<String, Any?> = emptyMap(),
): AbstractCoroutineContextElement(Key) {

    companion object Key: CoroutineContext.Key<PropertyCoroutineContext>

    private val _props: MutableMap<String, Any?> = ConcurrentHashMap(props)

    /**
     * 현재 속성의 스냅샷을 반환합니다.
     *
     * ## 동작/계약
     * - 내부 맵을 그대로 노출하지 않고 새 `Map` 복사본을 반환합니다.
     * - 반환된 맵을 수정해도 컨텍스트 내부 상태는 바뀌지 않습니다.
     * - 속성 개수에 비례한 복사 할당이 발생합니다.
     *
     * ```kotlin
     * val ctx = PropertyCoroutineContext(mapOf("a" to 1))
     * val snapshot = ctx.properties
     * // snapshot == {"a" to 1}
     * ```
     */
    val properties: Map<String, Any?> get() = _props.toMap()

    /**
     * 지정한 이름의 속성 값을 조회합니다.
     *
     * ## 동작/계약
     * - 키가 없으면 `null`을 반환합니다.
     * - 조회 전용 연산이며 내부 상태를 변경하지 않습니다.
     * - 키 이름에 대한 별도 검증은 없습니다.
     *
     * ```kotlin
     * val ctx = PropertyCoroutineContext(mapOf("a" to 1))
     * val value = ctx["a"]
     * // value == 1
     * ```
     * @param name 조회할 속성 이름입니다.
     */
    operator fun get(name: String): Any? = _props[name]

    /**
     * 지정한 이름의 속성 값을 저장합니다.
     *
     * ## 동작/계약
     * - 같은 키가 이미 존재하면 값을 덮어씁니다.
     * - `value`에 `null`을 저장할 수 있습니다.
     * - 수신 객체의 내부 상태를 변경합니다.
     *
     * ```kotlin
     * val ctx = PropertyCoroutineContext()
     * ctx["a"] = 1
     * // ctx["a"] == 1
     * ```
     * @param name 저장할 속성 이름입니다.
     * @param value 저장할 속성 값입니다.
     */
    operator fun set(name: String, value: Any?) {
        _props[name] = value
    }

    /**
     * 가변 인자 키-값 쌍을 한 번에 병합합니다.
     *
     * ## 동작/계약
     * - 전달된 키가 기존에 있으면 새 값으로 덮어씁니다.
     * - 수신 객체의 내부 상태를 변경합니다.
     * - 입력 순회에 비례한 연산이 수행됩니다.
     *
     * ```kotlin
     * val ctx = PropertyCoroutineContext(mapOf("a" to 1))
     * ctx.putAll("b" to 2, "a" to 3)
     * // ctx.properties == {"a" to 3, "b" to 2}
     * ```
     * @param props 병합할 키-값 쌍 목록입니다.
     */
    fun putAll(vararg props: Pair<String, Any?>) {
        _props.putAll(props)
    }

    /**
     * 맵 형태의 속성 집합을 한 번에 병합합니다.
     *
     * ## 동작/계약
     * - 전달된 키가 기존에 있으면 새 값으로 덮어씁니다.
     * - 수신 객체의 내부 상태를 변경합니다.
     * - 전달된 맵 크기에 비례해 항목을 순회합니다.
     *
     * ```kotlin
     * val ctx = PropertyCoroutineContext(mapOf("a" to 1))
     * ctx.putAll(mapOf("b" to 2, "a" to 3))
     * // ctx.properties == {"a" to 3, "b" to 2}
     * ```
     * @param props 병합할 속성 맵입니다.
     */
    fun putAll(props: Map<String, Any?>) {
        _props.putAll(props)
    }

    override fun toString(): String = "PropertyCoroutineContext(props=$_props)"
}
