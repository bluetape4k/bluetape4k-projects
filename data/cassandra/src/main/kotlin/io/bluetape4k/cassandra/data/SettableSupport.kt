package io.bluetape4k.cassandra.data

import com.datastax.oss.driver.api.core.CqlIdentifier
import com.datastax.oss.driver.api.core.data.SettableById
import com.datastax.oss.driver.api.core.data.SettableByIndex
import com.datastax.oss.driver.api.core.data.SettableByName

//
// SettableById
//

/**
 * [CqlIdentifier]를 사용하여 [SettableById]에 reified 타입 값을 설정합니다.
 *
 * ```kotlin
 * val nameId = CqlIdentifier.fromInternal("name")
 * udtValue.setValue(nameId, "Alice")
 * ```
 *
 * @param id 컬럼 식별자
 * @param value 설정할 값
 * @return 설정된 [SettableById] 인스턴스
 */
inline fun <T : SettableById<T>, reified V> SettableById<T>.setValue(
    id: CqlIdentifier,
    value: V,
): T = set(id, value, V::class.java)

/**
 * [CqlIdentifier]를 사용하여 [SettableById]에 리스트 값을 설정합니다.
 *
 * ```kotlin
 * val tagsId = CqlIdentifier.fromInternal("tags")
 * udtValue.setList(tagsId, listOf("admin", "user"))
 * ```
 *
 * @param id 컬럼 식별자
 * @param values 설정할 리스트 값
 * @return 설정된 [SettableById] 인스턴스
 */
inline fun <T : SettableById<T>, reified V> SettableById<T>.setList(
    id: CqlIdentifier,
    values: List<V>,
): T = setList(id, values, V::class.java)

/**
 * [CqlIdentifier]를 사용하여 [SettableById]에 세트 값을 설정합니다.
 *
 * ```kotlin
 * val rolesId = CqlIdentifier.fromInternal("roles")
 * udtValue.setSet(rolesId, setOf("admin", "user"))
 * ```
 *
 * @param id 컬럼 식별자
 * @param values 설정할 세트 값
 * @return 설정된 [SettableById] 인스턴스
 */
inline fun <T : SettableById<T>, reified V> SettableById<T>.setSet(
    id: CqlIdentifier,
    values: Set<V>,
): T = setSet(id, values, V::class.java)

/**
 * [CqlIdentifier]를 사용하여 [SettableById]에 맵 값을 설정합니다.
 *
 * ```kotlin
 * val propsId = CqlIdentifier.fromInternal("props")
 * udtValue.setMap(propsId, mapOf("key" to "value"))
 * ```
 *
 * @param id 컬럼 식별자
 * @param values 설정할 맵 값
 * @return 설정된 [SettableById] 인스턴스
 */
inline fun <T : SettableById<T>, reified K, reified V> SettableById<T>.setMap(
    id: CqlIdentifier,
    values: Map<K, V>,
): T = setMap(id, values, K::class.java, V::class.java)

//
// SettableByIndex
//

/**
 * 인덱스를 사용하여 [SettableByIndex]에 reified 타입 값을 설정합니다.
 *
 * ```kotlin
 * tupleValue.setValue(0, "Alice")
 * ```
 *
 * @param index 컬럼 인덱스
 * @param value 설정할 값
 * @return 설정된 [SettableByIndex] 인스턴스
 */
inline fun <T : SettableByIndex<T>, reified V> SettableByIndex<T>.setValue(
    index: Int,
    value: V,
): T = set(index, value, V::class.java)

/**
 * 인덱스를 사용하여 [SettableByIndex]에 리스트 값을 설정합니다.
 *
 * ```kotlin
 * tupleValue.setList(1, listOf("admin", "user"))
 * ```
 *
 * @param index 컬럼 인덱스
 * @param values 설정할 리스트 값
 * @return 설정된 [SettableByIndex] 인스턴스
 */
inline fun <T : SettableByIndex<T>, reified V> SettableByIndex<T>.setList(
    index: Int,
    values: List<V>,
): T = setList(index, values, V::class.java)

/**
 * 인덱스를 사용하여 [SettableByIndex]에 세트 값을 설정합니다.
 *
 * ```kotlin
 * tupleValue.setSet(2, setOf("a", "b"))
 * ```
 *
 * @param index 컬럼 인덱스
 * @param values 설정할 세트 값
 * @return 설정된 [SettableByIndex] 인스턴스
 */
inline fun <T : SettableByIndex<T>, reified V> SettableByIndex<T>.setSet(
    index: Int,
    values: Set<V>,
): T = setSet(index, values, V::class.java)

/**
 * 인덱스를 사용하여 [SettableByIndex]에 맵 값을 설정합니다.
 *
 * ```kotlin
 * tupleValue.setMap(3, mapOf("key" to "value"))
 * ```
 *
 * @param index 컬럼 인덱스
 * @param values 설정할 맵 값
 * @return 설정된 [SettableByIndex] 인스턴스
 */
inline fun <T : SettableByIndex<T>, reified K, reified V> SettableByIndex<T>.setMap(
    index: Int,
    values: Map<K, V>,
): T = setMap(index, values, K::class.java, V::class.java)

//
// SettableByName
//

/**
 * 컬럼 이름을 사용하여 [SettableByName]에 reified 타입 값을 설정합니다.
 *
 * ```kotlin
 * boundStatement.setValue("name", "Alice")
 * ```
 *
 * @param name 컬럼 이름
 * @param value 설정할 값 (nullable)
 * @return 설정된 [SettableByName] 인스턴스
 */
inline fun <T : SettableByName<T>, reified V : Any> SettableByName<T>.setValue(
    name: String,
    value: V?,
): T = set(name, value, V::class.java)

/**
 * 컬럼 이름을 사용하여 [SettableByName]에 리스트 값을 설정합니다.
 *
 * ```kotlin
 * boundStatement.setList("tags", listOf("admin", "user"))
 * ```
 *
 * @param name 컬럼 이름
 * @param values 설정할 리스트 값
 * @return 설정된 [SettableByName] 인스턴스
 */
inline fun <T : SettableByName<T>, reified V> SettableByName<T>.setList(
    name: String,
    values: List<V>,
): T = setList(name, values, V::class.java)

/**
 * 컬럼 이름을 사용하여 [SettableByName]에 세트 값을 설정합니다.
 *
 * ```kotlin
 * boundStatement.setSet("roles", setOf("admin", "user"))
 * ```
 *
 * @param name 컬럼 이름
 * @param values 설정할 세트 값
 * @return 설정된 [SettableByName] 인스턴스
 */
inline fun <T : SettableByName<T>, reified V> SettableByName<T>.setSet(
    name: String,
    values: Set<V>,
): T = setSet(name, values, V::class.java)

/**
 * 컬럼 이름을 사용하여 [SettableByName]에 맵 값을 설정합니다.
 *
 * ```kotlin
 * boundStatement.setMap("props", mapOf("key" to "value"))
 * ```
 *
 * @param name 컬럼 이름
 * @param values 설정할 맵 값
 * @return 설정된 [SettableByName] 인스턴스
 */
inline fun <T : SettableByName<T>, reified K, reified V> SettableByName<T>.setMap(
    name: String,
    values: Map<K, V>,
): T = setMap(name, values, K::class.java, V::class.java)
