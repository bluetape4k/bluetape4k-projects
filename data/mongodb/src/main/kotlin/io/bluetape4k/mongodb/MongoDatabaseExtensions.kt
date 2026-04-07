package io.bluetape4k.mongodb

import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.toList

/**
 * reified 타입 파라미터로 [MongoCollection]을 가져옵니다.
 *
 * ```kotlin
 * val collection = database.getCollectionOf<Person>("persons")
 * ```
 *
 * @param T 컬렉션 문서 타입
 * @param name 컬렉션 이름
 * @return 지정한 타입의 [MongoCollection]
 */
inline fun <reified T: Any> MongoDatabase.getCollectionOf(
    name: String,
): MongoCollection<T> = getCollection(name, T::class.java)

/**
 * 데이터베이스에 존재하는 컬렉션 이름 목록을 [List]로 반환합니다.
 *
 * 네이티브 `listCollectionNames()`는 `Flow<String>`을 반환하므로,
 * 이 함수는 즉시 [List]로 수집하는 편의 함수입니다.
 *
 * ```kotlin
 * val names = database.listCollectionNamesList()
 * println(names) // ["users", "orders", ...]
 * ```
 *
 * @return 컬렉션 이름 목록
 */
suspend fun MongoDatabase.listCollectionNamesList(): List<String> =
    listCollectionNames().toList()
