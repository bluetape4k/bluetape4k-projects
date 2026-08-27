package io.bluetape4k.science.exposed.service.internal

import io.bluetape4k.science.exposed.NetCdfException
import ucar.ma2.Array as UcarArray
import ucar.nc2.Variable
import java.util.concurrent.CancellationException

/** NetCDF variable를 bounded section으로 읽는 추상화입니다. */
internal fun interface VariableReader {
    fun read(origin: IntArray, shape: IntArray): UcarArray
}

/** 1D/2D 좌표축의 tile window만 읽는 추상화입니다. */
internal interface CoordinateReader {
    fun read1D(axisName: String, origin: Int, length: Int): DoubleArray

    fun read2D(
        axisName: String,
        rowOrigin: Int,
        columnOrigin: Int,
        rowCount: Int,
        columnCount: Int,
    ): DoubleArray
}

/** 한 셀을 즉시 소비하기 위한 좌표 샘플러입니다. */
internal fun interface CoordinateSampler {
    fun sample(globalRow: Int, globalColumn: Int, target: MutableCoordinateSample)
}

/** sampler 호출자가 소유하는 재사용 가능한 mutable cell buffer입니다. */
internal class MutableCoordinateSample {
    var longitude: Double = 0.0
    var latitude: Double = 0.0
    val auxiliary: MutableMap<String, Double> = LinkedHashMap()

    fun clear() {
        longitude = 0.0
        latitude = 0.0
        auxiliary.clear()
    }

    fun readOnlyCopy(): CoordinateSample = CoordinateSample(longitude, latitude, auxiliary.toMap())
}

/** tile 밖으로 mutable 상태가 누출되지 않는 immutable cell snapshot입니다. */
internal data class CoordinateSample(
    val longitude: Double,
    val latitude: Double,
    val auxiliary: Map<String, Double>,
)

/** 좌표의 finite/bounds 계약을 검사합니다. */
internal fun validateGeographicCoordinate(longitude: Double, latitude: Double): Boolean =
    longitude.isFinite() && latitude.isFinite() && longitude in -180.0..180.0 && latitude in -90.0..90.0

/** UCAR [CoordinateAxis]를 요청된 window만큼 읽는 구현입니다. */
internal class UcarCoordinateReader(
    private val axes: Map<String, Variable>,
    private val variableName: String,
): CoordinateReader {

    override fun read1D(axisName: String, origin: Int, length: Int): DoubleArray {
        val axis = axis(axisName)
        if (axis.rank != 1 || origin < 0 || length < 0 ||
            origin.toLong() + length.toLong() > axis.shape[0].toLong()
        ) {
            throw NetCdfException.UnsupportedCoordinateAxis(variableName, axisName, "invalid-1d-window")
        }
        return try {
            val values = axis.read(intArrayOf(origin), intArrayOf(length))
            values.toDoubleArray()
        } catch (e: CancellationException) {
            throw e
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw e
        } catch (e: Exception) {
            throw NetCdfException.UnsupportedCoordinateAxis(variableName, axisName, "read-1d")
        }
    }

    override fun read2D(
        axisName: String,
        rowOrigin: Int,
        columnOrigin: Int,
        rowCount: Int,
        columnCount: Int,
    ): DoubleArray {
        val axis = axis(axisName)
        val shape = axis.shape
        if (axis.rank != 2 || rowOrigin < 0 || columnOrigin < 0 || rowCount < 0 || columnCount < 0 ||
            rowOrigin.toLong() + rowCount.toLong() > shape[0].toLong() ||
            columnOrigin.toLong() + columnCount.toLong() > shape[1].toLong()
        ) {
            throw NetCdfException.UnsupportedCoordinateAxis(variableName, axisName, "invalid-2d-window")
        }
        return try {
            val values = axis.read(
                intArrayOf(rowOrigin, columnOrigin),
                intArrayOf(rowCount, columnCount),
            )
            values.toDoubleArray()
        } catch (e: CancellationException) {
            throw e
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw e
        } catch (e: Exception) {
            throw NetCdfException.UnsupportedCoordinateAxis(variableName, axisName, "read-2d")
        }
    }

    private fun axis(name: String): Variable = axes[name]
        ?: throw NetCdfException.UnsupportedCoordinateAxis(variableName, name, "axis-not-found")
}

