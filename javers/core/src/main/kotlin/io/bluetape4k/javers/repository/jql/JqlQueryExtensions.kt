package io.bluetape4k.javers.repository.jql

import org.javers.core.Changes
import org.javers.core.Javers
import org.javers.core.metamodel.`object`.CdoSnapshot
import org.javers.repository.jql.JqlQuery
import org.javers.shadow.Shadow
import java.util.stream.Stream
import kotlin.streams.asSequence

/**
 * [JqlQuery]에서 Shadow 목록을 조회한다.
 *
 * ```kotlin
 * val query = queryByInstanceId<Person>("bob")
 * val shadows = query.findShadows<Person>(javers)
 * ```
 */
inline fun <reified T: Any> JqlQuery.findShadows(javers: Javers): MutableList<Shadow<T>> =
    javers.findShadows(this)

/**
 * [JqlQuery]에서 Shadow를 [Stream]으로 조회한다.
 */
inline fun <reified T: Any> JqlQuery.findShadowsAndStream(javers: Javers): Stream<Shadow<T>> =
    javers.findShadowsAndStream(this)

/**
 * [JqlQuery]에서 Shadow를 [Sequence]로 조회한다.
 */
inline fun <reified T: Any> JqlQuery.findShadowsAndSequence(javers: Javers): Sequence<Shadow<T>> =
    javers.findShadowsAndStream<T>(this).asSequence()

/**
 * [JqlQuery]에서 스냅샷 목록을 조회한다.
 */
fun JqlQuery.findSnapshots(javers: Javers): MutableList<CdoSnapshot> =
    javers.findSnapshots(this)

/**
 * [JqlQuery]에서 변경 목록을 조회한다.
 */
fun JqlQuery.findChanges(javers: Javers): Changes =
    javers.findChanges(this)
