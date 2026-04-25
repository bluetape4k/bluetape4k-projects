package io.bluetape4k.science.exposed.service.internal

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.science.exposed.NetCdfException
import ucar.nc2.Variable
import ucar.nc2.constants.AxisType
import ucar.nc2.dataset.CoordinateAxis1D
import ucar.nc2.dataset.NetcdfDataset

/**
 * NetCDF 변수의 차원 인덱스를 [AxisType] 별로 매핑한 결과.
 *
 * `rank` 가 아니라 dimension 별 의미로 매핑하므로, 비표준 dim order
 * (`[lat, lon, time]` 같은 CF 비준수 변수) 도 정상 처리할 수 있다 (Codex C5).
 *
 * 각 필드는 해당 axis 가 변수의 몇 번째 dimension 인지를 나타낸다 (없으면 null).
 *
 * @param timeDim  time 축 dimension 인덱스
 * @param levelDim level (height/depth/pressure) 축 dimension 인덱스
 * @param latDim   lat 축 dimension 인덱스
 * @param lonDim   lon 축 dimension 인덱스
 */
internal data class VariableAxisMap(
    val timeDim: Int? = null,
    val levelDim: Int? = null,
    val latDim: Int? = null,
    val lonDim: Int? = null,
) {

    companion object: KLogging() {

        /** level 축 이름 fallback 리스트 (CF 비준수 파일용 — Spec D4) */
        val LEVEL_AXIS_NAME_FALLBACKS: Set<String> = setOf(
            "level", "lev", "plev", "pressure", "depth", "z", "height",
        )

        /** lat 축 이름 fallback */
        val LAT_AXIS_NAME_FALLBACKS: Set<String> = setOf("lat", "latitude", "y", "rlat")

        /** lon 축 이름 fallback */
        val LON_AXIS_NAME_FALLBACKS: Set<String> = setOf("lon", "longitude", "x", "rlon")

        /** time 축 이름 fallback */
        val TIME_AXIS_NAME_FALLBACKS: Set<String> = setOf("time", "t")

        /**
         * [Variable] 의 각 dimension 을 순회하면서 [AxisType] 1차 + 이름 fallback 으로 매핑한다.
         *
         * 1. `dataset.findCoordinateAxis(dimensionName)` 으로 해당 dimension 의 [CoordinateAxis1D] 조회
         * 2. `axis.axisType` 으로 `Time / Lat / Lon / Pressure|Height|GeoZ` 판별
         * 3. 판별 실패 시 dimension 이름 기반 fallback
         * 4. `CoordinateAxis2D` 등 1D 가 아닌 축은 [NetCdfException.MissingCoordinate] throw (curvilinear 비지원)
         *
         * @return 매핑 결과. 매핑되지 않은 dimension 은 모든 필드가 null
         */
        fun build(variable: Variable, dataset: NetcdfDataset): VariableAxisMap {
            var timeDim: Int? = null
            var levelDim: Int? = null
            var latDim: Int? = null
            var lonDim: Int? = null

            for (dimIdx in 0 until variable.rank) {
                val dim = variable.getDimension(dimIdx)
                val dimName = dim.shortName ?: continue
                val axis = dataset.findCoordinateAxis(dimName)

                // Curvilinear (CoordinateAxis2D) 등 1D 가 아닌 축은 비지원
                if (axis != null && axis !is CoordinateAxis1D) {
                    log.debug { "non-1D axis detected — dim='$dimName' type=${axis::class.simpleName}" }
                    throw NetCdfException.MissingCoordinate(
                        "$dimName (CoordinateAxis2D / curvilinear is not supported)"
                    )
                }

                val axisType = axis?.axisType
                val resolved = when {
                    axisType == AxisType.Time -> AxisCategory.TIME
                    axisType == AxisType.Lat || axisType == AxisType.GeoY -> AxisCategory.LAT
                    axisType == AxisType.Lon || axisType == AxisType.GeoX -> AxisCategory.LON
                    axisType == AxisType.Pressure ||
                        axisType == AxisType.Height ||
                        axisType == AxisType.GeoZ -> AxisCategory.LEVEL

                    dimName.lowercase() in TIME_AXIS_NAME_FALLBACKS -> AxisCategory.TIME
                    dimName.lowercase() in LAT_AXIS_NAME_FALLBACKS -> AxisCategory.LAT
                    dimName.lowercase() in LON_AXIS_NAME_FALLBACKS -> AxisCategory.LON
                    dimName.lowercase() in LEVEL_AXIS_NAME_FALLBACKS -> AxisCategory.LEVEL
                    else -> null
                }

                when (resolved) {
                    AxisCategory.TIME -> timeDim = dimIdx
                    AxisCategory.LEVEL -> levelDim = dimIdx
                    AxisCategory.LAT -> latDim = dimIdx
                    AxisCategory.LON -> lonDim = dimIdx
                    null -> log.debug { "unmapped dimension: name='$dimName' axisType=$axisType" }
                }
            }

            return VariableAxisMap(
                timeDim = timeDim,
                levelDim = levelDim,
                latDim = latDim,
                lonDim = lonDim,
            )
        }
    }

    /** [build] 내부 분기 라벨 */
    private enum class AxisCategory { TIME, LEVEL, LAT, LON }
}
