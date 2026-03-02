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

/**
 * reified 타입 파라미터로 [EntityType] 매핑 정보를 조회한다.
 *
 * ```kotlin
 * val entityType = javers.getEntityTypeMapping<Person>()
 * // entityType.baseJavaType == Person::class.java
 * ```
 */
inline fun <reified T: Any> Javers.getEntityTypeMapping(): EntityType =
    this.getTypeMapping(T::class.java)

/**
 * reified 타입 파라미터로 [ValueObjectType] 매핑 정보를 조회한다.
 *
 * ```kotlin
 * val voType = javers.getValueObjectTypeMapping<Address>()
 * // voType.baseJavaType == Address::class.java
 * ```
 */
inline fun <reified T: Any> Javers.getValueObjectTypeMapping(): ValueObjectType =
    this.getTypeMapping(T::class.java)

/**
 * 엔티티 인스턴스로부터 [InstanceId]를 생성한다.
 *
 * ```kotlin
 * val bob = Person("Bob", "Dev")
 * val id = javers.createEntityInstanceId(bob)
 * // id.value() == "Person/Bob"
 * ```
 */
inline fun <reified T: Any> Javers.createEntityInstanceId(entity: T): InstanceId =
    this.getEntityTypeMapping<T>().createIdFromInstance(entity)

/**
 * 엔티티의 로컬 ID 값으로부터 [InstanceId]를 생성한다.
 *
 * ```kotlin
 * val id = javers.createEntityInstanceIdByEntityId<Person>("Bob")
 * // id.value() == "Person/Bob"
 * ```
 */
inline fun <reified T: Any> Javers.createEntityInstanceIdByEntityId(localId: Any): InstanceId =
    this.getEntityTypeMapping<T>().createIdFromInstanceId(localId)

/**
 * 두 컬렉션의 차이를 비교하여 [Diff]를 반환한다.
 *
 * ```kotlin
 * val oldList = listOf(Person("Tommy", "Tommy Smart"))
 * val newList = listOf(Person("Tommy", "Tommy C. Smart"))
 * val diff = javers.compareCollections(oldList, newList)
 * // diff.changes.size == 1
 * ```
 */
inline fun <reified T: Any> Javers.compareCollections(oldVersion: Collection<T>, newVersion: Collection<T>): Diff =
    this.compareCollections(oldVersion, newVersion, T::class.java)

/**
 * 지정한 엔티티의 최신 [CdoSnapshot]을 반환하거나, 없으면 null을 반환한다.
 *
 * ```kotlin
 * val snapshot = javers.latestSnapshotOrNull(1, SnapshotEntity::class)
 * // snapshot?.globalId?.value() == "...SnapshotEntity/1"
 * ```
 */
fun Javers.latestSnapshotOrNull(localId: Any, entityClass: KClass<*>): CdoSnapshot? =
    getLatestSnapshot(localId, entityClass.java).getOrNull()

/**
 * reified 타입 파라미터로 지정한 엔티티의 최신 [CdoSnapshot]을 반환하거나, 없으면 null을 반환한다.
 *
 * ```kotlin
 * val snapshot = javers.latestSnapshotOrNull<SnapshotEntity>(1)
 * // snapshot?.globalId?.value() == "...SnapshotEntity/1"
 * ```
 */
inline fun <reified T: Any> Javers.latestSnapshotOrNull(localId: Any): CdoSnapshot? =
    getLatestSnapshot(localId, T::class.java).getOrNull()

/**
 * [CdoSnapshot]을 원본 엔티티를 감싼 [Shadow]로 변환한다.
 *
 * ## 동작/계약
 * - [ShadowFactory]를 통해 snapshot의 상태를 원본 엔티티 객체로 복원한다
 * - 반환된 Shadow의 `get()`으로 복원된 엔티티를 얻을 수 있다
 *
 * ```kotlin
 * val snapshot = javers.commit("a", entity).snapshots.first()
 * val shadow: Shadow<SnapshotEntity> = javers.getShadow(snapshot)
 * // shadow.get() == entity
 * ```
 */
@Suppress("UNCHECKED_CAST")
fun <T> Javers.getShadow(snapshot: CdoSnapshot): Shadow<T> {
    return shadowFactory.createShadow(snapshot, snapshot.commitMetadata, null) as Shadow<T>
}

/**
 * [Javers] 인스턴스에 연결된 [ShadowFactory]를 반환한다.
 */
val Javers.shadowFactory: ShadowFactory
    get() = ShadowProvider.getShadowFactory(this)

/**
 * JQL 쿼리로 Shadow를 조회하여 [Sequence]로 반환한다.
 *
 * ```kotlin
 * val query = queryByInstanceId<SnapshotEntity>(1)
 * val shadows = javers.findShadowsAndSequence<SnapshotEntity>(query)
 * // shadows.toList().size >= 1
 * ```
 */
fun <T: Any> Javers.findShadowsAndSequence(jql: JqlQuery): Sequence<Shadow<T>> =
    findShadowsAndStream<T>(jql).asSequence()