/** axis map에 맞춰 lon/lat과 numeric CF auxiliary를 한 셀씩 채웁니다. */
internal class NetCdfCoordinateSampler(
    private val map: VariableAxisMap,
    private val reader: CoordinateReader,
    private val pointProvider: ((Int, Int) -> Pair<Double, Double>)? = null,
) : CoordinateSampler {

    override fun sample(globalRow: Int, globalColumn: Int, target: MutableCoordinateSample) {
        target.clear()
        val (longitude, latitude) = pointProvider?.invoke(globalRow, globalColumn)
            ?: rawPoint(globalRow, globalColumn)
        if (!validateGeographicCoordinate(longitude, latitude)) {
            throw NetCdfException.UnsupportedCoordinateAxis(
                variableName = "coordinate",
                coordinateName = "lon/lat",
                reason = "non-finite-or-out-of-range",
            )
        }
        target.longitude = longitude
        target.latitude = latitude
        map.auxiliaryAxes.forEach { auxiliary ->
            val value = readBinding(auxiliary, globalRow, globalColumn)
            if (value.isFinite()) {
                target.auxiliary[auxiliary.name] = value
            }
        }
    }

    private fun rawPoint(globalRow: Int, globalColumn: Int): Pair<Double, Double> {
        val latBinding = map.latAxis
            ?: throw NetCdfException.MissingCoordinate("lat")
        val lonBinding = map.lonAxis
            ?: throw NetCdfException.MissingCoordinate("lon")
        val longitude = readBinding(lonBinding, globalRow, globalColumn)
        val latitude = readBinding(latBinding, globalRow, globalColumn)
        return longitude to latitude
    }

    private fun readBinding(binding: VariableAxisMap.AxisBinding, row: Int, column: Int): Double {
        return if (binding.isTwoDimensional) {
            val rowDim = map.gridRowDim
                ?: throw NetCdfException.MissingCoordinate("lat")
            val columnDim = map.gridColumnDim
                ?: throw NetCdfException.MissingCoordinate("lon")
            val axisRow = binding.dimensionIndices.indexOf(rowDim)
            val axisColumn = binding.dimensionIndices.indexOf(columnDim)
            if (axisRow < 0 || axisColumn < 0) {
                throw NetCdfException.UnsupportedCoordinateAxis(binding.name, binding.name, "grid-dimension-order")
            }
            val window = if (axisRow == 0 && axisColumn == 1) {
                reader.read2D(binding.name, row, column, 1, 1)
            } else {
                reader.read2D(binding.name, column, row, 1, 1)
            }
            window.firstOrNull()
                ?: throw NetCdfException.UnsupportedCoordinateAxis(binding.name, binding.name, "empty-window")
        } else {
            val axisDim = binding.dimensionIndices.singleOrNull()
                ?: throw NetCdfException.UnsupportedCoordinateAxis(binding.name, binding.name, "axis-dimension")
            val index = if (axisDim == map.gridRowDim) row else column
            reader.read1D(binding.name, index, 1).firstOrNull()
                ?: throw NetCdfException.UnsupportedCoordinateAxis(binding.name, binding.name, "empty-window")
        }
    }
}

