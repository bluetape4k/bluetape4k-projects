package io.bluetape4k.hibernate.reactive.stage

import jakarta.persistence.EntityGraph
import org.hibernate.LockMode
import org.hibernate.reactive.common.AffectedEntities
import org.hibernate.reactive.common.Identifier
import org.hibernate.reactive.common.ResultSetMapping
import org.hibernate.reactive.stage.Stage
import java.util.concurrent.CompletionStage

inline fun <reified T> Stage.Session.findAs(id: java.io.Serializable): CompletionStage<T> =
    find(T::class.java, id)

inline fun <reified T> Stage.Session.findAs(id: java.io.Serializable, lockMode: LockMode): CompletionStage<T> =
    find(T::class.java, id, lockMode)

inline fun <reified T> Stage.Session.findAs(vararg ids: java.io.Serializable): CompletionStage<List<T>> =
    find(T::class.java, *ids)

inline fun <reified T> Stage.Session.findAs(naturalId: Identifier<T>): CompletionStage<T> =
    find(T::class.java, naturalId)

inline fun <reified T> Stage.Session.getReferenceAs(id: java.io.Serializable): T =
    getReference(T::class.java, id)

inline fun <reified R> Stage.Session.createQueryAs(queryString: String): Stage.SelectionQuery<R> =
    createQuery(queryString, R::class.java)

inline fun <reified R> Stage.Session.createNamedQueryAs(queryName: String): Stage.SelectionQuery<R> =
    createNamedQuery(queryName, R::class.java)

inline fun <reified R> Stage.Session.createNativeQueryAs(queryString: String): Stage.SelectionQuery<R> =
    createNativeQuery(queryString, R::class.java)

inline fun <reified R> Stage.Session.createNativeQueryAs(
    queryString: String,
    affectedEntities: AffectedEntities,
): Stage.SelectionQuery<R> =
    createNativeQuery(queryString, R::class.java, affectedEntities)

inline fun <reified T> Stage.Session.getResultSetMappingAs(mappingName: String): ResultSetMapping<T> =
    getResultSetMapping(T::class.java, mappingName)

inline fun <reified T> Stage.Session.getEntityGraphAs(graphName: String): EntityGraph<T> =
    getEntityGraph(T::class.java, graphName)

inline fun <reified T> Stage.Session.createEntityGraphAs(): EntityGraph<T> =
    createEntityGraph(T::class.java)

inline fun <reified T> Stage.Session.createEntityGraphAs(graphName: String): EntityGraph<T> =
    createEntityGraph(T::class.java, graphName)
