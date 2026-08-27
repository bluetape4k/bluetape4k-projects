package io.bluetape4k.science.exposed.service.internal

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.science.exposed.NetCdfException
import ucar.ma2.DataType
import ucar.nc2.Variable
import ucar.nc2.constants.AxisType
import ucar.nc2.dataset.CoordinateAxis
import ucar.nc2.dataset.NetcdfDataset

private val NUMERIC_DATA_TYPES: Set<DataType> = setOf(
    DataType.BYTE,
    DataType.UBYTE,
    DataType.SHORT,
    DataType.USHORT,
    DataType.INT,
    DataType.UINT,
    DataType.LONG,
    DataType.ULONG,
    DataType.FLOAT,
    DataType.DOUBLE,
)

private fun Variable.isNumericCoordinate(): Boolean = dataType in NUMERIC_DATA_TYPES

/** 변수의 데이터 dimension과 CF 좌표축의 의미를 연결한 immutable map입니다. */
internal data class VariableAxisMap(
    val timeAxis: AxisBinding? = null,
    val levelAxis: AxisBinding? = null,
    val latAxis: AxisBinding? = null,
    val lonAxis: AxisBinding? = null,
    val auxiliaryAxes: List<AxisBinding> = emptyList(),
    val gridRowDim: Int? = latAxis?.dimensionIndices?.firstOrNull(),
    val gridColumnDim: Int? = lonAxis?.dimensionIndices?.lastOrNull(),
) {

    /** 기존 rank별 import 코드가 사용하는 의미 dimension index입니다. */
    val timeDim: Int? get() = timeAxis?.dimensionIndices?.singleOrNull()
    val levelDim: Int? get() = levelAxis?.dimensionIndices?.singleOrNull()
    val latDim: Int? get() = gridRowDim
    val lonDim: Int? get() = gridColumnDim

    /** 축 하나의 이름·원본 axis·변수 dimension 순서입니다. */
    internal data class AxisBinding(
        val name: String,
        val axis: Variable,
        val dimensionNames: List<String>,
        val dimensionIndices: IntArray,
    ) {
        val isTwoDimensional: Boolean get() = dimensionNames.size == 2
        val shortName: String get() = axis.shortName
    }

    companion object: KLogging() {

        val LEVEL_AXIS_NAME_FALLBACKS: Set<String> = setOf(
            "level", "lev", "plev", "pressure", "depth", "z", "height",
        )
        val LAT_AXIS_NAME_FALLBACKS: Set<String> = setOf("lat", "latitude", "y", "rlat")
        val LON_AXIS_NAME_FALLBACKS: Set<String> = setOf("lon", "longitude", "x", "rlon")
        val TIME_AXIS_NAME_FALLBACKS: Set<String> = setOf("time", "t")

        /**
         * axis type, CF `standard_name`, units/name fallback 순서로 좌표축을 해석합니다.
         * 2D 축은 원본 축의 row/column dimension 순서를 그대로 보존합니다.
         */
        fun build(variable: Variable, dataset: NetcdfDataset): VariableAxisMap {
            val dimensions = variable.dimensions
            val dimensionNames = dimensions.map { it.shortName }
            val tokens = variable.findAttributeString("coordinates", "")
                .trim()
                .split(Regex("\\s+"))
                .filter { it.isNotBlank() }
            if (tokens.size.toLong() > MAX_COORDINATE_TOKENS) {
                throw NetCdfException.ResourceLimitExceeded(
                    "coordinate-tokens",
                    MAX_COORDINATE_TOKENS,
                    tokens.size.toLong(),
                )
            }

            val allVariables = (dataset.variables + dataset.coordinateAxes)
                .distinctBy { it.fullName }
            val tokenVariables = tokens.map { token ->
                val exactMatches = allVariables.filter { it.fullName == token }
                val shortMatches = if (exactMatches.isEmpty()) {
                    allVariables.filter { it.shortName == token }
                } else {
                    emptyList()
                }
                val matches = exactMatches.ifEmpty { shortMatches }
                val resolved = when {
                    matches.size == 1 -> matches.single()
                    matches.size > 1 -> throw NetCdfException.UnsupportedCoordinateAxis(
                        variable.fullName,
                        token,
                        "ambiguous-coordinate",
                    )
                    else -> dataset.findVariable(token)
                } ?: throw NetCdfException.UnsupportedCoordinateAxis(
                    variable.fullName,
                    token,
                    "unresolved-coordinate",
                )
                if (!resolved.isNumericCoordinate()) {
                    throw NetCdfException.UnsupportedCoordinateAxis(variable.fullName, token, "non-numeric")
                }
                resolved
            }
            val axes = (dataset.coordinateAxes + tokenVariables)
                .asSequence()
                .distinctBy { it.fullName }
                .toList()

            fun binding(axis: Variable): AxisBinding? {
                val axisDims = axis.dimensions.map { it.shortName }
                if (axisDims.isEmpty() || axisDims.size > 2) {
                    if (tokenVariables.any { it.fullName == axis.fullName }) {
                        throw NetCdfException.UnsupportedCoordinateAxis(
                            variable.fullName,
                            axis.shortName,
                            "auxiliary-rank",
                        )
                    }
                    return null
                }
                val indices = axisDims.map { axisName -> dimensionNames.indexOf(axisName) }
                if (indices.any { it < 0 }) return null
                if (axisDims.indices.any { axisIndex ->
                        axis.shape[axisIndex].toLong() != variable.shape[indices[axisIndex]].toLong()
                    }
                ) {
                    throw NetCdfException.UnsupportedCoordinateAxis(
                        variable.fullName,
                        axis.shortName,
                        "axis-shape-mismatch",
                    )
                }
                return AxisBinding(axis.fullName, axis, axisDims, indices.toIntArray())
            }

            val bindings = axes.mapNotNull(::binding)
            val tokenBindings = tokenVariables.map { tokenVariable ->
                bindings.firstOrNull { it.axis.fullName == tokenVariable.fullName }
                    ?: throw NetCdfException.UnsupportedCoordinateAxis(
                        variable.fullName,
                        tokenVariable.fullName,
                        "unsupported-coordinate",
                    )
            }
            fun applicableRoles(binding: AxisBinding): List<AxisRole> {
                val axis = binding.axis
                val name = binding.shortName.lowercase()
                val standardName = axis.findAttributeString("standard_name", "")?.lowercase().orEmpty()
                val units = axis.findAttributeString("units", "").lowercase()
                return buildList {
                    // CF surface altitude is a data auxiliary, not a vertical
                    // coordinate axis. UCAR may classify it as Height from its
                    // standard name, so keep it available for `coordinates=`.
                    if (standardName == "surface_altitude") return@buildList
                    if ((axis as? CoordinateAxis)?.axisType == AxisType.Time || standardName == "time" ||
                        name in TIME_AXIS_NAME_FALLBACKS || units.contains(" since ")
                    ) add(AxisRole.TIME)
                    if ((axis as? CoordinateAxis)?.axisType == AxisType.Lat ||
                        (axis as? CoordinateAxis)?.axisType == AxisType.GeoY ||
                        standardName == "latitude" || standardName == "grid_latitude" ||
                        units.contains("degrees_north") || name in LAT_AXIS_NAME_FALLBACKS
                    ) add(AxisRole.LAT)
                    if ((axis as? CoordinateAxis)?.axisType == AxisType.Lon ||
                        (axis as? CoordinateAxis)?.axisType == AxisType.GeoX ||
                        standardName == "longitude" || standardName == "grid_longitude" ||
                        units.contains("degrees_east") || name in LON_AXIS_NAME_FALLBACKS
                    ) add(AxisRole.LON)
                    if ((axis as? CoordinateAxis)?.axisType == AxisType.Pressure ||
                        (axis as? CoordinateAxis)?.axisType == AxisType.Height ||
                        (axis as? CoordinateAxis)?.axisType == AxisType.GeoZ ||
                        name in LEVEL_AXIS_NAME_FALLBACKS
                    ) add(AxisRole.LEVEL)
                }
            }

            fun roleOf(binding: AxisBinding): AxisRole? {
                val roles = applicableRoles(binding)
                return when (roles.size) {
                    0 -> null
                    1 -> roles.single()
                    else -> throw NetCdfException.UnsupportedCoordinateAxis(
                        variable.fullName,
                        binding.name,
                        "role-collision:${roles.joinToString(",") { it.name.lowercase() }}",
                    )
                }
            }

            fun candidates(role: AxisRole): List<AxisBinding> = bindings.filter { roleOf(it) == role }

            fun choose(role: AxisRole): AxisBinding? {
                val roleCandidates = candidates(role)
                if (roleCandidates.isEmpty()) return null
                val tokenCandidate = tokenVariables.asSequence()
                    .mapNotNull { tokenVariable ->
                        roleCandidates.firstOrNull { it.axis.fullName == tokenVariable.fullName }
                    }
                    .distinctBy { it.axis.fullName }
                    .toList()
                val chosen = when {
                    tokenCandidate.size == 1 -> tokenCandidate.single()
                    tokenCandidate.size > 1 -> throw ambiguous(variable, role, tokenCandidate)
                    roleCandidates.size == 1 -> roleCandidates.single()
                    else -> {
                        val named = roleCandidates.filter { it.shortName.lowercase() in role.fallbacks }
                        when (named.size) {
                            1 -> named.single()
                            else -> throw ambiguous(variable, role, roleCandidates)
                        }
                    }
                }
                if (!chosen.axis.isNumericCoordinate()) {
                    throw NetCdfException.UnsupportedCoordinateAxis(variable.fullName, chosen.name, "non-numeric")
                }
                return chosen
            }

            val time = choose(AxisRole.TIME)
            val level = choose(AxisRole.LEVEL)
            val lat = choose(AxisRole.LAT)
            val lon = choose(AxisRole.LON)

            if (time?.isTwoDimensional == true || level?.isTwoDimensional == true) {
                val offending = time ?: level
                throw NetCdfException.UnsupportedCoordinateAxis(
                    variable.fullName,
                    offending?.name,
                    "time-level-rank",
                )
            }

            if (lat != null && lon != null && lat.isTwoDimensional != lon.isTwoDimensional) {
                throw NetCdfException.UnsupportedCoordinateAxis(variable.fullName, lat.name, "mixed-rank-spatial-axes")
            }
            if (lat != null && lon != null && lat.isTwoDimensional &&
                lat.dimensionNames != lon.dimensionNames
            ) {
                throw NetCdfException.UnsupportedCoordinateAxis(variable.fullName, lon.name, "spatial-dimension-order")
            }

            val auxiliaries = tokenBindings.asSequence()
                .filter { candidate ->
                    candidate.name != lat?.name && candidate.name != lon?.name &&
                        candidate.name != time?.name && candidate.name != level?.name
                }
                .distinctBy { it.name }
                .onEach { candidate ->
                    if (candidate.dimensionNames.size !in 1..2) {
                        throw NetCdfException.UnsupportedCoordinateAxis(
                            variable.fullName,
                            candidate.name,
                            "auxiliary-rank",
                        )
                    }
                    if (lat != null && candidate.dimensionIndices.any { index ->
                            index != lat.dimensionIndices.firstOrNull() && index != lon?.dimensionIndices?.lastOrNull()
                        }
                    ) {
                        throw NetCdfException.UnsupportedCoordinateAxis(
                            variable.fullName,
                            candidate.name,
                            "auxiliary-dimension-subset",
                        )
                    }
                }
                .toList()

            if (auxiliaries.size.toLong() > MAX_AUXILIARY_AXES) {
                throw NetCdfException.ResourceLimitExceeded(
                    "auxiliary-axes",
                    MAX_AUXILIARY_AXES,
                    auxiliaries.size.toLong(),
                )
            }

            val rowDim = when {
                lat?.isTwoDimensional == true -> lat.dimensionIndices.first()
                lat != null -> lat.dimensionIndices.single()
                else -> null
            }
            val columnDim = when {
                lon?.isTwoDimensional == true -> lon.dimensionIndices.last()
                lon != null -> lon.dimensionIndices.single()
                else -> null
            }
            log.debug {
                "axis map built — variable=${variable.fullName} time=${time?.name} level=${level?.name} " +
                    "lat=${lat?.name} lon=${lon?.name} auxiliary=${auxiliaries.map { it.name }}"
            }
            return VariableAxisMap(time, level, lat, lon, auxiliaries, rowDim, columnDim)
        }

        private fun ambiguous(variable: Variable, role: AxisRole, candidates: List<AxisBinding>): NetCdfException =
            NetCdfException.UnsupportedCoordinateAxis(
                variable.fullName,
                candidates.joinToString(",") { it.name },
                "ambiguous-${role.name.lowercase()}",
            )
    }

    private enum class AxisRole(val fallbacks: Set<String>) {
        TIME(TIME_AXIS_NAME_FALLBACKS),
        LEVEL(LEVEL_AXIS_NAME_FALLBACKS),
        LAT(LAT_AXIS_NAME_FALLBACKS),
        LON(LON_AXIS_NAME_FALLBACKS),
    }
}
