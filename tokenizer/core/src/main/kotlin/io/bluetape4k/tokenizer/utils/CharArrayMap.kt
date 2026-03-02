package io.bluetape4k.tokenizer.utils

import io.bluetape4k.logging.KLogging
import java.io.Serializable

/**
 * `CharArray` 키에 최적화된 open addressing 해시 맵 구현체다.
 *
 * ## 동작/계약
 * - 키 타입으로 `Any`를 받지만 내부적으로 `CharArray` 또는 문자열 표현으로 정규화한다.
 * - 충돌 해결은 선형이 아닌 증가값(`inc`) 기반 probe 방식으로 수행한다.
 * - 삭제 시 probe chain 보존을 위해 엔트리를 재구성한다.
 *
 * ```kotlin
 * val map = CharArrayMap<Int>(4)
 * map.put("token", 1)
 * // map["token"] == 1
 * // map.containsKey("token") == true
 * ```
 */
@Suppress("UNCHECKED_CAST")
open class CharArrayMap<V>(startSize: Int): AbstractMutableMap<Any, V>(), Serializable {

    companion object: KLogging() {
        private const val INIT_SIZE = 8
        private val EMPTY_MAP: CharArrayMap<Any> = EmptyCharArrayMap()

        @JvmStatic
        /**
         * 수정이 불가능한 맵 뷰를 반환한다.
         *
         * ## 동작/계약
         * - 빈 맵은 공유 `emptyMap()` 인스턴스를 반환한다.
         * - 이미 읽기 전용 래퍼인 경우 동일 인스턴스를 그대로 반환한다.
         * - 그 외에는 `UnmodifiableCharArrayMap`으로 감싼다.
         *
         * ```kotlin
         * val source = CharArrayMap<Int>(2).apply { put("a", 1) }
         * val readonly = CharArrayMap.unmodifiableMap(source)
         * // readonly["a"] == 1
         * // readonly.put("b", 2) throws UnsupportedOperationException
         * ```
         */
        fun <V> unmodifiableMap(map: CharArrayMap<V>): CharArrayMap<V> {
            return when {
                map.isEmpty() -> emptyMap()
                else          -> when (map) {
                    is UnmodifiableCharArrayMap -> map
                    else                        -> UnmodifiableCharArrayMap(map)
                }
            }
        }

        @JvmStatic
        /**
         * 일반 `Map`을 `CharArrayMap`으로 복사한다.
         *
         * ## 동작/계약
         * - 입력 맵의 모든 엔트리를 새 맵에 `putAll`로 적재한다.
         * - 원본 맵과 결과 맵은 독립된 인스턴스다.
         *
         * ```kotlin
         * val copied = CharArrayMap.copy(mapOf<Any, Int>("x" to 10))
         * // copied["x"] == 10
         * ```
         */
        fun <V> copy(map: Map<Any, V>): CharArrayMap<V> = CharArrayMap(map)

        @JvmStatic
        /**
         * 불변 빈 맵 singleton을 반환한다.
         *
         * ## 동작/계약
         * - 항상 동일한 내부 `EMPTY_MAP` 인스턴스를 타입 캐스팅해 반환한다.
         * - 삽입/삭제 연산은 지원하지 않는다.
         *
         * ```kotlin
         * val empty = CharArrayMap.emptyMap<Int>()
         * // empty.isEmpty() == true
         * ```
         */
        fun <V> emptyMap(): CharArrayMap<V> = EMPTY_MAP as CharArrayMap<V>
    }

    @Suppress("LeakingThis")
    /**
     * 일반 `Map` 내용을 복사해 맵을 생성한다.
     *
     * ## 동작/계약
     * - 초기 용량은 `c.size`를 기준으로 계산한다.
     * - 생성 시 `putAll(c)`를 수행한다.
     *
     * ```kotlin
     * val map = CharArrayMap(mapOf<Any, Int>("a" to 1))
     * // map.size == 1
     * ```
     */
    constructor(c: Map<Any, V>): this(c.size) {
        putAll(c)
    }

