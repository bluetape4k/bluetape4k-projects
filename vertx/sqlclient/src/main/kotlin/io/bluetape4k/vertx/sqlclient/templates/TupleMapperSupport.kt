package io.bluetape4k.vertx.sqlclient.templates

import io.vertx.sqlclient.templates.TupleMapper
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

private val recordPropertyCache = ConcurrentHashMap<KClass<*>, List<KProperty1<Any, *>>>()

@Suppress("UNCHECKED_CAST")
private fun propertiesOf(recordType: KClass<*>): List<KProperty1<Any, *>> =
    recordPropertyCache.computeIfAbsent(recordType) {
        it.memberProperties.map { property -> property as KProperty1<Any, *> }
    }

private fun recordToParameterMap(record: Any): Map<String, Any?> {
    val properties = propertiesOf(record::class)
    return properties.associate { property ->
        property.name to runCatching { property.get(record) }.getOrNull()
    }
}

/**
 * Vert.x SQL Client Templates 사용 시, 레코드의 프로퍼티를 SQL 파라미터 맵으로 매핑하는 [TupleMapper]를 생성합니다.
 *
 * 성능 최적화를 위해 레코드 타입별 리플렉션 결과([kotlin.reflect.KProperty])를 캐시합니다.
 *
 * ```
 * val result = SqlTemplate
 *     .forQuery(pool, "INSERT INTO users VALUES (#{id}, #{firstName}, #{lastName})")
 *     .mapFrom(tupleMapperOfRecord())
 *     .execute(user)
 *     .await()
 * ```
 *
 * @param T 레코드 타입
 * @return SQL Template `mapFrom`에 전달할 [TupleMapper]
 */
fun <T: Any> tupleMapperOfRecord(): TupleMapper<T> =
    TupleMapper.mapper { record: T -> recordToParameterMap(record) }

/**
 * 레코드 목록을 SQL Template `execute`에 전달 가능한 파라미터 맵으로 변환합니다.
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
 * 각 레코드의 프로퍼티 이름 뒤에 레코드 인덱스를 붙여 키를 만듭니다.
 * 예: `id0`, `name0`, `id1`, `name1`.
 *
 * @param T 레코드 타입
 * @return SQL Template 실행 파라미터 맵
 */
fun <T: Any> List<T>.toParameters(): Map<String, Any?> {
    return this.flatMapIndexed { index, record ->
        val properties = propertiesOf(record::class)
        properties.map { property ->
            "${property.name}$index" to runCatching { property.get(record) }.getOrNull()
        }
    }.toMap()
}
