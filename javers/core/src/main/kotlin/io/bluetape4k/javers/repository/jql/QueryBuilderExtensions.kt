package io.bluetape4k.javers.repository.jql

import org.javers.repository.jql.JqlQuery
import org.javers.repository.jql.QueryBuilder
import kotlin.reflect.KClass

inline fun queryAnyDomainObject(
    @BuilderInference builder: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.anyDomainObject().apply(builder).build()
}

inline fun <reified T: Any> query(
    @BuilderInference builder: QueryBuilder.() -> Unit,
): JqlQuery {
    return QueryBuilder.byClass(T::class.java).apply(builder).build()
}

inline fun <reified T: Any> queryByInstance(
    instance: T,
    @BuilderInference builder: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byInstance(instance).apply(builder).build()
}

inline fun <reified T: Any> queryByInstanceId(
    localId: Any,
    @BuilderInference builder: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byInstanceId(localId, T::class.java).apply(builder).build()
}

inline fun <reified T: Any> queryByValueObject(
    path: String,
    @BuilderInference builder: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byValueObject(T::class.java, path).apply(builder).build()
}

inline fun <reified T: Any> queryByValueObjectId(
    ownerLocalId: Any,
    path: String,
    @BuilderInference builder: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byValueObjectId(ownerLocalId, T::class.java, path).apply(builder).build()
}

inline fun <reified T: Any> queryByClass(
    @BuilderInference builder: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byClass(T::class.java).apply(builder).build()
}

@JvmName("queryByClassesCollection")
inline fun queryByClasses(
    classes: Collection<Class<*>>,
    @BuilderInference builder: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byClass(*classes.toTypedArray()).apply(builder).build()
}

@JvmName("queryByClassesArray")
inline fun queryByClasses(
    vararg classes: Class<*>,
    @BuilderInference builder: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byClass(*classes).apply(builder).build()
}


inline fun queryByClasses(
    kclasses: Collection<KClass<*>>,
    @BuilderInference builder: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byClass(*kclasses.map { it.java }.toTypedArray()).apply(builder).build()
}

inline fun queryByClasses(
    vararg kclasses: KClass<*>,
    @BuilderInference builder: QueryBuilder.() -> Unit = {},
): JqlQuery {
    return QueryBuilder.byClass(*kclasses.map { it.java }.toTypedArray()).apply(builder).build()
}
