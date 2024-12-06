package io.bluetape4k.javers.repository.jql

import org.javers.core.Changes
import org.javers.core.Javers
import org.javers.core.metamodel.`object`.CdoSnapshot
import org.javers.repository.jql.JqlQuery
import org.javers.shadow.Shadow
import java.util.stream.Stream
import kotlin.streams.asSequence

// TODO: 필요없을 것 같다.

inline fun <reified T: Any> JqlQuery.findShadows(javers: Javers): MutableList<Shadow<T>> =
    javers.findShadows(this)

inline fun <reified T: Any> JqlQuery.findShadowsAndStream(javers: Javers): Stream<Shadow<T>> =
    javers.findShadowsAndStream(this)

inline fun <reified T: Any> JqlQuery.findShadowsAndSequence(javers: Javers): Sequence<Shadow<T>> =
    javers.findShadowsAndStream<T>(this).asSequence()

fun JqlQuery.findSnapshots(javers: Javers): MutableList<CdoSnapshot> =
    javers.findSnapshots(this)

fun JqlQuery.findChanges(javers: Javers): Changes =
    javers.findChanges(this)
