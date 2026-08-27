package io.bluetape4k.science.exposed.service

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.science.exposed.NetCdfException
import io.bluetape4k.science.exposed.model.NetCdfFileRecord
import io.bluetape4k.science.exposed.model.NetCdfImportProgress
import io.bluetape4k.science.exposed.model.NetCdfImportStatus
import io.bluetape4k.science.exposed.model.NetCdfVariableInfo
import io.bluetape4k.science.exposed.repository.NetCdfFileRepository
import io.bluetape4k.science.exposed.repository.NetCdfImportProgressRepository
import io.bluetape4k.science.exposed.service.internal.CoordinateKeySet
import io.bluetape4k.science.exposed.service.internal.CoordinateReprojector
import io.bluetape4k.science.exposed.service.internal.JdbcTileBatchWriter
import io.bluetape4k.science.exposed.service.internal.MAX_AUXILIARY_JSONB_BYTES
import io.bluetape4k.science.exposed.service.internal.MAX_BATCH_ROWS
import io.bluetape4k.science.exposed.service.internal.MAX_CELLS
import io.bluetape4k.science.exposed.service.internal.MAX_DUPLICATE_ENTRY_BYTES
import io.bluetape4k.science.exposed.service.internal.MAX_GROUP_COUNT
import io.bluetape4k.science.exposed.service.internal.MAX_GROUP_DEPTH
import io.bluetape4k.science.exposed.service.internal.MAX_GROUP_DIMENSIONS
import io.bluetape4k.science.exposed.service.internal.MAX_METADATA_BYTES
import io.bluetape4k.science.exposed.service.internal.MAX_SLICES
import io.bluetape4k.science.exposed.service.internal.MAX_TILE_CELLS
import io.bluetape4k.science.exposed.service.internal.MAX_VARIABLES
import io.bluetape4k.science.exposed.service.internal.MAX_VARIABLE_NAME_BYTES
import io.bluetape4k.science.exposed.service.internal.MemoryBudget
import io.bluetape4k.science.exposed.service.internal.MutableCoordinateSample
import io.bluetape4k.science.exposed.service.internal.NetCdfFileGuard
import io.bluetape4k.science.exposed.service.internal.NetCdfTileCoordinateSampler
import io.bluetape4k.science.exposed.service.internal.NetCdfTile
import io.bluetape4k.science.exposed.service.internal.NetCdfTilePlanner
import io.bluetape4k.science.exposed.service.internal.TileRow
import io.bluetape4k.science.exposed.service.internal.UcarCoordinateReader
import io.bluetape4k.science.exposed.service.internal.VariableAxisMap
import io.bluetape4k.science.exposed.service.internal.checkedProduct
import io.bluetape4k.science.exposed.service.internal.serializeAuxiliaryAttributes
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import ucar.ma2.Array as UcarArray
import ucar.nc2.Attribute
import ucar.nc2.Group
import ucar.nc2.NetcdfFile
import ucar.nc2.NetcdfFiles
import ucar.nc2.Variable
import ucar.nc2.dataset.NetcdfDataset
import ucar.nc2.dataset.NetcdfDatasets
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.ArrayDeque
import java.util.concurrent.CancellationException
import kotlin.math.abs

/**
 * NetCDF 파일 등록과 bounded 격자 값 임포트를 담당하는 blocking 서비스입니다.
 *
 * 파일 identity를 등록·재개 시점에 확인하고, CF 1D/2D 좌표축을 tile 단위로
 * 읽습니다. 좌표와 값은 두 번 순회하여 한 slice의 duplicate를 먼저 검증한
 * 뒤에만 JDBC batch를 실행합니다.
 */
