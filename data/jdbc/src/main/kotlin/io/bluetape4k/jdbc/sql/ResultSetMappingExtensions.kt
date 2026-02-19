package io.bluetape4k.jdbc.sql

import java.sql.ResultSet
import java.sql.SQLException

/**
 * ResultSet을 객체로 매핑하는 함수형 인터페이스입니다.
 */
fun interface ResultSetMapper<T> {
    /**
     * ResultSet의 현재 행을 객체로 매핑합니다.
     *
     * @param rs 매핑할 ResultSet
     * @return 매핑된 객체
     * @throws SQLException ResultSet 접근 중 오류 발생 시
     */
    @Throws(SQLException::class)
    fun map(rs: ResultSet): T
}

/**
 * ResultSet의 첫 번째 행을 객체로 매핑하여 반환합니다.
 * 결과가 없으면 null을 반환합니다.
 *
 * ```kotlin
 * val user = rs.mapFirst { row ->
 *     User(row.getInt("id"), row.getString("name"))
 * }
 * ```
 *
 * @param T 결과 타입
 * @param mapper ResultSet을 객체로 매핑하는 함수
 * @return 매핑된 객체 또는 null
 */
inline fun <T> ResultSet.mapFirst(crossinline mapper: (ResultSet) -> T): T? = if (this.next()) mapper(this) else null

/**
 * ResultSet의 첫 번째 행을 객체로 매핑하여 반환합니다.
 * 결과가 없으면 예외를 발생시킵니다.
 *
 * ```kotlin
 * val user = rs.mapFirstOrThrow { row ->
 *     User(row.getInt("id"), row.getString("name"))
 * }
 * ```
 *
 * @param T 결과 타입
 * @param mapper ResultSet을 객체로 매핑하는 함수
 * @return 매핑된 객체
 * @throws NoSuchElementException 결과가 없을 경우
 */
inline fun <T> ResultSet.mapFirstOrThrow(crossinline mapper: (ResultSet) -> T): T =
    mapFirst(mapper) ?: throw NoSuchElementException("ResultSet is empty")

/**
 * ResultSet의 단일 행을 객체로 매핑하여 반환합니다.
 * 결과가 없거나 2개 이상이면 예외를 발생시킵니다.
 *
 * ```kotlin
 * val user = rs.mapSingle { row ->
 *     User(row.getInt("id"), row.getString("name"))
 * }
 * ```
 *
 * @param T 결과 타입
 * @param mapper ResultSet을 객체로 매핑하는 함수
 * @return 매핑된 객체
 * @throws NoSuchElementException 결과가 없을 경우
 * @throws IllegalStateException 결과가 2개 이상일 경우
 */
inline fun <T> ResultSet.mapSingle(crossinline mapper: (ResultSet) -> T): T {
    if (!this.next()) {
        throw NoSuchElementException("ResultSet is empty")
    }
    val result = mapper(this)
    if (this.next()) {
        throw IllegalStateException("ResultSet contains more than one row")
    }
    return result
}

/**
 * ResultSet을 리스트로 변환합니다.
 *
 * ```kotlin
 * val users = rs.toList { row ->
 *     User(row.getInt("id"), row.getString("name"))
 * }
 * ```
 *
 * @param T 결과 타입
 * @param mapper ResultSet을 객체로 매핑하는 함수
 * @return 객체 리스트
 */
inline fun <T> ResultSet.toList(crossinline mapper: (ResultSet) -> T): List<T> {
    val list = mutableListOf<T>()
    while (this.next()) {
        list.add(mapper(this))
    }
    return list
}

/**
 * ResultSet을 가변 리스트로 변환합니다.
 *
 * ```kotlin
 * val users = rs.toMutableList { row ->
 *     User(row.getInt("id"), row.getString("name"))
 * }
 * ```
 *
 * @param T 결과 타입
 * @param mapper ResultSet을 객체로 매핑하는 함수
 * @return 객체 가변 리스트
 */
inline fun <T> ResultSet.toMutableList(crossinline mapper: (ResultSet) -> T): MutableList<T> {
    val list = mutableListOf<T>()
    while (this.next()) {
        list.add(mapper(this))
    }
    return list
}

/**
 * ResultSet을 집합으로 변환합니다.
 *
 * ```kotlin
 * val userIds = rs.toSet { row ->
 *     row.getInt("id")
 * }
 * ```
 *
 * @param T 결과 타입
 * @param mapper ResultSet을 객체로 매핑하는 함수
 * @return 객체 집합
 */
