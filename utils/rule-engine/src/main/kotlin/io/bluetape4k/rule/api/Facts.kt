package io.bluetape4k.rule.api

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap

/**
 * Rule 실행 시 Condition, Action에 전달할 데이터를 나타냅니다.
 *
 * ```kotlin
 * val facts = Facts.of("name" to "debop", "age" to 30)
 * val name = facts.get<String>("name")
 * facts["score"] = 100
 * ```
 *
 * @see Rule
 * @see Condition
 * @see Action
 */
class Facts private constructor(
    private val facts: ConcurrentHashMap<String, Any?>,
): Serializable {

    companion object: KLogging() {
        private const val serialVersionUID = 1L

        /**
         * 빈 [Facts] 인스턴스를 생성합니다.
         */
        @JvmStatic
        fun empty(): Facts = Facts(ConcurrentHashMap())

        /**
         * 키-값 쌍으로 [Facts]를 생성합니다.
         *
         * @param pairs 키-값 쌍
         * @return [Facts] 인스턴스
         */
        @JvmStatic
        fun of(vararg pairs: Pair<String, Any?>): Facts {
            val map = ConcurrentHashMap<String, Any?>()
            pairs.forEach { (k, v) -> map[k] = v }
            return Facts(map)
        }

        /**
         * Map으로부터 [Facts]를 생성합니다.
         *
         * @param map 데이터 Map
         * @return [Facts] 인스턴스
         */
        @JvmStatic
        fun from(map: Map<String, Any?>): Facts {
            return Facts(ConcurrentHashMap(map))
        }
    }

    /**
     * Fact 값을 가져옵니다.
     *
     * @param name Fact 이름
     * @return Fact 값, 없으면 null
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> get(name: String): T? {
        name.requireNotBlank("name")
        return facts[name] as? T
    }

    /**
     * Fact 값을 설정합니다.
     *
     * @param name Fact 이름
     * @param value Fact 값
     */
    operator fun set(name: String, value: Any?) {
        name.requireNotBlank("name")
        facts[name] = value
    }

    /**
     * Fact를 추가합니다. (Map 호환)
     *
     * @param name Fact 이름
     * @param value Fact 값
     */
    fun put(name: String, value: Any?) {
        name.requireNotBlank("name")
        facts[name] = value
    }

    /**
     * 여러 Fact를 한번에 추가합니다.
     *
     * @param pairs 키-값 쌍
     */
    fun putAll(vararg pairs: Pair<String, Any?>) {
        pairs.forEach { (k, v) -> put(k, v) }
    }

    /**
     * Fact를 제거합니다.
     *
     * @param name Fact 이름
     * @return 제거된 값, 없으면 null
     */
    fun remove(name: String): Any? {
        name.requireNotBlank("name")
        return facts.remove(name)
    }

    /**
     * 해당 이름의 Fact가 존재하는지 확인합니다.
     *
     * @param name Fact 이름
     * @return 존재 여부
     */
    fun containsKey(name: String): Boolean = facts.containsKey(name)

    /**
     * Facts의 개수를 반환합니다.
     */
    val size: Int get() = facts.size

    /**
     * Facts가 비어있는지 확인합니다.
     */
    fun isEmpty(): Boolean = facts.isEmpty()

    /**
     * 모든 Fact를 제거합니다.
     */
    fun clear() {
        facts.clear()
    }

    /**
     * 읽기 전용 Map으로 변환합니다.
     *
     * @return 읽기 전용 Map
     */
    fun asMap(): Map<String, Any?> = facts.toMap()

    override fun toString(): String = buildString {
        append("Facts={")
        facts.entries.joinTo(this) { "${it.key}=${it.value}" }
        append("}")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Facts) return false
        return facts == other.facts
    }

    override fun hashCode(): Int = facts.hashCode()
}
