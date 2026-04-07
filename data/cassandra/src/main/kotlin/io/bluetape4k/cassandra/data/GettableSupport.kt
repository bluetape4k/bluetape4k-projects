package io.bluetape4k.cassandra.data

import com.datastax.oss.driver.api.core.CqlIdentifier
import com.datastax.oss.driver.api.core.data.CqlDuration
import com.datastax.oss.driver.api.core.data.GettableById
import com.datastax.oss.driver.api.core.data.GettableByIndex
import com.datastax.oss.driver.api.core.data.GettableByName
import com.datastax.oss.driver.api.core.data.TupleValue
import com.datastax.oss.driver.api.core.data.UdtValue
import com.datastax.oss.driver.api.core.metadata.token.Token
import io.bluetape4k.io.getBytes
import io.bluetape4k.support.requireZeroOrPositiveNumber
import java.math.BigDecimal
import java.math.BigInteger
import java.net.InetAddress
import java.nio.ByteBuffer
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.*
import kotlin.reflect.KClass

/**
 * [CqlIdentifier]와 [KClass]를 사용하여 [GettableById]에서 값을 가져옵니다.
 *
 * ```kotlin
 * val nameId = CqlIdentifier.fromInternal("name")
 * val name = row.getValue(nameId, String::class)
 * // name == "Alice"
 * ```
 *
 * @param id 컬럼 식별자
 * @param kclass 값의 타입 클래스
 * @return 해당 컬럼의 값 또는 `null`
 */
fun <V: Any> GettableById.getValue(
    id: CqlIdentifier,
    kclass: KClass<V>,
): V? = get(id, kclass.java)

/**
 * [CqlIdentifier]를 사용하여 [GettableById]에서 reified 타입으로 값을 가져옵니다.
 *
 * ```kotlin
 * val nameId = CqlIdentifier.fromInternal("name")
 * val name = row.getValue<String>(nameId)
 * // name == "Alice"
 * ```
 *
 * @param id 컬럼 식별자
 * @return 해당 컬럼의 값 또는 `null`
 */
inline fun <reified V: Any> GettableById.getValue(id: CqlIdentifier): V? = get(id, V::class.java)

/**
 * [CqlIdentifier]를 사용하여 [GettableById]에서 리스트 값을 가져옵니다.
 *
 * ```kotlin
 * val tagsId = CqlIdentifier.fromInternal("tags")
 * val tags = row.getList<String>(tagsId)
 * // tags == mutableListOf("admin", "user")
 * ```
 *
 * @param id 컬럼 식별자
 * @return 해당 컬럼의 리스트 값 또는 `null`
 */
inline fun <reified V: Any> GettableById.getList(id: CqlIdentifier): MutableList<V>? = getList(id, V::class.java)

/**
 * [CqlIdentifier]를 사용하여 [GettableById]에서 세트 값을 가져옵니다.
 *
 * ```kotlin
 * val rolesId = CqlIdentifier.fromInternal("roles")
 * val roles = row.getSet<String>(rolesId)
 * // roles?.contains("admin") == true
 * ```
 *
 * @param id 컬럼 식별자
 * @return 해당 컬럼의 세트 값 또는 `null`
 */
inline fun <reified V: Any> GettableById.getSet(id: CqlIdentifier): MutableSet<V>? = getSet(id, V::class.java)

/**
 * [CqlIdentifier]를 사용하여 [GettableById]에서 맵 값을 가져옵니다.
 *
 * ```kotlin
 * val propsId = CqlIdentifier.fromInternal("props")
 * val props = row.getMap<String, String>(propsId)
 * // props?.get("key") == "value"
 * ```
 *
 * @param id 컬럼 식별자
 * @return 해당 컬럼의 맵 값 또는 `null`
 */
inline fun <reified K, reified V> GettableById.getMap(id: CqlIdentifier): MutableMap<K, V>? =
    getMap(id, K::class.java, V::class.java)

