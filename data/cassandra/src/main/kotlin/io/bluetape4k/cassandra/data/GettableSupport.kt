package io.bluetape4k.cassandra.data

import com.datastax.oss.driver.api.core.CqlIdentifier
import com.datastax.oss.driver.api.core.data.GettableById
import com.datastax.oss.driver.api.core.data.GettableByIndex
import com.datastax.oss.driver.api.core.data.GettableByName
import com.datastax.oss.driver.api.core.data.TupleValue
import com.datastax.oss.driver.api.core.data.UdtValue
import com.datastax.oss.driver.api.core.metadata.token.Token
import io.bluetape4k.io.getBytes
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.ByteBuffer
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.*
import kotlin.reflect.KClass


fun <V: Any> GettableById.getValue(id: CqlIdentifier, kclass: KClass<V>): V? = get(id, kclass.java)
inline fun <reified V: Any> GettableById.getValue(id: CqlIdentifier): V? = get(id, V::class.java)
inline fun <reified V: Any> GettableById.getList(id: CqlIdentifier): MutableList<V>? = getList(id, V::class.java)
inline fun <reified V: Any> GettableById.getSet(id: CqlIdentifier): MutableSet<V>? = getSet(id, V::class.java)
inline fun <reified K, reified V> GettableById.getMap(id: CqlIdentifier): MutableMap<K, V>? =
    getMap(id, K::class.java, V::class.java)

fun GettableById.getObject(id: CqlIdentifier, requireType: KClass<*>): Any? =
    getObject(firstIndexOf(id), requireType)


fun <V: Any> GettableByIndex.getValue(index: Int, kclass: KClass<V>): V? = get(index, kclass.java)
inline fun <reified V: Any> GettableById.getValue(index: Int): V? = get(index, V::class.java)
inline fun <reified V: Any> GettableByIndex.getList(index: Int): MutableList<V>? = getList(index, V::class.java)
inline fun <reified V: Any> GettableByIndex.getSet(index: Int): MutableSet<V>? = getSet(index, V::class.java)
inline fun <reified K, reified V> GettableByIndex.getMap(index: Int): MutableMap<K, V>? =
    getMap(index, K::class.java, V::class.java)

fun GettableByIndex.getObject(index: Int, requireType: KClass<*>): Any? {
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
        Token::class      -> getToken(index)
        TupleValue::class -> getTupleValue(index)
        UdtValue::class   -> getUdtValue(index)
        UUID::class       -> getUuid(index)
        else              -> get(index, requireType.java)
    }
}

fun <V: Any> GettableByName.getValue(name: String, kclass: KClass<V>): V? = get(name, kclass.java)
inline fun <reified V: Any> GettableByName.getValue(name: String): V? = get(name, V::class.java)
inline fun <reified V: Any> GettableByName.getList(name: String): MutableList<V>? = getList(name, V::class.java)
inline fun <reified V: Any> GettableByName.getSet(name: String): MutableSet<V>? = getSet(name, V::class.java)
inline fun <reified K, reified V> GettableByName.getMap(name: String): MutableMap<K, V>? =
    getMap(name, K::class.java, V::class.java)

fun GettableByName.getObject(name: String, requireType: KClass<*>): Any? =
    getObject(firstIndexOf(name), requireType)
