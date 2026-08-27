package io.bluetape4k.science.exposed.service.support

import ucar.ma2.Array
import ucar.ma2.DataType
import ucar.nc2.Attribute
import ucar.nc2.write.NetcdfFormatWriter
import java.nio.file.Path

/**
 * 테스트용 NetCDF 파일을 동적 생성하는 헬퍼.
 *
 * `ucar.nc2.write.NetcdfFormatWriter` 기반. NetCDF-3 (CDM-1) 포맷.
 *
 * 격자 기본값: timeN=2, levelN=2, latN=3, lonN=4, lat=[0.0, 45.5, 89.9], lon=[-180.0, -90.0, 0.0, 90.0].
 *
 * @param path                  출력 파일 경로 (`@TempDir` 권장)
 * @param rank                  변수 rank (1/2/3/4)
 * @param withLatAxis           lat 축 생성 여부 (false 면 `MissingCoordinate` 테스트용)
 * @param withLevelAxisByName   true 면 AxisType 없이 이름만 `lev` 로 생성 — fallback 검증
 * @param withFillValue         `_FillValue` 속성 추가 + 일부 셀에 fill value 주입
 * @param sourceCrs             grid_mapping 속성 EPSG (`"EPSG:4326"`/`"EPSG:3857"`/`"EPSG:32633"` 등)
 * @param nonStandardDimOrder   true 면 (lat, lon, time) 같은 비표준 dim order 로 생성 — Codex C5 검증
 * @return 생성된 파일 경로
 */
internal object NetCdfSampleWriter {

    /** DB read-back helper value used by the curvilinear tests. */
    data class SpatialTuple(
        val timeIdx: Int,
        val levelIdx: Int,
        val row: Int,
        val column: Int,
        val longitude: Double,
        val latitude: Double,
        val value: Double,
    )

    const val DEFAULT_TIME_N: Int = 2
    const val DEFAULT_LEVEL_N: Int = 2
    const val DEFAULT_LAT_N: Int = 3
    const val DEFAULT_LON_N: Int = 4

    val DEFAULT_LAT_VALUES: DoubleArray = doubleArrayOf(0.0, 45.5, 89.9)
    val DEFAULT_LON_VALUES: DoubleArray = doubleArrayOf(-180.0, -90.0, 0.0, 90.0)

    const val FILL_VALUE: Double = -9999.0

    const val CURVILINEAR_ROWS: Int = 3
    const val CURVILINEAR_COLUMNS: Int = 4

    val CURVILINEAR_VALUES: DoubleArray = doubleArrayOf(
        0.0, 1.0, 2.0, 3.0,
        4.0, 5.0, 6.0, 7.0,
        8.0, 9.0, 10.0, 11.0,
    )

    private val CURVILINEAR_LONGITUDES: DoubleArray = doubleArrayOf(
        120.0, 121.0, 122.0, 123.0,
        120.5, 121.5, 122.5, 123.5,
        121.0, 122.0, 123.0, 124.0,
    )

    private val CURVILINEAR_LATITUDES: DoubleArray = doubleArrayOf(
        10.0, 10.25, 10.5, 10.75,
        15.0, 15.25, 15.5, 15.75,
        20.0, 20.25, 20.5, 20.75,
    )

    val CURVILINEAR_TUPLES: List<SpatialTuple> = buildList {
        repeat(CURVILINEAR_ROWS) { row ->
            repeat(CURVILINEAR_COLUMNS) { column ->
                val offset = row * CURVILINEAR_COLUMNS + column
                add(
                    SpatialTuple(
                        timeIdx = 0,
                        levelIdx = 0,
                        row = row,
                        column = column,
                        longitude = CURVILINEAR_LONGITUDES[offset],
                        latitude = CURVILINEAR_LATITUDES[offset],
                        value = CURVILINEAR_VALUES[offset],
                    ),
                )
            }
        }
    }

