package io.bluetape4k.javers.repository

import org.javers.core.metamodel.`object`.CdoSnapshot
import org.javers.core.metamodel.`object`.GlobalId
import org.javers.repository.api.JaversRepository

/**
 * [JaversRepository]를 상속받아 [CdoSnapshot]을 저장소에 저장, 로드하는 기본 Repository
 */
interface CdoSnapshotRepository: JaversRepository {

    fun saveSnapshot(snapshot: CdoSnapshot)

    fun loadSnapshots(globalIdValue: String): List<CdoSnapshot>

    fun loadSnapshots(globalId: GlobalId): List<CdoSnapshot> = loadSnapshots(globalId.value())
}