    /**
     * 다른 `CharArrayMap`의 내부 저장소를 공유해 얕은 복사를 생성한다.
     *
     * ## 동작/계약
     * - `_keys`, `_values` 배열 참조를 그대로 공유한다.
     * - 수정 가능한 맵에서 공유 복사를 사용할 경우 서로 영향이 있을 수 있다.
     *
     * ```kotlin
     * val source = CharArrayMap<Int>(2).apply { put("a", 1) }
     * val copy = CharArrayMap(source)
     * // copy["a"] == 1
     * ```
     */
    constructor(src: CharArrayMap<V>): this(0) {
        this._keys = src._keys
        this._values = src._values
        this._count = src._count
        this.charUtils = src.charUtils
    }

    private var charUtils: CharacterUtils = CharacterUtils.getInstance()
    private var _count: Int = 0
    private var _keys: Array<CharArray?>
    private var _values: Array<V?>

    init {
        var size = INIT_SIZE
        while (startSize + (startSize shr 2) > size) {
            size = size shl 1
        }
        _keys = arrayOfNulls(size)
        _values = arrayOfNulls<Any>(size) as Array<V?>
    }

    /**
     * 맵의 모든 엔트리를 제거한다.
     *
     * ## 동작/계약
     * - 키/값 배열 슬롯을 모두 `null`로 초기화한다.
     * - `_count`를 0으로 재설정한다.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2).apply { put("a", 1) }
     * map.clear()
     * // map.isEmpty() == true
     * ```
     */
    override fun clear() {
        _count = 0
        _keys.fill(null)
        _values.fill(null)
    }

    /**
     * 문자 배열 구간 키의 존재 여부를 확인한다.
     *
     * ## 동작/계약
     * - `text[off, off + len)` 구간 해시 슬롯을 계산해 조회한다.
     * - 해당 슬롯에 키가 존재하면 `true`를 반환한다.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2).apply { put("ab", 1) }
     * val chars = "ab".toCharArray()
     * // map.containsKey(chars, 0, chars.size) == true
     * ```
     */
    open fun containsKey(text: CharArray, off: Int, len: Int): Boolean {
        return _keys[getSlot(text, off, len)] != null
    }

    /**
     * `CharSequence` 키의 존재 여부를 확인한다.
     *
     * ## 동작/계약
     * - 문자열 내용을 기준으로 슬롯을 계산한다.
     * - 동일 문자 시퀀스가 등록되어 있으면 `true`를 반환한다.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2).apply { put("key", 1) }
     * // map.containsKey("key") == true
     * ```
     */
    open fun containsKey(cs: CharSequence): Boolean = _keys[getSlot(cs)] != null

    /**
     * 일반 키 객체 존재 여부를 확인한다.
     *
     * ## 동작/계약
     * - `CharArray` 키는 배열 경로로 조회한다.
     * - 그 외 타입은 `toString()` 결과를 키로 사용해 조회한다.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2).apply { put("a", 1) }
     * // map.containsKey("a") == true
     * ```
     */
    override fun containsKey(key: Any): Boolean = when (key) {
        is CharArray -> containsKey(key, 0, key.size)
        else         -> containsKey(key.toString())
    }

    /**
     * 문자 배열 구간 키에 연결된 값을 조회한다.
     *
     * ## 동작/계약
     * - 슬롯에 값이 없으면 `null`을 반환한다.
     * - 키 비교는 문자 단위 동등성으로 수행한다.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2).apply { put("x", 3) }
     * // map.get("x".toCharArray(), 0, 1) == 3
     * ```
     */
    open fun get(text: CharArray, off: Int, len: Int): V? {
        return _values[getSlot(text, off, len)]
    }

    /**
     * `CharSequence` 키에 연결된 값을 조회한다.
     *
     * ## 동작/계약
     * - 내부적으로 문자열 기반 슬롯 계산을 수행한다.
     * - 존재하지 않는 키는 `null`을 반환한다.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2).apply { put("y", 7) }
     * // map.get("y") == 7
     * ```
     */
    open fun get(cs: CharSequence): V? = _values[getSlot(cs)]

    /**
     * 일반 키 객체에 대응하는 값을 조회한다.
     *
     * ## 동작/계약
     * - `CharArray`는 배열 경로, 그 외는 `toString()` 경로를 사용한다.
     * - 키가 없으면 `null`을 반환한다.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2).apply { put("b", 2) }
     * // map["b"] == 2
     * ```
     */
    override fun get(key: Any): V? = when (key) {
        is CharArray -> get(key, 0, key.size)
        else         -> get(key.toString())
    }

