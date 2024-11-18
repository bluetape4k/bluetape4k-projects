package io.bluetape4k.hibernate.reactive.stage

import jakarta.persistence.EntityGraph
import org.hibernate.LockMode
import org.hibernate.reactive.common.ResultSetMapping
import org.hibernate.reactive.stage.Stage
import java.util.concurrent.CompletionStage

inline fun <reified T> Stage.StatelessSession.getAs(id: java.io.Serializable): CompletionStage<T> =
    get(T::class.java, id)

inline fun <reified T> Stage.StatelessSession.getAs(id: java.io.Serializable, lockMode: LockMode): CompletionStage<T> =
    get(T::class.java, id, lockMode)

inline fun <reified R> Stage.StatelessSession.createQueryAs(queryString: String): Stage.SelectionQuery<R> =
    createQuery(queryString, R::class.java)

inline fun <reified R> Stage.StatelessSession.createNamedQueryAs(queryName: String): Stage.SelectionQuery<R> =
    createNamedQuery(queryName, R::class.java)

inline fun <reified R> Stage.StatelessSession.createNativeQueryAs(queryString: String): Stage.SelectionQuery<R> =
    createNativeQuery(queryString, R::class.java)

inline fun <reified T> Stage.StatelessSession.getResultSetMappingAs(mappingName: String): ResultSetMapping<T> =
    getResultSetMapping(T::class.java, mappingName)

inline fun <reified T> Stage.StatelessSession.getEntityGraphAs(graphName: String): EntityGraph<T> =
    getEntityGraph(T::class.java, graphName)

inline fun <reified T> Stage.StatelessSession.createEntityGraphAs(): EntityGraph<T> =
    createEntityGraph(T::class.java)

inline fun <reified T> Stage.StatelessSession.createEntityGraphAs(graphName: String): EntityGraph<T> =
    createEntityGraph(T::class.java, graphName)
