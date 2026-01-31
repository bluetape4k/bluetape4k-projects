package io.bluetape4k.spring.cassandra

import io.bluetape4k.coroutines.flow.extensions.toFastList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactor.mono
import org.springframework.data.cassandra.core.ReactiveCassandraBatchOperations
import org.springframework.data.cassandra.core.cql.WriteOptions

fun ReactiveCassandraBatchOperations.insertFlow(entities: Flow<*>): ReactiveCassandraBatchOperations =
    insert(mono { entities.toFastList() })

fun ReactiveCassandraBatchOperations.insertFlow(
    entities: Flow<*>,
    options: WriteOptions,
): ReactiveCassandraBatchOperations =
    insert(mono { entities.toFastList() }, options)

fun ReactiveCassandraBatchOperations.updateFlow(entities: Flow<*>): ReactiveCassandraBatchOperations =
    update(mono { entities.toFastList() })

fun ReactiveCassandraBatchOperations.updateFlow(
    entities: Flow<*>,
    options: WriteOptions,
): ReactiveCassandraBatchOperations =
    update(mono { entities.toFastList() }, options)

fun ReactiveCassandraBatchOperations.deleteFlow(entities: Flow<*>): ReactiveCassandraBatchOperations =
    delete(mono { entities.toFastList() })

fun ReactiveCassandraBatchOperations.deleteFlow(
    entities: Flow<*>,
    options: WriteOptions,
): ReactiveCassandraBatchOperations =
    delete(mono { entities.toFastList() }, options)
