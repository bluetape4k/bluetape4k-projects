package io.bluetape4k.hibernate

import io.bluetape4k.hibernate.model.JpaEntity
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requireNotEmpty
import jakarta.persistence.EntityManager
import jakarta.persistence.TypedQuery
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.internal.SessionImpl
import java.io.Serializable
import java.sql.Connection
import kotlin.reflect.KClass

/**
 * 현 [EntityManager] 가 사용하는 Hibernate [Session] 을 가져옵니다.
 *
 * ## 동작/계약
 * - 내부적으로 `unwrap(Session::class.java)`를 호출합니다.
 * - Hibernate 세션으로 unwrap할 수 없으면 JPA provider 예외가 전파됩니다.
 */
fun EntityManager.currentSession(): Session = unwrap(Session::class.java)

/**
 * 현 [EntityManager] 가 사용하는 Hibernate [Session] 을 가져옵니다.
 *
 * ## 동작/계약
 * - [currentSession]과 동일한 동작의 별칭입니다.
 */
fun EntityManager.asSession(): Session = unwrap(Session::class.java)

/**
 * 현 [EntityManager] 가 사용하는 Hibernate [SessionImpl] 을 가져옵니다.
 *
 * ## 동작/계약
 * - 구현체 타입으로 unwrap하므로 provider가 Hibernate가 아니면 실패할 수 있습니다.
 */
fun EntityManager.currentSessionImpl(): SessionImpl = unwrap(SessionImpl::class.java)

/**
 * 현 [EntityManager] 가 사용하는 Hibernate [SessionImpl] 을 가져옵니다.
 *
 * ## 동작/계약
 * - [currentSessionImpl]과 동일한 동작의 별칭입니다.
 */
fun EntityManager.asSessionImpl(): SessionImpl = unwrap(SessionImpl::class.java)

/**
 * 현 [EntityManager] 가 사용하는 Hibernate [SessionFactory] 를 가져옵니다.
 *
 * ## 동작/계약
 * - 현재 Session의 `sessionFactory`를 그대로 반환합니다.
 */
fun EntityManager.sessionFactory(): SessionFactory = currentSession().sessionFactory

/** 엔티티 전체 삭제를 위한 JPQL 템플릿입니다. `%s`에 엔티티명이 대입됩니다. */
const val QUERY_DELETE_ALL = "delete from %s x"

/** 엔티티 전체 개수 조회를 위한 JPQL 템플릿입니다. `%s`에 엔티티명이 대입됩니다. */
const val QUERY_COUNT = "select count(*) from %s x"

/** count 쿼리에서 사용하는 placeholder 문자열입니다. */
const val QUERY_COUNT_HOLDER = "*"

/**
 * JPQL 템플릿 [template]에 [entityName]을 대입하여 쿼리 문자열을 생성합니다.
 */
@PublishedApi
internal fun queryString(
    template: String,
    entityName: String,
): String = template.format(entityName)

/**
 * 새로운 [TypedQuery]`<T>` 를 생성합니다.
 *
 * ## 동작/계약
 * - `criteriaBuilder.createQuery(resultClass)`로 루트 엔티티를 선택하는 기본 조회 쿼리를 만듭니다.
 * - 조건/정렬은 포함하지 않으며 호출자가 반환된 [TypedQuery]에 추가 설정합니다.
 *
 * ```kotlin
 * val query = entityManager.newQuery(User::class.java)
 * // query.resultList 는 User 전체 조회
 * ```
 *
 * @param resultClass 결과 수형입니다.
 */
fun <T> EntityManager.newQuery(resultClass: Class<T>): TypedQuery<T> {
    val builder = criteriaBuilder
    val query = builder.createQuery(resultClass)
    val root = query.from(resultClass)
    query.select(root)

    return createQuery(query)
}

/**
 * 새로운 [TypedQuery]`<T>` 를 생성합니다.
 *
 * ## 동작/계약
 * - [newQuery]의 reified 오버로드입니다.
 */
inline fun <reified T> EntityManager.newQuery(): TypedQuery<T> = newQuery(T::class.java)

/**
 * JPQL 문자열과 결과 수형을 받아 [TypedQuery]를 생성합니다.
 */
