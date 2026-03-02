package io.bluetape4k.javers.repository.jql

import org.javers.repository.jql.JqlQuery
import org.javers.repository.jql.QueryBuilder
import kotlin.reflect.KClass

/**
 * 모든 도메인 객체를 대상으로 [JqlQuery]를 생성한다.
 *
 * ```kotlin
 * val query = queryAnyDomainObject { limit(10) }
 * val snapshots = javers.findSnapshots(query)
 * ```
 */
inline fun queryAnyDomainObject(
    @BuilderInference builder: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.anyDomainObject().apply(builder).build()
}

/**
 * 지정한 타입의 엔티티를 대상으로 [JqlQuery]를 생성한다.
 *
 * ```kotlin
 * val query = query<Person> { limit(5) }
 * val changes = javers.findChanges(query)
 * ```
 */
inline fun <reified T: Any> query(
    @BuilderInference builder: QueryBuilder.() -> Unit,
): JqlQuery {
    return QueryBuilder.byClass(T::class.java).apply(builder).build()
}

/**
 * 특정 엔티티 인스턴스를 대상으로 [JqlQuery]를 생성한다.
 *
 * ```kotlin
 * val query = queryByInstance(person) { limit(10) }
 * val snapshots = javers.findSnapshots(query)
 * ```
 */
inline fun <reified T: Any> queryByInstance(
    instance: T,
    @BuilderInference builder: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byInstance(instance).apply(builder).build()
}

/**
 * 엔티티의 로컬 ID로 [JqlQuery]를 생성한다.
 *
 * ```kotlin
 * val query = queryByInstanceId<Person>("bob")
 * val shadows = javers.findShadows<Person>(query)
 * // shadows.size >= 1
 * ```
 */
inline fun <reified T: Any> queryByInstanceId(
    localId: Any,
    @BuilderInference builder: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byInstanceId(localId, T::class.java).apply(builder).build()
}

/**
 * 지정한 타입의 ValueObject를 [path]로 조회하는 [JqlQuery]를 생성한다.
 */
inline fun <reified T: Any> queryByValueObject(
    path: String,
    @BuilderInference builder: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byValueObject(T::class.java, path).apply(builder).build()
}

/**
 * 소유자의 로컬 ID와 [path]로 ValueObject를 조회하는 [JqlQuery]를 생성한다.
 */
inline fun <reified T: Any> queryByValueObjectId(
    ownerLocalId: Any,
    path: String,
    @BuilderInference builder: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byValueObjectId(ownerLocalId, T::class.java, path).apply(builder).build()
}

/**
 * 지정한 클래스를 대상으로 [JqlQuery]를 생성한다.
 *
 * ```kotlin
 * val query = queryByClass<Person> { withNewObjectChanges() }
 * val changes = javers.findChanges(query)
 * ```
 */
inline fun <reified T: Any> queryByClass(
    @BuilderInference builder: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byClass(T::class.java).apply(builder).build()
}

/**
 * 여러 Java 클래스([Collection])를 대상으로 [JqlQuery]를 생성한다.
 */
@JvmName("queryByClassesCollection")
inline fun queryByClasses(
    classes: Collection<Class<*>>,
    @BuilderInference builder: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byClass(*classes.toTypedArray()).apply(builder).build()
}

/**
 * 여러 Java 클래스(vararg)를 대상으로 [JqlQuery]를 생성한다.
 */
@JvmName("queryByClassesArray")
inline fun queryByClasses(
    vararg classes: Class<*>,
    @BuilderInference builder: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byClass(*classes).apply(builder).build()
}

/**
 * 여러 Kotlin 클래스([Collection])를 대상으로 [JqlQuery]를 생성한다.
 */
inline fun queryByClasses(
    kclasses: Collection<KClass<*>>,
    @BuilderInference builder: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byClass(*kclasses.map { it.java }.toTypedArray()).apply(builder).build()
}

/**
 * 여러 Kotlin 클래스(vararg)를 대상으로 [JqlQuery]를 생성한다.
 */
inline fun queryByClasses(
    vararg kclasses: KClass<*>,
    @BuilderInference builder: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byClass(*kclasses.map { it.java }.toTypedArray()).apply(builder).build()
}