    fun writeSample(
        path: Path,
        rank: Int,
        withLatAxis: Boolean = true,
        withLevelAxisByName: Boolean = false,
        withFillValue: Boolean = false,
        sourceCrs: String = "EPSG:4326",
        nonStandardDimOrder: Boolean = false,
        withCfConventions: Boolean = false,
    ): Path {
        require(rank in 1..4) { "rank must be 1..4: $rank" }

        val builder = NetcdfFormatWriter.createNewNetcdf3(path.toAbsolutePath().toString())

        if (withCfConventions) {
            builder.addAttribute(Attribute("Conventions", "CF-1.0"))
            builder.addAttribute(Attribute("title", "Synthetic CF-1.x sample"))
            builder.addAttribute(Attribute("institution", "bluetape4k test"))
        }

        // dimensions
        val timeDim = builder.addDimension("time", DEFAULT_TIME_N)
        val levelDim = if (rank == 4) {
            builder.addDimension(
                if (withLevelAxisByName) "lev" else "level",
                DEFAULT_LEVEL_N,
            )
        } else null
        val latDim = if (withLatAxis) builder.addDimension("lat", DEFAULT_LAT_N) else null
        val lonDim = builder.addDimension("lon", DEFAULT_LON_N)

        // coordinate variables
        builder.addVariable("time", DataType.DOUBLE, listOf(timeDim))
            .addAttribute(Attribute("units", "hours since 2024-01-01"))
            .addAttribute(Attribute("axis", "T"))

        if (rank == 4 && levelDim != null) {
            val levelVar = builder.addVariable(levelDim.shortName, DataType.DOUBLE, listOf(levelDim))
                .addAttribute(Attribute("units", "Pa"))
            if (!withLevelAxisByName) {
                levelVar.addAttribute(Attribute("axis", "Z"))
                levelVar.addAttribute(Attribute("positive", "down"))
            }
        }

        if (latDim != null) {
            builder.addVariable("lat", DataType.DOUBLE, listOf(latDim))
                .addAttribute(Attribute("units", "degrees_north"))
                .addAttribute(Attribute("axis", "Y"))
        }

        builder.addVariable("lon", DataType.DOUBLE, listOf(lonDim))
            .addAttribute(Attribute("units", "degrees_east"))
            .addAttribute(Attribute("axis", "X"))

        // grid_mapping for non-WGS84
        if (sourceCrs != "EPSG:4326") {
            val epsgCode = sourceCrs.removePrefix("EPSG:").toInt()
            builder.addVariable("crs", DataType.INT, emptyList())
                .addAttribute(Attribute("grid_mapping_name", "transverse_mercator"))
                .addAttribute(Attribute("epsg_code", epsgCode))
        }

        // data variable — temperature
        val dataDims = when (rank) {
            1 -> listOf(timeDim)
            2 -> if (latDim != null) listOf(latDim, lonDim) else listOf(lonDim, lonDim)
            3 -> if (nonStandardDimOrder && latDim != null) listOf(latDim, lonDim, timeDim)
            else if (latDim != null) listOf(timeDim, latDim, lonDim)
            else listOf(timeDim, lonDim)
            4 -> if (nonStandardDimOrder && latDim != null && levelDim != null)
                listOf(latDim, lonDim, timeDim, levelDim)
            else if (latDim != null && levelDim != null)
                listOf(timeDim, levelDim, latDim, lonDim)
            else listOf(timeDim, lonDim)
            else -> error("unreachable")
        }
        val dataVar = builder.addVariable("temperature", DataType.DOUBLE, dataDims)
            .addAttribute(Attribute("units", "K"))
            .addAttribute(Attribute("long_name", "Air Temperature"))

        if (withFillValue) {
            dataVar.addAttribute(Attribute("_FillValue", FILL_VALUE))
        }
        if (sourceCrs != "EPSG:4326") {
            dataVar.addAttribute(Attribute("grid_mapping", "crs"))
        }

        // build + write
        builder.build().use { writer ->
            // coord values
            writer.write(writer.findVariable("time"), Array.factory(DataType.DOUBLE, intArrayOf(DEFAULT_TIME_N), DoubleArray(DEFAULT_TIME_N) { it.toDouble() }))
            if (rank == 4 && levelDim != null) {
                writer.write(
                    writer.findVariable(levelDim.shortName),
                    Array.factory(DataType.DOUBLE, intArrayOf(DEFAULT_LEVEL_N), DoubleArray(DEFAULT_LEVEL_N) { (it + 1) * 100.0 }),
                )
            }
            if (latDim != null) {
                writer.write(writer.findVariable("lat"), Array.factory(DataType.DOUBLE, intArrayOf(DEFAULT_LAT_N), DEFAULT_LAT_VALUES))
            }
            writer.write(
                writer.findVariable("lon"),
                Array.factory(DataType.DOUBLE, intArrayOf(DEFAULT_LON_N), DEFAULT_LON_VALUES),
            )

            // temperature data
            val v = writer.findVariable("temperature")
            val totalSize = dataDims.fold(1) { acc, d -> acc * d.length }
            val arr = DoubleArray(totalSize) { i ->
                if (withFillValue && i % 5 == 0) FILL_VALUE else 273.15 + i.toDouble()
            }
            val shape = dataDims.map { it.length }.toIntArray()
            writer.write(v, Array.factory(DataType.DOUBLE, shape, arr))
        }
        return path
    }

