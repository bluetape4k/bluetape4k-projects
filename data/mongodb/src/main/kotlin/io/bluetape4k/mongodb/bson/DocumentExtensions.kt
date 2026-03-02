package io.bluetape4k.mongodb.bson

import org.bson.Document

/**
 * 키-값 쌍으로 [Document]를 생성합니다.
 *
 * ```kotlin
 * val doc = documentOf("name" to "Alice", "age" to 30, "city" to "Seoul")
 * ```
 *
 * @param pairs 키-값 쌍 목록
 * @return 생성된 [Document]
 */
fun documentOf(vararg pairs: Pair<String, Any?>): Document {
    return Document().apply {
        pairs.forEach { (key, value) -> put(key, value) }
    }
}

/**
 * DSL 빌더로 [Document]를 생성합니다.
 *
 * ```kotlin
 * val doc = documentOf {
 *     put("name", "Alice")
 *     put("age", 30)
 *     put("tags", listOf("admin", "user"))
 * }
 * ```
 *
 * @param builder [Document] 초기화 람다
 * @return 생성된 [Document]
 */
fun documentOf(builder: Document.() -> Unit): Document =
    Document().apply(builder)

/**
 * 키에 해당하는 값을 지정한 타입 [T]로 안전하게 캐스팅하여 반환합니다.
 *
 * 값이 없거나 타입이 일치하지 않으면 `null`을 반환합니다.
 *
 * ```kotlin
 * val name = doc.getAs<String>("name")
 * val age = doc.getAs<Int>("age")
 * ```
 *
 * @param T 반환할 타입
 * @param key 조회할 키
 * @return 해당 키의 값을 [T]로 캐스팅한 결과 또는 `null`
 */
inline fun <reified T> Document.getAs(key: String): T? =
    get(key) as? T
