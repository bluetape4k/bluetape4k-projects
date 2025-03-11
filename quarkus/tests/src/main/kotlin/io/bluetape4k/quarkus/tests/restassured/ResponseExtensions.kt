package io.bluetape4k.quarkus.tests.restassured

import io.restassured.common.mapper.TypeRef
import io.restassured.response.ExtractableResponse
import io.restassured.response.ResponseOptions

inline fun <reified T> typeRef(): TypeRef<T> = object: TypeRef<T>() {}

/**
 * Rest Assured의 response body를 지정한 수형으로 변환합니다.
 *
 * ```
 * Then {
 *      val entity:Entity = extract().bodyAs<Entity>()
 * }
 * ```
 *
 * @param T 변환할 수형
 * @return 지정한 수형의 인스턴스
 */
inline fun <reified T> ExtractableResponse<out ResponseOptions<*>>.bodyAs(): T =
    this.body().`as`(typeRef<T>())


/**
 * Rest Assured의 response body를 지정한 Class 의 List 수형으로  변환합니다.
 *
 * ```
 * Then {
 *      val entities: List<Entity> = extract().bodyAsList<Entity>()
 * }
 * ```
 *
 * @param T 변환할 수형
 * @return 지정한 수형의 List
 */
inline fun <reified T> ExtractableResponse<out ResponseOptions<*>>.bodyAsList(): List<T> =
    this.body().`as`(typeRef<List<T>>())