    private fun getSlot(text: CharArray, off: Int, len: Int = text.size): Int {
        var code = getHashCode(text, off, len)
        var pos = code and (_keys.size - 1)
        var text2 = _keys[pos]

        fun isTextDifferent(text2: CharArray?): Boolean = text2?.let { !equals(text, off, len, text2) } ?: false

        val inc = ((code shr 8) + code) or 1
        while (isTextDifferent(text2)) {
            code += inc
            pos = code and (_keys.size - 1)
            text2 = _keys[pos]
        }

        return pos
    }

    private fun getSlot(text: CharSequence): Int {
        return getSlot(text.toString().toCharArray(), 0, text.length)
    }

    /**
     * `CharSequence` 키와 값을 저장한다.
     *
     * ## 동작/계약
     * - 키를 문자열로 변환한 뒤 삽입 로직에 위임한다.
     * - 동일 키가 있으면 이전 값을 반환하고 값을 교체한다.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2)
     * map.put("z", 1)
     * // map.put("z", 2) == 1
     * ```
     */
    open fun put(text: CharSequence, value: V): V? = put(text.toString(), value)

    /**
     * 일반 키 객체와 값을 저장한다.
     *
     * ## 동작/계약
     * - `CharArray` 키는 배열 삽입 경로를 사용한다.
     * - 그 외 키는 `toString()`으로 변환해 삽입한다.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2)
     * map.put("id", 1)
     * // map["id"] == 1
     * ```
     */
    override fun put(key: Any, value: V): V? = when (key) {
        is CharArray -> put(key, value)
        else         -> put(key.toString(), value)
    }

    /**
     * 문자열 키와 값을 저장한다.
     *
     * ## 동작/계약
     * - 키를 `CharArray`로 변환해 내부 삽입 함수로 위임한다.
     * - 기존 키가 있으면 이전 값을 반환한다.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2)
     * // map.put("k", 9) == null
     * ```
     */
    open fun put(text: String, value: V): V? = put(text.toCharArray(), value)

    /**
     * 문자 배열 키와 값을 맵에 저장한다.
     *
     * ## 동작/계약
     * - 키가 이미 존재하면 값을 교체하고 이전 값을 반환한다.
     * - 신규 키면 `_count`를 증가시키고 load factor(약 0.8)를 넘으면 `rehash`를 수행한다.
     * - 키 배열은 복사하지 않고 그대로 저장하므로 호출 후 외부 수정 시 동작이 달라질 수 있다.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2)
     * val key = "id".toCharArray()
     * map.put(key, 10)
     * // map["id"] == 10
     * ```
     */
    open fun put(text: CharArray, value: V): V? {
        val slot = getSlot(text, 0, text.size)

        _keys[slot]?.let {
            val oldValue = _values[slot]
            _values[slot] = value
            return oldValue
        }

        _keys[slot] = text
        _values[slot] = value
        _count++

        if (_count + (_count shr 2) > _keys.size) {
            rehash()
        }
        return null
    }

    private fun rehash() {
        assert(_keys.size == _values.size) {
            "keys size [${_keys.size}] must equals to _values size[${_values.size}"
        }

        val newSize = 2 * _keys.size
        val oldKeys = _keys
        val oldValues = _values

        _keys = arrayOfNulls(newSize)
        _values = arrayOfNulls<Any>(newSize) as Array<V?>

        oldKeys.forEachIndexed { i, text ->
            text?.let {
                // TODO: could be faster... no need to compare strings on collision
                val slot = getSlot(text, 0, text.size)
                _keys[slot] = text
                _values[slot] = oldValues[i]
            }
        }
        //        oldKeys.fill(null)
        //        oldValues.fill(null)
    }

    private fun equals(text1: CharArray, off: Int, len: Int, text2: CharArray): Boolean {
        if (len != text2.size)
            return false

        repeat(len) {
            if (text1[off + it] != text2[it])
                return false
        }
        return true
    }

    private fun equals(text1: CharSequence, text2: CharArray): Boolean {
        val len = text1.length
        if (len != text2.size)
            return false

        repeat(len) {
            if (text1[it] != text2[it])
                return false
        }
        return true
    }

    private fun getHashCode(text: CharArray, offset: Int, len: Int): Int {
        var code = 0
        val stop = offset + len

        for (i in offset until stop) {
            code = code * 31 + text[i].code
        }
        return code
    }

