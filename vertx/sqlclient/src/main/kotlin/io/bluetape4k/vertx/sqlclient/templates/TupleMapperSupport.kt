package io.bluetape4k.vertx.sqlclient.templates

import io.vertx.sqlclient.templates.TupleMapper
import kotlin.reflect.full.memberProperties


/**
 * Vertx SQL Client Templates 사용 시, Record의 값과 SQL Template의 Parameter를 매핑시키는 [TupleMapper]를 생성합니다.
 *
 * `record`의 모든 속성 명: 속성 값의 [TupleMapper] 를 생성합니다.
 *
 * ```
 * val result = SqlTemplate
 *     .forQuery(pool, "INSERT INTO users VALUES (#{id}, #{firstName}, #{lastName})")
 *     .mapFrom(tupleMapperOf())
 *     .execute(user)
 *     .await()
 * ```
 *
 * @param T
 * @return
 */
fun <T: Any> tupleMapperOfRecord(): TupleMapper<T> = TupleMapper.mapper { record: T ->
    record.javaClass.kotlin.memberProperties.associate { property ->
        property.name to runCatching { property.get(record) }.getOrNull()
    }
}

/**
 * Record 를 SQL Client Template 를 통해 실행 시 Parameter 로 변환해줍니다.
 *
 * ```
 * val record1 = PersonRecord(100, "Joe", "Jones", Date(), true, "Developer", 1)
 * val record2 = PersonRecord(101, "Sarah", "Smith", Date(), true, "Architect", 2)
 *
 * val insertProvider = insertMultiple(listOf(record1, record2)) {
 *      into(person)
 *      map(person.id) toProperty PersonRecord::id.name
 *      map(person.firstName) toProperty PersonRecord::firstName.name
 *      map(person.lastName) toProperty PersonRecord::lastName.name
 *      map(person.birthDate) toProperty PersonRecord::birthDate.name
 *      map(person.employed) toProperty PersonRecord::employed.name
 *      map(person.occupation) toProperty PersonRecord::occupation.name
 *      map(person.addressId) toProperty PersonRecord::addressId.name
 * }.render(VERTX_SQL_CLIENT_RENDERING_STRATEGY)
 *
 * // val rowCount = sqlClient.insertMultiple(insertProvider)
 * val result = SqlTemplate.forUpdate(this, insertProvider.insertStatement)
 *      .execute(insertProvider.records.toParameters())
 *      .await()
 * ```
 *
 * @param T
 * @return parameter name to value map
 */
fun <T: Any> List<T>.toParameters(): Map<String, Any?> {
    return this.flatMapIndexed { index, record ->
        val properties = record.javaClass.kotlin.memberProperties
        properties.map { property ->
            "${property.name}$index" to runCatching { property.get(record) }.getOrNull()
        }
    }.toMap()
}