inline fun <T> ResultSet.toSet(crossinline mapper: (ResultSet) -> T): Set<T> {
    val set = mutableSetOf<T>()
    while (this.next()) {
        set.add(mapper(this))
    }
    return set
}

/**
 * ResultSet을 맵으로 변환합니다.
 * 지정된 키 추출 함수를 사용하여 키-값 쌍을 생성합니다.
 *
 * ```kotlin
 * val userMap = rs.toMap(
 *     keyMapper = { row -> row.getInt("id") },
 *     valueMapper = { row -> User(row.getInt("id"), row.getString("name")) }
 * )
 * ```
 *
 * @param K 키 타입
 * @param V 값 타입
 * @param keyMapper 키를 추출하는 함수
 * @param valueMapper 값을 추출하는 함수
 * @return 키-값 맵
 */
inline fun <K, V> ResultSet.toMap(
    crossinline keyMapper: (ResultSet) -> K,
    crossinline valueMapper: (ResultSet) -> V,
): Map<K, V> {
    val map = mutableMapOf<K, V>()
    while (this.next()) {
        map[keyMapper(this)] = valueMapper(this)
    }
    return map
}

/**
 * ResultSet을 맵으로 변환합니다.
 * 동일한 키를 가진 값들은 리스트로 그룹화됩니다.
 *
 * ```kotlin
 * val usersByStatus = rs.groupBy(
 *     keyMapper = { row -> row.getString("status") },
 *     valueMapper = { row -> User(row.getInt("id"), row.getString("name")) }
 * )
 * ```
 *
 * @param K 키 타입
 * @param V 값 타입
 * @param keyMapper 키를 추출하는 함수
 * @param valueMapper 값을 추출하는 함수
 * @return 키-값 리스트 맵
 */
inline fun <K, V> ResultSet.groupBy(
    crossinline keyMapper: (ResultSet) -> K,
    crossinline valueMapper: (ResultSet) -> V,
): Map<K, List<V>> {
    val map = mutableMapOf<K, MutableList<V>>()
    while (this.next()) {
        val key = keyMapper(this)
        val value = valueMapper(this)
        map.getOrPut(key) { mutableListOf() }.add(value)
    }
    return map
}

/**
 * ResultSet을 순회하며 각 행에 대해 작업을 수행합니다.
 *
 * ```kotlin
 * rs.forEach { row ->
 *     println("User: ${row.getString("name")}")
 * }
 * ```
 *
 * @param action 각 행에서 수행할 작업
 */
inline fun ResultSet.forEach(action: (ResultSet) -> Unit) {
    while (this.next()) {
        action(this)
    }
}

/**
 * ResultSet을 순회하며 인덱스와 함께 각 행에 대해 작업을 수행합니다.
 *
 * ```kotlin
 * rs.forEachIndexed { index, row ->
 *     println("$index: User: ${row.getString("name")}")
 * }
 * ```
 *
 * @param action 각 행에서 수행할 작업 (인덱스, ResultSet)
 */
inline fun ResultSet.forEachIndexed(action: (Int, ResultSet) -> Unit) {
    var index = 0
    while (this.next()) {
        action(index++, this)
    }
}

/**
 * ResultSet을 필터링하여 조건을 만족하는 행들만 매핑합니다.
 *
 * ```kotlin
 * val activeUsers = rs.filterMap(
 *     predicate = { row -> row.getString("status") == "active" },
 *     mapper = { row -> User(row.getInt("id"), row.getString("name")) }
 * )
 * ```
 *
 * @param T 결과 타입
 * @param predicate 필터링 조건
 * @param mapper ResultSet을 객체로 매핑하는 함수
 * @return 필터링된 객체 리스트
 */
inline fun <T> ResultSet.filterMap(
    crossinline predicate: (ResultSet) -> Boolean,
    crossinline mapper: (ResultSet) -> T,
): List<T> {
    val list = mutableListOf<T>()
    while (this.next()) {
        if (predicate(this)) {
            list.add(mapper(this))
        }
    }
    return list
}

/**
 * ResultSet의 모든 행이 주어진 조건을 만족하는지 확인합니다.
 *
 * ```kotlin
 * val allActive = rs.all { row ->
 *     row.getString("status") == "active"
 * }
 * ```
 *
 * @param predicate 확인할 조건
 * @return 모든 행이 조건을 만족하면 true, 그렇지 않으면 false
 */
