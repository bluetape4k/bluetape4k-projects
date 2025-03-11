package io.bluetape4k.javers

import org.javers.core.Javers
import org.javers.core.diff.Diff
import org.javers.core.metamodel.`object`.CdoSnapshot
import org.javers.core.metamodel.`object`.InstanceId
import org.javers.core.metamodel.type.EntityType
import org.javers.core.metamodel.type.ValueObjectType
import org.javers.repository.jql.JqlQuery
import org.javers.shadow.Shadow
import org.javers.shadow.ShadowFactory
import kotlin.jvm.optionals.getOrNull
import kotlin.reflect.KClass
import kotlin.streams.asSequence

inline fun <reified T: Any> Javers.getEntityTypeMapping(): EntityType =
    this.getTypeMapping(T::class.java)

inline fun <reified T: Any> Javers.getValueObjectTypeMapping(): ValueObjectType =
    this.getTypeMapping(T::class.java)

inline fun <reified T: Any> Javers.createEntityInstanceId(entity: T): InstanceId =
    this.getEntityTypeMapping<T>().createIdFromInstance(entity)


inline fun <reified T: Any> Javers.createEntityInstanceIdByEntityId(localId: Any): InstanceId =
    this.getEntityTypeMapping<T>().createIdFromInstanceId(localId)

inline fun <reified T: Any> Javers.compareCollections(oldVersion: Collection<T>, newVersion: Collection<T>): Diff =
    this.compareCollections(oldVersion, newVersion, T::class.java)

fun Javers.latestSnapshotOrNull(localId: Any, entityClass: KClass<*>): CdoSnapshot? =
    getLatestSnapshot(localId, entityClass.java).getOrNull()

inline fun <reified T: Any> Javers.latestSnapshotOrNull(localId: Any): CdoSnapshot? =
    getLatestSnapshot(localId, T::class.java).getOrNull()

/**
 * Snapshot을 시스템에서 사용하는 실제 Entity를 가진 Shadow로 변환합니다.
 *
 * @param T business entity type
 * @param snapshot snapshot 정보
 * @return `Shadow<T>` 인스턴스
 */
@Suppress("UNCHECKED_CAST")
fun <T> Javers.getShadow(snapshot: CdoSnapshot): Shadow<T> {
    return shadowFactory.createShadow(snapshot, snapshot.commitMetadata, null) as Shadow<T>
}

val Javers.shadowFactory: ShadowFactory
    get() = ShadowProvider.getShadowFactory(this)

fun <T: Any> Javers.findShadowsAndSequence(jql: JqlQuery): Sequence<Shadow<T>> =
    findShadowsAndStream<T>(jql).asSequence()