    private fun getHashCode(text: CharSequence): Int {
        var code = 0
        val len = text.length

        for (i in 0 until len) {
            code = code * 31 + text[i].code
        }
        return code
    }

    /**
     * 키를 제거하고 기존 값을 반환한다.
     *
     * ## 동작/계약
     * - 키가 없으면 `null`을 반환한다.
     * - 키가 있으면 해당 엔트리를 제외한 나머지를 재삽입해 probe chain을 재구성한다.
     * - 삭제 성공 시 제거된 이전 값을 반환한다.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2).apply { put("a", 1) }
     * val removed = map.remove("a")
     * // removed == 1
     * // map.containsKey("a") == false
     * ```
     */
    override fun remove(key: Any): V? {
        val keyChars = when (key) {
            is CharArray -> key
            else         -> key.toString().toCharArray()
        }
        val slot = getSlot(keyChars, 0, keyChars.size)
        if (_keys[slot] == null) return null  // 키가 존재하지 않음

        val oldValue = _values[slot]

        // 삭제 대상을 제외한 모든 엔트리를 수집
        @Suppress("UNCHECKED_CAST")
        val entries = mutableListOf<Pair<CharArray, V>>()
        for (i in _keys.indices) {
            if (i != slot && _keys[i] != null) {
                @Suppress("UNCHECKED_CAST")
                entries.add(_keys[i]!! to (_values[i] as V))
            }
        }

        // Open addressing에서 삭제 시 probe chain이 끊어지므로 전체 재구성
        clear()
        for ((k, v) in entries) {
            put(k, v)
        }

        return oldValue
    }

    @Suppress("PROPERTY_HIDES_JAVA_FIELD")
    /**
     * 현재 저장된 엔트리 개수다.
     *
     * ## 동작/계약
     * - 삽입 시 증가하고 `clear`/`remove`에서 감소 또는 재계산된다.
     * - 내부 카운터 `_count` 값을 그대로 반환한다.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2)
     * map.put("a", 1)
     * // map.size == 1
     * ```
     */
    override val size: Int get() = _count

    override fun toString(): String = buildString {
        append("{")
        this@CharArrayMap.entries.forEach { entry ->
            if (length > 1) append(", ")
            append(entry)
        }
        append("}")
    }

    private val _entrySet: EntrySet by lazy { createEntrySet() }

    /**
     * 엔트리 집합 생성을 담당하는 확장 포인트다.
     *
     * ## 동작/계약
     * - 기본 구현은 수정 가능한 `EntrySet(true)`를 반환한다.
     * - 읽기 전용 하위 클래스는 이 메서드를 재정의해 수정 불가 집합을 제공한다.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2)
     * // map.entries.isEmpty() == true
     * ```
     */
    protected open fun createEntrySet(): EntrySet = EntrySet(true)
    /**
     * 맵의 엔트리 뷰를 반환한다.
     *
     * ## 동작/계약
     * - 지연 초기화된 `_entrySet` 인스턴스를 재사용한다.
     * - 반환된 뷰의 수정 가능 여부는 `createEntrySet` 구현에 따릅니다.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2).apply { put("a", 1) }
     * // map.entries.first().value == 1
     * ```
     */
    override val entries: MutableSet<MutableMap.MutableEntry<Any, V>> get() = _entrySet

    private val _keySet: CharArraySet by lazy {
        object: CharArraySet(this@CharArrayMap as CharArrayMap<Any>) {
            override fun add(element: Any): Boolean = throw UnsupportedOperationException()
            override fun add(text: CharSequence): Boolean = throw UnsupportedOperationException()
            override fun add(text: String): Boolean = throw UnsupportedOperationException()
            override fun add(text: CharArray): Boolean = throw UnsupportedOperationException()
        }
    }

