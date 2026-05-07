package io.bluetape4k.science.exposed.service.internal

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.science.exposed.NetCdfException
import org.locationtech.proj4j.CRSFactory
import org.locationtech.proj4j.CoordinateTransformFactory
import org.locationtech.proj4j.ProjCoordinate
import ucar.nc2.Variable
import ucar.nc2.dataset.CoordinateAxis1D
import ucar.nc2.dataset.NetcdfDataset

/**
 * NetCDF 좌표축을 EPSG:4326 (WGS84) 으로 재투영해 캐싱하는 헬퍼.
 *
 * 파일당 1회 계산, 이후 셀 순회에서 [pointAt] 으로 O(1) 조회.
 *
 * 두 모드:
 * - [Geographic] : EPSG:4326 / 4269 등 lat/lon 분리 가능 CRS. 1D 배열 캐싱 (lonValues × latValues)
 * - [Projected]  : EPSG:3857 / UTM / Polar 등 projected CRS. 격자 cell 단위 (lat, lon) pair 사전 계산 후 flat 배열 캐싱 (Codex C4)
 *
 * Thread-safety: 단일 import 호출 수명 내 단일 스레드 사용 가정. 내부 `DoubleArray` 는 immutable snapshot.
 *
 * @see io.bluetape4k.science.exposed.service.NetCdfCatalogService
 */
internal sealed class CoordinateReprojector {

    /**
     * (latIdx, lonIdx) 격자 좌표를 EPSG:4326 의 (lon, lat) 으로 변환.
     *
     * @return `Pair<lon, lat>` — PostGIS POINT 순서 (R4)
     */
    abstract fun pointAt(latIdx: Int, lonIdx: Int): Pair<Double, Double>

    /** 진단·로그용 source CRS */
    abstract val sourceCrs: String

    /**
     * Geographic CRS 전용 (lat/lon 분리 가능 — EPSG:4326 / EPSG:4269).
     *
     * lonValues / latValues 를 그대로 캐싱.
     */
    class Geographic(
        private val lonValues: DoubleArray,
        private val latValues: DoubleArray,
        override val sourceCrs: String,
    ): CoordinateReprojector() {
        override fun pointAt(latIdx: Int, lonIdx: Int): Pair<Double, Double> =
            lonValues[lonIdx] to latValues[latIdx]
    }

    /**
     * Projected CRS 전용 (UTM / Polar / Web Mercator).
     *
     * 격자 cell 단위 `(x[lonIdx], y[latIdx])` 를 EPSG:4326 (lat, lon) 으로 사전 변환해 캐싱.
     *
     * @param projected flat 배열 — `[(latIdx * lonCount + lonIdx) * 2]` = lon, `[... + 1]` = lat
     */
    class Projected(
        private val projected: DoubleArray,
        private val lonCount: Int,
        override val sourceCrs: String,
    ): CoordinateReprojector() {
        override fun pointAt(latIdx: Int, lonIdx: Int): Pair<Double, Double> {
            val base = (latIdx * lonCount + lonIdx) * 2
            return projected[base] to projected[base + 1]
        }
    }

