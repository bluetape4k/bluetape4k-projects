package io.bluetape4k.javers.repository

import org.javers.core.metamodel.`object`.CdoSnapshot
import org.javers.core.metamodel.`object`.GlobalId
import org.javers.repository.api.JaversRepository

/**
 * [CdoSnapshot]을 저장·로드하는 JaVers 저장소 인터페이스.
 *
 * ## 동작/계약
 * - [saveSnapshot]으로 단일 스냅샷을 저장한다
 * - [loadSnapshots]로 GlobalId에 해당하는 스냅샷 목록을 조회한다 (최신순)
 *
 * ```kotlin
 * val repo: CdoSnapshotRepository = CaffeineCdoSnapshotRepository()
 * val javers = JaversBuilder.javers()
 *     .registerJaversRepository(repo)
 *     .build()
 * ```
 */
interface CdoSnapshotRepository: JaversRepository {

    /**
     * [CdoSnapshot]을 저장소에 저장한다.
     */
    fun saveSnapshot(snapshot: CdoSnapshot)

    /**
     * 지정한 GlobalId 값에 해당하는 스냅샷 목록을 반환한다.
     */
    fun loadSnapshots(globalIdValue: String): List<CdoSnapshot>

    /**
     * 지정한 [GlobalId]에 해당하는 스냅샷 목록을 반환한다.
     */
    fun loadSnapshots(globalId: GlobalId): List<CdoSnapshot> = loadSnapshots(globalId.value())
}