    /**
     * 원본 키(`CharArray`)를 복제 없이 노출하는 읽기 전용 키 집합이다.
     *
     * ## 동작/계약
     * - 순회 시 내부 `_keys` 배열의 실제 참조를 반환한다.
     * - 키 추가/삭제 연산은 지원하지 않는다.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2).apply { put("x", 1) }
     * val firstKey = map.originalKeySet.first() as CharArray
     * // String(firstKey) == "x"
     * ```
     */
    val originalKeySet: MutableSet<Any> by lazy {
        object: AbstractMutableSet<Any>() {
            override fun iterator(): MutableIterator<Any> = object: MutableIterator<Any> {

                private var pos = -1
                private var lastPos: Int = 0

                private fun goNext() {
                    lastPos = pos
                    pos++
                    while (pos < _keys.size && _keys[pos] == null) pos++
                }

                init {
                    goNext()
                }

                override fun hasNext(): Boolean = pos < _keys.size
                override fun next(): Any {
                    goNext()
                    return _keys[lastPos]!!
                }

                override fun remove() = throw UnsupportedOperationException()
            }

            override fun add(element: Any): Boolean = throw UnsupportedOperationException()

            override val size: Int
                get() = this@CharArrayMap.size

            override fun contains(element: Any): Boolean = this@CharArrayMap.containsKey(element)
        }
    }

    /**
     * 표준 `Map.keys` 뷰를 반환한다.
     *
     * ## 동작/계약
     * - 키 추가는 허용되지 않으며 조회 용도로만 사용한다.
     * - 내부적으로 `CharArraySet` 래퍼를 사용한다.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2).apply { put("k", 3) }
     * // map.keys.contains("k") == true
     * ```
     */
    override val keys: MutableSet<Any> get() = _keySet

    /**
     * 내부 슬롯을 순회하며 엔트리를 생성하는 반복자 구현체다.
     *
     * ## 동작/계약
     * - `allowModify=false`면 값 변경 API(`setValue`)를 차단한다.
     * - `nextKey`는 내부 `CharArray` 참조를 그대로 반환한다.
     * - `remove()`는 지원하지 않는다.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2).apply { put("a", 1) }
     * val it = map.EntryIterator(true)
     * // it.hasNext() == true
     * ```
     */
    inner class EntryIterator(private val allowModify: Boolean):
        MutableIterator<MutableMap.MutableEntry<Any, V>> {

        init {
            goNext()
        }

        private var pos = -1
        private var lastPos: Int = 0

        private fun goNext() {
            lastPos = pos
            pos++
            while (pos < _keys.size && _keys[pos] == null) pos++
        }

        /**
         * 다음 엔트리가 존재하는지 확인한다.
         *
         * ## 동작/계약
         * - 현재 포인터가 배열 범위 내 유효 슬롯을 가리키면 `true`를 반환한다.
         * - 상태를 변경하지 않는 조회 연산이다.
         *
         * ```kotlin
         * val map = CharArrayMap<Int>(2).apply { put("a", 1) }
         * // map.EntryIterator(true).hasNext() == true
         * ```
         */
        override fun hasNext(): Boolean {
            return pos < _keys.size
        }

        /**
         * 다음 키의 원본 `CharArray`를 반환한다.
         *
         * ## 동작/계약
         * - 내부 배열 참조를 직접 반환한다.
         * - 반환 배열을 외부에서 수정하면 맵 동작에 영향을 줄 수 있다.
         *
         * ```kotlin
         * val map = CharArrayMap<Int>(2).apply { put("a", 1) }
         * val key = map.EntryIterator(true).nextKey()
         * // String(key).isNotEmpty() == true
         * ```
         */
        fun nextKey(): CharArray {
            goNext()
            return _keys[lastPos]!!
        }

        /**
         * 다음 키를 새 `String` 인스턴스로 반환한다.
         *
         * ## 동작/계약
         * - `nextKey()` 결과를 복사 문자열로 변환한다.
         * - 원본 키 배열 수정 영향 없이 안전하게 조회할 때 사용한다.
         *
         * ```kotlin
         * val map = CharArrayMap<Int>(2).apply { put("a", 1) }
         * val key = map.EntryIterator(true).nextKeyString()
         * // key == "a"
         * ```
         */
        fun nextKeyString(): String {
            return String(nextKey())
        }

        /**
         * 마지막으로 반환된 키에 연결된 현재 값을 조회한다.
         *
         * ## 동작/계약
         * - `nextKey` 또는 `next` 호출 이후의 위치를 기준으로 값을 반환한다.
         * - 값이 없으면 `null`을 반환할 수 있다.
         *
         * ```kotlin
         * val map = CharArrayMap<Int>(2).apply { put("a", 1) }
         * val it = map.EntryIterator(true)
         * it.nextKey()
         * // it.currentValue() == 1
         * ```
         */
        fun currentValue(): V? {
            return _values[lastPos]
        }

        /**
         * 마지막으로 반환된 키의 값을 변경한다.
         *
         * ## 동작/계약
         * - `allowModify=false`면 `UnsupportedOperationException`을 던진다.
         * - 성공 시 이전 값을 반환한다.
         *
         * ```kotlin
         * val map = CharArrayMap<Int>(2).apply { put("a", 1) }
         * val it = map.EntryIterator(true)
         * it.nextKey()
         * // it.setValue(2) == 1
         * ```
         */
        fun setValue(value: V): V? {
            if (!allowModify)
                throw UnsupportedOperationException()
            val old = _values[lastPos]
            _values[lastPos] = value
            return old
        }

        /**
         * 현재 위치 엔트리를 `MutableEntry` 객체로 반환한다.
         *
         * ## 동작/계약
         * - 호출 시 내부 포인터를 다음 유효 슬롯으로 이동한다.
         * - 반환 엔트리는 `allowModify` 설정에 따라 값 변경 가능 여부가 달라진다.
         *
         * ```kotlin
         * val map = CharArrayMap<Int>(2).apply { put("a", 1) }
         * val entry = map.EntryIterator(true).next()
         * // entry.value == 1
         * ```
         */
        override fun next(): MutableMap.MutableEntry<Any, V> {
            goNext()
            return MapEntry(lastPos, allowModify)
        }

        /**
         * 반복자 기반 제거 연산을 지원하지 않는다.
         *
         * ## 동작/계약
         * - 호출 시 항상 `UnsupportedOperationException`을 던진다.
         * - 맵 삭제는 상위 `remove(key)` API를 사용해야 한다.
         *
         * ```kotlin
         * val map = CharArrayMap<Int>(2).apply { put("a", 1) }
         * // map.EntryIterator(true).remove() throws UnsupportedOperationException
         * ```
         */
        override fun remove() {
            throw UnsupportedOperationException()
        }
    }

