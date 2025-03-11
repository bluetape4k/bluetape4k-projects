package io.bluetape4k.javers.repository.jql

import org.javers.repository.jql.JqlQuery
import org.javers.repository.jql.QueryBuilder
import kotlin.reflect.KClass

inline fun queryAnyDomainObject(
    initializer: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.anyDomainObject().apply(initializer).build()
}

inline fun <reified T: Any> query(
    initializer: QueryBuilder.() -> Unit,
): JqlQuery {
    return QueryBuilder.byClass(T::class.java).apply(initializer).build()
}

inline fun <reified T: Any> queryByInstance(
    instance: T,
    initializer: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byInstance(instance).apply(initializer).build()
}

inline fun <reified T: Any> queryByInstanceId(
    localId: Any,
    initializer: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byInstanceId(localId, T::class.java).apply(initializer).build()
}

inline fun <reified T: Any> queryByValueObject(
    path: String,
    initializer: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byValueObject(T::class.java, path).apply(initializer).build()
}

inline fun <reified T: Any> queryByValueObjectId(
    ownerLocalId: Any,
    path: String,
    initializer: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byValueObjectId(ownerLocalId, T::class.java, path).apply(initializer).build()
}

inline fun <reified T: Any> queryByClass(
    initializer: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byClass(T::class.java).apply(initializer).build()
}

@JvmName("queryByClassesCollection")
inline fun queryByClasses(
    classes: Collection<Class<*>>,
    initializer: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byClass(*classes.toTypedArray()).apply(initializer).build()
}

@JvmName("queryByClassesArray")
inline fun queryByClasses(
    vararg classes: Class<*>,
    initializer: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byClass(*classes).apply(initializer).build()
}


inline fun queryByClasses(
    kclasses: Collection<KClass<*>>,
    initializer: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byClass(*kclasses.map { it.java }.toTypedArray()).apply(initializer).build()
}

inline fun queryByClasses(
    vararg kclasses: KClass<*>,
    initializer: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byClass(*kclasses.map { it.java }.toTypedArray()).apply(initializer).build()
}