/** 현재 tile의 좌표 window만 보유하는 sampler입니다. */
internal class NetCdfTileCoordinateSampler(
    private val map: VariableAxisMap,
    private val reader: CoordinateReader,
    private val rowOrigin: Int,
    private val columnOrigin: Int,
    private val rowCount: Int,
    private val columnCount: Int,
    private val pointProvider: ((Int, Int) -> Pair<Double, Double>)? = null,
) : CoordinateSampler {

    private val longitudeWindow = if (pointProvider == null) map.lonAxis?.let(::readWindow) else null
    private val latitudeWindow = if (pointProvider == null) map.latAxis?.let(::readWindow) else null
    private val auxiliaryValues = map.auxiliaryAxes.associate { it.name to readWindow(it) }

    override fun sample(globalRow: Int, globalColumn: Int, target: MutableCoordinateSample) {
        target.clear()
        val (longitude, latitude) = pointProvider?.invoke(globalRow, globalColumn)
            ?: rawPoint(globalRow, globalColumn)
        if (!validateGeographicCoordinate(longitude, latitude)) {
            throw NetCdfException.UnsupportedCoordinateAxis(
                variableName = "coordinate",
                coordinateName = "lon/lat",
                reason = "non-finite-or-out-of-range",
            )
        }
        target.longitude = longitude
        target.latitude = latitude
        map.auxiliaryAxes.forEach { auxiliary ->
            val value = valueAt(auxiliaryValues.getValue(auxiliary.name), globalRow, globalColumn)
            if (value.isFinite()) target.auxiliary[auxiliary.name] = value
        }
    }

    private fun rawPoint(globalRow: Int, globalColumn: Int): Pair<Double, Double> {
        if (map.latAxis == null) throw NetCdfException.MissingCoordinate("lat")
        if (map.lonAxis == null) throw NetCdfException.MissingCoordinate("lon")
        val lat = latitudeWindow ?: throw NetCdfException.MissingCoordinate("lat")
        val lon = longitudeWindow ?: throw NetCdfException.MissingCoordinate("lon")
        return valueAt(lon, globalRow, globalColumn) to
            valueAt(lat, globalRow, globalColumn)
    }

    private fun readWindow(binding: VariableAxisMap.AxisBinding): CoordinateWindow {
        if (!binding.isTwoDimensional) {
            val axisDim = binding.dimensionIndices.singleOrNull()
                ?: throw NetCdfException.UnsupportedCoordinateAxis(binding.name, binding.name, "axis-dimension")
            return if (axisDim == map.gridRowDim) {
                CoordinateWindow(reader.read1D(binding.name, rowOrigin, rowCount), true)
            } else if (axisDim == map.gridColumnDim) {
                CoordinateWindow(reader.read1D(binding.name, columnOrigin, columnCount), false)
            } else {
                throw NetCdfException.UnsupportedCoordinateAxis(binding.name, binding.name, "grid-dimension")
            }
        }
        val rowDim = map.gridRowDim
            ?: throw NetCdfException.MissingCoordinate("lat")
        val columnDim = map.gridColumnDim
            ?: throw NetCdfException.MissingCoordinate("lon")
        val axisRow = binding.dimensionIndices.indexOf(rowDim)
        val axisColumn = binding.dimensionIndices.indexOf(columnDim)
        if (axisRow < 0 || axisColumn < 0) {
            throw NetCdfException.UnsupportedCoordinateAxis(binding.name, binding.name, "grid-dimension-order")
        }
        return if (axisRow == 0 && axisColumn == 1) {
            CoordinateWindow(
                reader.read2D(binding.name, rowOrigin, columnOrigin, rowCount, columnCount),
                null,
            )
        } else {
            val source = reader.read2D(binding.name, columnOrigin, rowOrigin, columnCount, rowCount)
            CoordinateWindow(DoubleArray(rowCount * columnCount) { index ->
                val row = index / columnCount
                val column = index % columnCount
                source[column * rowCount + row]
            }, null)
        }
    }

    private fun valueAt(
        window: CoordinateWindow,
        globalRow: Int,
        globalColumn: Int,
    ): Double {
        val localRow = globalRow - rowOrigin
        val localColumn = globalColumn - columnOrigin
        if (localRow !in 0 until rowCount || localColumn !in 0 until columnCount) {
            throw NetCdfException.UnsupportedCoordinateAxis("coordinate", null, "tile-index")
        }
        return when (window.rowAxis) {
            true -> window.values[localRow]
            false -> window.values[localColumn]
            null -> window.values[localRow * columnCount + localColumn]
        }
    }

    private data class CoordinateWindow(val values: DoubleArray, val rowAxis: Boolean?)
}

private fun UcarArray.toDoubleArray(): DoubleArray {
    val values = DoubleArray(size.toInt())
    val iterator = indexIterator
    var index = 0
    while (iterator.hasNext()) {
        values[index++] = iterator.getDoubleNext()
    }
    return values
}
