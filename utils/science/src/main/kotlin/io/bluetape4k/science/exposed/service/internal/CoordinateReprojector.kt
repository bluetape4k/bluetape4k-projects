package io.bluetape4k.science.exposed.service.internal

import io.bluetape4k.logging.KLogging
import io.bluetape4k.science.exposed.NetCdfException
import org.locationtech.proj4j.CRSFactory
import org.locationtech.proj4j.CoordinateTransform
import org.locationtech.proj4j.CoordinateTransformFactory
import org.locationtech.proj4j.ProjCoordinate
import ucar.nc2.Variable
import ucar.nc2.dataset.NetcdfDataset

/** 좌표축 값을 필요할 때 bounded window로 읽어 EPSG:4326 좌표를 반환합니다. */
internal sealed class CoordinateReprojector {

    /** (row, column) 셀의 `(longitude, latitude)`를 반환합니다. */
    abstract fun pointAt(latIdx: Int, lonIdx: Int): Pair<Double, Double>

    /** 현재 tile의 원본 좌표 window를 한 번 읽어 재사용하는 point provider입니다. */
    open fun tilePointProvider(
        rowOrigin: Int,
        columnOrigin: Int,
        rowCount: Int,
        columnCount: Int,
    ): (Int, Int) -> Pair<Double, Double> = { row, column -> pointAt(row, column) }

    /** 진단·로그용 source CRS */
    abstract val sourceCrs: String

    /** 기존 직접 배열 주입 호출을 위한 geographic 구현입니다. */
    class Geographic(
        private val lonValues: DoubleArray,
        private val latValues: DoubleArray,
        override val sourceCrs: String,
    ): CoordinateReprojector() {
        override fun pointAt(latIdx: Int, lonIdx: Int): Pair<Double, Double> =
            lonValues[lonIdx] to latValues[latIdx]
    }

    /** 기존 직접 배열 주입 호출을 위한 projected 구현입니다. */
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

    private class ReaderBacked(
        override val sourceCrs: String,
        private val pointReader: (Int, Int) -> Pair<Double, Double>,
        private val tileReader: ((Int, Int, Int, Int) -> (Int, Int) -> Pair<Double, Double>)? = null,
    ): CoordinateReprojector() {
        override fun pointAt(latIdx: Int, lonIdx: Int): Pair<Double, Double> = pointReader(latIdx, lonIdx)

        override fun tilePointProvider(
            rowOrigin: Int,
            columnOrigin: Int,
            rowCount: Int,
            columnCount: Int,
        ): (Int, Int) -> Pair<Double, Double> =
            tileReader?.invoke(rowOrigin, columnOrigin, rowCount, columnCount)
                ?: super.tilePointProvider(rowOrigin, columnOrigin, rowCount, columnCount)
    }

