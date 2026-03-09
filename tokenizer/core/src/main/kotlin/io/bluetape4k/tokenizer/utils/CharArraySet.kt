package io.bluetape4k.tokenizer.utils

import io.bluetape4k.logging.KLogging
import java.io.Serializable

/**
 * 문자 배열 기반 키를 중복 없이 저장하는 가변 집합 구현체다.
 *
 * ## 동작/계약
 * - 내부 저장소로 `CharArrayMap`을 사용하며 값에는 플레이스홀더만 저장한다.
 * - `Any`, `String`, `CharSequence`, `CharArray` 입력을 모두 수용한다.
 * - 문자열 사전/금칙어 목록처럼 membership 조회가 많은 용도에 맞춘 구조다.
 *
 * ```kotlin
 * val set = CharArraySet(4)
 * set.add("token")
 * // set.contains("token") == true
 * // set.size == 1
 * ```
 */
open class CharArraySet(val map: CharArrayMap<Any>): AbstractMutableSet<Any>(), Serializable {

    companion object: KLogging() {
        private val EMPTY_SET = CharArraySet(CharArrayMap.emptyMap())
        private val PLACEHOLDER = Any()

        @Suppress("USELESS_IS_CHECK")
        @JvmStatic
        /**
         * 읽기 전용 뷰를 반환한다.
         *
         * ## 동작/계약
         * - 빈 집합은 공유 singleton(`EMPTY_SET`)을 그대로 반환한다.
         * - 그 외 입력은 `UnmodifiableCharArrayMap` 기반 래퍼로 감싼 새 인스턴스를 반환한다.
         *
         * ```kotlin
         * val source = CharArraySet(2).apply { add("a") }
         * val readonly = CharArraySet.unmodifiableSet(source)
         * // readonly.contains("a") == true
         * // readonly.add("b") throws UnsupportedOperationException
         * ```
         */
        fun unmodifiableSet(set: CharArraySet): CharArraySet {
            return when (set) {
                EMPTY_SET -> EMPTY_SET
                // is CharArrayMap.UnmodifiableCharArrayMap<*> -> set
                else -> CharArraySet(CharArrayMap.unmodifiableMap(set.map))
            }
        }

        @JvmStatic
        /**
         * 입력 집합의 내용을 복사한 새 `CharArraySet`을 생성한다.
         *
         * ## 동작/계약
         * - 입력이 `CharArraySet`이면 내부 맵 복사 생성자를 사용한다.
         * - 입력이 비어 있는 공유 집합이면 동일 singleton을 반환한다.
         *
         * ```kotlin
         * val copied = CharArraySet.copy(setOf("가", "나"))
         * // copied.contains("가") == true
         * // copied.size == 2
         * ```
         */
        fun copy(set: Set<Any>): CharArraySet = when (set) {
            EMPTY_SET -> EMPTY_SET
            is CharArraySet -> CharArraySet(CharArrayMap.copy(set.map))
            else      -> CharArraySet(set)
        }
    }

    /**
     * 예상 원소 수를 기준으로 초기 용량을 지정해 집합을 생성한다.
     *
     * ## 동작/계약
     * - 내부적으로 `CharArrayMap(startSize)`를 생성한다.
     * - 용량은 리해시 시점을 늦추기 위한 힌트로 사용된다.
     *
     * ```kotlin
     * val set = CharArraySet(128)
     * // set.isEmpty() == true
     * ```
     */
    constructor(startSize: Int): this(CharArrayMap<Any>(startSize))

    /**
     * 컬렉션 내용을 복사해 집합을 생성한다.
     *
     * ## 동작/계약
     * - `c.size`로 초기 용량을 계산한 뒤 `addAll`로 원소를 채운다.
     * - 중복 원소는 집합 특성상 하나만 유지된다.
     *
     * ```kotlin
     * val set = CharArraySet(listOf("a", "a", "b"))
     * // set.size == 2
     * ```
     */
    constructor(c: Collection<Any>): this(c.size) {
        @Suppress("LeakingThis")
        addAll(c)
    }

