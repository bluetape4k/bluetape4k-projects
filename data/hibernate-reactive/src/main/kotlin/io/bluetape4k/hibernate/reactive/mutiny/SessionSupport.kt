package io.bluetape4k.hibernate.reactive.mutiny

import io.smallrye.mutiny.Uni
import jakarta.persistence.EntityGraph
import org.hibernate.LockMode
import org.hibernate.reactive.common.AffectedEntities
import org.hibernate.reactive.common.Identifier
import org.hibernate.reactive.common.ResultSetMapping
import org.hibernate.reactive.mutiny.Mutiny

inline fun <reified T> Mutiny.Session.findAs(id: java.io.Serializable): Uni<T> =
    find(T::class.java, id)

inline fun <reified T> Mutiny.Session.findAs(id: java.io.Serializable, lockMode: LockMode): Uni<T> =
    find(T::class.java, id, lockMode)

inline fun <reified T> Mutiny.Session.findAs(vararg ids: java.io.Serializable): Uni<List<T>> =
    find(T::class.java, *ids)

inline fun <reified T> Mutiny.Session.findAs(naturalId: Identifier<T>): Uni<T> =
    find(T::class.java, naturalId)

inline fun <reified T> Mutiny.Session.getReferenceAs(id: java.io.Serializable): T =
    getReference(T::class.java, id)

inline fun <reified R> Mutiny.Session.createQueryAs(queryString: String): Mutiny.SelectionQuery<R> =
    createQuery(queryString, R::class.java)

inline fun <reified R> Mutiny.Session.createNamedQueryAs(queryName: String): Mutiny.SelectionQuery<R> =
    createNamedQuery(queryName, R::class.java)

inline fun <reified R> Mutiny.Session.createNativeQueryAs(queryString: String): Mutiny.SelectionQuery<R> =
    createNativeQuery(queryString, R::class.java)

inline fun <reified R> Mutiny.Session.createNativeQueryAs(
    queryString: String,
    affectedEntities: AffectedEntities,
): Mutiny.SelectionQuery<R> =
    createNativeQuery(queryString, R::class.java, affectedEntities)

inline fun <reified T> Mutiny.Session.getResultSetMappingAs(mappingName: String): ResultSetMapping<T> =
    getResultSetMapping(T::class.java, mappingName)

inline fun <reified T> Mutiny.Session.getEntityGraphAs(graphName: String): EntityGraph<T> =
    getEntityGraph(T::class.java, graphName)

inline fun <reified T> Mutiny.Session.createEntityGraphAs(): EntityGraph<T> =
    createEntityGraph(T::class.java)

inline fun <reified T> Mutiny.Session.createEntityGraphAs(graphName: String): EntityGraph<T> =
    createEntityGraph(T::class.java, graphName)
