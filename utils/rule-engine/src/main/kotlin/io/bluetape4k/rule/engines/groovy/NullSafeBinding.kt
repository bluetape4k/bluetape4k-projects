package io.bluetape4k.rule.engines.groovy

import groovy.lang.Binding

/**
 * 존재하지 않는 변수에 접근 시 `MissingPropertyException` 대신 `null`을 반환하는 [Binding]입니다.
 *
 * Groovy 기본 [Binding]은 바인딩에 없는 변수를 참조하면 예외를 발생시킵니다.
 * 이로 인해 Elvis 연산자(`name ?: 'Guest'`)나 안전 호출(`name?.toUpperCase()`) 같은
 * null-safe 패턴이 Facts에 키가 없을 때 동작하지 않습니다.
 *
 * 이 클래스는 누락된 변수에 대해 `null`을 반환하여 Groovy의 null-safe 연산자들이
 * 의도대로 동작하도록 합니다.
 *
 * ```kotlin
 * val binding = NullSafeBinding(mapOf("amount" to 1000))
 * // binding.getVariable("amount")  → 1000
 * // binding.getVariable("unknown") → null (예외 아님)
 * ```
 *
 * @param initialVars Facts에서 가져온 초기 변수 맵
 */
class NullSafeBinding(initialVars: Map<String, Any?> = emptyMap()): Binding() {

    init {
        initialVars.forEach { (key, value) -> setVariable(key, value) }
    }

    /**
     * 변수를 조회합니다. 바인딩에 없는 변수는 `null`을 반환합니다.
     */
    override fun getVariable(name: String): Any? {
        return if (hasVariable(name)) {
            super.getVariable(name)
        } else {
            null
        }
    }
}