/**
 * [CqlIdentifier]를 사용하여 [GettableById]에서 지정 타입의 객체를 가져옵니다.
 *
 * ```kotlin
 * val nameId = CqlIdentifier.fromInternal("name")
 * val name = row.getObject(nameId, String::class)
 * // name == "Alice"
 * ```
 *
 * @param id 컬럼 식별자
 * @param requireType 기대하는 타입의 [KClass]
 * @return 해당 컬럼의 값 또는 `null`
 */
fun GettableById.getObject(
    id: CqlIdentifier,
    requireType: KClass<*>,
): Any? = getObject(firstIndexOf(id), requireType)

/**
 * 인덱스와 [KClass]를 사용하여 [GettableByIndex]에서 값을 가져옵니다.
 *
 * ```kotlin
 * val name = row.getValue(0, String::class)
 * // name == "Alice"
 * ```
 *
 * @param index 컬럼 인덱스
 * @param kclass 값의 타입 클래스
 * @return 해당 인덱스의 값 또는 `null`
 */
fun <V: Any> GettableByIndex.getValue(
    index: Int,
    kclass: KClass<V>,
): V? = get(index, kclass.java)

/**
 * 인덱스를 사용하여 [GettableByIndex]에서 reified 타입으로 값을 가져옵니다.
 *
 * ```kotlin
 * val name = row.getValue<String>(0)
 * // name == "Alice"
 * ```
 *
 * @param index 컬럼 인덱스
 * @return 해당 인덱스의 값 또는 `null`
 */
inline fun <reified V: Any> GettableByIndex.getValue(index: Int): V? = get(index, V::class.java)

/**
 * 인덱스를 사용하여 [GettableByIndex]에서 리스트 값을 가져옵니다.
 *
 * ```kotlin
 * val tags = row.getList<String>(2)
 * // tags == mutableListOf("admin", "user")
 * ```
 *
 * @param index 컬럼 인덱스
 * @return 해당 인덱스의 리스트 값 또는 `null`
 */
inline fun <reified V: Any> GettableByIndex.getList(index: Int): MutableList<V>? = getList(index, V::class.java)

/**
 * 인덱스를 사용하여 [GettableByIndex]에서 세트 값을 가져옵니다.
 *
 * ```kotlin
 * val roles = row.getSet<String>(3)
 * // roles?.contains("admin") == true
 * ```
 *
 * @param index 컬럼 인덱스
 * @return 해당 인덱스의 세트 값 또는 `null`
 */
inline fun <reified V: Any> GettableByIndex.getSet(index: Int): MutableSet<V>? = getSet(index, V::class.java)

/**
 * 인덱스를 사용하여 [GettableByIndex]에서 맵 값을 가져옵니다.
 *
 * ```kotlin
 * val props = row.getMap<String, String>(4)
 * // props?.get("key") == "value"
 * ```
 *
 * @param index 컬럼 인덱스
 * @return 해당 인덱스의 맵 값 또는 `null`
 */
inline fun <reified K, reified V> GettableByIndex.getMap(index: Int): MutableMap<K, V>? =
    getMap(index, K::class.java, V::class.java)

/**
 * 인덱스와 요구 타입을 사용하여 [GettableByIndex]에서 타입별로 적절한 getter를 호출하여 값을 가져옵니다.
 *
 * @param index 컬럼 인덱스 (0 이상이어야 함)
 * @param requireType 기대하는 타입의 [KClass]
 * @return 해당 인덱스의 값 또는 `null`
 */