inline fun ResultSet.all(predicate: (ResultSet) -> Boolean): Boolean {
    while (this.next()) {
        if (!predicate(this)) {
            return false
        }
    }
    return true
}

/**
 * ResultSet의 행 중 하나라도 주어진 조건을 만족하는지 확인합니다.
 *
 * ```kotlin
 * val hasAdmin = rs.any { row ->
 *     row.getString("role") == "admin"
 * }
 * ```
 *
 * @param predicate 확인할 조건
 * @return 하나라도 조건을 만족하면 true, 그렇지 않으면 false
 */
inline fun ResultSet.any(predicate: (ResultSet) -> Boolean): Boolean {
    while (this.next()) {
        if (predicate(this)) {
            return true
        }
    }
    return false
}

/**
 * ResultSet의 행 중 주어진 조건을 만족하는 행이 없는지 확인합니다.
 *
 * ```kotlin
 * val noInactive = rs.none { row ->
 *     row.getString("status") == "inactive"
 * }
 * ```
 *
 * @param predicate 확인할 조건
 * @return 조건을 만족하는 행이 없으면 true, 그렇지 않으면 false
 */
inline fun ResultSet.none(predicate: (ResultSet) -> Boolean): Boolean = !any(predicate)

/**
 * ResultSet에서 주어진 조건을 만족하는 첫 번째 행을 반환합니다.
 *
 * ```kotlin
 * val firstAdmin = rs.firstOrNull { row ->
 *     row.getString("role") == "admin"
 * }
 * ```
 *
 * @param T 결과 타입
 * @param predicate 검색 조건
 * @param mapper ResultSet을 객체로 매핑하는 함수
 * @return 조건을 만족하는 첫 번째 객체 또는 null
 */
inline fun <T> ResultSet.firstOrNull(
    crossinline predicate: (ResultSet) -> Boolean,
    crossinline mapper: (ResultSet) -> T,
): T? {
    while (this.next()) {
        if (predicate(this)) {
            return mapper(this)
        }
    }
    return null
}

/**
 * ResultSet에서 주어진 조건을 만족하는 첫 번째 행을 반환합니다.
 * 조건을 만족하는 행이 없으면 예외를 발생시킵니다.
 *
 * ```kotlin
 * val firstAdmin = rs.first { row ->
 *     row.getString("role") == "admin"
 * }
 * ```
 *
 * @param T 결과 타입
 * @param predicate 검색 조건
 * @param mapper ResultSet을 객체로 매핑하는 함수
 * @return 조건을 만족하는 첫 번째 객체
 * @throws NoSuchElementException 조건을 만족하는 행이 없을 경우
 */
inline fun <T> ResultSet.first(
    crossinline predicate: (ResultSet) -> Boolean,
    crossinline mapper: (ResultSet) -> T,
): T =
    firstOrNull(predicate, mapper)
        ?: throw NoSuchElementException("No element satisfies the predicate")

/**
 * ResultSet의 행 수를 계산합니다.
 *
 * ```kotlin
 * val count = rs.count()
 * val activeCount = rs.count { row ->
 *     row.getString("status") == "active"
 * }
 * ```
 *
 * @param predicate 카운팅 조건 (기본값: 모든 행)
 * @return 조건을 만족하는 행 수
 */
inline fun ResultSet.count(crossinline predicate: (ResultSet) -> Boolean = { true }): Int {
    var count = 0
    while (this.next()) {
        if (predicate(this)) {
            count++
        }
    }
    return count
}

/**
 * ResultSet이 비어있는지 확인합니다.
 *
 * ```kotlin
 * if (rs.isEmpty()) {
 *     println("No results found")
 * }
 * ```
 *
 * @return ResultSet이 비어있으면 true, 그렇지 않으면 false
 */
fun ResultSet.isEmpty(): Boolean = !this.next()

/**
 * ResultSet이 비어있지 않은지 확인합니다.
 *
 * ```kotlin
 * if (rs.isNotEmpty()) {
 *     println("Found results")
 * }
 * ```
 *
 * @return ResultSet에 행이 있으면 true, 그렇지 않으면 false
 */
fun ResultSet.isNotEmpty(): Boolean = this.next().also { if (it) this.previous() }

/**
 * ResultSet의 커서를 이전 위치로 되돌립니다.
 * 커서가 첫 번째 행 이전에 있으면 false를 반환합니다.
 *
 * @return 이전 위치로 이동 성공 여부
 */
fun ResultSet.moveToPrevious(): Boolean =
    try {
        this.previous()
    } catch (e: SQLException) {
        false
    }
