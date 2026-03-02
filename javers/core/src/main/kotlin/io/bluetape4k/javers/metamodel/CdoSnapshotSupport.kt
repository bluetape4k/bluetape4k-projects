package io.bluetape4k.javers.metamodel

import io.bluetape4k.javers.isDateInRange
import org.javers.core.commit.CommitId
import org.javers.core.metamodel.`object`.CdoSnapshot
import org.javers.core.metamodel.`object`.SnapshotType
import org.javers.repository.api.QueryParams

/**
 * [CdoSnapshot]의 속성들을 [mapper]로 변환하여 리스트로 반환한다.
 */
fun <R> CdoSnapshot.mapProperties(mapper: (key: String, value: Any?) -> R): List<R> =
    this.state.mapProperties(mapper)

/**
 * [CdoSnapshot]의 속성들을 순회하며 [consumer]를 실행한다.
 */
fun <R> CdoSnapshot.forEachProperties(consumer: (key: String, value: Any?) -> Unit): Unit =
    this.state.forEachProperty(consumer)

/**
 * 지정한 [commitId] 이전(이하)의 스냅샷만 필터링한다.
 */
fun Sequence<CdoSnapshot>.filterByToCommitId(commitId: CommitId): Sequence<CdoSnapshot> =
    filter { it.commitId.isBeforeOrEqual(commitId) }

/**
 * 지정한 [commitIds]에 포함되는 스냅샷만 필터링한다.
 */
fun Sequence<CdoSnapshot>.filterByCommitIds(commitIds: Collection<CommitId>): Sequence<CdoSnapshot> =
    filter { commitIds.contains(it.commitId) }

/**
 * 지정한 [version]과 일치하는 스냅샷만 필터링한다.
 */
fun Sequence<CdoSnapshot>.filterByVersion(version: Long): Sequence<CdoSnapshot> =
    filter { it.version == version }

/**
 * 지정한 [author]가 커밋한 스냅샷만 필터링한다.
 */
fun Sequence<CdoSnapshot>.filterByAuthor(author: String): Sequence<CdoSnapshot> =
    filter { it.commitMetadata.author == author }

/**
 * [QueryParams]의 from~to 날짜 범위에 포함되는 스냅샷만 필터링한다.
 */
fun Sequence<CdoSnapshot>.filterByCommitDate(queryParams: QueryParams): Sequence<CdoSnapshot> =
    filter { queryParams.isDateInRange(it.commitMetadata.commitDate) }

/**
 * 지정한 [propertyName]에 변경이 있는 스냅샷만 필터링한다.
 */
fun Sequence<CdoSnapshot>.filterByChangedPropertyName(propertyName: String): Sequence<CdoSnapshot> =
    filter { it.hasChangeAt(propertyName) }

/**
 * 지정한 [propertyNames] 중 하나라도 변경된 스냅샷만 필터링한다.
 */
fun Sequence<CdoSnapshot>.filterByChangedPropertyNames(propertyNames: Set<String>): Sequence<CdoSnapshot> =
    filter { snapshot ->
        propertyNames.any { propertyName ->
            snapshot.hasChangeAt(propertyName)
        }
    }

/**
 * 지정한 [snapshotType]과 일치하는 스냅샷만 필터링한다.
 */
fun Sequence<CdoSnapshot>.filterByType(snapshotType: SnapshotType): Sequence<CdoSnapshot> =
    filter { it.type == snapshotType }

/**
 * 커밋 속성(properties)이 [commitProperties] 조건을 모두 만족하는 스냅샷만 필터링한다.
 */
fun Sequence<CdoSnapshot>.filterByCommitProperties(
    commitProperties: Map<String, Collection<String>>,
): Sequence<CdoSnapshot> =
    filter {
        val props = it.commitMetadata.properties
        commitProperties.all { (key, values) ->
            props.containsKey(key) && values.contains(props[key])
        }
    }

/**
 * [skip]개를 건너뛰고 [limit]개만 취하여 리스트로 반환한다.
 */
fun Sequence<CdoSnapshot>.trimToRequestedSlice(skip: Int, limit: Int): List<CdoSnapshot> =
    drop(skip).take(limit).toList()