    companion object: KLogging() {

        const val WGS84: String = "EPSG:4326"
        private val crsFactory = CRSFactory()
        private val transformFactory = CoordinateTransformFactory()

        val SUPPORTED_CRS: Set<String> = buildSet {
            add("EPSG:4326")
            add("EPSG:4269")
            add("EPSG:3857")
            (32601..32660).forEach { add("EPSG:$it") }
            (32701..32760).forEach { add("EPSG:$it") }
            add("EPSG:3413")
            add("EPSG:3031")
        }

        private val SUPPORTED_GRID_MAPPING_NAMES: Set<String> = setOf(
            "latitude_longitude",
            "transverse_mercator",
            "mercator",
            "polar_stereographic",
            "stereographic",
            "lambert_conformal_conic",
            "lambert_azimuthal_equal_area",
            "albers_conical_equal_area",
        )

        private val SPATIAL_REF_EPSG_PATTERN = Regex(
            """EPSG:([0-9]+)(?![0-9A-Za-z.+-])""",
            RegexOption.IGNORE_CASE,
        )
        private val SPATIAL_REF_AUTHORITY_EPSG_PATTERN = Regex(
            """AUTHORITY\s*\[\s*[\"']EPSG[\"']\s*,\s*(?:[\"']([0-9]+)[\"']|([0-9]+))\s*\]""",
            RegexOption.IGNORE_CASE,
        )
        private val SPATIAL_REF_ID_EPSG_PATTERN = Regex(
            """ID\s*\[\s*[\"']EPSG[\"']\s*,\s*(?:[\"']([0-9]+)[\"']|([0-9]+))\s*\]""",
            RegexOption.IGNORE_CASE,
        )
        private val SPATIAL_REF_EPSG_MARKER_PATTERN = Regex(
            """(?<![0-9A-Za-z])EPSG:|(?:AUTHORITY|ID)\s*\[\s*[\"']EPSG[\"']\s*,""",
            RegexOption.IGNORE_CASE,
        )

        private fun isGeographic(crs: String): Boolean = crs == "EPSG:4326" || crs == "EPSG:4269"

        /** variable/grid_mapping 속성에서 strict ASCII EPSG 코드를 해석합니다. */
        private fun detectSourceCrs(variable: Variable, dataset: NetcdfDataset, axisMap: VariableAxisMap): String {
            val mappingAttribute = variable.findAttribute("grid_mapping")
            if (mappingAttribute != null) {
                val mappingName = mappingAttribute.stringValue?.trim()
                    ?: throw NetCdfException.UnsupportedProjection("grid_mapping")
                if (mappingName.isEmpty()) {
                    throw NetCdfException.UnsupportedProjection("grid_mapping")
                }
                val mapping = dataset.findVariable(mappingName)
                    ?: throw NetCdfException.UnsupportedProjection(mappingName)
                val gridMappingAttribute = mapping.findAttribute("grid_mapping_name")
                val gridMappingName = gridMappingAttribute?.stringValue?.trim()
                    ?: if (gridMappingAttribute == null) null else
                    throw NetCdfException.UnsupportedProjection("grid_mapping_name")
                if (gridMappingName.isNullOrEmpty() && gridMappingAttribute != null) {
                    throw NetCdfException.UnsupportedProjection("grid_mapping_name")
                }
                if (gridMappingName != null && gridMappingName !in SUPPORTED_GRID_MAPPING_NAMES) {
                    throw NetCdfException.UnsupportedProjection(gridMappingName)
                }
                if (gridMappingName == null) {
                    throw NetCdfException.UnsupportedProjection("grid_mapping_name")
                }
                val epsgAttribute = mapping.findAttribute("epsg_code")
                val epsg = epsgAttribute?.let { parseEpsgAttribute(it) }
                val spatialRefAttribute = mapping.findAttribute("spatial_ref")
                val spatialRef = if (spatialRefAttribute == null) {
                    null
                } else {
                    val rawSpatialRef = spatialRefAttribute.stringValue?.trim()
                        ?: throw NetCdfException.UnsupportedProjection("spatial_ref")
                    if (rawSpatialRef.isEmpty()) {
                        throw NetCdfException.UnsupportedProjection("spatial_ref")
                    }
                    parseSpatialRefEpsg(rawSpatialRef)
                }
                if (epsg != null && spatialRef != null && epsg != spatialRef) {
                    throw NetCdfException.UnsupportedProjection("conflicting:$epsg:$spatialRef")
                }
                val resolved = epsg ?: spatialRef
                if (gridMappingName == "latitude_longitude") {
                    if (resolved != null && resolved != WGS84) {
                        throw NetCdfException.UnsupportedProjection("conflicting:latitude_longitude:$resolved")
                    }
                    return resolved ?: WGS84
                }
                if (resolved != null) return resolved
                throw NetCdfException.UnsupportedProjection(gridMappingName)
            }
            val latStandardName = axisMap.latAxis?.axis?.findAttributeString("standard_name", "")?.lowercase()
            val lonStandardName = axisMap.lonAxis?.axis?.findAttributeString("standard_name", "")?.lowercase()
            val latUnits = axisMap.latAxis?.axis?.unitsString?.lowercase().orEmpty()
            val lonUnits = axisMap.lonAxis?.axis?.unitsString?.lowercase().orEmpty()
            val geographicNames = setOf("latitude", "longitude", "grid_latitude", "grid_longitude")
            val geographicAxes =
                (latStandardName in geographicNames || latUnits.contains("degrees_north")) &&
                    (lonStandardName in geographicNames || lonUnits.contains("degrees_east"))
            return if (geographicAxes) WGS84 else throw NetCdfException.UnsupportedProjection("missing-grid-mapping")
        }

        private fun parseEpsgAttribute(attribute: ucar.nc2.Attribute): String {
            val raw = attribute.stringValue ?: attribute.numericValue?.toString()
                ?: throw NetCdfException.UnsupportedProjection(attribute.shortName)
            val digits = raw.removePrefix("EPSG:")
            if (digits.isEmpty() || digits.any { it !in '0'..'9' }) {
                throw NetCdfException.UnsupportedProjection(raw)
            }
            val code = try {
                digits.toLong()
            } catch (e: NumberFormatException) {
                throw NetCdfException.UnsupportedProjection(raw, e)
            }
            if (code !in 1L..Int.MAX_VALUE) throw NetCdfException.UnsupportedProjection(raw)
            return "EPSG:$code"
        }

        private fun parseSpatialRefEpsg(raw: String): String? {
            val directMatches = SPATIAL_REF_EPSG_PATTERN.findAll(raw).toList()
            val structuredMatches = (
                SPATIAL_REF_AUTHORITY_EPSG_PATTERN.findAll(raw).map { match ->
                    match to (match.groupValues[1].ifEmpty { match.groupValues[2] })
                } + SPATIAL_REF_ID_EPSG_PATTERN.findAll(raw).map { match ->
                    match to (match.groupValues[1].ifEmpty { match.groupValues[2] })
                }
                ).sortedBy { it.first.range.first }
                .toList()
            val markerCount = SPATIAL_REF_EPSG_MARKER_PATTERN.findAll(raw).count()
            if (markerCount != directMatches.size + structuredMatches.size) {
                throw NetCdfException.UnsupportedProjection("invalid-spatial-ref")
            }
            val directCodes = directMatches.map { it.groupValues[1].toLongOrNull() }
            val structuredCodes = structuredMatches.map { it.second.toLongOrNull() }
            val allCodes = directCodes + structuredCodes
            if (allCodes.isEmpty() || allCodes.any { it == null || it !in 1L..Int.MAX_VALUE }) {
                throw NetCdfException.UnsupportedProjection("invalid-spatial-ref")
            }
            val rootCode = structuredCodes.lastOrNull() ?: directCodes.singleOrNull()
            if (rootCode == null) throw NetCdfException.UnsupportedProjection("conflicting-spatial-ref")
            val directDistinct = directCodes.filterNotNull().distinct()
            if (directDistinct.size > 1 || (directDistinct.singleOrNull() != null &&
                    structuredCodes.lastOrNull() != null && directDistinct.single() != structuredCodes.last()
                )
            ) {
                throw NetCdfException.UnsupportedProjection("conflicting-spatial-ref")
            }
            return "EPSG:$rootCode"
        }

        /** variable의 축을 읽는 bounded reprojection 구현을 만듭니다. */
        fun from(
            variable: Variable,
            dataset: NetcdfDataset,
            axisMap: VariableAxisMap,
        ): CoordinateReprojector {
            val latBinding = axisMap.latAxis
                ?: throw NetCdfException.MissingCoordinate("lat")
            val lonBinding = axisMap.lonAxis
                ?: throw NetCdfException.MissingCoordinate("lon")
            val sourceCrs = detectSourceCrs(variable, dataset, axisMap)
            if (sourceCrs !in SUPPORTED_CRS) {
                throw NetCdfException.UnsupportedProjection(sourceCrs)
            }

            val axes = buildMap {
                put(latBinding.name, latBinding.axis)
                put(lonBinding.name, lonBinding.axis)
            }
            val reader = UcarCoordinateReader(axes, variable.fullName)
            val rawPoint: (Int, Int) -> Pair<Double, Double> = { row, column ->
                val x = readBinding(lonBinding, row, column, axisMap, reader)
                val y = readBinding(latBinding, row, column, axisMap, reader)
                if (!x.isFinite() || !y.isFinite()) {
                    throw NetCdfException.UnsupportedCoordinateAxis(variable.fullName, "lon/lat", "non-finite")
                }
                x to y
            }

            if (isGeographic(sourceCrs)) {
                return ReaderBacked(
                    sourceCrs = sourceCrs,
                    pointReader = { row, column ->
                        val (lon, lat) = rawPoint(row, column)
                        if (!validateGeographicCoordinate(lon, lat)) {
                            throw NetCdfException.UnsupportedProjection("$sourceCrs:${variable.fullName}:out-of-range")
                        }
                        lon to lat
                    },
                )
            }

            val transform = try {
                val source = crsFactory.createFromName(sourceCrs)
                val target = crsFactory.createFromName(WGS84)
                transformFactory.createTransform(source, target)
            } catch (e: Exception) {
                throw NetCdfException.UnsupportedProjection(sourceCrs, e)
            }
            return ReaderBacked(
                sourceCrs = sourceCrs,
                pointReader = { row, column ->
                    val (x, y) = rawPoint(row, column)
                    transformToWgs84(transform, x, y, sourceCrs, variable.fullName)
                },
                tileReader = { rowOrigin, columnOrigin, rowCount, columnCount ->
                    val xWindow = readBindingWindow(
                        lonBinding,
                        axisMap,
                        reader,
                        rowOrigin,
                        columnOrigin,
                        rowCount,
                        columnCount,
                    )
                    val yWindow = readBindingWindow(
                        latBinding,
                        axisMap,
                        reader,
                        rowOrigin,
                        columnOrigin,
                        rowCount,
                        columnCount,
                    )
                    val provider: (Int, Int) -> Pair<Double, Double> = { row, column ->
                        val localRow = row - rowOrigin
                        val localColumn = column - columnOrigin
                        val offset = localRow * columnCount + localColumn
                        transformToWgs84(
                            transform,
                            xWindow[offset],
                            yWindow[offset],
                            sourceCrs,
                            variable.fullName,
                        )
                    }
                    provider
                },
            )
        }

        private fun readBinding(
            binding: VariableAxisMap.AxisBinding,
            row: Int,
            column: Int,
            axisMap: VariableAxisMap,
            reader: CoordinateReader,
        ): Double {
            return if (binding.isTwoDimensional) {
                val gridRowDim = axisMap.gridRowDim
                    ?: throw NetCdfException.UnsupportedCoordinateAxis(binding.name, binding.name, "missing-row-dimension")
                val gridColumnDim = axisMap.gridColumnDim
                    ?: throw NetCdfException.UnsupportedCoordinateAxis(binding.name, binding.name, "missing-column-dimension")
                val rowAxis = binding.dimensionIndices.indexOf(gridRowDim)
                val columnAxis = binding.dimensionIndices.indexOf(gridColumnDim)
                if (rowAxis < 0 || columnAxis < 0) {
                    throw NetCdfException.UnsupportedCoordinateAxis(
                        binding.name,
                        binding.name,
                        "grid-dimension-order",
                    )
                }
                val values = if (rowAxis == 0 && columnAxis == 1) {
                    reader.read2D(binding.name, row, column, 1, 1)
                } else {
                    reader.read2D(binding.name, column, row, 1, 1)
                }
                values.firstOrNull()
                    ?: throw NetCdfException.UnsupportedCoordinateAxis(binding.name, binding.name, "empty-window")
            } else {
                val axisDim = binding.dimensionIndices.singleOrNull()
                    ?: throw NetCdfException.UnsupportedCoordinateAxis(binding.name, binding.name, "axis-dimension")
                val index = if (axisDim == axisMap.gridRowDim) row else column
                reader.read1D(binding.name, index, 1).firstOrNull()
                    ?: throw NetCdfException.UnsupportedCoordinateAxis(binding.name, binding.name, "empty-window")
            }
        }

        private fun readBindingWindow(
            binding: VariableAxisMap.AxisBinding,
            axisMap: VariableAxisMap,
            reader: CoordinateReader,
            rowOrigin: Int,
            columnOrigin: Int,
            rowCount: Int,
            columnCount: Int,
        ): DoubleArray {
            if (!binding.isTwoDimensional) {
                val axisDim = binding.dimensionIndices.singleOrNull()
                    ?: throw NetCdfException.UnsupportedCoordinateAxis(binding.name, binding.name, "axis-dimension")
                val source = if (axisDim == axisMap.gridRowDim) {
                    reader.read1D(binding.name, rowOrigin, rowCount)
                } else if (axisDim == axisMap.gridColumnDim) {
                    reader.read1D(binding.name, columnOrigin, columnCount)
                } else {
                    throw NetCdfException.UnsupportedCoordinateAxis(binding.name, binding.name, "grid-dimension")
                }
                return DoubleArray(rowCount * columnCount) { index ->
                    val row = index / columnCount
                    val column = index % columnCount
                    source[if (axisDim == axisMap.gridRowDim) row else column]
                }
            }
            val rowDim = axisMap.gridRowDim
                ?: throw NetCdfException.MissingCoordinate("lat")
            val columnDim = axisMap.gridColumnDim
                ?: throw NetCdfException.MissingCoordinate("lon")
            val axisRow = binding.dimensionIndices.indexOf(rowDim)
            val axisColumn = binding.dimensionIndices.indexOf(columnDim)
            if (axisRow < 0 || axisColumn < 0) {
                throw NetCdfException.UnsupportedCoordinateAxis(binding.name, binding.name, "grid-dimension-order")
            }
            if (axisRow == 0 && axisColumn == 1) {
                return reader.read2D(binding.name, rowOrigin, columnOrigin, rowCount, columnCount)
            }
            val source = reader.read2D(binding.name, columnOrigin, rowOrigin, columnCount, rowCount)
            return DoubleArray(rowCount * columnCount) { index ->
                val row = index / columnCount
                val column = index % columnCount
                source[column * rowCount + row]
            }
        }

        private fun transformToWgs84(
            transform: CoordinateTransform,
            x: Double,
            y: Double,
            sourceCrs: String,
            variableName: String,
        ): Pair<Double, Double> {
            val src = ProjCoordinate(x, y)
            val dst = ProjCoordinate()
            return try {
                transform.transform(src, dst)
                if (!validateGeographicCoordinate(dst.x, dst.y)) {
                    throw NetCdfException.UnsupportedProjection(sourceCrs)
                }
                dst.x to dst.y
            } catch (e: NetCdfException) {
                throw e
            } catch (e: Exception) {
                throw NetCdfException.UnsupportedProjection("$sourceCrs:$variableName", e)
            }
        }
    }
}
