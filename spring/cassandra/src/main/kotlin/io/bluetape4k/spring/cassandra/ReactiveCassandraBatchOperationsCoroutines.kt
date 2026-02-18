package io.bluetape4k.spring.cassandra

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.mono
import org.springframework.data.cassandra.core.ReactiveCassandraBatchOperations
import org.springframework.data.cassandra.core.cql.WriteOptions

fun ReactiveCassandraBatchOperations.insertFlow(entities: Flow<*>): ReactiveCassandraBatchOperations =
    insert(mono { entities.toList() })

fun ReactiveCassandraBatchOperations.insertFlow(
    entities: Flow<*>,
    options: WriteOptions,
): ReactiveCassandraBatchOperations =
    insert(mono { entities.toList() }, options)

fun ReactiveCassandraBatchOperations.updateFlow(entities: Flow<*>): ReactiveCassandraBatchOperations =
    update(mono { entities.toList() })

fun ReactiveCassandraBatchOperations.updateFlow(
    entities: Flow<*>,
    options: WriteOptions,
): ReactiveCassandraBatchOperations =
    update(mono { entities.toList() }, options)

fun ReactiveCassandraBatchOperations.deleteFlow(entities: Flow<*>): ReactiveCassandraBatchOperations =
    delete(mono { entities.toList() })

fun ReactiveCassandraBatchOperations.deleteFlow(
    entities: Flow<*>,
    options: WriteOptions,
): ReactiveCassandraBatchOperations =
    delete(mono { entities.toList() }, options)