fun GettableByIndex.getObject(
    index: Int,
    requireType: KClass<*>,
): Any? {
    index.requireZeroOrPositiveNumber("index")
    if (isNull(index)) {
        return null
    }
    return when (requireType) {
        String::class     -> getString(index)
        Boolean::class    -> getBoolean(index)
        Byte::class       -> getByte(index)
        Short::class      -> getShort(index)
        Int::class        -> getInt(index)
        Long::class       -> getLong(index)
        Float::class      -> getFloat(index)
        Double::class     -> getDouble(index)
        BigDecimal::class -> getBigDecimal(index)
        BigInteger::class -> getBigInteger(index)
        LocalDate::class  -> getLocalDate(index)
        LocalTime::class  -> getLocalTime(index)
        Date::class       -> Date.from(getInstant(index))
        Timestamp::class  -> Timestamp(getInstant(index)!!.toEpochMilli())
        Instant::class    -> getInstant(index)
        ByteBuffer::class -> getByteBuffer(index)
        ByteArray::class  -> getByteBuffer(index)?.getBytes()
        InetAddress::class -> getInetAddress(index)
        CqlDuration::class -> getCqlDuration(index)
        Token::class      -> getToken(index)
        TupleValue::class -> getTupleValue(index)
        UdtValue::class   -> getUdtValue(index)
        UUID::class       -> getUuid(index)
        else              -> get(index, requireType.java)
    }
}

/**
 * 컬럼 이름과 [KClass]를 사용하여 [GettableByName]에서 값을 가져옵니다.
 *
 * ```kotlin
 * val name = row.getValue("name", String::class)
 * // name == "Alice"
 * ```
 *
 * @param name 컬럼 이름
 * @param kclass 값의 타입 클래스
 * @return 해당 컬럼의 값 또는 `null`
 */
fun <V: Any> GettableByName.getValue(
    name: String,
    kclass: KClass<V>,
): V? = get(name, kclass.java)

/**
 * 컬럼 이름을 사용하여 [GettableByName]에서 reified 타입으로 값을 가져옵니다.
 *
 * ```kotlin
 * val name = row.getValue<String>("name")
 * // name == "Alice"
 * ```
 *
 * @param name 컬럼 이름
 * @return 해당 컬럼의 값 또는 `null`
 */
inline fun <reified V: Any> GettableByName.getValue(name: String): V? = get(name, V::class.java)

/**
 * 컬럼 이름을 사용하여 [GettableByName]에서 리스트 값을 가져옵니다.
 *
 * ```kotlin
 * val tags = row.getList<String>("tags")
 * // tags == mutableListOf("admin", "user")
 * ```
 *
 * @param name 컬럼 이름
 * @return 해당 컬럼의 리스트 값 또는 `null`
 */
inline fun <reified V: Any> GettableByName.getList(name: String): MutableList<V>? = getList(name, V::class.java)

/**
 * 컬럼 이름을 사용하여 [GettableByName]에서 세트 값을 가져옵니다.
 *
 * ```kotlin
 * val roles = row.getSet<String>("roles")
 * // roles?.contains("admin") == true
 * ```
 *
 * @param name 컬럼 이름
 * @return 해당 컬럼의 세트 값 또는 `null`
 */
inline fun <reified V: Any> GettableByName.getSet(name: String): MutableSet<V>? = getSet(name, V::class.java)

/**
 * 컬럼 이름을 사용하여 [GettableByName]에서 맵 값을 가져옵니다.
 *
 * ```kotlin
 * val props = row.getMap<String, String>("props")
 * // props?.get("key") == "value"
 * ```
 *
 * @param name 컬럼 이름
 * @return 해당 컬럼의 맵 값 또는 `null`
 */
inline fun <reified K, reified V> GettableByName.getMap(name: String): MutableMap<K, V>? =
    getMap(name, K::class.java, V::class.java)

/**
 * 컬럼 이름과 요구 타입을 사용하여 [GettableByName]에서 지정 타입의 객체를 가져옵니다.
 *
 * ```kotlin
 * val name = row.getObject("name", String::class)
 * // name == "Alice"
 * ```
 *
 * @param name 컬럼 이름
 * @param requireType 기대하는 타입의 [KClass]
 * @return 해당 컬럼의 값 또는 `null`
 */
fun GettableByName.getObject(
    name: String,
    requireType: KClass<*>,
): Any? = getObject(firstIndexOf(name), requireType)