class NetCdfCatalogService(
    private val fileRepo: NetCdfFileRepository,
    private val progressRepo: NetCdfImportProgressRepository,
    private val meterRegistry: MeterRegistry? = null,
) {

    companion object: KLogging() {
        /** heartbeat lease TTL — 5분 */
        val LEASE_TTL: Duration = Duration.ofMinutes(5)

        /** heartbeat 갱신 주기 — N 슬라이스마다 또는 30초 경과 시 */
        const val HEARTBEAT_EVERY_SLICES: Int = 10

        /** heartbeat 갱신 시간 임계 */
        val HEARTBEAT_INTERVAL: Duration = Duration.ofSeconds(30)

        private val SUPPORTED_RANKS: IntRange = 1..4
        private const val MAX_SPATIAL_RANK: Int = 4
        private const val FILL_VALUE_TOLERANCE: Double = 1e-7
    }

    /** NetCDF 파일을 bounded metadata와 identity fingerprint와 함께 등록합니다. */
    fun registerFile(filePath: String): Long {
        val sample = meterRegistry?.let { Timer.start(it) }
        var success = false
        try {
            val identity = NetCdfFileGuard.validateForRegister(filePath)
            val record = NetCdfFileGuard.openVerified(
                fileId = 0L,
                filePath = identity.path.toString(),
                expectedFingerprint = identity.fingerprint,
            ) {
                NetcdfFiles.open(identity.path.toString())
            }.use { netcdf ->
                buildFileRecord(identity.path, identity.fileSize, identity.fingerprint, netcdf)
            }
            val id = transaction { fileRepo.save(record).id }
            success = true
            log.info { "NetCDF file registered — id=$id path=${identity.path} vars=${record.variables.size}" }
            return id
        } finally {
            sample?.stop(
                checkNotNull(meterRegistry).timer(
                    "netcdf.register.duration",
                    "status",
                    if (success) "success" else "failure",
                ),
            )
        }
    }

    /** 등록된 변수의 값을 slice/tile 단위로 임포트합니다. */
    fun importGridValues(fileId: Long, variableName: String) {
        try {
            importGridValuesInternal(fileId, variableName)
        } catch (e: NetCdfException) {
            if (e !is NetCdfException.FileRecordNotFound) {
                recordRejection(e)
            }
            throw e
        }
    }

    private fun importGridValuesInternal(fileId: Long, variableName: String) {
        require(variableName.isNotBlank()) { "variableName must not be blank" }
        val record = transaction { fileRepo.findById(fileId) }
            ?: throw NetCdfException.FileRecordNotFound(fileId)

        val expectedFingerprint = record.globalAttrs[NetCdfFileGuard.FINGERPRINT_ATTRIBUTE]
            ?: run {
                val actual = NetCdfFileGuard.validateForRegister(record.filePath).fingerprint
                throw NetCdfException.FileChanged(fileId, "missing-fingerprint", actual)
            }
        val verifiedIdentity = NetCdfFileGuard.verifyForResume(fileId, record.filePath, expectedFingerprint)
        val dataset = NetCdfFileGuard.openVerified(
            fileId = fileId,
            filePath = record.filePath,
            expectedFingerprint = expectedFingerprint,
        ) {
            NetcdfDatasets.openDataset(verifiedIdentity.path.toString())
        }

        try {
            dataset.use { ncd ->
                val variable = ncd.findVariable(variableName)
                    ?: throw NetCdfException.VariableNotFound(fileId, variableName)
                if (variable.rank !in SUPPORTED_RANKS) {
                    throw NetCdfException.UnsupportedVariable(variableName, variable.rank)
                }
                val axisMap = VariableAxisMap.build(variable, ncd)
                val layout = ImportLayout.create(variable, axisMap)
                val prepared = PreparedImport(
                    fileId = fileId,
                    variableName = variableName,
                    variable = variable,
                    axisMap = axisMap,
                    layout = layout,
                    reprojector = if (layout.hasSpatialGrid) {
                        CoordinateReprojector.from(variable, ncd, axisMap)
                    } else null,
                    coordinateReader = UcarCoordinateReader(
                        buildMap {
                            (ncd.variables + ncd.coordinateAxes).forEach { candidate ->
                                put(candidate.fullName, candidate)
                                putIfAbsent(candidate.shortName, candidate)
                            }
                        },
                        variable.fullName,
                    ),
                    fillValue = variable.findAttribute("_FillValue")?.numericValue?.toDouble(),
                )
                runPreparedImport(prepared)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw e
        }
    }

    private fun runPreparedImport(prepared: PreparedImport) {
        val progress = transaction {
            progressRepo.acquireLease(prepared.fileId, prepared.variableName, LEASE_TTL)
        }
        validateProgress(prepared, progress)
        if (progress.status == NetCdfImportStatus.COMPLETED) {
            log.info { "import skipped — already completed: fileId=${prepared.fileId} var=${prepared.variableName}" }
            return
        }

        val initialExpiry = progress.leaseExpiresAt
            ?: throw NetCdfException.CorruptProgress(progress.id, "active lease is null")
        val lease = ImportLease(initialExpiry)
        val context = ImportContext(prepared, progress.id, lease)
        val startSlice = (progress.lastSliceIdx ?: -1L) + 1L
        if (progress.lastSliceIdx != null) {
            meterRegistry?.counter("netcdf.import.status", "status", "resumed")?.increment()
            log.info {
                "resuming import — fileId=${prepared.fileId} var=${prepared.variableName} " +
                    "startSliceIdx=$startSlice"
            }
        }

        try {
            if (prepared.layout.isRankOne) {
                importRankOne(context, startSlice)
            } else {
                importSpatial(context, startSlice)
            }
            meterRegistry?.counter("netcdf.import.status", "status", "success")?.increment()
            log.info { "import COMPLETED — fileId=${prepared.fileId} var=${prepared.variableName}" }
        } catch (e: NetCdfException.ImportAlreadyRunning) {
            throw e
        } catch (e: NetCdfException.ImportLeaseLost) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw e
        } catch (e: Exception) {
            try {
                transaction { progressRepo.markFailed(progress.id, lease.expiresAt, e.message.orEmpty()) }
            } catch (failure: Exception) {
                e.addSuppressed(failure)
            }
            meterRegistry?.counter("netcdf.import.status", "status", "failure")?.increment()
            throw e
        }
    }

    private fun importRankOne(context: ImportContext, startSlice: Long) {
        if (startSlice > 0L) {
            checkNotInterrupted()
            transaction {
                checkNotInterrupted()
                progressRepo.markCompleted(context.progressId, context.lease.expiresAt)
                checkNotInterrupted()
            }
            return
        }
        val variable = context.prepared.variable
        val length = variable.shape.singleOrNull()
            ?: throw NetCdfException.UnsupportedVariable(variable.fullName, variable.rank)
        val reader = { origin: IntArray, shape: IntArray -> variable.read(origin, shape) }
        val writer = JdbcTileBatchWriter()
        val batchPayloadBytes = checkedProduct(
            MAX_BATCH_ROWS,
            io.bluetape4k.science.exposed.service.internal.MAX_FIXED_ROW_BYTES + MAX_AUXILIARY_JSONB_BYTES,
        )
        MemoryBudget(
            tileBufferBytes = checkedProduct(MAX_TILE_CELLS, Double.SIZE_BYTES.toLong()),
            coordinateBytes = 0L,
            serializerScratchBytes = maxOf(MAX_AUXILIARY_JSONB_BYTES, batchPayloadBytes),
            duplicateSetBytes = 0L,
        ).requireWithinLimit()
        var inserted = 0
        var skipped = 0
        var offset = 0
        while (offset < length) {
            checkNotInterrupted()
            val count = minOf(MAX_BATCH_ROWS.toInt(), length - offset)
            val data = reader(intArrayOf(offset), intArrayOf(count))
            val index = data.index
            val result = transaction {
                context.lease.expiresAt = progressRepo.touchLease(
                    context.progressId,
                    context.lease.expiresAt,
                    leaseTtl = LEASE_TTL,
                )
                checkNotInterrupted()
                val pending = ArrayList<TileRow>(MAX_BATCH_ROWS.toInt())
                var windowInserted = 0
                repeat(count) { local ->
                    checkNotInterrupted()
                    val value = data.getDouble(index.set(local))
                    if (isMissing(value, context.prepared.fillValue)) {
                        skipped++
                    } else {
                        pending += TileRow(
                            fileId = context.prepared.fileId,
                            variableName = context.prepared.variableName,
                            longitude = null,
                            latitude = null,
                            timeIdx = offset + local,
                            levelIdx = 0,
                            value = value,
                        )
                        if (pending.size == MAX_BATCH_ROWS.toInt()) {
                            windowInserted += writer.write(
                                connection.connection as java.sql.Connection,
                                pending,
                            ).inserted
                            checkNotInterrupted()
                            pending.clear()
                        }
                    }
                }
                if (pending.isNotEmpty()) {
                    checkNotInterrupted()
                    windowInserted += writer.write(
                        connection.connection as java.sql.Connection,
                        pending,
                    ).inserted
                    checkNotInterrupted()
                    pending.clear()
                }
                checkNotInterrupted()
                if (offset + count == length) {
                    checkNotInterrupted()
                    context.lease.expiresAt = progressRepo.renewLease(
                        context.progressId,
                        context.lease.expiresAt,
                        lastSliceIdx = 0L,
                        leaseTtl = LEASE_TTL,
                    )
                    checkNotInterrupted()
                    progressRepo.markCompleted(context.progressId, context.lease.expiresAt)
                    checkNotInterrupted()
                } else {
                    checkNotInterrupted()
                    context.lease.expiresAt = progressRepo.touchLease(
                        context.progressId,
                        context.lease.expiresAt,
                        leaseTtl = LEASE_TTL,
                    )
                    checkNotInterrupted()
                }
                windowInserted
            }
            inserted += result
            offset += count
        }
        recordMetrics(inserted, skipped)
    }

    private fun importSpatial(context: ImportContext, startSlice: Long) {
        val layout = context.prepared.layout
        val finalSlice = layout.totalSlices - 1L
        if (startSlice > finalSlice) {
            checkNotInterrupted()
            transaction {
                checkNotInterrupted()
                progressRepo.markCompleted(context.progressId, context.lease.expiresAt)
                checkNotInterrupted()
            }
            return
        }
        var slice = startSlice
        while (slice <= finalSlice) {
            checkNotInterrupted()
            val timeIdx = (slice / layout.levelCount).toInt()
            val levelIdx = (slice % layout.levelCount).toInt()
            importSlice(context, timeIdx, levelIdx, slice)
            slice++
        }
    }

    private fun importSlice(
        context: ImportContext,
        timeIdx: Int,
        levelIdx: Int,
        sliceIdx: Long,
    ) {
        val sample = meterRegistry?.let { Timer.start(it) }
        var success = false
        try {
            importSliceInternal(context, timeIdx, levelIdx, sliceIdx)
            success = true
        } finally {
            sample?.stop(
                checkNotNull(meterRegistry).timer(
                    "netcdf.import.slice.duration",
                    "status",
                    if (success) "success" else "failure",
                ),
            )
        }
    }

    private fun importSliceInternal(
        context: ImportContext,
        timeIdx: Int,
        levelIdx: Int,
        sliceIdx: Long,
    ) {
        val prepared = context.prepared
        val layout = prepared.layout
        val tiles = NetCdfTilePlanner.plan(layout.rowCount, layout.columnCount)
        val sliceCells = checkedProduct(layout.rowCount.toLong(), layout.columnCount.toLong())
        val duplicateSetBytes = checkedProduct(sliceCells, MAX_DUPLICATE_ENTRY_BYTES)
        if (duplicateSetBytes > io.bluetape4k.science.exposed.service.internal.MAX_DUPLICATE_SET_BYTES) {
            throw NetCdfException.ResourceLimitExceeded(
                "duplicate-coordinate-set",
                io.bluetape4k.science.exposed.service.internal.MAX_DUPLICATE_SET_BYTES,
                duplicateSetBytes,
            )
        }
        val duplicateKeys = CoordinateKeySet(
            expectedSize = sliceCells.toInt(),
        )

        // 첫 번째 pass: DB transaction 전에 전체 slice의 canonical coordinate를 검증합니다.
        tiles.forEach { tile ->
            checkNotInterrupted()
            transaction {
                // duplicate preflight도 NetCDF read 전에 lease fence를 확인합니다.
                checkNotInterrupted()
                context.lease.expiresAt = progressRepo.touchLease(
                    context.progressId,
                    context.lease.expiresAt,
                    leaseTtl = LEASE_TTL,
                )
                checkNotInterrupted()
                val data = readTile(prepared.variable, layout, tile, timeIdx, levelIdx)
                checkNotInterrupted()
                scanTile(prepared, layout, tile, data) { sample, _ ->
                    if (sample != null &&
                        !duplicateKeys.add(timeIdx, levelIdx, sample.longitude, sample.latitude)
                    ) {
                        throw NetCdfException.DuplicateCoordinate(
                            fileId = prepared.fileId,
                            variableName = prepared.variableName,
                            timeIdx = timeIdx,
                            levelIdx = levelIdx,
                            longitude = sample.longitude,
                            latitude = sample.latitude,
                        )
                    }
                }
                checkNotInterrupted()
            }
        }
        val tileCells = MAX_TILE_CELLS
        val coordinateBytes = checkedProduct(
            tileCells,
            (2 + prepared.axisMap.auxiliaryAxes.size).toLong(),
            Double.SIZE_BYTES.toLong(),
        )
        val batchPayloadBytes = checkedProduct(
            MAX_BATCH_ROWS,
            io.bluetape4k.science.exposed.service.internal.MAX_FIXED_ROW_BYTES + MAX_AUXILIARY_JSONB_BYTES,
        )
        MemoryBudget(
            tileBufferBytes = checkedProduct(tileCells, Double.SIZE_BYTES.toLong()),
            coordinateBytes = coordinateBytes,
            serializerScratchBytes = maxOf(MAX_AUXILIARY_JSONB_BYTES, batchPayloadBytes),
            duplicateSetBytes = duplicateSetBytes,
        ).requireWithinLimit()

        // 두 번째 pass: 검증이 끝난 뒤 tile별로 같은 Exposed transaction에서 기록합니다.
        var inserted = 0
        var skipped = 0
        tiles.forEachIndexed { tileIndex, tile ->
            checkNotInterrupted()
            val result = transaction {
                val writer = JdbcTileBatchWriter()
                val pending = ArrayList<TileRow>(MAX_BATCH_ROWS.toInt())
                var tileInserted = 0
                // 첫 read/write 전에 fence하여 만료된 owner가 SQL을 실행한 뒤
                // lease 손실을 발견하는 경로를 차단합니다.
                context.lease.expiresAt = progressRepo.touchLease(
                    context.progressId,
                    context.lease.expiresAt,
                    leaseTtl = LEASE_TTL,
                )
                checkNotInterrupted()
                val data = readTile(prepared.variable, layout, tile, timeIdx, levelIdx)
                checkNotInterrupted()
                scanTile(prepared, layout, tile, data) { sample, value ->
                    if (sample == null) {
                        skipped++
                    } else {
                        pending += TileRow(
                            fileId = prepared.fileId,
                            variableName = prepared.variableName,
                            longitude = sample.longitude,
                            latitude = sample.latitude,
                            timeIdx = timeIdx,
                            levelIdx = levelIdx,
                            value = value,
                            attrsJson = serializeAuxiliaryAttributes(sample.auxiliary),
                        )
                        if (pending.size == MAX_BATCH_ROWS.toInt()) {
                            checkNotInterrupted()
                            tileInserted += writer.write(connection.connection as java.sql.Connection, pending).inserted
                            checkNotInterrupted()
                            pending.clear()
                        }
                    }
                }
                checkNotInterrupted()
                if (pending.isNotEmpty()) {
                    checkNotInterrupted()
                    tileInserted += writer.write(connection.connection as java.sql.Connection, pending).inserted
                    checkNotInterrupted()
                    pending.clear()
                }
                checkNotInterrupted()
                if (tileIndex == tiles.lastIndex && sliceIdx == layout.totalSlices - 1L) {
                    checkNotInterrupted()
                    context.lease.expiresAt = progressRepo.renewLease(
                        context.progressId,
                        context.lease.expiresAt,
                        lastSliceIdx = sliceIdx,
                        leaseTtl = LEASE_TTL,
                    )
                    checkNotInterrupted()
                    progressRepo.markCompleted(context.progressId, context.lease.expiresAt)
                    checkNotInterrupted()
                } else if (tileIndex == tiles.lastIndex) {
                    checkNotInterrupted()
                    context.lease.expiresAt = progressRepo.renewLease(
                        context.progressId,
                        context.lease.expiresAt,
                        lastSliceIdx = sliceIdx,
                        leaseTtl = LEASE_TTL,
                    )
                    checkNotInterrupted()
                } else {
                    checkNotInterrupted()
                    context.lease.expiresAt = progressRepo.touchLease(
                        context.progressId,
                        context.lease.expiresAt,
                        leaseTtl = LEASE_TTL,
                    )
                    checkNotInterrupted()
                }
                tileInserted
            }
            inserted += result
        }
        recordMetrics(inserted, skipped)
    }

    private fun scanTile(
        prepared: PreparedImport,
        layout: ImportLayout,
        tile: NetCdfTile,
        data: UcarArray,
        consume: (io.bluetape4k.science.exposed.service.internal.CoordinateSample?, Double) -> Unit,
    ) {
        val index = data.index
        val indices = IntArray(prepared.variable.rank)
        layout.timeDim?.let { indices[it] = 0 }
        layout.levelDim?.let { indices[it] = 0 }
        val target = MutableCoordinateSample()
        val reprojector = prepared.reprojector
        val pointProvider = reprojector
            ?.takeUnless { it.sourceCrs == CoordinateReprojector.WGS84 || it.sourceCrs == "EPSG:4269" }
            ?.tilePointProvider(
                rowOrigin = tile.rowOrigin,
                columnOrigin = tile.columnOrigin,
                rowCount = tile.rowCount,
                columnCount = tile.columnCount,
            )
        val sampler = NetCdfTileCoordinateSampler(
            prepared.axisMap,
            prepared.coordinateReader,
            rowOrigin = tile.rowOrigin,
            columnOrigin = tile.columnOrigin,
            rowCount = tile.rowCount,
            columnCount = tile.columnCount,
            pointProvider = pointProvider,
        )
        repeat(tile.rowCount) { localRow ->
            repeat(tile.columnCount) { localColumn ->
                indices[layout.rowDim] = localRow
                indices[layout.columnDim] = localColumn
                val value = data.getDouble(index.set(indices))
                if (isMissing(value, prepared.fillValue)) {
                    consume(null, value)
                } else {
                    sampler.sample(tile.rowOrigin + localRow, tile.columnOrigin + localColumn, target)
                    consume(target.readOnlyCopy(), value)
                }
            }
        }
    }

    private fun readTile(
        variable: Variable,
        layout: ImportLayout,
        tile: NetCdfTile,
        timeIdx: Int,
        levelIdx: Int,
    ): UcarArray {
        val origin = IntArray(variable.rank)
        val shape = IntArray(variable.rank) { 1 }
        layout.timeDim?.let { origin[it] = timeIdx }
        layout.levelDim?.let { origin[it] = levelIdx }
        origin[layout.rowDim] = tile.rowOrigin
        origin[layout.columnDim] = tile.columnOrigin
        shape[layout.rowDim] = tile.rowCount
        shape[layout.columnDim] = tile.columnCount
        return variable.read(origin, shape)
    }

    private fun validateProgress(prepared: PreparedImport, progress: NetCdfImportProgress) {
        val finalSlice = prepared.layout.totalSlices - 1L
        val checkpoint = progress.lastSliceIdx
        val detail = when {
            progress.status == NetCdfImportStatus.COMPLETED &&
                (progress.leaseExpiresAt != null || progress.completedAt == null) ->
                "COMPLETED lease/completedAt invariant"
            progress.status == NetCdfImportStatus.IN_PROGRESS &&
                (progress.leaseExpiresAt == null || progress.completedAt != null) ->
                "IN_PROGRESS lease/completedAt invariant"
            checkpoint != null && (checkpoint < -1L || checkpoint > finalSlice) ->
                "lastSliceIdx=$checkpoint finalSlice=$finalSlice"
            progress.status == NetCdfImportStatus.COMPLETED && checkpoint != finalSlice ->
                "COMPLETED checkpoint=$checkpoint finalSlice=$finalSlice"
            else -> null
        }
        if (detail != null) {
            if (progress.status != NetCdfImportStatus.COMPLETED) {
                transaction { progressRepo.quarantineCorruptProgress(progress.id, detail) }
            }
            throw NetCdfException.CorruptProgress(progress.id, detail)
        }
    }

    private fun buildFileRecord(
        path: Path,
        fileSize: Long,
        fingerprint: String,
        netcdf: NetcdfFile,
    ): NetCdfFileRecord {
        val budget = MetadataBudget()
        inspectGroup(netcdf.rootGroup, depth = 0, budget)
        if (netcdf.variables.size.toLong() > MAX_VARIABLES) {
            throw NetCdfException.ResourceLimitExceeded("variables", MAX_VARIABLES, netcdf.variables.size.toLong())
        }
        val variables = netcdf.variables.map { variable ->
            val nameBytes = variable.fullName.toByteArray(StandardCharsets.UTF_8).size.toLong()
            if (nameBytes > MAX_VARIABLE_NAME_BYTES) {
                throw NetCdfException.ResourceLimitExceeded("variable-name-bytes", MAX_VARIABLE_NAME_BYTES, nameBytes)
            }
            NetCdfVariableInfo(
                name = variable.fullName,
                dataType = variable.dataType.name,
                shape = variable.shape.toList(),
                attributes = variable.attributes().associate { attribute ->
                    attribute.shortName to attributeValue(attribute)
                },
            )
        }
        val dimensions = netcdf.rootGroup.dimensions.associate { it.shortName to it.length }
        val globalAttrs = LinkedHashMap<String, String>()
        netcdf.globalAttributes.forEach { attribute ->
            if (attribute.shortName == NetCdfFileGuard.FINGERPRINT_ATTRIBUTE) {
                throw NetCdfException.ResourceLimitExceeded(
                    "reserved-fingerprint-key",
                    0L,
                    1L,
                )
            }
            globalAttrs[attribute.shortName] = attributeValue(attribute)
        }
        globalAttrs[NetCdfFileGuard.FINGERPRINT_ATTRIBUTE] = fingerprint
        return NetCdfFileRecord(
            filename = path.fileName.toString(),
            filePath = path.toString(),
            fileSize = fileSize,
            variables = variables,
            dimensions = dimensions,
            globalAttrs = globalAttrs,
        )
    }

    private fun inspectGroup(group: Group, depth: Int, budget: MetadataBudget) {
        val pending = ArrayDeque<Pair<Group, Int>>()
        pending.addLast(group to depth)
        while (pending.isNotEmpty()) {
            val (current, currentDepth) = pending.removeLast()
            if (currentDepth > MAX_GROUP_DEPTH) {
                throw NetCdfException.ResourceLimitExceeded("group-depth", MAX_GROUP_DEPTH, currentDepth.toLong())
            }
            budget.groupCount++
            budget.dimensionCount = try {
                Math.addExact(budget.dimensionCount, current.dimensions.size.toLong())
            } catch (_: ArithmeticException) {
                Long.MAX_VALUE
            }
            if (budget.groupCount > MAX_GROUP_COUNT) {
                throw NetCdfException.ResourceLimitExceeded("groups", MAX_GROUP_COUNT, budget.groupCount)
            }
            if (budget.dimensionCount > MAX_GROUP_DIMENSIONS) {
                throw NetCdfException.ResourceLimitExceeded(
                    "group-dimensions",
                    MAX_GROUP_DIMENSIONS,
                    budget.dimensionCount,
                )
            }
            budget.add(current.fullName)
            current.dimensions.forEach { dimension ->
                budget.add(dimension.shortName, dimension.length.toString())
            }
            current.variables.forEach { variable ->
                budget.variableCount++
                if (budget.variableCount > MAX_VARIABLES) {
                    throw NetCdfException.ResourceLimitExceeded("variables", MAX_VARIABLES, budget.variableCount)
                }
                val nameBytes = variable.fullName.toByteArray(StandardCharsets.UTF_8).size.toLong()
                if (nameBytes > MAX_VARIABLE_NAME_BYTES) {
                    throw NetCdfException.ResourceLimitExceeded(
                        "variable-name-bytes",
                        MAX_VARIABLE_NAME_BYTES,
                        nameBytes,
                    )
                }
                budget.add(variable.fullName)
                variable.attributes().forEach { attribute -> budget.add(attribute.shortName, attributeValue(attribute)) }
            }
            current.attributes().forEach { attribute -> budget.add(attribute.shortName, attributeValue(attribute)) }
            current.groups.forEach { child -> pending.addLast(child to (currentDepth + 1)) }
        }
    }

    private fun attributeValue(attribute: Attribute): String =
        attribute.stringValue ?: attribute.numericValue?.toString().orEmpty()

    private fun recordMetrics(inserted: Int, skipped: Int) {
        meterRegistry?.counter("netcdf.import.variable.records")?.increment(inserted.toDouble())
        if (skipped > 0) meterRegistry?.counter("netcdf.import.nan.skipped")?.increment(skipped.toDouble())
    }

    private fun recordRejection(exception: NetCdfException) {
        val reason = when (exception) {
            is NetCdfException.ResourceLimitExceeded -> "resource"
            is NetCdfException.UnsupportedCoordinateAxis,
            is NetCdfException.MissingCoordinate -> "axis"
            is NetCdfException.DuplicateCoordinate -> "duplicate"
            is NetCdfException.UnsupportedProjection -> "crs"
            is NetCdfException.FileChanged,
            is NetCdfException.FileOpen -> "path"
            is NetCdfException.CorruptProgress -> "progress"
            is NetCdfException.VariableNotFound,
            is NetCdfException.UnsupportedVariable,
            is NetCdfException.ImportAlreadyRunning,
            is NetCdfException.ImportLeaseLost,
            is NetCdfException.FileRecordNotFound -> return
        }
        meterRegistry?.counter("netcdf.import.rejected", "reason", reason)?.increment()
    }

    private fun checkNotInterrupted() {
        if (Thread.currentThread().isInterrupted) throw InterruptedException("NetCDF import interrupted")
    }

    private fun isMissing(raw: Double, fillValue: Double?): Boolean {
        if (raw.isNaN()) return true
        if (fillValue != null &&
            abs(raw - fillValue) <= maxOf(abs(fillValue), 1.0) * FILL_VALUE_TOLERANCE
        ) return true
        return false
    }

    private data class PreparedImport(
        val fileId: Long,
        val variableName: String,
        val variable: Variable,
        val axisMap: VariableAxisMap,
        val layout: ImportLayout,
        val reprojector: CoordinateReprojector?,
        val coordinateReader: UcarCoordinateReader,
        val fillValue: Double?,
    )

    private data class ImportContext(
        val prepared: PreparedImport,
        val progressId: Long,
        val lease: ImportLease,
    )

    private data class ImportLease(var expiresAt: Instant)

    private class MetadataBudget {
        var bytes: Long = 0L
        var groupCount: Long = 0L
        var dimensionCount: Long = 0L
        var variableCount: Long = 0L

        fun add(vararg values: String) {
            bytes = try {
                values.fold(bytes) { total, value ->
                    Math.addExact(total, value.toByteArray(StandardCharsets.UTF_8).size.toLong())
                }
            } catch (_: ArithmeticException) {
                Long.MAX_VALUE
            }
            if (bytes > MAX_METADATA_BYTES) {
                throw NetCdfException.ResourceLimitExceeded("metadata-bytes", MAX_METADATA_BYTES, bytes)
            }
        }
    }

    private data class ImportLayout(
        val isRankOne: Boolean,
        val hasSpatialGrid: Boolean,
        val timeDim: Int?,
        val levelDim: Int?,
        val rowDim: Int,
        val columnDim: Int,
        val rowCount: Int,
        val columnCount: Int,
        val timeCount: Long,
        val levelCount: Long,
        val totalSlices: Long,
    ) {
        companion object {
            fun create(variable: Variable, map: VariableAxisMap): ImportLayout {
                val shape = variable.shape
                val hasLat = map.latAxis != null
                val hasLon = map.lonAxis != null
                if (hasLat != hasLon) throw NetCdfException.MissingCoordinate(if (hasLat) "lon" else "lat")
                if (!hasLat) {
                    val timeDim = map.timeDim
                    if (variable.rank != 1 || timeDim == null) {
                        throw NetCdfException.MissingCoordinate("lat/lon")
                    }
                    val time = shape[timeDim].toLong()
                    checkDimension(time, "time")
                    if (time > MAX_CELLS) {
                        throw NetCdfException.ResourceLimitExceeded("cells", MAX_CELLS, time)
                    }
                    return ImportLayout(true, false, timeDim, null, 0, 0, 1, 1, time, 1L, 1L)
                }
                val rowDim = map.gridRowDim ?: throw NetCdfException.MissingCoordinate("lat")
                val columnDim = map.gridColumnDim ?: throw NetCdfException.MissingCoordinate("lon")
                if (rowDim == columnDim || rowDim !in shape.indices || columnDim !in shape.indices) {
                    throw NetCdfException.UnsupportedCoordinateAxis(variable.fullName, "lat/lon", "spatial-dimensions")
                }
                val timeDim = map.timeDim
                val levelDim = map.levelDim
                if (variable.rank == MAX_SPATIAL_RANK && levelDim == null) {
                    throw NetCdfException.MissingCoordinate("level")
                }
                val used = buildSet {
                    add(rowDim)
                    add(columnDim)
                    timeDim?.let(::add)
                    levelDim?.let(::add)
                }
                if (used.size != variable.rank) {
                    throw NetCdfException.UnsupportedCoordinateAxis(variable.fullName, null, "unmapped-data-dimension")
                }
                val rows = shape[rowDim].toLong()
                val columns = shape[columnDim].toLong()
                val time = timeDim?.let { shape[it].toLong() } ?: 1L
                val level = levelDim?.let { shape[it].toLong() } ?: 1L
                val cells = checkedProduct(rows, columns)
                if (cells > MAX_CELLS) throw NetCdfException.ResourceLimitExceeded("cells", MAX_CELLS, cells)
                checkDimension(time, "time")
                checkDimension(level, "level")
                val slices = checkedProduct(time, level)
                if (slices > MAX_SLICES) throw NetCdfException.ResourceLimitExceeded("slices", MAX_SLICES, slices)
                val totalCells = checkedProduct(cells, slices)
                if (totalCells > MAX_CELLS) {
                    throw NetCdfException.ResourceLimitExceeded("total-cells", MAX_CELLS, totalCells)
                }
                return ImportLayout(
                    false,
                    true,
                    timeDim,
                    levelDim,
                    rowDim,
                    columnDim,
                    rows.toIntExact("rows"),
                    columns.toIntExact("columns"),
                    time,
                    level,
                    slices,
                )
            }

            private fun checkDimension(value: Long, name: String) {
                if (value <= 0L || value > Int.MAX_VALUE) {
                    throw NetCdfException.ResourceLimitExceeded(name, Int.MAX_VALUE.toLong(), value)
                }
            }

            private fun Long.toIntExact(resource: String): Int = try {
                Math.toIntExact(this)
            } catch (_: ArithmeticException) {
                throw NetCdfException.ResourceLimitExceeded(resource, Int.MAX_VALUE.toLong(), this)
            }
        }
    }
}
