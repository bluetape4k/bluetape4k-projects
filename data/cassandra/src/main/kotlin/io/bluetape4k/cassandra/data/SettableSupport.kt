package io.bluetape4k.cassandra.data

import com.datastax.oss.driver.api.core.CqlIdentifier
import com.datastax.oss.driver.api.core.data.SettableById
import com.datastax.oss.driver.api.core.data.SettableByIndex
import com.datastax.oss.driver.api.core.data.SettableByName


//
// SettableById
//

inline fun <T: SettableById<T>, reified V> SettableById<T>.setValue(id: CqlIdentifier, value: V): T =
    set(id, value, V::class.java)

inline fun <T: SettableById<T>, reified V> SettableById<T>.setList(id: CqlIdentifier, values: List<V>): T =
    setList(id, values, V::class.java)

inline fun <T: SettableById<T>, reified V> SettableById<T>.setSet(id: CqlIdentifier, values: Set<V>): T =
    setSet(id, values, V::class.java)

inline fun <T: SettableById<T>, reified K, reified V> SettableById<T>.setMap(id: CqlIdentifier, values: Map<K, V>): T =
    setMap(id, values, K::class.java, V::class.java)


//
// SettableByIndex
//

inline fun <T: SettableByIndex<T>, reified V> SettableByIndex<T>.setValue(index: Int, value: V): T =
    set(index, value, V::class.java)

inline fun <T: SettableById<T>, reified V> SettableByIndex<T>.setList(index: Int, values: List<V>): T =
    setList(index, values, V::class.java)

inline fun <T: SettableById<T>, reified V> SettableByIndex<T>.setSet(index: Int, values: Set<V>): T =
    setSet(index, values, V::class.java)

inline fun <T: SettableById<T>, reified K, reified V> SettableByIndex<T>.setMap(index: Int, values: Map<K, V>): T =
    setMap(index, values, K::class.java, V::class.java)


//
// SettableByName
//

inline fun <T: SettableByName<T>, reified V: Any> SettableByName<T>.setValue(name: String, value: V?): T =
    set(name, value, V::class.java)

inline fun <T: SettableByName<T>, reified V> SettableByName<T>.setList(name: String, values: List<V>): T =
    setList(name, values, V::class.java)

inline fun <T: SettableByName<T>, reified V> SettableByName<T>.setSet(name: String, values: Set<V>): T =
    setSet(name, values, V::class.java)

inline fun <T: SettableByName<T>, reified K, reified V> SettableByName<T>.setMap(name: String, values: Map<K, V>): T =
    setMap(name, values, K::class.java, V::class.java)