    private inner class MapEntry(
        private val pos: Int,
        private val allowModify: Boolean,
    ): MutableMap.MutableEntry<Any, V> {
        override val key: Any
            get() = _keys[pos]!!.clone()

        override val value: V
            get() = _values[pos]!!

        override fun setValue(newValue: V): V {
            if (!allowModify)
                throw UnsupportedOperationException()

            val old = _values[pos]
            _values[pos] = value
            return old!!
        }

        override fun toString(): String {
            return String(_keys[pos]!!) + '=' +
                    if (_values[pos] === this@CharArrayMap) "(this Map)" else _values[pos]
        }
    }

    /**
     * 엔트리 집합 뷰 구현체다.
     *
     * ## 동작/계약
     * - 순회는 `EntryIterator`를 사용한다.
     * - `allowModify=false`일 때 `clear`를 포함한 변경 작업이 차단된다.
     * - 개수는 외부 맵 `_count`를 그대로 반영한다.
     *
     * ```kotlin
     * val map = CharArrayMap<Int>(2).apply { put("a", 1) }
     * // map.EntrySet(true).size == 1
     * ```
     */
    inner class EntrySet(private val allowModify: Boolean): AbstractMutableSet<MutableMap.MutableEntry<Any, V>>() {

        /**
         * 엔트리 순회 반복자를 반환한다.
         *
         * ## 동작/계약
         * - `allowModify` 설정을 공유하는 `EntryIterator`를 생성한다.
         * - 반복자 `remove`는 지원하지 않는다.
         *
         * ```kotlin
         * val map = CharArrayMap<Int>(2).apply { put("a", 1) }
         * // map.EntrySet(true).iterator().hasNext() == true
         * ```
         */
        override fun iterator(): MutableIterator<MutableMap.MutableEntry<Any, V>> {
            return EntryIterator(allowModify)
        }

        /**
         * 주어진 엔트리가 현재 맵에 동일 값으로 존재하는지 확인한다.
         *
         * ## 동작/계약
         * - 키로 현재 값을 조회한 뒤 전달된 값과 동등 비교한다.
         * - 키가 없으면 `false`를 반환한다.
         *
         * ```kotlin
         * val map = CharArrayMap<Int>(2).apply { put("a", 1) }
         * val entry = map.entries.first()
         * // map.EntrySet(true).contains(entry) == true
         * ```
         */
        override fun contains(element: MutableMap.MutableEntry<Any, V>): Boolean {
            val key = element.key
            val value = element.value
            val v = get(key)
            return if (v == null) value == null else v == value
        }

        /**
         * 엔트리 직접 추가는 지원하지 않는다.
         *
         * ## 동작/계약
         * - 호출 시 항상 `UnsupportedOperationException`을 던진다.
         * - 엔트리 추가는 `put` API를 사용해야 한다.
         *
         * ```kotlin
         * val map = CharArrayMap<Int>(2)
         * // map.EntrySet(true).add(map.entries.first()) throws UnsupportedOperationException
         * ```
         */
        override fun add(element: MutableMap.MutableEntry<Any, V>): Boolean {
            throw UnsupportedOperationException()
        }

        /**
         * 엔트리 직접 삭제는 지원하지 않는다.
         *
         * ## 동작/계약
         * - 호출 시 `UnsupportedOperationException`을 던진다.
         * - 삭제는 상위 `remove(key)` API를 통해 수행해야 한다.
         *
         * ```kotlin
         * val map = CharArrayMap<Int>(2).apply { put("a", 1) }
         * // map.EntrySet(true).remove(map.entries.first()) throws UnsupportedOperationException
         * ```
         */
        override fun remove(element: MutableMap.MutableEntry<Any, V>): Boolean {
            throw UnsupportedOperationException()
        }

        /**
         * 엔트리 개수를 반환한다.
         *
         * ## 동작/계약
         * - 외부 맵의 `_count`를 그대로 반영한다.
         * - 조회 연산이며 상태를 변경하지 않는다.
         *
         * ```kotlin
         * val map = CharArrayMap<Int>(2).apply { put("a", 1) }
         * // map.EntrySet(true).size == 1
         * ```
         */
        override val size: Int
            get() = _count

        /**
         * 엔트리 집합을 비운다.
         *
         * ## 동작/계약
         * - `allowModify=false`면 `UnsupportedOperationException`을 던진다.
         * - 허용된 경우 외부 맵의 `clear()`를 호출한다.
         *
         * ```kotlin
         * val map = CharArrayMap<Int>(2).apply { put("a", 1) }
         * map.EntrySet(true).clear()
         * // map.isEmpty() == true
         * ```
         */
        override fun clear() {
            if (!allowModify) throw UnsupportedOperationException()
            this@CharArrayMap.clear()
        }
    }

