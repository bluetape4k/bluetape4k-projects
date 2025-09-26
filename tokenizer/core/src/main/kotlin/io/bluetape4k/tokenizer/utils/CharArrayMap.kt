package io.bluetape4k.tokenizer.utils

import io.bluetape4k.logging.KLogging
import java.io.Serializable
import java.util.*

/**
 * Lucene 에 있는 CharArrayMap 을 porting 한 클래스로, 사전 정보를 관리합니다.
 */
@Suppress("UNCHECKED_CAST")
open class CharArrayMap<V>(startSize: Int): HashMap<Any, V>(), Serializable {
    // java.util.AbstractMap<Any, V>(), Serializable {

    companion object: KLogging() {
        private const val INIT_SIZE = 8
        private val EMPTY_MAP: CharArrayMap<Any> = EmptyCharArrayMap()

        @JvmStatic
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
        fun <V> copy(map: Map<Any, V>): CharArrayMap<V> = CharArrayMap(map)

        @JvmStatic
        fun <V> emptyMap(): CharArrayMap<V> = EMPTY_MAP as CharArrayMap<V>
    }

    @Suppress("LeakingThis")
    constructor(c: Map<Any, V>): this(c.size) {
        putAll(c)
    }

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

    override fun clear() {
        _count = 0
        _keys.fill(null)
        _values.fill(null)
    }

    open fun containsKey(text: CharArray, off: Int, len: Int): Boolean {
        return _keys[getSlot(text, off, len)] != null
    }

    open fun containsKey(cs: CharSequence): Boolean = _keys[getSlot(cs)] != null

    override fun containsKey(key: Any): Boolean = when (key) {
        is CharArray -> containsKey(key, 0, key.size)
        else         -> containsKey(key.toString())
    }

    open fun get(text: CharArray, off: Int, len: Int): V? {
        return _values[getSlot(text, off, len)]
    }

    open fun get(cs: CharSequence): V? = _values[getSlot(cs)]

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

    open fun put(text: CharSequence, value: V): V? = put(text.toString(), value)

    override fun put(key: Any, value: V): V? = when (key) {
        is CharArray -> put(key, value)
        else         -> put(key.toString(), value)
    }

    open fun put(text: String, value: V): V? = put(text.toCharArray(), value)

    /**
     * Add the given mapping.
     * If ignoreCase is true for this Set, the text array will be directly modified.
     * The user should never modify this text array after calling this method.
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
        assert(_keys.size == _values.size) { "keys size [${_keys.size}] must equals to _values size[${_values.size}" }

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
        //        (0 until len).forEach { i ->
        //            if (text1[i] != text2[i])
        //                return false
        //        }
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

    override fun remove(key: Any): V? {
        val slot = getSlot(key.toString().toCharArray(), 0)
        _keys[slot] = null
        return null
    }

    @Suppress("PROPERTY_HIDES_JAVA_FIELD")
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

    protected open fun createEntrySet(): EntrySet = EntrySet(true)
    override val entries: EntrySet get() = _entrySet

    private val _keySet: CharArraySet by lazy {
        object: CharArraySet(this@CharArrayMap as CharArrayMap<Any>) {
            override fun add(element: Any): Boolean = throw UnsupportedOperationException()
            override fun add(text: CharSequence): Boolean = throw UnsupportedOperationException()
            override fun add(text: String): Boolean = throw UnsupportedOperationException()
            override fun add(text: CharArray): Boolean = throw UnsupportedOperationException()
        }
    }

    val originalKeySet: MutableSet<Any> by lazy {
        object: AbstractSet<Any>() {
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

            override val size: Int
                get() = this@CharArrayMap.size

            override fun isEmpty(): Boolean = this@CharArrayMap.isEmpty()
            override fun clear() = this@CharArrayMap.clear()
            override fun contains(element: Any?): Boolean = this@CharArrayMap.containsKey(element)
        }
    }

    override val keys: CharArraySet get() = _keySet

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

        override fun hasNext(): Boolean {
            return pos < _keys.size
        }

        /**
         * gets the next key... do not modify the returned char[]
         */
        fun nextKey(): CharArray {
            goNext()
            return _keys[lastPos]!!
        }

        /**
         * gets the next key as a newly created String object
         */
        fun nextKeyString(): String {
            return String(nextKey())
        }

        /**
         * returns the value associated with the last key returned
         */
        fun currentValue(): V? {
            return _values[lastPos]
        }

        /**
         * sets the value associated with the last key returned
         */
        fun setValue(value: V): V? {
            if (!allowModify)
                throw UnsupportedOperationException()
            val old = _values[lastPos]
            _values[lastPos] = value
            return old
        }

        /**
         * use nextCharArray() + currentValue() for better efficiency.
         */
        override fun next(): MutableMap.MutableEntry<Any, V> {
            goNext()
            return MapEntry(lastPos, allowModify)
        }

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

    inner class EntrySet(private val allowModify: Boolean): java.util.AbstractSet<MutableMap.MutableEntry<Any, V>>() {

        override fun iterator(): MutableIterator<MutableMap.MutableEntry<Any, V>> {
            return EntryIterator(allowModify)
        }

        override fun contains(element: MutableMap.MutableEntry<Any, V>?): Boolean {
            val key = element?.key
            val value = element?.value

            key?.let {
                val v = get(it)
                return if (v == null) value == null else v == value
            }
            return false
        }

        override fun remove(element: MutableMap.MutableEntry<Any, V>?): Boolean {
            throw UnsupportedOperationException()
        }

        override val size: Int
            get() = _count

        override fun clear() {
            if (!allowModify) throw UnsupportedOperationException()
            this@CharArrayMap.clear()
        }
    }

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