fun <T : Any> EntityManager.createQueryAs(
    queryString: String,
    resultClass: KClass<T>,
): TypedQuery<T> = createQuery(queryString, resultClass.java)

/**
 * JPQL 문자열과 reified 타입으로 [TypedQuery]를 생성합니다.
 */
inline fun <reified T> EntityManager.createQueryAs(queryString: String): TypedQuery<T> =
    createQuery(queryString, T::class.java)

/**
 * [TypedQuery] 에 Paging 정보를 설정합니다.
 *
 * ## 동작/계약
 * - [firstResult], [maxResults]를 현재 쿼리에 설정하고 자기 자신을 반환합니다.
 * - 음수 인자 검증은 provider 구현에 위임됩니다.
 *
 * ```kotlin
 * val page = query.setPaging(firstResult = 0, maxResults = 20)
 * // page === query
 * ```
 */
fun <X> TypedQuery<X>.setPaging(
    firstResult: Int,
    maxResults: Int,
): TypedQuery<X> =
    apply {
        setFirstResult(firstResult)
        setMaxResults(maxResults)
    }

/**
 * [entity]가 Load 된 Persistence Object 인지 판단합니다.
 *
 * @param entity 대상 entity
 * @return proxy 가 아니라면 true
 */
fun EntityManager.isLoaded(entity: Any?): Boolean =
    entity?.run { entityManagerFactory.persistenceUnitUtil.isLoaded(this) } ?: false

/**
 * [entity]의 [property] 속성이 Proxy가 아닌, Load 된 Persistence Object 인지 판단합니다.
 *
 * @param entity 대상 entity
 * @param property entity manager 에 load 된 객체인지 검사할 속성
 * @return proxy 가 아니라면 true
 */
fun EntityManager.isLoaded(
    entity: Any?,
    property: String,
): Boolean = entity?.run { entityManagerFactory.persistenceUnitUtil.isLoaded(this, property) } ?: false

/**
 * [entity]를 상황에 따라 `merge` 하거나 `persist` 합니다.
 *
 * ## 동작/계약
 * - `isPersisted && !contains(entity)`이면 `merge`, 그 외에는 `persist`를 수행합니다.
 * - 반환값은 병합 결과 또는 입력 엔티티입니다.
 */
fun <T : JpaEntity<*>> EntityManager.save(entity: T): T =
    if (entity.isPersisted && !contains(entity)) {
        merge(entity)
    } else {
        persist(entity)
        entity
    }

/**
 * [entity]를 삭제합니다.
 *
 * ## 동작/계약
 * - 영속 상태가 아니면 `merge` 후 삭제합니다.
 * - `isPersisted == false`이면 아무 작업도 하지 않습니다.
 */
fun <T : JpaEntity<*>> EntityManager.delete(entity: T) {
    if (entity.isPersisted) {
        if (!contains(entity)) {
            remove(merge(entity))
        } else {
            remove(entity)
        }
    }
}

/**
 * id에 해당하는 엔티티를 삭제합니다.
 *
 * ## 동작/계약
 * - reference 조회 실패 시 예외를 삼키고 삭제를 건너뜁니다.
 */
inline fun <reified T> EntityManager.deleteById(id: Serializable) {
    tryGetReference<T>(id).getOrNull()?.let { remove(it) }
}

/**
 * reified 타입 [T]와 식별자 [id]로 엔티티 레퍼런스(프록시)를 가져옵니다.
 *
 * ## 동작/계약
 * - `getReference(T::class.java, id)`로 위임합니다.
 * - 프록시 초기화 시점에 엔티티가 존재하지 않으면 예외가 발생할 수 있습니다.
 */
inline fun <reified T> EntityManager.getReference(id: Serializable): T = getReference(T::class.java, id)

/**
 * reified 타입 [T]와 식별자 [id]로 엔티티 레퍼런스(프록시) 조회를 시도하고, 결과를 [Result]로 감싸 반환합니다.
 *
 * ## 동작/계약
 * - `getReference` 호출을 `runCatching`으로 감싸 예외 발생 시 `Result.failure`를 반환합니다.
 * - 성공 시 `Result.success(entity)`를 반환합니다.
 */
inline fun <reified T> EntityManager.tryGetReference(id: Serializable): Result<T> =
    runCatching { getReference(T::class.java, id) }

