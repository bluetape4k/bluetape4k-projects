package io.bluetape4k.hibernate

import io.bluetape4k.hibernate.model.JpaEntity
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
 */
fun EntityManager.currentSession(): Session = unwrap(Session::class.java)

/**
 * 현 [EntityManager] 가 사용하는 Hibernate [Session] 을 가져옵니다.
 */
fun EntityManager.asSession(): Session = unwrap(Session::class.java)

/**
 * 현 [EntityManager] 가 사용하는 Hibernate [SessionImpl] 을 가져옵니다.
 */
fun EntityManager.currentSessionImpl(): SessionImpl = unwrap(SessionImpl::class.java)

/**
 * 현 [EntityManager] 가 사용하는 Hibernate [SessionImpl] 을 가져옵니다.
 */
fun EntityManager.asSessionImpl(): SessionImpl = unwrap(SessionImpl::class.java)

/**
 * 현 [EntityManager] 가 사용하는 Hibernate [SessionFactory] 를 가져옵니다.
 */
fun EntityManager.sessionFactory(): SessionFactory = currentSession().sessionFactory


const val QUERY_DELETE_ALL = "delete from %s x"
const val QUERY_COUNT = "select count(*) from %s x"
const val QUERY_COUNT_HOLDER = "*"


@PublishedApi
internal fun queryString(template: String, entityName: String): String = template.format(entityName)

/**
 * 새로운 [TypedQuery]`<T>` 를 생성합니다.
 *
 * @param resultClass 결과 수형
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
 */
inline fun <reified T> EntityManager.newQuery(): TypedQuery<T> =
    newQuery(T::class.java)

/**
 * JPQL 문자열과 결과 수형을 받아 [TypedQuery]를 생성합니다.
 */
fun <T: Any> EntityManager.createQueryAs(queryString: String, resultClass: KClass<T>): TypedQuery<T> =
    createQuery(queryString, resultClass.java)

/**
 * JPQL 문자열과 reified 타입으로 [TypedQuery]를 생성합니다.
 */
inline fun <reified T> EntityManager.createQueryAs(queryString: String): TypedQuery<T> =
    createQuery(queryString, T::class.java)


/**
 * [TypedQuery] 에 Paging 정보를 설정합니다.
 *
 * @param firstResult 시작 위치
 * @param maxResults 최대 결과 수
 */
fun <X> TypedQuery<X>.setPaging(firstResult: Int, maxResults: Int): TypedQuery<X> = apply {
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
fun EntityManager.isLoaded(entity: Any?, property: String): Boolean =
    entity?.run { entityManagerFactory.persistenceUnitUtil.isLoaded(this, property) } ?: false


/**
 * [entity]를 상황에 따라 `merge` 하거나 `persist` 합니다.
 */
fun <T: JpaEntity<*>> EntityManager.save(entity: T): T {
    return if (entity.isPersisted && !contains(entity)) {
        merge(entity)
    } else {
        persist(entity)
        entity
    }
}

/**
 * [entity]를 삭제합니다.
 */
fun <T: JpaEntity<*>> EntityManager.delete(entity: T) {
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
 */
inline fun <reified T> EntityManager.deleteById(id: Serializable) {
    tryGetReference<T>(id).getOrNull()?.let { remove(it) }
}

inline fun <reified T> EntityManager.getReference(id: Serializable): T =
    getReference(T::class.java, id)

inline fun <reified T> EntityManager.tryGetReference(id: Serializable): Result<T> =
    runCatching { getReference(T::class.java, id) }

/**
 * id에 해당하는 엔티티를 조회합니다. 없으면 null을 반환합니다.
 */
inline fun <reified T> EntityManager.findAs(id: Serializable): T? = find(T::class.java, id)

/**
 * id에 해당하는 엔티티를 조회합니다. 없으면 null을 반환합니다.
 */
inline fun <reified T> EntityManager.findOne(id: Serializable): T? = find(T::class.java, id)

/**
 * id에 해당하는 엔티티가 존재하는지 확인합니다.
 */
inline fun <reified T> EntityManager.exists(id: Serializable): Boolean =
    findOne<T>(id) != null

/**
 * [clazz] 수형의 모든 엔티티를 조회합니다.
 */
fun <T> EntityManager.findAll(clazz: Class<T>): List<T> {
    return newQuery(clazz).resultList
}

/**
 * [T] 수형의 엔티티 전체 개수를 반환합니다.
 */
inline fun <reified T> EntityManager.countAll(): Long {
    val entityName = sessionFactory().getEntityName<T>() ?: T::class.java.simpleName
    val query = queryString(QUERY_COUNT, entityName)
    return (createQuery(query).singleResult as Number).toLong()
}

/**
 * [T] 수형의 엔티티를 모두 삭제하고, 삭제한 행 수를 반환합니다.
 */
inline fun <reified T> EntityManager.deleteAll(): Int {
    val entityName = sessionFactory().getEntityName<T>() ?: T::class.java.simpleName
    val query = queryString(QUERY_DELETE_ALL, entityName)
    return createQuery(query).executeUpdate()
}

/**
 * 현 [EntityManager] 가 사용하는 [Connection] 을 가져옵니다.
 */
fun EntityManager.currentConnection(): Connection {
    return currentSessionImpl()
        .jdbcCoordinator
        .logicalConnection
        .physicalConnection
}
