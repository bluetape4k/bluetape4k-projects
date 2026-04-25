package io.bluetape4k.science.exposed.service

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.science.exposed.NetCdfException
import io.bluetape4k.science.exposed.model.NetCdfFileRecord
import io.bluetape4k.science.exposed.model.NetCdfImportStatus
import io.bluetape4k.science.exposed.model.NetCdfVariableInfo
import io.bluetape4k.science.exposed.repository.NetCdfFileRepository
import io.bluetape4k.science.exposed.repository.NetCdfImportProgressRepository
import io.bluetape4k.science.exposed.service.internal.CoordinateReprojector
import io.bluetape4k.science.exposed.service.internal.VariableAxisMap
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import ucar.ma2.Array as UcarArray
import ucar.nc2.NetcdfFiles
import ucar.nc2.Variable
import ucar.nc2.dataset.NetcdfDataset
import ucar.nc2.dataset.NetcdfDatasets
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant

/**
 * NetCDF 파일 등록 및 격자 값 임포트를 담당하는 서비스입니다.
 *
 * ## 주요 기능
 *
 * - [registerFile] : NetCDF 파일 메타데이터를 DB 에 등록합니다.
 * - [importGridValues] : NetCDF 변수의 격자 값을 슬라이스 단위로 DB 에 임포트합니다.
 *   - rank 1 (시계열) ~ rank 4 (time × level × lat × lon) 지원
 *   - heartbeat lease 기반 동시성 제어 + sliceIdx 기반 재개
 *   - proj4j 기반 비 WGS84 CRS 자동 재투영 (Geographic 1D / Projected 2D pair)
 *   - NaN / `_FillValue` 자동 skip
 *   - 선택적 Micrometer 계측
 *
 * ## 호출 컨텍스트
 *
 * blocking API. 호출자는 Spring Boot Virtual Thread executor 등에서 호출 권장 (VT pinning 위험은 Spec §2.4 R2 참조).
 *
 * ## CoordinateAxis2D 비지원
 *
 * curvilinear / rotated pole / tripolar grid 같은 [ucar.nc2.dataset.CoordinateAxis2D] 좌표축은 본 구현 스코프 외입니다.
 * 1D 가 아닌 lat/lon 축이 발견되면 [NetCdfException.MissingCoordinate] 가 발생합니다.
 *
 * @param fileRepo      파일 메타데이터 Repository
 * @param progressRepo  변수 단위 진행 상태 Repository
 * @param meterRegistry 선택 — null 이면 계측 no-op
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
    }

    /**
     * NetCDF 파일을 열어 메타데이터를 DB 에 등록합니다.
     *
     * Micrometer `netcdf.register.duration` Timer 로 경과 시간이 계측됩니다 (`status=success|failure` tag).
     *
     * 호출자가 [filePath] 의 신뢰성을 검증해야 합니다 (path traversal 등은 호출자 책임 — Spec R11).
     * 본 메서드는 blocking 으로 동작하므로 향후 `suspend` 변환 시 `runCatching` 대신 try/catch 로
     * `CancellationException` rethrow 처리 필요 (coding-style 규칙).
     *
     * @param filePath NetCDF 파일 전체 경로 (blank 금지)
     * @return 생성된 파일 레코드 ID
     * @throws IllegalArgumentException filePath 가 blank 일 때
     * @throws NetCdfException.FileOpen 파일을 열 수 없을 때
     */
    fun registerFile(filePath: String): Long {
        require(filePath.isNotBlank()) { "filePath must not be blank" }

        val sample = meterRegistry?.let { Timer.start(it) }
        var success = false

        try {
            val record = runCatching {
                NetcdfFiles.open(filePath).use { nc ->
                    val variables = nc.variables.map { v ->
                        NetCdfVariableInfo(
                            name = v.fullName,
                            dataType = v.dataType.name,
                            shape = v.shape.toList(),
                            attributes = v.attributes.associate { attr ->
                                attr.shortName to (attr.stringValue ?: attr.numericValue?.toString().orEmpty())
                            },
                        )
                    }
                    val dimensions = nc.dimensions.associate { it.shortName to it.length }
                    val globalAttrs = nc.globalAttributes.associate { attr ->
                        attr.shortName to (attr.stringValue ?: attr.numericValue?.toString().orEmpty())
                    }

                    val path = Paths.get(filePath)
                    NetCdfFileRecord(
                        filename = path.fileName.toString(),
                        filePath = filePath,
                        fileSize = runCatching { Files.size(path) }.getOrDefault(0L),
                        variables = variables,
                        dimensions = dimensions,
                        globalAttrs = globalAttrs,
                    )
                }
            }.getOrElse { throw NetCdfException.FileOpen(filePath, it) }

            val id = transaction { fileRepo.save(record).id }
            success = true
            log.info { "NetCDF file registered — id=$id path=$filePath vars=${record.variables.size}" }
            return id
        } finally {
            sample?.let { sampler ->
                meterRegistry?.let { registry ->
                    sampler.stop(
                        registry.timer(
                            "netcdf.register.duration",
                            "status", if (success) "success" else "failure",
                        )
                    )
                }
            }
        }
    }

    /**
     * NetCDF 파일의 지정된 변수의 격자 값을 DB 에 임포트합니다.
     *
     * ## 지원 rank
     *
     * - 1D (time)                       : timeIdx=t, levelIdx=0, location=null
     * - 2D (lat, lon)                   : timeIdx=0, levelIdx=0, location=Point
     * - 3D (time, lat, lon)             : timeIdx=t, levelIdx=0, location=Point
     * - 4D (time, level, lat, lon)      : timeIdx=t, levelIdx=k, location=Point
     *
     * ## 재개
     * `(fileId, variableName)` 단위 progress 테이블 + heartbeat lease (5분 TTL).
     * COMPLETED 면 즉시 no-op. FAILED / 만료된 IN_PROGRESS 면 `lastSliceIdx + 1` 부터 재개.
     *
     * ## CRS 재투영
     * proj4j 화이트리스트 (EPSG:4326/4269/3857/3031/3413/UTM 32601~32660·32701~32760) 외이면 [NetCdfException.UnsupportedProjection].
     *
     * ## NaN / `_FillValue`
     * 자동 skip + `netcdf.import.nan.skipped` counter 증가 + `log.debug`.
     *
     * @throws IllegalArgumentException variableName 이 blank 일 때
     * @throws NetCdfException.FileRecordNotFound DB 에 파일 레코드가 없을 때 (lease 미획득 → metric 미기록)
     * @throws NetCdfException.VariableNotFound 변수가 없을 때
     * @throws NetCdfException.UnsupportedVariable rank 가 1~4 외일 때
     * @throws NetCdfException.MissingCoordinate lat/lon/level 좌표축이 없거나 1D 가 아닐 때
     * @throws NetCdfException.UnsupportedProjection 화이트리스트 외 CRS 일 때
     * @throws NetCdfException.ImportAlreadyRunning 다른 프로세스가 활성 lease 보유 중일 때
     */
    fun importGridValues(fileId: Long, variableName: String) {
        require(variableName.isNotBlank()) { "variableName must not be blank" }

        // FileRecordNotFound 는 lease 획득 전 → markFailed/counter 호출 없이 raise (Codex Plan v2.1 Medium#4)
        val record = transaction { fileRepo.findByIdOrNull(fileId) }
            ?: throw NetCdfException.FileRecordNotFound(fileId)

        val progress = transaction { progressRepo.acquireLease(fileId, variableName, LEASE_TTL) }
        if (progress.status == NetCdfImportStatus.COMPLETED) {
            log.info { "import skipped — already completed: fileId=$fileId var=$variableName" }
            return
        }
        val startSliceIdx: Long = (progress.lastSliceIdx ?: -1L) + 1L
        if (progress.lastSliceIdx != null) {
            meterRegistry?.counter("netcdf.import.status", "status", "resumed")?.increment()
            log.info { "resuming import — fileId=$fileId var=$variableName startSliceIdx=$startSliceIdx" }
        }

        val dataset = runCatching { NetcdfDatasets.openDataset(record.filePath) }
            .getOrElse {
                transaction { progressRepo.markFailed(progress.id, it.message.orEmpty()) }
                meterRegistry?.counter("netcdf.import.status", "status", "failure")?.increment()
                throw NetCdfException.FileOpen(record.filePath, it)
            }

        try {
            dataset.use { ncd ->
                val v = ncd.findVariable(variableName)
                    ?: throw NetCdfException.VariableNotFound(fileId, variableName)

                if (v.rank !in 1..4) {
                    throw NetCdfException.UnsupportedVariable(variableName, v.rank)
                }

                val axisMap = VariableAxisMap.build(v, ncd)
                val ctx = ImportContext(
                    fileId = fileId,
                    variableName = variableName,
                    progressId = progress.id,
                    variable = v,
                    axisMap = axisMap,
                    fillValue = v.findAttribute("_FillValue")?.numericValue?.toDouble(),
                )

                when (v.rank) {
                    1 -> importRank1(ctx, startSliceIdx)
                    2 -> importRank2(ctx, ncd, startSliceIdx)
                    3 -> importRank3(ctx, ncd, startSliceIdx)
                    4 -> importRank4(ctx, ncd, startSliceIdx)
                }

                transaction { progressRepo.markCompleted(progress.id) }
                meterRegistry?.counter("netcdf.import.status", "status", "success")?.increment()
                log.info { "import COMPLETED — fileId=$fileId var=$variableName" }
            }
        } catch (e: NetCdfException.ImportAlreadyRunning) {
            // 다른 프로세스 소유 — progress 변경 없음, failure counter 미증가 (M4)
            throw e
        } catch (e: Exception) {
            transaction { progressRepo.markFailed(progress.id, e.message.orEmpty()) }
            meterRegistry?.counter("netcdf.import.status", "status", "failure")?.increment()
            throw e
        }
    }

    /**
     * rank=1 (time only) — location/level null, timeIdx=t.
     *
     * 단일 슬라이스: 모든 time step 을 한 번에 read.
     */
    private fun importRank1(ctx: ImportContext, startSliceIdx: Long) {
        if (startSliceIdx > 0L) return  // sliceIdx 는 0 단일이므로 재시작 무의미

        val data = ctx.variable.read()
        var inserted = 0
        var skipped = 0

        val sql = """
            INSERT INTO netcdf_grid_values (file_id, variable_name, location, time_idx, level_idx, value)
            VALUES (?, ?, NULL, ?, 0, ?)
            ON CONFLICT DO NOTHING
        """.trimIndent()

        transaction {
            val conn = connection.connection as java.sql.Connection
            conn.prepareStatement(sql).use { ps ->
                val iter = data.indexIterator
                var t = 0
                while (iter.hasNext()) {
                    val raw = iter.getDoubleNext()
                    if (isMissing(raw, ctx.fillValue)) {
                        skipped++
                    } else {
                        ps.setLong(1, ctx.fileId)
                        ps.setString(2, ctx.variableName)
                        ps.setInt(3, t)
                        ps.setDouble(4, raw)
                        ps.addBatch()
                        inserted++
                    }
                    t++
                }
                ps.executeBatch()
            }
            progressRepo.renewLease(ctx.progressId, lastSliceIdx = 0L, leaseTtl = LEASE_TTL)
        }
        meterRegistry?.counter("netcdf.import.variable.records", "variable", ctx.variableName)
            ?.increment(inserted.toDouble())
        if (skipped > 0) {
            meterRegistry?.counter("netcdf.import.nan.skipped")?.increment(skipped.toDouble())
        }
        log.debug { "rank1 imported — variable=${ctx.variableName} inserted=$inserted skipped=$skipped" }
    }

    /**
     * rank=2 (lat, lon) — 단일 슬라이스, timeIdx=0/levelIdx=0.
     */
    private fun importRank2(ctx: ImportContext, ncd: NetcdfDataset, startSliceIdx: Long) {
        if (startSliceIdx > 0L) return  // 단일 슬라이스

        // CoordinateReprojector.from 내부에서 latDim/lonDim 검증되므로 not-null 단정 (M4)
        val reprojector = CoordinateReprojector.from(ctx.variable, ncd, ctx.axisMap)
        val origin = IntArray(2)
        val shape = ctx.variable.shape
        val data = ctx.variable.read(origin, shape)

        val latDim = checkNotNull(ctx.axisMap.latDim) { "axisMap.latDim must not be null after CoordinateReprojector.from" }
        val lonDim = checkNotNull(ctx.axisMap.lonDim) { "axisMap.lonDim must not be null after CoordinateReprojector.from" }
        val latN = shape[latDim]
        val lonN = shape[lonDim]

        importSlice2D(
            ctx = ctx,
            data = data,
            reprojector = reprojector,
            latN = latN,
            lonN = lonN,
            timeIdxValue = 0,
            levelIdxValue = 0,
            sliceIdx = 0L,
        )
    }

    /**
     * rank=3 (time, lat, lon) — time 슬라이스별 1 tx. sliceIdx = timeIdx.
     */
    private fun importRank3(ctx: ImportContext, ncd: NetcdfDataset, startSliceIdx: Long) {
        val reprojector = CoordinateReprojector.from(ctx.variable, ncd, ctx.axisMap)
        val timeDim = ctx.axisMap.timeDim ?: throw NetCdfException.MissingCoordinate("time")
        val latDim = ctx.axisMap.latDim ?: throw NetCdfException.MissingCoordinate("lat")
        val lonDim = ctx.axisMap.lonDim ?: throw NetCdfException.MissingCoordinate("lon")
        val shape = ctx.variable.shape
        val timeN = shape[timeDim]
        val latN = shape[latDim]
        val lonN = shape[lonDim]
        var lastHeartbeat = Instant.now()
        var slicesSinceHeartbeat = 0

        for (t in startSliceIdx.toInt() until timeN) {
            val origin = IntArray(3).also { it[timeDim] = t }
            val sliceShape = IntArray(3).also { dims ->
                dims[timeDim] = 1
                dims[latDim] = latN
                dims[lonDim] = lonN
            }
            val data = ctx.variable.read(origin, sliceShape)
            slicesSinceHeartbeat++
            // heartbeat throttle: 마지막 슬라이스 또는 N 슬라이스마다 또는 30초 경과 시 lease 갱신
            val shouldRenew = (t == timeN - 1) ||
                slicesSinceHeartbeat >= HEARTBEAT_EVERY_SLICES ||
                Duration.between(lastHeartbeat, Instant.now()) >= HEARTBEAT_INTERVAL
            importSlice2D(
                ctx = ctx,
                data = data,
                reprojector = reprojector,
                latN = latN,
                lonN = lonN,
                timeIdxValue = t,
                levelIdxValue = 0,
                sliceIdx = t.toLong(),
                renewProgress = shouldRenew,
            )
            if (shouldRenew) {
                lastHeartbeat = Instant.now()
                slicesSinceHeartbeat = 0
            }
        }
    }

    /**
     * rank=4 (time, level, lat, lon) — (time, level) 슬라이스별 1 tx.
     *
     * sliceIdx = timeIdx × levelN + levelIdx (Codex C3 선형화).
     */
    private fun importRank4(ctx: ImportContext, ncd: NetcdfDataset, startSliceIdx: Long) {
        val reprojector = CoordinateReprojector.from(ctx.variable, ncd, ctx.axisMap)
        val timeDim = ctx.axisMap.timeDim ?: throw NetCdfException.MissingCoordinate("time")
        val levelDim = ctx.axisMap.levelDim ?: throw NetCdfException.MissingCoordinate("level")
        val latDim = ctx.axisMap.latDim ?: throw NetCdfException.MissingCoordinate("lat")
        val lonDim = ctx.axisMap.lonDim ?: throw NetCdfException.MissingCoordinate("lon")
        val shape = ctx.variable.shape
        val timeN = shape[timeDim]
        val levelN = shape[levelDim]
        val latN = shape[latDim]
        val lonN = shape[lonDim]
        val totalSlices = timeN.toLong() * levelN.toLong()
        var lastHeartbeat = Instant.now()
        var slicesSinceHeartbeat = 0

        for (sliceIdx in startSliceIdx until totalSlices) {
            val (t, l) = decomposeSliceIdx(sliceIdx, levelN)
            val origin = IntArray(4).also {
                it[timeDim] = t
                it[levelDim] = l
            }
            val sliceShape = IntArray(4).also { dims ->
                dims[timeDim] = 1
                dims[levelDim] = 1
                dims[latDim] = latN
                dims[lonDim] = lonN
            }
            val data = ctx.variable.read(origin, sliceShape)
            slicesSinceHeartbeat++
            val isLast = sliceIdx == totalSlices - 1L
            val shouldRenew = isLast ||
                slicesSinceHeartbeat >= HEARTBEAT_EVERY_SLICES ||
                Duration.between(lastHeartbeat, Instant.now()) >= HEARTBEAT_INTERVAL
            importSlice2D(
                ctx = ctx,
                data = data,
                reprojector = reprojector,
                latN = latN,
                lonN = lonN,
                timeIdxValue = t,
                levelIdxValue = l,
                sliceIdx = sliceIdx,
                renewProgress = shouldRenew,
            )
            if (shouldRenew) {
                lastHeartbeat = Instant.now()
                slicesSinceHeartbeat = 0
            }
        }
    }

    /**
     * 단일 (lat × lon) 슬라이스를 한 트랜잭션에서 insert + progress 갱신.
     *
     * 중복 방지: ON CONFLICT DO NOTHING — partial expression unique index 자동 매칭 (Spec §4.1 M4).
     * `geoPointOf` 헬퍼가 부재하므로 PostGIS `ST_SetSRID(ST_MakePoint(lon, lat), 4326)` 직접 사용.
     */
    private fun importSlice2D(
        ctx: ImportContext,
        data: UcarArray,
        reprojector: CoordinateReprojector,
        latN: Int,
        lonN: Int,
        timeIdxValue: Int,
        levelIdxValue: Int,
        sliceIdx: Long,
        renewProgress: Boolean = true,
    ): Pair<Int, Int> {
        var inserted = 0
        var skipped = 0
        val sliceTimer = meterRegistry?.let { Timer.start(it) }

        val sql = """
            INSERT INTO netcdf_grid_values (file_id, variable_name, location, time_idx, level_idx, value)
            VALUES (?, ?, ST_SetSRID(ST_MakePoint(?, ?), 4326), ?, ?, ?)
            ON CONFLICT DO NOTHING
        """.trimIndent()

        transaction {
            val conn = connection.connection as java.sql.Connection
            conn.prepareStatement(sql).use { ps ->
                val iter = data.indexIterator
                for (i in 0 until latN) {
                    for (j in 0 until lonN) {
                        if (!iter.hasNext()) break
                        val raw = iter.getDoubleNext()
                        if (isMissing(raw, ctx.fillValue)) {
                            skipped++
                            continue
                        }
                        val (lon, lat) = reprojector.pointAt(i, j)
                        ps.setLong(1, ctx.fileId)
                        ps.setString(2, ctx.variableName)
                        ps.setDouble(3, lon)
                        ps.setDouble(4, lat)
                        ps.setInt(5, timeIdxValue)
                        ps.setInt(6, levelIdxValue)
                        ps.setDouble(7, raw)
                        ps.addBatch()
                        inserted++
                    }
                }
                ps.executeBatch()
            }
            if (renewProgress) {
                progressRepo.renewLease(ctx.progressId, lastSliceIdx = sliceIdx, leaseTtl = LEASE_TTL)
            }
        }

        sliceTimer?.let { sampler ->
            meterRegistry?.let { registry ->
                sampler.stop(registry.timer("netcdf.import.slice.duration"))
            }
        }
        meterRegistry?.counter("netcdf.import.variable.records", "variable", ctx.variableName)
            ?.increment(inserted.toDouble())
        if (skipped > 0) {
            meterRegistry?.counter("netcdf.import.nan.skipped")?.increment(skipped.toDouble())
        }
        return inserted to skipped
    }

    /**
     * 선형 sliceIdx 를 (timeIdx, levelIdx) 로 분해. row-major 4D ordering.
     */
    private fun decomposeSliceIdx(sliceIdx: Long, levelN: Int): Pair<Int, Int> {
        val t = (sliceIdx / levelN).toInt()
        val l = (sliceIdx % levelN).toInt()
        return t to l
    }

    /**
     * NaN / `_FillValue` 일치 시 missing 으로 판정.
     *
     * `_FillValue` 가 Float 인 경우 Double 변환 시 LSB 차이로 strict equality 가 실패할 수 있어
     * 절대 오차 (`abs(raw - fillValue) <= max(|fillValue|, 1) * 1e-7`) 로 비교한다 (M1).
     * Float 의 7 자리 정밀도를 고려한 허용 오차.
     */
    private fun isMissing(raw: Double, fillValue: Double?): Boolean {
        if (raw.isNaN()) return true
        if (fillValue != null) {
            val tolerance = kotlin.math.max(kotlin.math.abs(fillValue), 1.0) * 1e-7
            if (kotlin.math.abs(raw - fillValue) <= tolerance) return true
        }
        return false
    }

    /**
     * import 컨텍스트 — slice 함수들에 공통으로 전달되는 immutable 데이터.
     */
    private data class ImportContext(
        val fileId: Long,
        val variableName: String,
        val progressId: Long,
        val variable: Variable,
        val axisMap: VariableAxisMap,
        val fillValue: Double?,
    )
}