    companion object: KLogging() {

        /** EPSG:4326 (WGS84) — 모든 재투영의 target. */
        const val WGS84: String = "EPSG:4326"

        /** proj4j factory — thread-safe, 내부 EPSG hsql DB 캐시 활용 위해 재사용 (M2). */
        private val crsFactory: CRSFactory = CRSFactory()
        private val transformFactory: CoordinateTransformFactory = CoordinateTransformFactory()

        /**
         * 화이트리스트된 source CRS (proj4j-epsg 가 해석 가능한 EPSG 코드).
         *
         * - Geographic: 4326 (WGS84), 4269 (NAD83)
         * - Projected:
         *   - 3857 (Web Mercator)
         *   - 32601~32660 (UTM 북반구 zone 1~60)
         *   - 32701~32760 (UTM 남반구 zone 1~60) — Codex Plan v2 High#3
         *   - 3413 (NSIDC Polar Stereographic North)
         *   - 3031 (Antarctic Polar Stereographic)
         */
        val SUPPORTED_CRS: Set<String> = buildSet {
            add("EPSG:4326")
            add("EPSG:4269")
            add("EPSG:3857")
            (32601..32660).forEach { add("EPSG:$it") }
            (32701..32760).forEach { add("EPSG:$it") }
            add("EPSG:3413")
            add("EPSG:3031")
        }

        /** Geographic CRS 인지 (EPSG:4326 / 4269) */
        private fun isGeographic(crs: String): Boolean = crs in setOf("EPSG:4326", "EPSG:4269")

        /**
         * source CRS 를 탐지한다.
         *
         * 우선순위:
         * 1. variable 의 `grid_mapping` 속성 → 참조된 grid_mapping 변수의 `epsg_code` 또는 `grid_mapping_name`
         * 2. lat/lon axis 가 있으면 EPSG:4326 (default)
         *
         * 미지원 CRS 면 [NetCdfException.UnsupportedProjection] throw.
         */
        private fun detectSourceCrs(variable: Variable, dataset: NetcdfDataset, axisMap: VariableAxisMap): String {
            val gridMapping = variable.findAttribute("grid_mapping")?.stringValue
            if (gridMapping != null) {
                val mappingVar = dataset.findVariable(gridMapping)
                val epsgCode = mappingVar?.findAttribute("epsg_code")?.numericValue?.toInt()
                if (epsgCode != null) return "EPSG:$epsgCode"
                // grid_mapping_name 이 있으면 보수적으로 latlon 만 인식
                val name = mappingVar?.findAttribute("grid_mapping_name")?.stringValue
                if (name == "latitude_longitude") return "EPSG:4326"
                if (name != null) {
                    throw NetCdfException.UnsupportedProjection(name)
                }
            }
            // grid_mapping 없으면 axisMap 의 lat/lon 존재 여부로 EPSG:4326 추정
            return if (axisMap.latDim != null && axisMap.lonDim != null) WGS84
            else throw NetCdfException.UnsupportedProjection("unknown")
        }

        /**
         * Reprojector 를 빌드한다.
         *
         * 1. [detectSourceCrs] 로 source CRS 결정
         * 2. 화이트리스트 검사 → 외이면 [NetCdfException.UnsupportedProjection]
         * 3. Geographic 이면 lat/lon 1D 배열 그대로 캐싱
         * 4. Projected 이면 격자 cell 단위 2D pair 변환
         */
        fun from(
            variable: Variable,
            dataset: NetcdfDataset,
            axisMap: VariableAxisMap,
        ): CoordinateReprojector {
            // 좌표축 누락은 CRS 미지원 보다 우선 검증 (Spec §3.3 — typed exception 명확성)
            val latDim = axisMap.latDim ?: throw NetCdfException.MissingCoordinate("lat")
            val lonDim = axisMap.lonDim ?: throw NetCdfException.MissingCoordinate("lon")

            val srcCrs = detectSourceCrs(variable, dataset, axisMap)
            if (srcCrs !in SUPPORTED_CRS) {
                throw NetCdfException.UnsupportedProjection(srcCrs)
            }

            val latAxis = dataset.findCoordinateAxis(variable.getDimension(latDim).shortName) as? CoordinateAxis1D
                ?: throw NetCdfException.MissingCoordinate("lat (CoordinateAxis1D required)")
            val lonAxis = dataset.findCoordinateAxis(variable.getDimension(lonDim).shortName) as? CoordinateAxis1D
                ?: throw NetCdfException.MissingCoordinate("lon (CoordinateAxis1D required)")

            return if (isGeographic(srcCrs)) {
                Geographic(
                    lonValues = lonAxis.coordValues.copyOf(),
                    latValues = latAxis.coordValues.copyOf(),
                    sourceCrs = srcCrs,
                )
            } else {
                buildProjected(latAxis, lonAxis, srcCrs)
            }
        }

        /**
         * Projected CRS 를 격자 cell 단위로 EPSG:4326 으로 변환.
         *
         * 메모리 비용: lat × lon × 2 doubles (예: 721 × 1440 × 16B = 16MB).
         */
        private fun buildProjected(
            latAxis: CoordinateAxis1D,
            lonAxis: CoordinateAxis1D,
            srcCrs: String,
        ): Projected {
            val sourceCrsObj = try {
                crsFactory.createFromName(srcCrs)
            } catch (e: Exception) {
                throw NetCdfException.UnsupportedProjection(srcCrs, e)
            }
            val targetCrsObj = crsFactory.createFromName(WGS84)
            val transform = transformFactory.createTransform(sourceCrsObj, targetCrsObj)

            val latN = latAxis.size.toInt()
            val lonN = lonAxis.size.toInt()
            val flat = DoubleArray(latN * lonN * 2)

            val src = ProjCoordinate()
            val dst = ProjCoordinate()
            for (i in 0 until latN) {
                val y = latAxis.getCoordValue(i)
                for (j in 0 until lonN) {
                    val x = lonAxis.getCoordValue(j)
                    src.setValue(x, y)
                    transform.transform(src, dst)
                    val base = (i * lonN + j) * 2
                    flat[base] = dst.x  // lon
                    flat[base + 1] = dst.y  // lat
                }
            }
            log.debug { "Projected reprojection done — srcCrs=$srcCrs gridSize=${latN}x${lonN}" }

            return Projected(projected = flat, lonCount = lonN, sourceCrs = srcCrs)
        }
    }
}