    /**
     * 2D curvilinear latitude/longitude fixture.
     *
     * The two-dimensional axes use `(y, x)` order while the data variable can
     * deliberately use `(time, x, y)` to expose transposition bugs.
     */
    fun writeCurvilinearSample(
        path: Path,
        dataOrder: List<String> = listOf("time", "y", "x"),
    ): Path {
        require(dataOrder == listOf("time", "y", "x") || dataOrder == listOf("time", "x", "y")) {
            "dataOrder must be [time, y, x] or [time, x, y]: $dataOrder"
        }

        return writeCurvilinearGrid(
            path = path,
            rows = CURVILINEAR_ROWS,
            columns = CURVILINEAR_COLUMNS,
            dataOrder = dataOrder,
            longitudes = CURVILINEAR_LONGITUDES,
            latitudes = CURVILINEAR_LATITUDES,
            valueAt = { row, column -> CURVILINEAR_VALUES[row * CURVILINEAR_COLUMNS + column] },
        )
    }

    /** CF `coordinates="time lat lon altitude"` fixture with one numeric auxiliary. */
    fun writeCfAuxiliarySample(path: Path): Path {
        val builder = NetcdfFormatWriter.createNewNetcdf3(path.toAbsolutePath().toString())
        builder.addAttribute(Attribute("Conventions", "CF-1.8"))

        val timeDim = builder.addDimension("time", DEFAULT_TIME_N)
        val yDim = builder.addDimension("y", 2)
        val xDim = builder.addDimension("x", 2)

        builder.addVariable("time", DataType.DOUBLE, listOf(timeDim))
            .addAttribute(Attribute("axis", "T"))
            .addAttribute(Attribute("units", "hours since 2024-01-01"))
        builder.addVariable("lat", DataType.DOUBLE, listOf(yDim))
            .addAttribute(Attribute("axis", "Y"))
            .addAttribute(Attribute("standard_name", "latitude"))
            .addAttribute(Attribute("units", "degrees_north"))
        builder.addVariable("lon", DataType.DOUBLE, listOf(xDim))
            .addAttribute(Attribute("axis", "X"))
            .addAttribute(Attribute("standard_name", "longitude"))
            .addAttribute(Attribute("units", "degrees_east"))
        builder.addVariable("altitude", DataType.DOUBLE, listOf(yDim, xDim))
            .addAttribute(Attribute("standard_name", "surface_altitude"))
            .addAttribute(Attribute("units", "m"))
        builder.addVariable("temperature", DataType.DOUBLE, listOf(timeDim, yDim, xDim))
            .addAttribute(Attribute("units", "K"))
            .addAttribute(Attribute("coordinates", "time lat lon altitude"))

        builder.build().use { writer ->
            writer.write(
                writer.findVariable("time"),
                Array.factory(DataType.DOUBLE, intArrayOf(DEFAULT_TIME_N), doubleArrayOf(0.0, 1.0)),
            )
            writer.write(
                writer.findVariable("lat"),
                Array.factory(DataType.DOUBLE, intArrayOf(2), doubleArrayOf(30.0, 31.0)),
            )
            writer.write(
                writer.findVariable("lon"),
                Array.factory(DataType.DOUBLE, intArrayOf(2), doubleArrayOf(120.0, 121.0)),
            )
            writer.write(
                writer.findVariable("altitude"),
                Array.factory(DataType.DOUBLE, intArrayOf(2, 2), doubleArrayOf(100.0, 101.0, 102.0, 103.0)),
            )
            val values = DoubleArray(DEFAULT_TIME_N * 2 * 2) { flat ->
                val time = flat / 4
                val cell = flat % 4
                273.15 + time * 10.0 + cell
            }
            writer.write(
                writer.findVariable("temperature"),
                Array.factory(DataType.DOUBLE, intArrayOf(DEFAULT_TIME_N, 2, 2), values),
            )
        }
        return path
    }

