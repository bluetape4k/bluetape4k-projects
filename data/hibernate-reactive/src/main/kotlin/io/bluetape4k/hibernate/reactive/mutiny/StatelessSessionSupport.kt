package io.bluetape4k.hibernate.reactive.mutiny

import io.smallrye.mutiny.Uni
import jakarta.persistence.EntityGraph
import org.hibernate.LockMode
import org.hibernate.reactive.common.ResultSetMapping
import org.hibernate.reactive.mutiny.Mutiny

inline fun <reified T> Mutiny.StatelessSession.getAs(id: java.io.Serializable): Uni<T> =
    get(T::class.java, id)

inline fun <reified T> Mutiny.StatelessSession.getAs(id: java.io.Serializable, lockMode: LockMode): Uni<T> =
    get(T::class.java, id, lockMode)

inline fun <reified R> Mutiny.StatelessSession.createQueryAs(queryString: String): Mutiny.SelectionQuery<R> =
    createQuery(queryString, R::class.java)

inline fun <reified R> Mutiny.StatelessSession.createNamedQueryAs(queryName: String): Mutiny.SelectionQuery<R> =
    createNamedQuery(queryName, R::class.java)

inline fun <reified R> Mutiny.StatelessSession.createNativeQueryAs(queryString: String): Mutiny.SelectionQuery<R> =
    createNativeQuery(queryString, R::class.java)

inline fun <reified T> Mutiny.StatelessSession.getResultSetMappingAs(mappingName: String): ResultSetMapping<T> =
    getResultSetMapping(T::class.java, mappingName)

inline fun <reified T> Mutiny.StatelessSession.getEntityGraphAs(graphName: String): EntityGraph<T> =
    getEntityGraph(T::class.java, graphName)

inline fun <reified T> Mutiny.StatelessSession.createEntityGraphAs(): EntityGraph<T> =
    createEntityGraph(T::class.java)

inline fun <reified T> Mutiny.StatelessSession.createEntityGraphAs(graphName: String): EntityGraph<T> =
    createEntityGraph(T::class.java, graphName)