    /**
     * 집합의 모든 원소를 제거한다.
     *
     * ## 동작/계약
     * - 내부 `CharArrayMap.clear()`를 호출해 키/값 배열을 초기화한다.
     * - 호출 후 `size`는 0이 된다.
     *
     * ```kotlin
     * val set = CharArraySet(2).apply { add("a") }
     * set.clear()
     * // set.isEmpty() == true
     * ```
     */
    override fun clear() {
        map.clear()
    }

    /**
     * 일반 키 객체 존재 여부를 확인한다.
     *
     * ## 동작/계약
     * - `Any` 입력을 내부 맵 키 비교 규칙으로 위임해 조회한다.
     * - 문자열/문자배열 입력도 동일 API에서 처리된다.
     *
     * ```kotlin
     * val set = CharArraySet(2).apply { add("hello") }
     * // set.contains("hello") == true
     * ```
     */
    override fun contains(element: Any): Boolean = map.containsKey(element)
    /**
     * 문자 배열 구간이 집합에 존재하는지 확인한다.
     *
     * ## 동작/계약
     * - `text[off, off + len)` 구간으로 키를 조회한다.
     * - 내부 맵의 슬롯 검색 결과를 그대로 반환한다.
     *
     * ```kotlin
     * val set = CharArraySet(4).apply { add("token") }
     * val chars = "token".toCharArray()
     * // set.contains(chars, 0, chars.size) == true
     * ```
     */
    fun contains(text: CharArray, off: Int, len: Int = text.size) = map.containsKey(text, off, len)
    /**
     * `CharSequence` 키 존재 여부를 확인한다.
     *
     * ## 동작/계약
     * - 문자열 내용을 기준으로 내부 맵을 조회한다.
     * - 대소문자 정규화 없이 원문 비교를 수행한다.
     *
     * ```kotlin
     * val set = CharArraySet(2).apply { add("hello") }
     * // set.contains("hello") == true
     * ```
     */
    fun contains(cs: CharSequence) = map.containsKey(cs)

    /**
     * 일반 키 객체를 집합에 추가한다.
     *
     * ## 동작/계약
     * - 내부 맵에 플레이스홀더 값을 저장해 집합 원소로 등록한다.
     * - 기존 키가 없을 때만 `true`를 반환한다.
     *
     * ```kotlin
     * val set = CharArraySet(2)
     * // set.add("x") == true
     * ```
     */
    override fun add(element: Any): Boolean = map.put(element, PLACEHOLDER) == null
    /**
     * `CharSequence` 키를 집합에 추가한다.
     *
     * ## 동작/계약
     * - 문자열 내용을 기준으로 중복 여부를 판단한다.
     * - 이미 존재하면 `false`를 반환한다.
     *
     * ```kotlin
     * val set = CharArraySet(2)
     * // set.add("abc" as CharSequence) == true
     * ```
     */
    open fun add(text: CharSequence) = map.put(text, PLACEHOLDER) == null
    /**
     * 문자열 키를 집합에 추가한다.
     *
     * ## 동작/계약
     * - 내부 맵 문자열 삽입 경로를 사용한다.
     * - 중복 키는 추가되지 않는다.
     *
     * ```kotlin
     * val set = CharArraySet(2)
     * // set.add("abc") == true
     * ```
     */
    open fun add(text: String) = map.put(text, PLACEHOLDER) == null
    /**
     * 문자 배열 키를 집합에 추가한다.
     *
     * ## 동작/계약
     * - 전달한 배열 참조를 내부 키로 사용한다.
     * - 동일 문자 시퀀스가 이미 있으면 `false`를 반환한다.
     *
     * ```kotlin
     * val set = CharArraySet(2)
     * // set.add("abc".toCharArray()) == true
     * ```
     */
    open fun add(text: CharArray) = map.put(text, PLACEHOLDER) == null