/**
 * id에 해당하는 엔티티를 조회합니다. 없으면 null을 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 JPA `find`를 호출합니다.
 */
inline fun <reified T> EntityManager.findAs(id: Serializable): T? = find(T::class.java, id)

/**
 * simple natural id 값으로 엔티티를 조회합니다. 없으면 `null`을 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 현재 Hibernate [Session]을 구해 [Session.findBySimpleNaturalId]에 위임합니다.
 */
inline fun <reified T : Any> EntityManager.findBySimpleNaturalId(naturalId: Any): T? =
    currentSession().findBySimpleNaturalId<T>(naturalId)

/**
 * 복합 natural id 속성으로 엔티티를 조회합니다. 없으면 `null`을 반환합니다.
 *
 * ## 동작/계약
 * - [naturalIdValues]는 비어 있을 수 없고, 각 속성명은 blank일 수 없습니다.
 * - 내부적으로 현재 Hibernate [Session]을 구해 [Session.findByNaturalId]에 위임합니다.
 */
inline fun <reified T : Any> EntityManager.findByNaturalId(
    naturalIdValues: Map<String, Any?>,
): T? {
    naturalIdValues.requireNotEmpty("naturalIdValues")
    naturalIdValues.keys.forEach { it.requireNotBlank("naturalIdValues.key") }
    return currentSession().findByNaturalId<T>(naturalIdValues)
}

/**
 * 복합 natural id 속성으로 엔티티를 조회합니다. 없으면 `null`을 반환합니다.
 *
 * ## 동작/계약
 * - [findByNaturalId]의 pair 오버로드입니다.
 */
inline fun <reified T : Any> EntityManager.findByNaturalId(
    vararg naturalIdValues: Pair<String, Any?>,
): T? = findByNaturalId(naturalIdValues.toMap())

/**
 * id에 해당하는 엔티티를 조회합니다. 없으면 null을 반환합니다.
 *
 * ## 동작/계약
 * - [findAs]와 동일한 동작의 별칭입니다.
 */
inline fun <reified T> EntityManager.findOne(id: Serializable): T? = find(T::class.java, id)

/**
 * id에 해당하는 엔티티가 존재하는지 확인합니다.
 *
 * ## 동작/계약
 * - [findOne] 결과가 `null`인지로 존재 여부를 판단합니다.
 */
inline fun <reified T> EntityManager.exists(id: Serializable): Boolean = findOne<T>(id) != null

/**
 * [clazz] 수형의 모든 엔티티를 조회합니다.
 *
 * ## 동작/계약
 * - 조건 없는 criteria 기반 전체 조회를 수행합니다.
 */
fun <T> EntityManager.findAll(clazz: Class<T>): List<T> = newQuery(clazz).resultList

/**
 * [T] 수형의 엔티티 전체 개수를 반환합니다.
 *
 * ## 동작/계약
 * - 엔티티명을 찾아 `"select count(*) from ..."` JPQL을 실행합니다.
 * - 결과는 Number로 받아 `Long`으로 변환합니다.
 */
inline fun <reified T> EntityManager.countAll(): Long {
    val entityName = sessionFactory().getEntityName<T>() ?: T::class.java.simpleName
    val query = queryString(QUERY_COUNT, entityName)
    return (createQuery(query).singleResult as Number).toLong()
}

/**
 * [T] 수형의 엔티티를 모두 삭제하고, 삭제한 행 수를 반환합니다.
 *
 * ## 동작/계약
 * - `"delete from ..."` JPQL bulk update를 실행합니다.
 */
inline fun <reified T> EntityManager.deleteAll(): Int {
    val entityName = sessionFactory().getEntityName<T>() ?: T::class.java.simpleName
    val query = queryString(QUERY_DELETE_ALL, entityName)
    return createQuery(query).executeUpdate()
}

/**
 * 현 [EntityManager] 가 사용하는 [Connection] 을 가져옵니다.
 *
 * ## 동작/계약
 * - Hibernate 내부 API(`SessionImpl -> jdbcCoordinator`)를 통해 물리 커넥션을 조회합니다.
 */
fun EntityManager.currentConnection(): Connection =
    currentSessionImpl()
        .jdbcCoordinator
        .logicalConnection
        .physicalConnection
