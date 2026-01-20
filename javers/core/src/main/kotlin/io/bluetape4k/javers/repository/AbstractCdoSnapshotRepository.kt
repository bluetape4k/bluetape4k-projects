package io.bluetape4k.javers.repository

import com.google.gson.JsonObject
import io.bluetape4k.idgenerators.snowflake.Snowflake
import io.bluetape4k.idgenerators.snowflake.Snowflakers
import io.bluetape4k.javers.codecs.JaversCodec
import io.bluetape4k.javers.metamodel.filterByAuthor
import io.bluetape4k.javers.metamodel.filterByChangedPropertyNames
import io.bluetape4k.javers.metamodel.filterByCommitDate
import io.bluetape4k.javers.metamodel.filterByCommitIds
import io.bluetape4k.javers.metamodel.filterByCommitProperties
import io.bluetape4k.javers.metamodel.filterByToCommitId
import io.bluetape4k.javers.metamodel.filterByType
import io.bluetape4k.javers.metamodel.filterByVersion
import io.bluetape4k.javers.metamodel.isChild
import io.bluetape4k.javers.metamodel.isParent
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import io.bluetape4k.support.toOptional
import org.javers.core.commit.Commit
import org.javers.core.commit.CommitId
import org.javers.core.json.JsonConverter
import org.javers.core.metamodel.`object`.CdoSnapshot
import org.javers.core.metamodel.`object`.GlobalId
import org.javers.core.metamodel.`object`.ValueObjectId
import org.javers.core.metamodel.type.EntityType
import org.javers.core.metamodel.type.ManagedType
import org.javers.repository.api.QueryParams
import org.javers.repository.api.SnapshotIdentifier
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.jvm.optionals.getOrNull

/**
 * [CdoSnapshotRepository] 를 구현한 최상위 추상화 클래스입니다.
 * [CdoSnapshot] 을 저장소에 저장하고, 로드하는 역할을 수행합니다.
 *
 * @param T
 * @property codec 저장소에 저장할 때 [CdoSnapshot] 을 인코딩할 codec
 * @property commitIdSupplier snapshot.commitMetadata.id 값을 제공하는 [Snowflake]
 */