    /**
     * 모든 수정 연산을 차단하는 읽기 전용 `CharArrayMap` 구현체다.
     *
     * ## 동작/계약
     * - `put`, `remove`, `clear` 호출 시 `UnsupportedOperationException`을 던진다.
     * - 조회 연산(`get`, `containsKey`)은 원본 데이터 기준으로 동작한다.
     *
     * ```kotlin
     * val readonly = CharArrayMap.UnmodifiableCharArrayMap(CharArrayMap<Int>(2))
     * // readonly.isEmpty() == true
     * ```
     */
    open class UnmodifiableCharArrayMap<V>(map: CharArrayMap<V>): CharArrayMap<V>(map) {

        override fun clear() = throw UnsupportedOperationException()

        override fun put(key: Any, value: V): V? = throw UnsupportedOperationException()
        override fun put(text: CharArray, value: V): V? = throw UnsupportedOperationException()
        override fun put(text: CharSequence, value: V): V? = throw UnsupportedOperationException()
        override fun put(text: String, value: V): V? = throw UnsupportedOperationException()

        override fun remove(key: Any, value: V): Boolean = throw UnsupportedOperationException()
        override fun remove(key: Any): V? = throw UnsupportedOperationException()

        override fun createEntrySet(): EntrySet = EntrySet(false)
    }

    private class EmptyCharArrayMap<V>: UnmodifiableCharArrayMap<V>(CharArrayMap(0)) {

        override fun containsKey(text: CharArray, off: Int, len: Int): Boolean = false
        override fun containsKey(cs: CharSequence): Boolean = false
        override fun containsKey(key: Any): Boolean = false
        override fun get(text: CharArray, off: Int, len: Int): V? = null
        override fun get(cs: CharSequence): V? = null
        override fun get(key: Any): V? = null
    }

}