    /**
     * 컬렉션 원소를 모두 추가한다.
     *
     * ## 동작/계약
     * - 각 원소를 순회하며 `add`를 호출한다.
     * - 하나라도 새 원소가 추가되면 `true`를 반환한다.
     *
     * ```kotlin
     * val set = CharArraySet(2)
     * // set.addAll(listOf("a", "b")) == true
     * ```
     */
    override fun addAll(elements: Collection<Any>): Boolean {
        var modified = false
        elements.forEach {
            if (add(it)) modified = true
        }
        return modified
    }

    /**
     * 일반 키 객체를 제거한다.
     *
     * ## 동작/계약
     * - 내부 맵에서 키를 제거하고 실제로 제거된 경우에만 `true`를 반환한다.
     * - 키가 존재하지 않으면 `false`를 반환하며 예외를 던지지 않는다.
     *
     * ```kotlin
     * val set = CharArraySet(2).apply { add("a") }
     * // set.remove("a") == true
     * ```
     */
    override fun remove(element: Any): Boolean = map.remove(element) != null

    /**
     * 문자열 키를 집합에서 제거한다.
     *
     * ## 동작/계약
     * - 내부 맵에서 키를 제거하고 실제로 제거된 경우에만 `true`를 반환한다.
     * - 키가 없던 경우 `false`를 반환한다.
     *
     * ```kotlin
     * val set = CharArraySet(2).apply { add("x") }
     * set.remove("x")
     * // set.contains("x") == false
     * ```
     */
    fun remove(text: String): Boolean = map.remove(text) != null

    /**
     * 컬렉션 원소를 모두 제거한다.
     *
     * ## 동작/계약
     * - 각 원소에 대해 `remove`를 호출한다.
     * - 하나라도 실제 제거가 발생하면 `true`를 반환한다.
     *
     * ```kotlin
     * val set = CharArraySet(2).apply { addAll(listOf("a", "b")) }
     * // set.removeAll(listOf("a", "b")) == true
     * ```
     */
    override fun removeAll(elements: Collection<Any>): Boolean {
        var modified = false
        elements.forEach {
            if (remove(it)) modified = true
        }
        return modified
    }

    /**
     * 문자열 목록을 모두 제거한다.
     *
     * ## 동작/계약
     * - 각 문자열에 `remove(String)`을 적용한다.
     * - 하나라도 실제 제거가 발생하면 `true`를 반환한다.
     *
     * ```kotlin
     * val set = CharArraySet(2).apply { addAll(listOf("x", "y")) }
     * // set.removeAll(listOf("x", "y")) == true
     * ```
     */
    fun removeAll(words: List<String>): Boolean {
        var modified = false
        words.forEach {
            if (remove(it)) modified = true
        }
        return modified
    }

    /**
     * 집합 원소 수를 반환한다.
     *
     * ## 동작/계약
     * - 내부 맵의 `size`를 그대로 노출한다.
     * - 조회 연산이며 상태를 변경하지 않는다.
     *
     * ```kotlin
     * val set = CharArraySet(2).apply { add("one") }
     * // set.size == 1
     * ```
     */
    override val size: Int
        get() = map.size

    /**
     * 원소 순회를 위한 반복자를 반환한다.
     *
     * ## 동작/계약
     * - 내부 맵의 원본 키 집합 반복자를 그대로 사용한다.
     * - 반환 원소는 `CharArray` 또는 입력 타입에 대응하는 키 표현이다.
     *
     * ```kotlin
     * val set = CharArraySet(2).apply { add("it") }
     * // set.iterator().hasNext() == true
     * ```
     */
    override fun iterator(): MutableIterator<Any> {
        return map.originalKeySet.iterator()
    }

    override fun toString(): String = buildString {
        append("[")
        this@CharArraySet.forEach { item ->
            if (this.length > 1) append(", ")
            when (item) {
                is CharArray -> append(item)
                else -> append(item.toString())
            }
        }
        append("]")
    }
}