abstract class AbstractCdoSnapshotRepository<T: Any>(
    protected val codec: JaversCodec<T>,
    protected val commitIdSupplier: Snowflake = Snowflakers.Global,
): CdoSnapshotRepository {

    companion object: KLogging()

    private var jsonConverter: JsonConverter? = null
    private val lock = ReentrantLock()

    fun getJsonConverter(): JsonConverter? = jsonConverter

    override fun setJsonConverter(jsonConverter: JsonConverter?) {
        this.jsonConverter = jsonConverter
    }

    protected abstract fun getKeys(): List<String>
    protected abstract fun contains(globalIdValue: String): Boolean
    protected fun contains(globalId: GlobalId): Boolean = contains(globalId.value())

    protected abstract fun getSeq(commitId: CommitId): Long
    protected abstract fun updateCommitId(commitId: CommitId, sequence: Long)

    protected abstract fun getSnapshotSize(globalIdValue: String): Int
    protected fun getSnapshotSize(globalId: GlobalId): Int = getSnapshotSize(globalId.value())

    protected var head: CommitId? = null

    protected fun encode(snapshot: CdoSnapshot): T {
        val jsonObject = jsonConverter?.toJsonElement(snapshot) as JsonObject
        return doEncode(jsonObject)
    }

    protected fun decode(data: T): CdoSnapshot? {
        val jsonObject = doDecode(data)
        return jsonObject?.let { jsonConverter?.fromJson(it, CdoSnapshot::class.java) }
    }

    protected fun doEncode(jsonObject: JsonObject): T = codec.encode(jsonObject)
    protected fun doDecode(data: T): JsonObject? = codec.decode(data)

    protected fun getAll(): List<CdoSnapshot> {
        return getKeys()
            .flatMap {
                loadSnapshots(it)
            }.sortedByDescending { getSeq(it.commitMetadata.id) }
            .apply {
                log.debug { "load all snapshot. size=${this.size}" }
            }
    }

    override fun ensureSchema() {
        // Nothing to do.
    }

    override fun getLatest(globalId: GlobalId): Optional<CdoSnapshot> = when {
        contains(globalId) -> loadSnapshots(globalId).firstOrNull().toOptional()
        else               -> Optional.empty()
    }

    override fun getLatest(globalIds: MutableCollection<GlobalId>): MutableList<CdoSnapshot> {
        return globalIds.mapNotNull { getLatest(it).getOrNull() }.toMutableList()
    }

    override fun getStateHistory(globalId: GlobalId, queryParams: QueryParams): MutableList<CdoSnapshot> {
        val filtered = mutableListOf<CdoSnapshot>()
        getAll().forEach snapshot@{ snapshot ->
            if (snapshot.globalId == globalId) {
                filtered.add(snapshot)
                return@snapshot
            }
            if (queryParams.isAggregate && globalId.isParent(snapshot.globalId)) {
                filtered.add(snapshot)
                return@snapshot
            }
        }
        return applyQueryParams(filtered.asSequence(), queryParams)
    }

    override fun getStateHistory(
        givenClasses: MutableSet<ManagedType>,
        queryParams: QueryParams,
    ): MutableList<CdoSnapshot> {
        val filtered = mutableListOf<CdoSnapshot>()

        getAll().forEach snapshot@{ snapshot ->
            givenClasses.forEach classes@{ givenClass ->
                if (snapshot.globalId.isTypeOf(givenClass)) {
                    filtered.add(snapshot)
                    return@classes
                }
                if (queryParams.isAggregate && snapshot.globalId.isChild(givenClass)) {
                    filtered.add(snapshot)
                    return@classes
                }
            }
        }
        return applyQueryParams(filtered.asSequence(), queryParams)
    }

    override fun getValueObjectStateHistory(
        ownerEntity: EntityType,
        path: String,
        queryParams: QueryParams,
    ): MutableList<CdoSnapshot> {
        val result = getAll().filter { snapshot ->
            val id = snapshot.globalId
            id is ValueObjectId && id.hasOwnerOfType(ownerEntity) && id.fragment.equals(path)
        }
        return applyQueryParams(result.asSequence(), queryParams)
    }

    override fun getSnapshots(queryParams: QueryParams): MutableList<CdoSnapshot> {
        return applyQueryParams(getAll().asSequence(), queryParams)
    }

    override fun getSnapshots(snapshotIdentifiers: MutableCollection<SnapshotIdentifier>): List<CdoSnapshot> {
        log.trace { "get snapshots by identifiers. $snapshotIdentifiers" }
        return getPersistedIdentifiers(snapshotIdentifiers)
            .map { snapshot ->
                val objectSnapshots = loadSnapshots(snapshot.globalId)
                objectSnapshots[objectSnapshots.size - snapshot.version.toInt()]
            }
    }

    override fun persist(commit: Commit?) {
        if (commit == null) {
            return
        }
        lock.withLock {
            commit.snapshots.forEach {
                saveSnapshot(it)
            }
            log.trace { "${commit.snapshots.size} snapshot(s) persisted" }
            head = commit.id
            head?.let {
                updateCommitId(it, commitIdSupplier.nextId())
            }
        }
    }

    override fun getHeadId(): CommitId? = head

    private fun applyQueryParams(snapshots: Sequence<CdoSnapshot>, queryParams: QueryParams): MutableList<CdoSnapshot> {
        var results = snapshots
        if (queryParams.commitIds().isNotEmpty()) {
            results = results.filterByCommitIds(queryParams.commitIds())
        }
        if (queryParams.toCommitId().isPresent) {
            results = results.filterByToCommitId(queryParams.toCommitId().get())
        }
        if (queryParams.version().isPresent) {
            results = results.filterByVersion(queryParams.version().get())
        }
        if (queryParams.author().isPresent) {
            results = results.filterByAuthor(queryParams.author().get())
        }
        if (queryParams.from().isPresent || queryParams.to().isPresent) {
            results = results.filterByCommitDate(queryParams)
        }
        if (queryParams.changedProperties().isNotEmpty()) {
            results = results.filterByChangedPropertyNames(queryParams.changedProperties())
        }
        if (queryParams.snapshotType().isPresent) {
            results = results.filterByType(queryParams.snapshotType().get())
        }
        results = results.filterByCommitProperties(queryParams.commitProperties())

        return results.drop(queryParams.skip()).take(queryParams.limit()).toMutableList()
    }

    private fun getPersistedIdentifiers(snapshotIdentifiers: Collection<SnapshotIdentifier>): List<SnapshotIdentifier> {
        return snapshotIdentifiers
            .filter {
                contains(it.globalId) && it.version <= getSnapshotSize(it.globalId)
            }
    }
}