    /** 2D projected x/y fixture whose EPSG token remains an unparsed string. */
    fun writeProjected2DSample(path: Path, sourceCrs: String): Path {
        val builder = NetcdfFormatWriter.createNewNetcdf3(path.toAbsolutePath().toString())
        val timeDim = builder.addDimension("time", 1)
        val yDim = builder.addDimension("y", 2)
        val xDim = builder.addDimension("x", 2)

        builder.addVariable("time", DataType.DOUBLE, listOf(timeDim))
            .addAttribute(Attribute("axis", "T"))
            .addAttribute(Attribute("units", "hours since 2024-01-01"))
        builder.addVariable("x", DataType.DOUBLE, listOf(yDim, xDim))
            .addAttribute(Attribute("axis", "X"))
            .addAttribute(Attribute("standard_name", "projection_x_coordinate"))
            .addAttribute(Attribute("units", "m"))
        builder.addVariable("y", DataType.DOUBLE, listOf(yDim, xDim))
            .addAttribute(Attribute("axis", "Y"))
            .addAttribute(Attribute("standard_name", "projection_y_coordinate"))
            .addAttribute(Attribute("units", "m"))
        builder.addVariable("crs", DataType.INT, emptyList())
            .addAttribute(Attribute("grid_mapping_name", "transverse_mercator"))
            .addAttribute(Attribute("epsg_code", sourceCrs.removePrefix("EPSG:")))
        builder.addVariable("temperature", DataType.DOUBLE, listOf(timeDim, yDim, xDim))
            .addAttribute(Attribute("units", "K"))
            .addAttribute(Attribute("grid_mapping", "crs"))

        builder.build().use { writer ->
            writer.write(
                writer.findVariable("time"),
                Array.factory(DataType.DOUBLE, intArrayOf(1), doubleArrayOf(0.0)),
            )
            writer.write(
                writer.findVariable("x"),
                Array.factory(DataType.DOUBLE, intArrayOf(2, 2), doubleArrayOf(0.0, 1_000.0, 0.0, 1_000.0)),
            )
            writer.write(
                writer.findVariable("y"),
                Array.factory(DataType.DOUBLE, intArrayOf(2, 2), doubleArrayOf(0.0, 0.0, 1_000.0, 1_000.0)),
            )
            writer.write(
                writer.findVariable("temperature"),
                Array.factory(DataType.DOUBLE, intArrayOf(1, 2, 2), doubleArrayOf(273.15, 274.15, 275.15, 276.15)),
            )
        }
        return path
    }

    /**
     * Curvilinear fixture with an optional duplicate across the planner's row
     * tile boundary (`257 x 257`, rows 0 and 255).
     */
    fun writeDuplicateCoordinateSample(path: Path, duplicateAcrossTiles: Boolean): Path {
        val rows = if (duplicateAcrossTiles) 257 else CURVILINEAR_ROWS
        val columns = if (duplicateAcrossTiles) 257 else CURVILINEAR_COLUMNS
        val size = rows * columns
        val longitudes = DoubleArray(size) { index ->
            val row = index / columns
            val column = index % columns
            if (duplicateAcrossTiles && row == 255 && column == 0) 100.0 else 100.0 + row * 0.1 + column * 0.001
        }
        val latitudes = DoubleArray(size) { index ->
            val row = index / columns
            val column = index % columns
            if (duplicateAcrossTiles && row == 255 && column == 0) 10.0 else 10.0 + row * 0.01 + column * 0.0001
        }
        return writeCurvilinearGrid(
            path = path,
            rows = rows,
            columns = columns,
            dataOrder = listOf("time", "y", "x"),
            longitudes = longitudes,
            latitudes = latitudes,
            valueAt = { row, column -> 200.0 + row * columns + column },
        )
    }

    private fun writeCurvilinearGrid(
        path: Path,
        rows: Int,
        columns: Int,
        dataOrder: List<String>,
        longitudes: DoubleArray,
        latitudes: DoubleArray,
        valueAt: (row: Int, column: Int) -> Double,
    ): Path {
        require(longitudes.size == rows * columns) { "longitude size must match grid" }
        require(latitudes.size == rows * columns) { "latitude size must match grid" }

        val builder = NetcdfFormatWriter.createNewNetcdf3(path.toAbsolutePath().toString())
        val timeDim = builder.addDimension("time", 1)
        val yDim = builder.addDimension("y", rows)
        val xDim = builder.addDimension("x", columns)
        val dimensions = mapOf("time" to timeDim, "y" to yDim, "x" to xDim)

        builder.addVariable("time", DataType.DOUBLE, listOf(timeDim))
            .addAttribute(Attribute("axis", "T"))
            .addAttribute(Attribute("units", "hours since 2024-01-01"))
        builder.addVariable("lat", DataType.DOUBLE, listOf(yDim, xDim))
            .addAttribute(Attribute("axis", "Y"))
            .addAttribute(Attribute("standard_name", "latitude"))
            .addAttribute(Attribute("units", "degrees_north"))
        builder.addVariable("lon", DataType.DOUBLE, listOf(yDim, xDim))
            .addAttribute(Attribute("axis", "X"))
            .addAttribute(Attribute("standard_name", "longitude"))
            .addAttribute(Attribute("units", "degrees_east"))
        val dataDims = dataOrder.map { dimensions.getValue(it) }
        builder.addVariable("temperature", DataType.DOUBLE, dataDims)
            .addAttribute(Attribute("units", "K"))

        builder.build().use { writer ->
            writer.write(
                writer.findVariable("time"),
                Array.factory(DataType.DOUBLE, intArrayOf(1), doubleArrayOf(0.0)),
            )
            writer.write(
                writer.findVariable("lat"),
                Array.factory(DataType.DOUBLE, intArrayOf(rows, columns), latitudes),
            )
            writer.write(
                writer.findVariable("lon"),
                Array.factory(DataType.DOUBLE, intArrayOf(rows, columns), longitudes),
            )
            val shape = dataDims.map { it.length }.toIntArray()
            val timeIndex = dataOrder.indexOf("time")
            val rowIndex = dataOrder.indexOf("y")
            val columnIndex = dataOrder.indexOf("x")
            val data = DoubleArray(shape.fold(1) { acc, length -> acc * length }) { flatIndex ->
                val indices = indicesForFlatIndex(flatIndex, shape)
                valueAt(indices[rowIndex], indices[columnIndex] )
            }
            check(timeIndex >= 0) { "temperature must include time dimension" }
            writer.write(
                writer.findVariable("temperature"),
                Array.factory(DataType.DOUBLE, shape, data),
            )
        }
        return path
    }

    private fun indicesForFlatIndex(flatIndex: Int, shape: IntArray): IntArray {
        var remaining = flatIndex
        return IntArray(shape.size) { index ->
            val stride = shape.drop(index + 1).fold(1) { acc, length -> acc * length }
            val coordinate = remaining / stride
            remaining %= stride
            coordinate
        }
    }
}
